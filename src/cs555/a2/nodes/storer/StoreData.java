package cs555.a2.nodes.storer;

import cs555.a2.chord.peer.ID;
import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.chord.peer.messages.*;
import cs555.a2.hash.CRC16;
import cs555.a2.nodes.discoverer.messages.*;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StoreData
{
    private static final Logger LOGGER = Logger.getLogger(StoreData.class.getName());
    private Messenger messenger;
    private PeerInfo ownInfo;
    private InetSocketAddress discoveryAddress;

    public static void printUsage()
    {
        System.out.println("Usage: " + StoreData.class.getCanonicalName() + " <ListeningPort> <DiscoveryHost> <Port>");
    }

    public StoreData(int listeningPort, InetSocketAddress discoveryAddress) throws UnknownHostException
    {
        this.ownInfo = PeerInfo.generatePeerInfo(new CRC16(), listeningPort, "StoreData");
        this.messenger = new Messenger(listeningPort, 2);
        this.discoveryAddress = discoveryAddress;
    }

    public final void send(Message msg, InetSocketAddress destination)
    {
        messenger.send(msg, destination);
    }

    public PeerInfo getOwnInfo()
    {
        return ownInfo;
    }

    public void run()
    {
        LOGGER.log(Level.INFO, "Begin listening for new connections");
        messenger.listen();

        send(new PeerRequest(getOwnInfo()), discoveryAddress);

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
        if (ev.getMessage().getMessageType() == ChordMessageType.DATA_ITEM)
        {
            DataItem d = (DataItem) ev.getMessage();
            LOGGER.log(Level.INFO, String.format("Successfully transferred the file %1$s (%2$s)", d.getFilePath(), d.getID()));
        }
        else
        {
            LOGGER.log(Level.FINE, "Sent a message: " + ev.getMessage().getMessageType());
        }
    }

    private HashMap<ID, DataItem> items = new HashMap<>();

    private void handleMessageReceivedEvent(MessageReceived ev)
    {
        Message msg = ev.getMessage();
        if(msg.getMessageType() == DiscovererMessageType.PEER_RESPONSE)
        {
            PeerResponse r = (PeerResponse) msg;
            LOGGER.log(Level.INFO, "Node resolving this query: " + r.getPeer());
            Scanner scanner = new Scanner(System.in);
            while(true)
            {
                try
                {
                    System.out.println("Enter file path:");
                    String filePath = scanner.nextLine();
                    System.out.println("Override ID? (Enter to skip)");
                    String id = scanner.nextLine();
                    boolean dummy = !id.trim().equals("");
                    DataItem d = null;
                    if (dummy)
                    {
                        d = new DataItem(filePath, new ID(id, 2));
                        LOGGER.log(Level.INFO, "Using manual hash value for " + d.getFilePath());
                    }
                    else
                    {
                        d = new DataItem(filePath);
                        LOGGER.log(Level.INFO, "Hashed the contents of " + d.getFilePath() + " to " + d.getID());
                    }
                    synchronized (items)
                    {
                        items.put(d.getID(), d);
                    }
                    send(new LookupRequest(d.getID(), getOwnInfo(), LookupCause.NEW_DATA), r.getPeer().getListeningAddress());
                    break;
                }
                catch(IllegalArgumentException ex)
                {
                    LOGGER.log(Level.INFO, ex.getMessage());
                }
                catch(IOException ex)
                {
                    LOGGER.log(Level.INFO, "Unable to read file");
                }
            }
        }
        else if (msg.getMessageType() == ChordMessageType.LOOKUP_RESULT)
        {
            LookupResult r = (LookupResult) msg;
            if (r.getCause() == LookupCause.NEW_DATA)
            {
                synchronized (items)
                {
                    LOGGER.log(Level.INFO, "Lookup request successful for ID " + r.getLookedUpID());
                    LOGGER.log(Level.INFO, "Path followed was: " + r.getPath());
                    DataItem d = items.get(r.getLookedUpID());
                    if (d != null)
                    {
                        send(d, r.getSuccessor().getListeningAddress());
                        LOGGER.log(Level.INFO, "Submitted the data item to be sent: " + d.getFilePath());
                        send(new PeerRequest(getOwnInfo()), discoveryAddress);
                    }
                    else
                    {
                        LOGGER.log(Level.SEVERE, "Unable to find the data item to send");
                    }
                }
            }
            else
            {
                LOGGER.log(Level.SEVERE, "Got a LookupResult with cause other than NEW_DATA!");
            }
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Got message " + msg.getMessageType());
        }
    }

    public static void main(String args[])
    {
        if (args.length >= 3)
        {
            StoreData sd = null;
            InetSocketAddress discoveryAddress;
            try {
                int listeningPort = Integer.parseInt(args[0]);
                String discoveryHost = args[1];
                int discoveryPort = Integer.parseInt(args[2]);
                discoveryAddress = new InetSocketAddress(discoveryHost, discoveryPort);
                sd = new StoreData(listeningPort, discoveryAddress);
            }
            catch (NumberFormatException ex) {
                LOGGER.log(Level.SEVERE, "Unable to parse port number");
                printUsage();
                System.exit(0);
            }
            catch (IllegalArgumentException ex)
            {
                LOGGER.log(Level.SEVERE, "Discovery port number seems to be invalid");
                printUsage();
                System.exit(0);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.exit(0);
            }
            sd.run();
        }
        else
        {
            printUsage();
        }
    }
}
