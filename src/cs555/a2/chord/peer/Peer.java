package cs555.a2.chord.peer;

import cs555.a2.chord.peer.messages.*;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

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

    public Peer(PeerInfo info, int hearbeatInterval)
    {
        this.ownInfo = info;
        this.fingerTable = new FingerTable(info.getID());
        this.predecessor = PeerInfo.NULL_PEER;
        this.messenger = new Messenger(info.getListeningAddress().getPort(), 4);
        this.joined = false;
        this.updater.schedule(this::hearbeat, hearbeatInterval, TimeUnit.MILLISECONDS);
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
                    PeerInfo succ = getSuccessor();
                    if (succ != PeerInfo.NULL_PEER)
                        send(new PredecessorRequest(ownInfo), succ.getListeningAddress());

                    // Fix fingers randomly
                    int idx = ThreadLocalRandom.current().nextInt(1, fingerTable.size());
                    lookup(fingerTable.getFinger(idx), LookupCause.FINGER_UPDATE);
                }
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
                send(new LookupResult(getSuccessor(), msg), msg.getSource().getListeningAddress());
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
            if (getSuccessor() != PeerInfo.NULL_PEER)
                succ = ownInfo;

            return succ;
        }
    }

    private PeerInfo getSuccessor()
    {
        return fingerTable.getPeerInfo(0);
    }

    private void setSuccessor(PeerInfo successor) {
        fingerTable.setPeerInfo(0, successor);
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

    private void handlePredUpdateMsg(PredecessorUpdate msg)
    {
        PeerInfo latestPredecessor = msg.getLatestPredecessor();
        synchronized (predecessor)
        {
            if (predecessor == PeerInfo.NULL_PEER || latestPredecessor.getID().inInterval(predecessor.getID(), ownInfo.getID()))
            {
                setPredecessor(latestPredecessor);
            }
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
                        setSuccessor(msg.getSuccessor());
                        joined = true;
                    }
                }
            }
        }
        else if (msg.getCause() == LookupCause.FINGER_UPDATE)
        {
            fingerTable.setPeerInfo(msg.getLookedUpID(), msg.getSuccessor());
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
            send(new PredecessorResult(getPredecessor()), msg.getSource().getListeningAddress());
        }
    }

    private void handlePredResultMsg(PredecessorResult msg)
    {
        synchronized (fingerTable)
        {
            // Get successor's predecessor
            PeerInfo succPred = msg.getPredecessor();
            PeerInfo succ = getSuccessor();
            if (succ != PeerInfo.NULL_PEER && succPred.getID().inInterval(ownInfo.getID(), succ.getID()))
            {
                setSuccessor(succPred);
                succ = succPred;
            }
            if (succ != PeerInfo.NULL_PEER)
                send(new PredecessorUpdate(ownInfo), getSuccessor().getListeningAddress());
        }
    }

    /*
    Core functionality of Chord ends
     */

    private void handleEvent(Event ev)
    {
        switch(ev.getEventType())
        {
            case INTERRUPT_RECEIVED:
                messenger.stop();
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
            }
        }
        else
        {
            handleOwnReceivedMessage(msg);
        }
    }

    private void handleFailedEvent(Event ev)
    {
        switch (ev.getEventType())
        {
            case MESSAGE_SENT: {
                Message msg = ((MessageSent) ev).getMessage();
                if (msg.getMessageType() instanceof ChordMessageType) {
                    switch ((ChordMessageType) msg.getMessageType()) {
                        case LOOKUP_REQUEST:
                        {

                            break;
                        }
                        case PRED_REQUEST:
                            setSuccessor(PeerInfo.NULL_PEER);
                            break;
                        default:
                            break;
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    protected final void send(Message msg, InetSocketAddress destination)
    {
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
    protected abstract void handleOwnFailedEvent(Event ev);

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
                send(new LookupResult(getSuccessor(), msg), ownInfo.getListeningAddress());
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
                for(int k = 0; k < fingerTable.size(); k++)
                    fingerTable.setPeerInfo(k, ownInfo);
                predecessor = ownInfo;
                joined = true;
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
                    handleOwnFailedEvent(ev);
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred while executing the event");
                ex.printStackTrace();
            }
        }
    }
}
