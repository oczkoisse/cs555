package cs555.a2.nodes.client;

import cs555.a2.chord.peer.*;
import cs555.a2.hash.*;
import cs555.a2.nodes.client.messages.ClientMessageType;
import cs555.a2.nodes.client.messages.DataItem;
import cs555.a2.nodes.discoverer.messages.*;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.*;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client extends Peer
{
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private InetSocketAddress discoveryAddress;
    private Boolean registered;

    private HashMap<ID, DataItem> storedFiles;

    private Hash hash = new CRC16();

    public Client(PeerInfo peerInfo, InetSocketAddress discoveryAddress, int hearbeatInterval)
    {
        super(peerInfo, hearbeatInterval);
        this.discoveryAddress = discoveryAddress;
        this.registered = false;
        this.storedFiles = new HashMap<>();
    }

    public static void main(String[] args)
    {
        if (args.length >= 4)
        {
            int listeningPort = -1;
            InetSocketAddress discoveryAddress = null;
            int hearbeatInterval = -1;
            PeerInfo ownInfo = null;
            try {

                listeningPort = Integer.parseInt(args[0]);
                String discoveryHost = args[1];
                int discoveryPort = Integer.parseInt(args[2]);
                discoveryAddress = new InetSocketAddress(discoveryHost, discoveryPort);
                ownInfo = PeerInfo.generatePeerInfo(new CRC16(), listeningPort);
                System.out.println("Default ID: " + ownInfo.getID());
                try {
                    hearbeatInterval = Integer.parseInt(args[3]);
                }
                catch(NumberFormatException ex)
                {
                    LOGGER.log(Level.SEVERE, "Unable to parse hearbeat interval");
                    printUsage();
                    System.exit(0);
                }
            }
            catch (NumberFormatException ex) {
                LOGGER.log(Level.SEVERE, "Unable to parse port number");
                Client.printUsage();
                System.exit(0);
            }
            catch (IllegalArgumentException ex)
            {
                LOGGER.log(Level.SEVERE, "Discovery port number seems to be invalid");
                Client.printUsage();
                System.exit(0);
            }
            catch (UnknownHostException ex)
            {
                LOGGER.log(Level.SEVERE, "Unable to identify host's network address");
                System.exit(0);
            }

            Client c = new Client(ownInfo, discoveryAddress, hearbeatInterval);
            Scanner scanner = new Scanner(System.in);
            while(true)
            {
                try
                {
                    System.out.println("Override ID? (Enter n/N to skip)");
                    String h = scanner.nextLine();
                    if (h.trim().toLowerCase().equals("n"))
                        break;
                    c.overrideID(new ID(h, 4));
                    LOGGER.log(Level.INFO, "ID overridden as " + c.getOwnInfo().getID());
                    break;
                }
                catch(IllegalArgumentException ex)
                {
                    LOGGER.log(Level.INFO, "Manually entered ID not valid");
                }
            }
            c.run();
        }
        else
        {
            Client.printUsage();
        }
    }

    private void register()
    {
        send(new RegisterRequest(getOwnInfo()), discoveryAddress);
    }

    private void deregister()
    {
        synchronized (registered)
        {
            if (registered)
                send(new DeregisterRequest(getOwnInfo()), discoveryAddress);
        }
    }

    @Override
    protected void transferDataItemsToNewNode(PeerInfo newNode)
    {
        synchronized (storedFiles)
        {
            for(DataItem d : storedFiles.values())
                if (d.getID().compareTo(newNode.getID()) <= 0)
                {
                    LOGGER.log(Level.INFO, "Transferring " + d + " to newly joined node" + newNode.getListeningAddress());
                    send(d, newNode.getListeningAddress());
                }
            printHeldDataItems();
        }
    }

    @Override
    protected void setup()
    {
        register();
    }

    public static void printUsage()
    {
        System.out.println("Usage: " + Client.class.getCanonicalName() + " <ListeningPort> <DiscoveryHost> <Discovery Port> <Heartbeat Interval>");
    }

    @Override
    protected void handleOwnReceivedMessage(Message msg)
    {
        if(msg.getMessageType() == DiscovererMessageType.REGISTER_RESPONSE)
            handleRegisterResponseMsg((RegisterResponse) msg);
        else if (msg.getMessageType() == DiscovererMessageType.DEREGISTER_RESPONSE)
            handleDeregisterResponseMsg((DeregisterResponse) msg);
        else if (msg.getMessageType() == DiscovererMessageType.PEER_RESPONSE)
            handlePeerResponseMsg((PeerResponse) msg);
        else if (msg.getMessageType() == ClientMessageType.DATA_ITEM)
            handleDataItemMsg((DataItem) msg);
    }

    private void handleDataItemMsg(DataItem msg)
    {
        LOGGER.log(Level.INFO, "Received file (id " + msg.getID() + "): " + msg.getFilePath());
        synchronized (storedFiles)
        {
            storedFiles.put(msg.getID(), msg);
            printHeldDataItems();
        }
    }

    private void printHeldDataItems()
    {
        synchronized(storedFiles) {
            LOGGER.log(Level.INFO, "Currently held data items: ");
            for (DataItem d: storedFiles.values())
            {
                LOGGER.log(Level.INFO, d.toString());
            }
        }
    }

    private void handlePeerResponseMsg(PeerResponse msg)
    {
        PeerInfo anotherPeer = msg.getPeer();
        join(anotherPeer);
    }

    private void handleDeregisterResponseMsg(DeregisterResponse msg)
    {

    }

    private void handleRegisterResponseMsg(RegisterResponse msg)
    {
        if (msg.getStatus())
        {
            synchronized(registered) {
                registered = true;
                send(new PeerRequest(getOwnInfo()), discoveryAddress);
            }
        }
        else {
            LOGGER.log(Level.INFO, "ID clash with an existing node. Creating a new one.");
            overrideID(new ID(new BigInteger(hash.randomHash()), hash.size()));
            register();
        }
    }

    @Override
    protected  void handleHigherFailedEvent(Event ev)
    {
        if (ev.getEventType() == EventType.MESSAGE_SENT) {
            Message msg = ((MessageSent) ev).getMessage();
            if (msg.getMessageType() == DiscovererMessageType.REGISTER_REQUEST || msg.getMessageType() == DiscovererMessageType.DEREGISTER_REQUEST) {
                LOGGER.log(Level.SEVERE, "Seems like Discovery is down. Exiting.");
                System.exit(0);
            }
        }
    }

    @Override
    protected void ownShutdown()
    {
        synchronized (registered)
        {
            if (registered)
                send(new DeregisterRequest(getOwnInfo()), discoveryAddress);
        }
    }
}
