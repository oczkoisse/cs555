package a4.nodes.controller;

import a4.nodes.client.messages.ClientMessageType;
import a4.nodes.client.messages.WriteRequest;
import a4.transport.Message;
import a4.transport.messenger.*;
import a4.chunker.Metadata;
import a4.nodes.controller.messages.*;

import a4.nodes.server.messages.MajorHeartbeat;
import a4.nodes.server.messages.MinorHeartbeat;
import a4.nodes.server.messages.ServerMessageType;

import java.net.InetSocketAddress;
import java.net.Socket;
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
    private NodeTable nodeTable;

    public Controller(int listeningPort)
    {
        this.messenger = new Messenger(listeningPort, 4);
        this.nodeTable = new NodeTable(REPLICATION);
    }

    private void beat()
    {
        LOGGER.log(Level.FINE, "BEAT");

        for(InetSocketAddress address: nodeTable.getAllNodes())
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
                    nodeTable.removeNode(msev.getDestination());
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
                    WriteRequest writeRequest = (WriteRequest) msg;
                    nodeTable.getCandidateReplicas(writeRequest.getFilename(), writeRequest.getSeqNum());
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + msgType);
                    break;
            }
        }
    }

    private void handleMinorHeartbeatMsg(MinorHeartbeat msg)
    {
        InetSocketAddress listeningAddress = msg.getListeningAddress();
        synchronized (nodeTable)
        {
            if (!nodeTable.hasNode(listeningAddress))
                messenger.send(majorHeartbeatRequest, listeningAddress);
            else
            {
                nodeTable.updateNode(listeningAddress, msg.getFreeSpace());
                for(Metadata m: msg.getChunksInfo())
                    nodeTable.addReplica(m.getFileName().toString(), m.getSequenceNum(), listeningAddress);
            }
        }
    }

    private void handleMajorHeartbeatMsg(MajorHeartbeat msg)
    {
        InetSocketAddress listeningAddress = msg.getListeningAddress();
        synchronized (nodeTable)
        {
            if (!nodeTable.hasNode(listeningAddress))
            {
                LOGGER.log(Level.INFO, "Detected a new chunk server at " + listeningAddress);
                nodeTable.addNode(listeningAddress, msg.getFreeSpace());
            }

            for(Metadata m: msg.getChunksInfo())
            {
                String filename = m.getFileName().toString();
                long seqNum = m.getSequenceNum();

                if(nodeTable.addReplica(m.getFileName().toString(), m.getSequenceNum(), listeningAddress))
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
