package cs555.a1.nodes;

import cs555.a1.messages.*;
import cs555.a1.transport.*;
import cs555.a1.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

public final class Process {

    private static final Logger LOGGER = Logger.getLogger(Process.class.getName());
    private static final int NUM_ROUNDS = 5000;
    private static final int MSGS_PER_ROUND = 5;

    private static int port = -1;
    private static InetSocketAddress collatorAddress;
    private static List<InetSocketAddress> addressList;

    private static AtomicInteger sent = new AtomicInteger(0);
    private static AtomicInteger received = new AtomicInteger(0);
    private static AtomicLong sentSummation = new AtomicLong(0);
    private static AtomicLong receivedSummation = new AtomicLong(0);

    private Process() {}

    private static void onPayload(Payload m) {
        received.incrementAndGet();
        receivedSummation.addAndGet(m.getData());
    }

    private static void onInitiate() {
        new Thread( () -> {
            Random nodeChooser = new Random();
            Random payloadGen = new Random();

            for (int i = 0; i < NUM_ROUNDS; i++) {
                int target = nodeChooser.nextInt(addressList.size());
                LOGGER.log(Level.FINEST, "Target index is " + target);
                try {
                    Sender s = new Sender(addressList.get(target));
                    LOGGER.log(Level.FINER, "Target is: " + addressList.get(target));
                    for (int j = 0; j < MSGS_PER_ROUND; j++) {
                        Payload m = new Payload(payloadGen.nextInt());
                        s.send(m);
                        sent.incrementAndGet();
                        sentSummation.addAndGet(m.getData());
                    }
                    s.close();
                } catch (IllegalStateException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                }
            }

            try {
                Thread.sleep(10000);
                Sender s = new Sender(Process.collatorAddress);
                s.send(new Summary(sent.get(), received.get(), sentSummation.get(), receivedSummation.get()));
                s.close();
            } catch (IllegalStateException | InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }).start();
    }

    private static void onDone()
    {
        System.exit(0);
    }

    private static int validateHost(InetAddress addr)
    {
        for(int i = 0; i < addressList.size(); i++)
        {
            if (addressList.get(i).getHostName().equals(addr.getHostName()))
            {
                return i;
            }
        }
        return -1;
    }

    private static void printUsage()
    {
        System.out.println("Usage: java Process <PORT> <COLLATOR_ADDRESS> <PATH_TO_CONFIG_FILE>");
    }

    public static boolean parseArgs(String[] args)
    {
        int port = -1;
        InetSocketAddress collatorAddress = null;
        List<InetSocketAddress> addresses = null;

        boolean success = false;

        if (args.length == 3) {
            try {
                port = TokenParser.parseAsInt(args[0], 0, 65535);

                collatorAddress = TokenParser.parseAsAddress(args[1]);

                File f = TokenParser.parseAsPath(args[2]);
                addresses = ConfigReader.read(f.getPath(), false, true);

                if (addresses != null)
                    success = true;
            }
            catch(IllegalArgumentException e)
            {
                LOGGER.log(Level.INFO, e.toString(), e);
                return false;
            }
        }

        if (success)
        {
            Process.port = port;
            Process.collatorAddress = collatorAddress;
            Process.addressList = Collections.unmodifiableList(addresses);
        }

        return success;
    }

    public static void main(String[] args)
    {
        LOGGER.setLevel(Level.ALL);
        if (Process.parseArgs(args))
        {
            LOGGER.log(Level.FINER, "Starting listener thread");
            Thread listener = new Thread(new Process.ProcessListener(Process.port));
            listener.start();

            LOGGER.log(Level.FINER, "Thread started");
            try
            {
                LOGGER.log(Level.FINER, "Sending READY message to Collator");
                Sender s = new Sender(Process.collatorAddress);
                s.send(new Ready());
                LOGGER.log(Level.FINER, "READY sent");
                s.close();
            }
            catch (IllegalStateException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
        else
        {
            printUsage();
        }
    }

    private static class ProcessListener extends Listener
    {
        public ProcessListener(int port)
        {
            super(port);
        }

        @Override
        public void handleClient(Socket s)
        {
            InetAddress clientAddress = s.getInetAddress();
            if (clientAddress != null)
            {
                boolean isValid =  Process.validateHost(clientAddress) >= 0;
                if (isValid)
                {
                    LOGGER.log(Level.FINE, "Connection request is valid");
                    LOGGER.log(Level.FINER, "Starting receiver");

                    Process.ProcessReceiver r = new Process.ProcessReceiver(s);
                    while(!r.shouldClose())
                    {
                        Message m = r.receive();
                        r.handleMessage(m, s.getInetAddress());
                    }
                    r.close();
                    LOGGER.log(Level.FINER, "Receiver completed");
                }
                else
                {
                    LOGGER.log(Level.INFO, "Connection request is invalid. Rejecting");
                    try {
                        s.close();
                    }
                    catch (IOException e) {
                        LOGGER.log(Level.WARNING, e.toString(), e);
                    }
                }
            }
            else
            {
                LOGGER.log(Level.WARNING, "Passed socket returned null on getInetAddress()");
            }
        }
    }

    private static class ProcessReceiver extends Receiver
    {
        public ProcessReceiver(Socket s)
        {
            super(s);
        }

        private int consecutivePayloads = 0;

        private boolean shouldClose()
        {
            if(consecutivePayloads == 5) {
                consecutivePayloads = 0;
                return true;
            }
            return false;
        }



        @Override
        public void handleMessage(Message m, InetAddress source)
        {
            LOGGER.log(Level.FINER, "Received a message");
            LOGGER.log(Level.FINEST, "Message type is " + m.getType());
            switch (m.getType()) {
                case INITIATE:
                    consecutivePayloads = 5;
                    Process.onInitiate();
                    break;
                case PAYLOAD:
                    consecutivePayloads++;
                    Process.onPayload((Payload) m);
                    break;
                default:
                    throw new UnsupportedOperationException("Invalid message received");
            }

        }
    }
}