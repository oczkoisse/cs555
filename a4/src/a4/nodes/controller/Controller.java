package a4.nodes.controller;

import a4.nodes.client.messages.ClientMessageType;
import a4.nodes.client.messages.ReadRequest;
import a4.nodes.client.messages.WriteRequest;
import a4.nodes.server.messages.RecoveryRequest;
import a4.transport.Notification;
import a4.transport.Request;
import a4.transport.messenger.*;
import a4.chunker.Metadata;
import a4.nodes.controller.messages.*;

import a4.nodes.server.messages.MajorHeartbeat;
import a4.nodes.server.messages.MinorHeartbeat;
import a4.nodes.server.messages.ServerMessageType;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
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

    public void printSummary()
    {
        synchronized (controllerTable)
        {
            Set<String> files = controllerTable.getAllFiles();
            StringBuilder sb = new StringBuilder();
            if (files != null)
            {
                for(String file: files)
                {
                    Set<Long> seqNums = controllerTable.getAllSequenceNums(file);
                    if (seqNums != null)
                    {
                        for(Long l: seqNums)
                        {
                            Set<InetSocketAddress> replicas = controllerTable.getAllReplicas(file, l);
                            if (replicas != null)
                            {
                                sb.append(file + ":" + l + " -> ");
                                for(InetSocketAddress addr: replicas)
                                    sb.append(addr.getHostString()+":"+addr.getPort() + ", ");
                                sb.delete(sb.length() - 2, sb.length());
                                sb.append("\n");
                            }

                        }
                    }
                }
            }
            Map<InetSocketAddress, Long> freeSpace = controllerTable.getFreeSpace();
            StringBuilder fsb = new StringBuilder();
            for(Map.Entry<InetSocketAddress, Long> i: freeSpace.entrySet())
            {
                InetSocketAddress addr = i.getKey();
                fsb.append(addr.getHostString()+":"+addr.getPort() + ": " + i.getValue() + "\n");
            }

            if (sb.length() > 0 || fsb.length() > 0)
                LOGGER.log(Level.INFO, (sb.length()> 0 ? "Replicas:\n" + sb.toString() + "\n" : "") +
                        (fsb.length() > 0 ? "Free space:\n" + fsb.toString() + "\n": ""));
        }
    }

    private void beat()
    {
        LOGGER.log(Level.FINE, "BEAT");
        printSummary();

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
            case NOTIFICATION_SENT: {
                NotificationSent msev = (NotificationSent) ev;
                Notification notification = msev.getNotification();

                if (notification.getMessageType() == ControllerMessageType.CHECK_IF_ALIVE ||
                        notification.getMessageType() == ControllerMessageType.TRANSFER  )
                {
                    String dest = msev.getDestination().getHostName();
                    LOGGER.log(Level.INFO, "Looks like the chunk server at " + dest + " died" );
                    if (controllerTable.hasNode(msev.getDestination())) {
                        controllerTable.removeNode(msev.getDestination());
                        printSummary();
                        initiateRecovery();
                    }
                }
                else
                    LOGGER.log(Level.WARNING, "Failed to send notification: " + msev.getNotification().getMessageType());
                break;
            }
            default:
                LOGGER.log(Level.WARNING, "Failed event: " + ev.getEventType());
                break;
        }
    }

    private void initiateRecovery()
    {
        synchronized (controllerTable)
        {
            boolean recoveryImpossible = false;

            done:
            for(String filename: controllerTable.getAllFiles())
            {
                for(Long seq: controllerTable.getAllSequenceNums(filename))
                {
                    Set<InetSocketAddress> candidates = controllerTable.getCandidates(filename, seq);
                    if (candidates == null)
                    {
                        recoveryImpossible = true;
                        break done;
                    }
                    else if (candidates.size() == 0)
                        continue;

                    LOGGER.log(Level.INFO, filename + ":" + seq + " needs to be replicated " + candidates.size() + " times");
                    for(InetSocketAddress dest: candidates)
                    {
                        InetSocketAddress exReplica = controllerTable.getExistingReplica(filename, seq);
                        if (exReplica != null)
                        {
                            LOGGER.log(Level.INFO, "Requesting transfer of " + filename + ":" + seq + " to " + exReplica);
                            messenger.send(new Transfer(filename, seq, dest), exReplica);
                        }
                        else
                        {
                            LOGGER.log(Level.SEVERE, "Failed to recover " + filename + ":" + seq);
                        }
                    }
                }
            }
            if (recoveryImpossible)
            {
                LOGGER.log(Level.WARNING, "Replication level fell below threshold. Recovery impossible");
                controllerTable.reset();
            }
        }
    }

    private void handleEvent(Event ev)
    {
        switch(ev.getEventType())
        {
            case INTERRUPT_RECEIVED:
                messenger.stop();
                break;
            case NOTIFICATION_RECEIVED:
                handleNotificationReceivedEvent((NotificationReceived) ev);
                break;
            case REQUEST_RECEIVED:
                handleRequestReceivedEvent((RequestReceived) ev);
                break;
            case NOTIFICATION_SENT:
                handleNotificationSentEvent((NotificationSent) ev);
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

    private void handleNotificationSentEvent(NotificationSent ev)
    {

    }

    private void handleRequestReceivedEvent(RequestReceived ev) {
        Request request = ev.getRequest();
        Enum msgType = request.getMessageType();
        if (msgType instanceof ClientMessageType)
        {
            switch((ClientMessageType) msgType)
            {
                case WRITE_REQUEST:
                    handleWriteRequestMsg((WriteRequest) request, ev.getSource());
                    break;
                case READ_REQUEST:
                    handleReadRequestMsg((ReadRequest) request, ev.getSource());
                    break;
            }
        }
        else if (msgType instanceof ServerMessageType)
        {
            switch((ServerMessageType) msgType)
            {
                case RECOVERY_REQUEST:
                    handleRecoveryRequestMsg((RecoveryRequest) request, ev.getSource());
                    break;
            }
        }
    }

    private void handleNotificationReceivedEvent(NotificationReceived ev)
    {
        Notification notification = ev.getNotification();
        Enum msgType = notification.getMessageType();

        if(msgType instanceof ServerMessageType)
        {
            switch((ServerMessageType) msgType)
            {
                case ALIVE_RESPONSE:
                    LOGGER.log(Level.FINE, "Chunk server at " + ev.getSource() + " is alive");
                    break;
                case MAJOR_HEARTBEAT:
                    handleMajorHeartbeatMsg((MajorHeartbeat) notification);
                    break;
                case MINOR_HEARTBEAT:
                    handleMinorHeartbeatMsg((MinorHeartbeat) notification);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Received unknown notification: " + msgType);
                    break;
            }
        }
        else {
            LOGGER.log(Level.WARNING, "Received unknown notification: " + msgType);
        }
    }

    private void handleRecoveryRequestMsg(RecoveryRequest msg, Socket sock) {
        LOGGER.log(Level.INFO, "Received RecoveryRequest for " + msg.getFilename() + ":" + msg.getSequenceNum());
        Set<InetSocketAddress> replica = controllerTable.getAllReplicas(msg.getFilename(), msg.getSequenceNum());
        if (replica != null)
        {
            LOGGER.log(Level.INFO, "Sending RecoveryReply with replica destinations");
            messenger.send(new RecoveryReply(msg.getFilename(), msg.getSequenceNum(), new ArrayList<>(replica)), sock);
        }
        else
        {
            LOGGER.log(Level.INFO, "No other replica destinations known to serve the RecoveryRequest");
        }
    }

    private void handleReadRequestMsg(ReadRequest msg, Socket sock) {
        InetSocketAddress replica = controllerTable.getExistingReplica(msg.getFilename(), msg.getSeqNum());
        if (replica != null)
            messenger.send(new ReadReply(replica), sock);
        else {
            messenger.send(new ReadReply(), sock);
            LOGGER.log(Level.INFO, "Ignoring read request for non-existent file");
        }
    }

    private void handleWriteRequestMsg(WriteRequest msg, Socket sock)
    {
        Set<InetSocketAddress> candidates = controllerTable.getCandidates(msg.getFilename(), msg.getSeqNum());
        if (candidates != null && candidates.size() == REPLICATION)
            messenger.send(new WriteReply(new ArrayList<>(candidates)), sock);
        else
            LOGGER.log(Level.INFO, "Ignoring write request");
    }

    private void handleMinorHeartbeatMsg(MinorHeartbeat msg)
    {
        LOGGER.log(Level.INFO, "Received minor heartbeat");
        InetSocketAddress listeningAddress = msg.getListeningAddress();
        synchronized (controllerTable)
        {
            if (!controllerTable.hasNode(listeningAddress)) {
                LOGGER.log(Level.INFO, "New chunk server detected at " + listeningAddress + ". Requesting major heartbeat");
                messenger.send(majorHeartbeatRequest, listeningAddress);
            }
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
        LOGGER.log(Level.INFO, "Received major heartbeat");
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
