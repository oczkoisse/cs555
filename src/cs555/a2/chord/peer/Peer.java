package cs555.a2.chord.peer;

import cs555.a2.chord.peer.messages.*;
import cs555.a2.chord.peer.messages.DataItem;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.HashMap;
import java.util.concurrent.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Peer implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(Peer.class.getName());

    private final ScheduledExecutorService updater = Executors.newSingleThreadScheduledExecutor();

    private PeerInfo ownInfo;
    private PeerInfo predecessor;
    private FingerTable fingerTable;
    private Messenger messenger;
    private Boolean joined;
    private HashMap<ID, DataItem> storedFiles;

    public Peer(PeerInfo info, int hearbeatInterval)
    {
        this.ownInfo = info;
        this.fingerTable = new FingerTable(info.getID());
        this.predecessor = PeerInfo.NULL_PEER;
        this.messenger = new Messenger(info.getListeningAddress().getPort(), 4);
        this.joined = false;
        this.storedFiles = new HashMap<>();
        this.updater.scheduleWithFixedDelay(this::hearbeat, 0, hearbeatInterval, TimeUnit.MILLISECONDS);
    }

    /*
    Core functionality of Chord
     */
    private void hearbeat()
    {
        synchronized (joined)
        {
            if (joined)
            {
                synchronized (fingerTable)
                {
                    PeerInfo succ = fingerTable.getSuccessor();
                    if (succ != PeerInfo.NULL_PEER) {
                        // if you are your own successor
                        if (succ.getID().compareTo(ownInfo.getID()) ==  0) {
                            synchronized (predecessor)
                            {
                                PeerInfo pred = getPredecessor();
                                if (pred.getID().compareTo(ownInfo.getID()) != 0)
                                    fingerTable.setSuccessor(pred);
                            }
                        } else {
                            send(new PredecessorRequest(ownInfo), succ.getListeningAddress());
                            send(new PredecessorUpdate(ownInfo), succ.getListeningAddress());
                        }
                    }

                    // Fix fingers randomly
                    int idx = ThreadLocalRandom.current().nextInt(1, fingerTable.size());
                    lookup(fingerTable.getFinger(idx), LookupCause.FINGER_UPDATE);
                }
            }
        }
    }

    private void printState()
    {
        LOGGER.log(Level.INFO, String.format("Predecessor: %1$s%n", getPredecessor().toString()) + fingerTable.toString());
        printHeldDataItems();
    }

    private void printHeldDataItems()
    {
        synchronized(storedFiles) {
            if (storedFiles.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("%n"));

                for (DataItem d : storedFiles.values()) {
                    stringBuilder.append(String.format("%1$s%n", d));
                }
                LOGGER.log(Level.INFO, "Currently held data items: " + stringBuilder.toString());
            }
        }
    }

    private void lookup(LookupRequest msg)
    {
        synchronized(fingerTable) {
            PeerInfo pred = getClosestNodePrecedingId(msg.getID());
            // Query dies if not enough info is there
            if (pred == PeerInfo.NULL_PEER)
                return;

            if (pred.getID().compareTo(ownInfo.getID()) == 0)
                send(new LookupResult(fingerTable.getSuccessor(), msg), msg.getSource().getListeningAddress());
            else
                send(msg, pred.getListeningAddress());
        }
    }

    // Returning NULL_PEER means any query that depends on its outcome will die
    private PeerInfo getClosestNodePrecedingId(ID id)
    {
        if (id == null)
            return PeerInfo.NULL_PEER;

        synchronized(fingerTable)
        {
            // Start at the bottom of the finger table
            // Continue up until we get to a finger whose successor is in interval (ownID, id)
            // Basically this means it is trying to find a node that is closer to 'id' than it is
            // Starting from the bottom means it is trying to find the closest one among such nodes
            PeerInfo succ = PeerInfo.NULL_PEER;
            for(int k = fingerTable.size() - 1; k >= 0; k--)
            {
                PeerInfo succOfFinger = fingerTable.getPeerInfo(k);
                if (succOfFinger != PeerInfo.NULL_PEER && succOfFinger.getID().inInterval(ownInfo.getID(), id))
                {
                    return succOfFinger;
                }
            }

            // Could be that we went through the entire finger table without finding any suitable entry
            // Maybe because all of them are NULL_PEER, or because none of the entry matched
            if (fingerTable.getSuccessor() != PeerInfo.NULL_PEER)
                succ = ownInfo;

            return succ;
        }
    }

    private PeerInfo getPredecessor()
    {
        synchronized (predecessor)
        {
            return predecessor;
        }
    }

    private void setPredecessor(PeerInfo newPredecessor)
    {
        synchronized (predecessor)
        {
            predecessor = newPredecessor;
        }
    }

    private void handleLookupResultMsg(LookupResult msg)
    {
        if (msg.getCause() == LookupCause.NEW_NODE)
        {
            synchronized (joined) {
                synchronized (fingerTable)
                {
                    if (!joined) {
                        fingerTable.setSuccessor(msg.getSuccessor());
                        joined = true;
                        LOGGER.log(Level.INFO, "Joined the Chord network");
                        printState();
                    }
                }
            }
        }
        else if (msg.getCause() == LookupCause.FINGER_UPDATE)
        {
            fingerTable.setPeerInfo(msg.getLookedUpID(), msg.getSuccessor());
            printState();
        }
        else
        {
            LOGGER.log(Level.WARNING, "Peer received a LOOKUP_RESULT with cause NEW_DATA!");
        }
    }

    private void handleLookupRequestMsg(LookupRequest msg)
    {
        LOGGER.log(Level.INFO, msg.toString());
        lookup(msg);
    }

    private void handlePredRequestMsg(PredecessorRequest msg)
    {
        synchronized(predecessor)
        {
            if (predecessor != PeerInfo.NULL_PEER)
                send(new PredecessorResult(getPredecessor()), msg.getSource().getListeningAddress());
        }
    }

    private void handlePredResultMsg(PredecessorResult msg)
    {
        synchronized (fingerTable)
        {
            // Get successor's predecessor
            PeerInfo predecessorOfSuccessor = msg.getPredecessor();
            PeerInfo succ = fingerTable.getSuccessor();
            if (succ != PeerInfo.NULL_PEER && predecessorOfSuccessor.getID().inInterval(ownInfo.getID(), succ.getID()))
            {
                fingerTable.setSuccessor(predecessorOfSuccessor);
                printState();
            }
        }
    }

    private void handlePredUpdateMsg(PredecessorUpdate msg)
    {
        PeerInfo latestPredecessor = msg.getLatestPredecessor();
        synchronized (predecessor)
        {
            if (predecessor == PeerInfo.NULL_PEER || latestPredecessor.getID().inInterval(predecessor.getID(), ownInfo.getID()))
            {
                setPredecessor(latestPredecessor);
                transferDataItemsToNewNode(latestPredecessor);
            }
        }
    }

    private void transferDataItemsToNewNode(PeerInfo newNode)
    {
        synchronized (storedFiles)
        {
            if (newNode != PeerInfo.NULL_PEER)
            {
                for(DataItem d : storedFiles.values())
                    if (d.getID().inInterval(ownInfo.getID(), newNode.getID()) || d.getID().compareTo(newNode.getID()) == 0)
                    {
                        LOGGER.log(Level.INFO, "Transferring " + d + " to newly joined node" + newNode.getListeningAddress());
                        send(d, newNode.getListeningAddress());
                    }
            }
        }
    }


    private void handleDataItemMsg(DataItem msg)
    {
        LOGGER.log(Level.INFO, "Received file (id " + msg.getID() + "): " + msg.getFilePath());
        synchronized (storedFiles)
        {
            storedFiles.put(msg.getID(), msg);
            printHeldDataItems();
        }
    }

    /*
    Core functionality of Chord ends
     */

    private void shutdown()
    {
        ownShutdown();

        // Send out a last gasp if needed
        synchronized (predecessor)
        {
            synchronized (fingerTable)
            {
                PeerInfo pred = getPredecessor();
                PeerInfo succ = fingerTable.getSuccessor();
                if (pred != PeerInfo.NULL_PEER)
                {
                    messenger.send(new LastGaspSuccessor(succ), pred.getListeningAddress());
                }
                if (succ != PeerInfo.NULL_PEER)
                {
                    messenger.send(new LastGaspPredecessor(pred), succ.getListeningAddress());
                }
            }
        }

        LOGGER.log(Level.INFO, "Shutting down the Chord layer");
        try
        {
            LOGGER.log(Level.INFO, "Waiting for 2 seconds for messaging to finish");
            messenger.stop(2);
        }
        catch(InterruptedException ex)
        {
            LOGGER.log(Level.INFO, "Interrupted before messaging could stop gracefully");
        }
    }

    private void handleEvent(Event ev)
    {
        switch(ev.getEventType())
        {
            case INTERRUPT_RECEIVED:
                shutdown();
                break;
            case MESSAGE_RECEIVED:
                handleMessageReceivedEvent((MessageReceived) ev);
                break;
            case MESSAGE_SENT:
                handleMessageSentEvent((MessageSent) ev);
                break;
            case CONNECTION_RECEIVED:
                handleConnectionReceivedEvent((ConnectionReceived) ev);
                break;
        }
    }

    private void handleMessageSentEvent(MessageSent ev)
    {
        LOGGER.log(Level.FINE, "Sent a message: " + ev.getMessage().getMessageType());

        if (ev.getMessage().getMessageType() == ChordMessageType.DATA_ITEM)
        {
            DataItem d = (DataItem) ev.getMessage();
            LOGGER.log(Level.INFO, "Successfully transferred the file " + d.getFilePath() + ". Now deleting it.");
            synchronized (storedFiles)
            {
                storedFiles.remove(d.getID());
            }
            d.delete();
            printHeldDataItems();
        }
    }

    private void handleConnectionReceivedEvent(ConnectionReceived ev)
    {
        Socket sock = ev.getSocket();
        if (isValidConnection(sock))
        {
            LOGGER.log(Level.INFO, "Received a new connection request from " + sock.getInetAddress());
            messenger.receive(sock);
        }
        else
        {
            try
            {
                LOGGER.log(Level.INFO, "Rejecting the connection");
                sock.close();
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "Unable to close the connection");
            }
        }
    }

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        Message msg = ev.getMessage();
        LOGGER.log(Level.INFO, "Received message: " + msg.getMessageType());
        if (msg.getMessageType() instanceof ChordMessageType)
        {
            switch((ChordMessageType) msg.getMessageType())
            {
                case PRED_REQUEST:
                    handlePredRequestMsg((PredecessorRequest) msg);
                    break;
                case PRED_RESULT:
                    handlePredResultMsg((PredecessorResult) msg);
                    break;
                case LOOKUP_REQUEST:
                    handleLookupRequestMsg((LookupRequest) msg);
                    break;
                case PRED_UPDATE:
                    handlePredUpdateMsg((PredecessorUpdate) msg);
                    break;
                case LOOKUP_RESULT:
                    handleLookupResultMsg((LookupResult) msg);
                    break;
                case LAST_GASP_PRED:
                    handleLastGaspPredMsg((LastGaspPredecessor) msg);
                    break;
                case LAST_GASP_SUCC:
                    handleLastGaspSuccMsg((LastGaspSuccessor) msg);
                    break;
                case DATA_ITEM:
                    handleDataItemMsg((DataItem) msg);
                    break;
            }
        }
        else
        {
            handleOwnReceivedMessage(msg);
        }
    }

    private void handleLastGaspPredMsg(LastGaspPredecessor msg) {

        PeerInfo pred = msg.getPredecessor();
        setPredecessor(pred);
        printState();
    }

    private void handleLastGaspSuccMsg(LastGaspSuccessor msg)
    {
        PeerInfo succ = msg.getSuccessor();
        fingerTable.setSuccessor(succ);
        printState();
    }

    private void handleFailedEvent(Event ev)
    {
        if (ev.getEventType() == EventType.MESSAGE_SENT)
        {
            MessageSent msev= ((MessageSent) ev);
            if (msev.getMessage().getMessageType() instanceof ChordMessageType)
            {
                switch ((ChordMessageType) msev.getMessage().getMessageType()) {
                    case LOOKUP_REQUEST: {
                        LookupRequest r = (LookupRequest) msev.getMessage();
                        switch (r.getCause()) {
                            case FINGER_UPDATE:
                            case NEW_NODE:
                                LOGGER.log(Level.WARNING, "Failed LOOKUP_REQUEST started by " + r.getSource() + " with cause " + r.getCause() + ". Retrying.");
                                fingerTable.setPeerInfo(r.getID(), PeerInfo.NULL_PEER);
                                new Thread(() -> {
                                    try
                                    {
                                        Thread.sleep(5000);
                                        lookup(r);
                                    }
                                    catch(InterruptedException ex)
                                    {

                                    }
                                }).start();
                                break;
                            default:
                                break;
                        }
                    }
                    case PRED_REQUEST:
                    {
                        LOGGER.log(Level.WARNING, "Failed to send PRED_REQUEST to successor");
                        break;
                    }
                    default:
                        break;
                }
            }
            else
                handleHigherFailedEvent(ev);
        }
    }

    protected final void send(Message msg, InetSocketAddress destination)
    {
        LOGGER.log(Level.INFO, "Sending " + msg.getMessageType() + " to " + destination);
        messenger.send(msg, destination);
    }

    protected synchronized void overrideID(ID id)
    {
        this.ownInfo = new PeerInfo(id, this.ownInfo.getListeningAddress(), this.ownInfo.getName());
        this.fingerTable = new FingerTable(this.ownInfo.getID());
        this.predecessor = PeerInfo.NULL_PEER;
        this.joined = false;
    }

    protected abstract void setup();
    protected abstract void handleOwnReceivedMessage(Message msg);
    protected abstract void handleHigherFailedEvent(Event ev);
    protected abstract void ownShutdown();


    protected boolean isValidConnection(Socket sock)
    {
        return true;
    }

    public PeerInfo getOwnInfo()
    {
        return ownInfo;
    }

    public void lookup(ID id, LookupCause cause)
    {
        synchronized (fingerTable) {
            PeerInfo pred = getClosestNodePrecedingId(id);
            // Query dies if not enough info is there
            if (pred == PeerInfo.NULL_PEER)
                return;

            LookupRequest msg = new LookupRequest(id, ownInfo, cause);

            if (pred.getID().compareTo(ownInfo.getID()) == 0)
                send(new LookupResult(fingerTable.getSuccessor(), msg), ownInfo.getListeningAddress());
            else
                send(msg, pred.getListeningAddress());
        }
    }

    public synchronized void join(PeerInfo anotherPeer)
    {
        if (!joined)
        {
            // If IDs match, that means this is the only node in the entire chord ring
            if (anotherPeer.getID().compareTo(ownInfo.getID()) == 0)
            {
                fingerTable.setSuccessor(ownInfo);
                predecessor = ownInfo;
                joined = true;
                LOGGER.log(Level.INFO, "Identified as first node in the network");
                printState();
            }
            else
            {
                send(new LookupRequest(ownInfo.getID(), ownInfo, LookupCause.NEW_NODE), anotherPeer.getListeningAddress());
            }
        }
    }

    @Override
    public void run()
    {
        LOGGER.log(Level.INFO, "Starting up the Chord Peer");

        messenger.listen();
        LOGGER.log(Level.INFO, "Begin listening for new connections...");

        LOGGER.log(Level.INFO, "Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                shutdown();
            }
        });

        setup();

        while(true)
        {
            try {
                Event ev = messenger.getEvent();
                LOGGER.log(Level.FINE, "Received an event");
                if (!ev.causedException())
                    handleEvent(ev);
                else
                {
                    LOGGER.log(Level.SEVERE, "Received event caused an exception: " + ev.getException().getMessage());
                    handleFailedEvent(ev);
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred while executing the event");
                ex.printStackTrace();
            }
        }
    }
}
