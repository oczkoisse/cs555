package a4.nodes.server;

import a2.transport.Message;
import a2.transport.messenger.*;
import a4.chunker.Metadata;
import a4.nodes.controller.Controller;
import a4.nodes.controller.messages.ControllerMessageType;
import a4.nodes.server.messages.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
    private final InetSocketAddress controllerAddress;
    private Messenger messenger;
    private int minorHearbeatsCount;

    public Server(int listeningPort, String controllerHost, int controllerPort)
    {
        this.messenger = new Messenger(listeningPort, 4);
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
        List<Metadata> chunks = new ArrayList<>();
        return majorHeartbeat ? new MajorHeartbeat(chunks) : new MinorHeartbeat(chunks, 0);
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
                    LOGGER.log(Level.SEVERE, "Received event caused an exception: " + ev.getException().getMessage());
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
        LOGGER.log(Level.INFO, "Received a new connection request from " + sock.getInetAddress());
        messenger.receive(sock);
    }

    private void handleMessageSentEvent(MessageSent ev)
    {

    }

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        Message msg = ev.getMessage();

        if (msg.getMessageType() == ControllerMessageType.MAJOR_HEARTBEAT_REQUEST)
        {
            this.messenger.send(prepareHeartbeatMessage(true), controllerAddress);
        }
        else if (msg.getMessageType() instanceof ServerMessageType)
        {
            switch ((ServerMessageType) msg.getMessageType())
            {
                case CHECK_IF_ALIVE:
                    handleCheckIfAliveMsg((CheckIfAlive) msg);
                    break;
            }
        }
    }

    private void handleCheckIfAliveMsg(CheckIfAlive msg)
    {
        messenger.send(aliveResponse, controllerAddress);
    }

    public void handleFailedEvent(Event ev)
    {

    }

    public static void main(String[] args)
    {

    }
}
