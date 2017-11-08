package a4.nodes.controller;

import a4.nodes.client.messages.ClientMessageType;
import a4.nodes.client.messages.ReadRequest;
import a4.nodes.client.messages.WriteRequest;
import a4.nodes.server.messages.RecoveryRequest;
import a4.transport.Message;
import a4.transport.messenger.*;
import a4.chunker.Metadata;
import a4.nodes.controller.messages.*;

import a4.nodes.server.messages.MajorHeartbeat;
import a4.nodes.server.messages.MinorHeartbeat;
import a4.nodes.server.messages.ServerMessageType;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Controller
{
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());
    private static final CheckIfAlive checkIfAlive = new CheckIfAlive();
    private static final MajorHeartbeatRequest majorHeartbeatRequest = new MajorHeartbeatRequest();

    private static final int REPLICATION = 3;
    private static final int heartbeatInterval = 5;

    private final ScheduledExecutorService heart = Executors.newSingleThreadScheduledExecutor();
    private Messenger messenger;
    private ControllerTable controllerTable;

    public Controller(int listeningPort)
    {
        this.messenger = new Messenger(listeningPort, 4);
        this.controllerTable = new ControllerTable(REPLICATION);
    }

    private void beat()
    {
        LOGGER.log(Level.FINE, "BEAT");

        for(InetSocketAddress address: controllerTable.getAllNodes())
        {
            messenger.send(checkIfAlive, address);
        }
    }

    private void listen()
    {
        messenger.listen();
        LOGGER.log(Level.INFO, "Begin listening for new connections...");
    }

    public void run()
    {
        LOGGER.log(Level.INFO, "Starting up the Controller node");

        listen();

        this.heart.scheduleWithFixedDelay(this::beat, 0, heartbeatInterval, TimeUnit.SECONDS);

        // Run event loop
        while(true)
        {
            try {
                LOGGER.log(Level.FINE, "Waiting for next event");
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

    private void handleFailedEvent(Event ev)
    {
        switch (ev.getEventType())
        {
            case MESSAGE_SENT: {
                MessageSent msev = (MessageSent) ev;
                Message msg = msev.getMessage();
                Enum msgType = msg.getMessageType();

                if (msgType == ControllerMessageType.CHECK_IF_ALIVE)
                {
                    String dest = msev.getDestination().getHostName();
                    LOGGER.log(Level.INFO, "Looks like the chunk server at " + dest + " died" );
                    controllerTable.removeNode(msev.getDestination());
                }
                break;
            }
            default:
                break;
        }
    }

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

    private void handleConnectionReceivedEvent(ConnectionReceived ev)
    {
        Socket sock = ev.getSocket();
        LOGGER.log(Level.FINE, "Received a new connection request from " + sock.getInetAddress());
        messenger.receive(sock);
    }

    private void handleMessageSentEvent(MessageSent ev)
    {

    }

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        Message msg = ev.getMessage();
        Enum msgType = msg.getMessageType();

        if(msgType instanceof ServerMessageType)
        {
            switch((ServerMessageType) msgType)
            {
                case ALIVE_RESPONSE:
                    break;
                case MAJOR_HEARTBEAT:
                    handleMajorHeartbeatMsg((MajorHeartbeat) msg);
                    break;
                case MINOR_HEARTBEAT:
                    handleMinorHeartbeatMsg((MinorHeartbeat) msg);
                    break;
                case RECOVERY_REQUEST:
                    handleRecoveryRequestMsg((RecoveryRequest) msg, ev.getSource().getHostName());
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + msgType);
                    break;
            }
        }
        else if (msgType instanceof ClientMessageType)
        {
            switch((ClientMessageType) msgType)
            {
                case WRITE_REQUEST:
                    handleWriteRequestMsg((WriteRequest) msg, ev.getSource().getHostName());
                    break;
                case READ_REQUEST:
                    handleReadRequestMsg((ReadRequest) msg, ev.getSource().getHostName());
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + msgType);
                    break;
            }
        }
    }

    private void handleRecoveryRequestMsg(RecoveryRequest msg, String hostname) {
        Set<InetSocketAddress> replica = controllerTable.getAllReplicas(msg.getFilename(), msg.getSequenceNum());
        if (replica != null)
            messenger.send(new RecoveryReply(msg.getFilename(), msg.getSequenceNum(), new ArrayList<>(replica)), new InetSocketAddress(hostname, msg.getListeningPort()));
    }

    private void handleReadRequestMsg(ReadRequest msg, String hostname) {
        InetSocketAddress talkbackAddr = new InetSocketAddress(hostname, msg.getPort());
        InetSocketAddress replica = controllerTable.getExistingReplica(msg.getFilename(), msg.getSeqNum());
        if (replica != null)
            messenger.send(new ReadReply(replica), talkbackAddr);
        else {
            messenger.send(new ReadReply(), talkbackAddr);
            LOGGER.log(Level.INFO, "Ignoring read request for non-existent file");
        }
    }

    private void handleWriteRequestMsg(WriteRequest msg, String hostname)
    {
        InetSocketAddress talkbackAddr = new InetSocketAddress(hostname, msg.getPort());
        Set<InetSocketAddress> candidates = controllerTable.getCandidates(msg.getFilename(), msg.getSeqNum());
        if (candidates != null && candidates.size() == REPLICATION)
            messenger.send(new WriteReply(new ArrayList<>(candidates)), talkbackAddr);
        else
            LOGGER.log(Level.INFO, "Ignoring write request");
    }

    private void handleMinorHeartbeatMsg(MinorHeartbeat msg)
    {
        InetSocketAddress listeningAddress = msg.getListeningAddress();
        synchronized (controllerTable)
        {
            if (!controllerTable.hasNode(listeningAddress))
                messenger.send(majorHeartbeatRequest, listeningAddress);
            else
            {
                controllerTable.updateNode(listeningAddress, msg.getFreeSpace());
                for(Metadata m: msg.getChunksInfo())
                {
                    String filename = m.getFileName().toString();
                    long seqNum = m.getSequenceNum();
                    if(controllerTable.addReplica(m.getFileName().toString(), m.getSequenceNum(), listeningAddress))
                    {
                        LOGGER.log(Level.INFO, String.format("Detected a replica for chunk %d for file %s at %s", seqNum, filename, listeningAddress));
                    }
                    else
                    {
                        LOGGER.log(Level.WARNING, "Minor hearbeat new replica info found to be stale");
                    }
                }
            }
        }
    }

    private void handleMajorHeartbeatMsg(MajorHeartbeat msg)
    {
        InetSocketAddress listeningAddress = msg.getListeningAddress();
        synchronized (controllerTable)
        {
            if (!controllerTable.hasNode(listeningAddress))
            {
                LOGGER.log(Level.INFO, "Detected a new chunk server at " + listeningAddress);
                controllerTable.addNode(listeningAddress, msg.getFreeSpace());
            }

            for(Metadata m: msg.getChunksInfo())
            {
                String filename = m.getFileName().toString();
                long seqNum = m.getSequenceNum();

                if(controllerTable.addReplica(m.getFileName().toString(), m.getSequenceNum(), listeningAddress))
                    LOGGER.log(Level.INFO, String.format("Detected a replica for chunk %d for file %s at %s", seqNum, filename, listeningAddress));
            }
        }
    }

    public static void printUsage()
    {
        System.out.println("Usage: " + Controller.class.getCanonicalName() + " <ListeningPort>");
    }


    public static void main(String[] args)
    {
        try
        {
            int ownPort = Integer.parseInt(args[0]);
            Controller c = new Controller(ownPort);
            c.run();
        }
        catch(NumberFormatException | IndexOutOfBoundsException ex)
        {
            printUsage();
        }
    }
}
