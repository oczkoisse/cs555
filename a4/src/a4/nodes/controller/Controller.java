package a4.nodes.controller;

import a2.transport.messenger.*;
import a4.nodes.server.messages.CheckIfAlive;
import a4.nodes.server.messages.MajorHeartbeat;
import a4.nodes.server.messages.MinorHeartbeat;
import a4.nodes.server.messages.ServerMessageType;

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

import static a4.nodes.server.messages.ServerMessageType.ALIVE_RESPONSE;

public class Controller
{
    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());
    private static final CheckIfAlive checkIfAlive = new CheckIfAlive();
    private static final int heartbeatInterval = 10;

    private final ScheduledExecutorService heart = Executors.newSingleThreadScheduledExecutor();
    private Messenger messenger;
    private List<InetSocketAddress> knownServers;

    public Controller(int listeningPort)
    {
        this.messenger = new Messenger(listeningPort, 4);
        this.knownServers = new ArrayList<>();
    }

    private void beat()
    {
        LOGGER.log(Level.FINE, "BEAT");

        for(InetSocketAddress address: knownServers)
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
                Enum msgType = msev.getMessage().getMessageType();

                if (msgType == ALIVE_RESPONSE)
                {
                    String dest = msev.getDestination().getHostName();
                    LOGGER.log(Level.INFO, "Looks like the chunk server at " + dest + " died" );

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

    private void handleMessageSentEvent(MessageSent ev)
    {

    }

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        if(ev.getMessage().getMessageType() instanceof ServerMessageType)
        {
            switch((ServerMessageType) ev.getMessage().getMessageType())
            {
                case ALIVE_RESPONSE:
                    break;
                case MAJOR_HEARTBEAT:
                    handleMajorHeartbeatMsg((MajorHeartbeat) ev.getMessage());
                    break;
                case MINOR_HEARTBEAT:
                    // If not seen before
                    // ask for major heartbeat
                    // else deal with minor heartbeat
                    handleMinorHeartbeatMsg((MinorHeartbeat) ev.getMessage());
                    break;
            }
        }

    }

    private void handleMinorHeartbeatMsg(MinorHeartbeat msg)
    {

    }

    private void handleMajorHeartbeatMsg(MajorHeartbeat msg)
    {

    }

    private void handleConnectionReceivedEvent(ConnectionReceived ev)
    {
        Socket sock = ev.getSocket();
        LOGGER.log(Level.INFO, "Received a new connection request from " + sock.getInetAddress());
        messenger.receive(sock);
    }

    private class

}
