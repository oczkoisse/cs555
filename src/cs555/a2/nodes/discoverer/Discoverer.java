package cs555.a2.nodes.discoverer;

import cs555.a2.nodes.discoverer.messages.*;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.*;

import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Discoverer
{
    private static final Logger LOGGER = Logger.getLogger(Discoverer.class.getName());

    private Messenger messenger;
    private BasicDiscoveryService discoveryService = new BasicDiscoveryService();

    public Discoverer(int listeningPort)
    {
        messenger = new Messenger(listeningPort, 1);
    }

    public void run()
    {
        LOGGER.log(Level.INFO, "Starting up the Discovery node");
        messenger.listen();

        LOGGER.log(Level.INFO, "Begin listening for new connections...");

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
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred while executing the event");
                ex.printStackTrace();
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
            case MESSAGE_RECEIVED:
                handleMessageReceivedEvent((MessageReceived) ev);
            case MESSAGE_SENT:
                handleMessageSentEvent((MessageSent) ev);
            case CONNECTION_RECEIVED:
                handleConnectionReceivedEvent((ConnectionReceived) ev);
                break;
        }
    }

    private void handleMessageSentEvent(MessageSent ev)
    {
        LOGGER.log(Level.FINE, "Sent a message");
    }

    private void handleConnectionReceivedEvent(ConnectionReceived ev)
    {
        Socket sock = ev.getSocket();
        LOGGER.log(Level.INFO, "Received a new connection request from " + sock.getInetAddress());
        messenger.receive(sock);
    }

    private void handleRegisterRequestMsg(RegisterRequest msg)
    {
        LOGGER.log(Level.INFO, "Received REGISTER_REQUEST from " + msg.getSource().getListeningAddress());
        boolean success = discoveryService.register(msg.getSource());
        LOGGER.log(Level.INFO, success ? "Accepting" : "Rejecting");
        messenger.send(new RegisterResponse(success), msg.getSource().getListeningAddress());
    }

    private void handleDeregisterRequestMsg(DeregisterRequest msg)
    {
        LOGGER.log(Level.INFO, "Received DEREGISTER_REQUEST from " + msg.getSource().getListeningAddress());
        boolean success = discoveryService.deregister(msg.getSource());
        LOGGER.log(Level.INFO, success ? "Accepting" : "Rejecting");
        messenger.send(new DeregisterResponse(success), msg.getSource().getListeningAddress());
    }

    private void handlePeerRequestMsg(PeerRequest msg)
    {
        LOGGER.log(Level.INFO, "Received PEER_REQUEST from " + msg.getSource().getListeningAddress());
        PeerResponse response = new PeerResponse(discoveryService.getAnyPeer(msg.getSource()));
        messenger.send(response, msg.getSource().getListeningAddress());
        LOGGER.log(Level.INFO, "Sending peer info");
    }

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        Message msg = ev.getMessage();
        switch ((DiscoverMessageType) msg.getMessageType())
        {
            case REGISTER_REQUEST:
                handleRegisterRequestMsg((RegisterRequest) msg);
                break;
            case DEREGISTER_REQUEST:
                handleDeregisterRequestMsg((DeregisterRequest) msg);
                break;
            case PEER_REQUEST:
                handlePeerRequestMsg((PeerRequest) msg);
                break;
        }
    }

    public static void printUsage()
    {
        System.out.println("Usage: " + Discoverer.class.getCanonicalName() + " <ListeningPort>");
    }


    public static void main(String[] args)
    {
        if (args.length >= 1) {
            int listeningPort = -1;
            try {
                listeningPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                Discoverer.printUsage();
                System.exit(0);
            }

            Discoverer discoverer = new Discoverer(listeningPort);
            discoverer.run();
        }
        else {
            Discoverer.printUsage();
        }

    }
}
