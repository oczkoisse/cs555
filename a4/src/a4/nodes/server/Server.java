package a4.nodes.server;

import a4.chunker.Chunk;
import a4.chunker.IntegrityCheckFailedException;
import a4.chunker.Slice;
import a4.nodes.client.messages.*;
import a4.nodes.controller.messages.*;
import a4.nodes.server.messages.*;

import a4.transport.*;
import a4.transport.messenger.*;
import a4.chunker.Metadata;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    private int heartbeats;

    public Server(int listeningPort, String controllerHost, int controllerPort) throws UnknownHostException
    {
        this.messenger = new Messenger(listeningPort, 4);

        this.ownAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostName(), listeningPort);

        this.controllerAddress = new InetSocketAddress(controllerHost, controllerPort);
        this.heartbeats = 0;
    }

    public static void printUsage()
    {
        System.out.println("Usage: " + Server.class.getCanonicalName() + " <ListeningPort> <ControllerHost> <ControllerPort>");
    }

    public void printSummary()
    {
        // List of files and the chunks being held for each of them
        synchronized (serverTable)
        {
            List<Metadata> chunks = serverTable.getAllChunks();
            if (chunks != null)
            {
                for(Metadata m: chunks)
                {
                    System.out.println(m.getFileName() + ":" + m.getSequenceNum());
                }
            }
        }
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
        printSummary();

        heartbeats++;
        boolean majorHearbeat = false;
        if (heartbeats == minorHeartbeatsPerMajorHeartbeat)
        {
            majorHearbeat = true;
            heartbeats = 0;
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
            case REQUEST_RECEIVED:
                handleRequestReceivedEvent((RequestReceived) ev);
                break;
            case NOTIFICATION_RECEIVED:
                handleNotificationReceivedEvent((NotificationReceived) ev);
                break;
            case NOTIFICATION_SENT:
                handleMessageSentEvent((NotificationSent) ev);
                break;
            case CONNECTION_RECEIVED:
                handleConnectionReceivedEvent((ConnectionReceived) ev);
                break;
        }
    }

    private void handleRequestReceivedEvent(RequestReceived ev) {
        Request request = ev.getRequest();
        Enum msgType = request.getMessageType();

        if (msgType == ClientMessageType.READ_DATA_REQUEST)
        {
            handleReadDataRequest((ReadDataRequest) request, ev.getSource());
        }
        else if (msgType == ServerMessageType.RECOVERY_DATA_REQUEST)
        {
            handleRecoveryDataRequestMsg((RecoveryDataRequest) request, ev.getSource());
        }
        else
        {
            LOGGER.log(Level.WARNING, "Received unknown request: " + request.getMessageType());
        }
    }

    private void handleConnectionReceivedEvent(ConnectionReceived ev)
    {
        Socket sock = ev.getSocket();
        LOGGER.log(Level.FINE, "Received a new connection request from " + sock.getInetAddress());
        messenger.receive(sock);
    }

    private void handleMessageSentEvent(NotificationSent ev)
    {



    }

    private void handleNotificationReceivedEvent(NotificationReceived ev)
    {
        Notification notification = ev.getNotification();

        if (notification.getMessageType() instanceof ControllerMessageType)
        {
            switch ((ControllerMessageType) notification.getMessageType())
            {
                case CHECK_IF_ALIVE:
                    handleCheckIfAliveMsg((CheckIfAlive) notification);
                    break;
                case TRANSFER:
                    handleTransferMsg((Transfer) notification);
                    break;
                case MAJOR_HEARTBEAT_REQUEST:
                    this.messenger.send(prepareHeartbeatMessage(true), controllerAddress);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown notification: " + notification.getMessageType());
                    break;
            }
        }
        else if (notification.getMessageType() instanceof ClientMessageType)
        {
            switch ((ClientMessageType) notification.getMessageType())
            {
                case WRITE_DATA:
                    handleWriteDataMsg((WriteData) notification);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown notification: " + notification.getMessageType());
                    break;
            }
        }
        else if (notification.getMessageType() instanceof ServerMessageType)
        {
            switch((ServerMessageType) notification.getMessageType())
            {
                case TRANSFER_DATA:
                    handleTransferDataMsg((TransferData) notification);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown notification: " + notification.getMessageType());
                    break;
            }
        }
    }

    private void handleRecoveryDataRequestMsg(RecoveryDataRequest msg, Socket sock) {
        LOGGER.log(Level.INFO, "Received RecoveryDataRequest");
        Metadata m = serverTable.getChunk(msg.getFilename(), msg.getSequenceNum());
        if (m == null)
        {
            LOGGER.log(Level.WARNING, "Chunk found to be missing in response to RecoveryDataRequest");
            return;
        }

        try
        {
            Chunk c = new Chunk(m.getStoragePath());
            int i = 0;
            List<Slice> slices = new ArrayList<>();
            for(Slice s: c)
            {
                if (msg.getFailedSlices().contains(i))
                    slices.add(s);
                i++;
            }

            RecoveryData recoveryData = new RecoveryData(msg.getFilename(), msg.getSequenceNum(), msg.getFailedSlices(), slices);
            messenger.send(recoveryData, sock);
        }
        catch(IntegrityCheckFailedException ex)
        {
            if (handleIntegrityCheckFailure(ex))
                handleRecoveryDataRequestMsg(msg, sock);
            else
            {
                LOGGER.log(Level.WARNING, "Unable to fix the chunk");
            }
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.WARNING, "Unable to read chunk in response to RecoveryDataRequest");
        }
    }

    private void handleTransferDataMsg(TransferData msg) {
        try {
            msg.getChunk().writeToFile();
            serverTable.addChunk(msg.getChunk().getMetadata());
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.WARNING, "Failed to write the transferred chunk to disk");
        }
    }

    private void handleTransferMsg(Transfer msg) {
        Metadata chunkInfo = serverTable.getChunk(msg.getFilename(), msg.getSequenceNum());
        if (chunkInfo == null)
        {
            LOGGER.log(Level.WARNING, "Chunk found to be absent in response to Transfer. Ignoring Transfer");
            return;
        }

        try
        {
            Chunk c = new Chunk(chunkInfo.getStoragePath());
            messenger.send(new TransferData(c), msg.getDestination());
        }
        catch(IntegrityCheckFailedException ex)
        {
            if (handleIntegrityCheckFailure(ex))
                handleTransferMsg(msg);
            else
            {
                LOGGER.log(Level.WARNING, "Unable to fix the chunk");
            }
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.INFO, "Unable to read chunk at " + chunkInfo.getStoragePath());
        }
    }

    private void handleReadDataRequest(ReadDataRequest msg, Socket sock) {
        Metadata m = serverTable.getChunk(msg.getFilename(), msg.getSeqNum());
        if (m == null)
        {
            LOGGER.log(Level.WARNING, "Chunk found to be absent in response to ReadDataRequest. Ignoring ReadRequest");
            return;
        }

        try
        {
            Chunk chunk = new Chunk(m.getStoragePath());
            messenger.send(new ReadData(chunk), sock);
        }
        catch(IntegrityCheckFailedException ex)
        {
            if(handleIntegrityCheckFailure(ex))
            {
                // Retry
                handleReadDataRequest(msg, sock);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Unable to fix the chunk");
            }
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.INFO, "Unable to read chunk at " + m.getStoragePath());
        }
    }

    private boolean handleIntegrityCheckFailure(IntegrityCheckFailedException except) {
        LOGGER.log(Level.INFO, except.getMessage());
        RecoveryRequest recoveryRequest = new RecoveryRequest(except.getChunk().getMetadata());
        LOGGER.log(Level.INFO, "Sending recovery request");
        try
        {
            Notification notification = Messenger.request(recoveryRequest, controllerAddress);
            if(handleRecoveryReplyMsg((RecoveryReply) notification, except.getChunk(), except.getFailedSlices()))
                return true;
        }
        catch(SenderException | ReceiverException ex)
        {
            LOGGER.log(Level.WARNING, "Unable to receive reply from Controller");
        }
        return false;
    }

    private boolean handleRecoveryReplyMsg(RecoveryReply msg, Chunk chunk, List<Integer> failedSlices) {
        LOGGER.log(Level.INFO, "Received recovery reply. Replicas are available at " + msg.getReplicas());
        for(InetSocketAddress addr: msg.getReplicas())
        {
            if(!addr.equals(ownAddress))
            {
                RecoveryDataRequest recoveryDataRequest = new RecoveryDataRequest(chunk.getMetadata(), failedSlices);

                try{
                    LOGGER.log(Level.INFO, "Sending recovery data request to " + addr);
                    Notification notification = Messenger.request(recoveryDataRequest, addr);

                    if(handleRecoveryDataMsg((RecoveryData) notification, chunk))
                        return true;
                }
                catch (SenderException | ReceiverException ex)
                {
                    LOGGER.log(Level.WARNING, "Looks like chunk server is unavailable for servicing RecoveryDataRequest");
                }
            }
        }
        return false;
    }

    private boolean handleRecoveryDataMsg(RecoveryData msg, Chunk chunk)
    {
        // Fix the chunk slices
        for(Map.Entry<Integer, Slice> e: msg.getRecoveryData())
        {
            LOGGER.log(Level.INFO, "Fixing slice " + e.getKey());
            chunk.fixSlice(e.getKey(), e.getValue());
        }

        try
        {
            LOGGER.log(Level.INFO, "Writing fixed chunk");
            // And write to file (overwriting)
            chunk.writeToFile();
            LOGGER.log(Level.INFO, "Successfully fixed chunk");
            return true;
        }
        catch(IOException ex)
        {
            LOGGER.log(Level.INFO, "Error while writing the fixed chunk to file");
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
            case NOTIFICATION_SENT: {
                NotificationSent msev = (NotificationSent) ev;
                Message msg = msev.getNotification();
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
        Server s = null;
        try
        {
            int ownPort = Integer.parseInt(args[0]);
            String controllerHost = args[1];
            int controllerPort = Integer.parseInt(args[2]);
            s = new Server(ownPort, controllerHost, controllerPort);
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
        if (s != null)
            s.run();

    }
}
