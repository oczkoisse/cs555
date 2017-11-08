package a4.nodes.server;

import a4.chunker.Chunk;
import a4.chunker.IntegrityCheckFailedException;
import a4.chunker.Slice;
import a4.nodes.client.messages.ClientMessageType;
import a4.nodes.client.messages.ReadDataRequest;
import a4.nodes.client.messages.WriteData;
import a4.nodes.controller.messages.RecoveryReply;
import a4.transport.Message;
import a4.transport.messenger.*;
import a4.chunker.Metadata;
import a4.nodes.controller.messages.CheckIfAlive;
import a4.nodes.controller.messages.ControllerMessageType;
import a4.nodes.server.messages.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server
{
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int minorHeartbeatInterval = 30; // seconds
    private static final int minorHeartbeatsPerMajorHeartbeat = 10;

    private static final AliveResponse aliveResponse = new AliveResponse();

    private final ScheduledExecutorService heart = Executors.newSingleThreadScheduledExecutor();
    private final InetSocketAddress ownAddress;
    private final InetSocketAddress controllerAddress;

    private final ServerTable serverTable = new ServerTable();

    private Messenger messenger;
    private int minorHearbeatsCount;

    public Server(int listeningPort, String controllerHost, int controllerPort) throws UnknownHostException
    {
        this.messenger = new Messenger(listeningPort, 4);

        this.ownAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostName(), listeningPort);

        this.controllerAddress = new InetSocketAddress(controllerHost, controllerPort);
        this.minorHearbeatsCount = 0;
    }

    public static void printUsage()
    {
        System.out.println("Usage: " + Server.class.getCanonicalName() + " <ListeningPort> <ControllerHost> <ControllerPort>");
    }

    private void listen()
    {
        messenger.listen();
        LOGGER.log(Level.INFO, "Begin listening for new connections...");
    }


    private Heartbeat prepareHeartbeatMessage(boolean majorHeartbeat)
    {
        return majorHeartbeat ? new MajorHeartbeat(ownAddress, serverTable.getAllChunks()) :
                new MinorHeartbeat(ownAddress, serverTable.getNewChunks(), serverTable.getAllChunks().size());
    }

    private void beat()
    {
        minorHearbeatsCount++;
        boolean majorHearbeat = false;
        if (minorHearbeatsCount == minorHeartbeatsPerMajorHeartbeat)
        {
            majorHearbeat = true;
            minorHearbeatsCount = 0;
        }

        LOGGER.log(Level.INFO, "BEAT " + (majorHearbeat ? "(major)" : "(minor)"));

        this.messenger.send(prepareHeartbeatMessage(majorHearbeat), controllerAddress);
    }

    public void run()
    {
        LOGGER.log(Level.INFO, "Starting up the Server node");

        listen();

        LOGGER.log(Level.INFO, "Initiating heartbeat");
        this.heart.scheduleWithFixedDelay(this::beat, 0, minorHeartbeatInterval, TimeUnit.SECONDS);

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
                    LOGGER.log(Level.SEVERE, "Received event caused an exception: " + ev.getException());
                    handleFailedEvent(ev);
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred while executing the event");
                ex.printStackTrace();
            }
        }
    }

    public void handleEvent(Event ev)
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

        if (msg.getMessageType() instanceof ControllerMessageType)
        {
            switch ((ControllerMessageType) msg.getMessageType())
            {
                case CHECK_IF_ALIVE:
                    handleCheckIfAliveMsg((CheckIfAlive) msg);
                    break;
                case MAJOR_HEARTBEAT_REQUEST:
                    this.messenger.send(prepareHeartbeatMessage(true), controllerAddress);
                    break;
                case RECOVERY_REPLY:
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + ev.getMessage().getMessageType());
                    break;
            }
        }
        else if (msg.getMessageType() instanceof ClientMessageType)
        {
            switch ((ClientMessageType) msg.getMessageType())
            {
                case WRITE_DATA:
                    handleWriteDataMsg((WriteData) msg);
                    break;
                case READ_DATA_REQUEST:
                    handleReadDataRequest((ReadDataRequest) msg, ev.getSource().getHostName());
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + ev.getMessage().getMessageType());
                    break;
            }
        }
        else if (msg.getMessageType() instanceof ServerMessageType)
        {
            switch((ServerMessageType) msg.getMessageType())
            {
                default:
                    LOGGER.log(Level.WARNING, "Received unknown message: " + ev.getMessage().getMessageType());
                    break;
            }
        }
    }

    private void handleReadDataRequest(ReadDataRequest msg, String hostname) {
        Metadata m = serverTable.getChunk(msg.getFilename(), msg.getSeqNum());
        try
        {
            Chunk chunk = new Chunk(m.getStoragePath());
            messenger.send(new ReadData(chunk), new InetSocketAddress(hostname, msg.getPort()));
        }
        catch(IntegrityCheckFailedException ex)
        {
            LOGGER.log(Level.INFO, ex.getMessage());
            RecoveryRequest recoveryRequest = new RecoveryRequest(ex.getChunk().getMetadata(), ownAddress.getPort());
            messenger.send(recoveryRequest, controllerAddress);
            MessageReceived ev = messenger.waitForReplyTo(recoveryRequest);
            if (ev == null)
                LOGGER.log(Level.WARNING, "Interrupted while waiting for response to " + recoveryRequest.getMessageType());
            else if(ev.causedException())
                LOGGER.log(Level.WARNING, "Exception while waiting for response to " + recoveryRequest.getMessageType());
            else
            {
                handleRecoveryReplyMsg((RecoveryReply) ev.getMessage(), ex.getChunk(), ex.getFailedSlices());
            }
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.INFO, "Unable to read chunk");
        }
    }

    private boolean handleRecoveryReplyMsg(RecoveryReply msg, Chunk chunk, List<Integer> failedSlices) {
        for(InetSocketAddress addr: msg.getReplicas())
        {
            if(!addr.equals(ownAddress))
            {
                RecoveryDataRequest recoveryDataRequest = new RecoveryDataRequest(chunk.getMetadata(), failedSlices, ownAddress.getPort());
                MessageReceived ev = messenger.waitForReplyTo(recoveryDataRequest);
                if (ev == null)
                    LOGGER.log(Level.WARNING, "Interrupted while waiting for response to " + recoveryDataRequest.getMessageType());
                else if(ev.causedException())
                    LOGGER.log(Level.WARNING, "Exception while waiting for response to " + recoveryDataRequest.getMessageType());
                else
                {
                    if(handleRecoveryDataMsg((RecoveryData) ev.getMessage(), chunk))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean handleRecoveryDataMsg(RecoveryData msg, Chunk chunk) {
        try
        {
            // Fix the chunk slices
            for(Map.Entry<Integer, Slice> e: msg.getRecoveryData())
                chunk.fixSlice(e.getKey(), e.getValue());

            // And write to file (overwriting)
            chunk.writeToFile();
            return true;
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.INFO, "Error while writing the corrected chunk to file");
            return false;
        }
    }

    private void handleWriteDataMsg(WriteData msg) {
        LOGGER.log(Level.INFO, "Received " + msg.getChunk().getMetadata().getFileName() + ":" + msg.getChunk().getMetadata().getSequenceNum());
        try
        {
            msg.getChunk().writeToFile();
            serverTable.addChunk(msg.getChunk().getMetadata());
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Unable to write the chunk file " + msg.getChunk().getMetadata().getFileName());
        }
        if (msg.getForwardingAddress() != null)
        {
            LOGGER.log(Level.INFO, "Forwarding to " + msg.getForwardingAddress());
            messenger.send(msg, msg.getForwardingAddress());
        }
        else
            LOGGER.log(Level.INFO, "Final destination");
    }

    private void handleCheckIfAliveMsg(CheckIfAlive msg)
    {
        messenger.send(aliveResponse, controllerAddress);
    }

    public void handleFailedEvent(Event ev)
    {
        switch (ev.getEventType())
        {
            case MESSAGE_SENT: {
                MessageSent msev = (MessageSent) ev;
                Message msg = msev.getMessage();
                Enum msgType = msg.getMessageType();

                if (msgType == ServerMessageType.MINOR_HEARTBEAT || msgType == ServerMessageType.MAJOR_HEARTBEAT)
                {
                    LOGGER.log(Level.INFO, "Looks like the controller at " + controllerAddress + " is dead");
                }
                break;
            }
            default:
                LOGGER.log(Level.WARNING, "Failed event: " + ev.getEventType());
                break;
        }
    }

    public static void main(String[] args)
    {

        try
        {
            int ownPort = Integer.parseInt(args[0]);
            String controllerHost = args[1];
            int controllerPort = Integer.parseInt(args[2]);
            Server s = new Server(ownPort, controllerHost, controllerPort);
            s.run();
        }
        catch(NumberFormatException | IndexOutOfBoundsException ex)
        {
            printUsage();
        }
        catch(UnknownHostException ex)
        {
            LOGGER.log(Level.INFO, "Cannot determine host name of the server");
            System.exit(0);
        }

    }
}
