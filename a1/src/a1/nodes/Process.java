package a1.nodes;

import a1.messages.*;
import a1.transport.*;
import a1.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static List<Boolean> done;
    private static boolean doneSending = false;
    private static final Object doneSendingBarrier = new Object();
    private static ProcessListener listener;

    private static AtomicInteger sent = new AtomicInteger(0);
    private static AtomicInteger received = new AtomicInteger(0);
    private static AtomicLong sentSummation = new AtomicLong(0);
    private static AtomicLong receivedSummation = new AtomicLong(0);

    private static boolean allDone()
    {
        synchronized (done) {
            for (boolean i : done)
                if (!i)
                    return false;
        }
        return true;
    }

    private Process() {}

    private static void onPayload(Payload m) {
        received.incrementAndGet();
        receivedSummation.addAndGet(m.getData());
    }

    private static void onInitiate() {
        Random nodeChooser = new Random();
        Random payloadGen = new Random();

        for (int i = 0; i < NUM_ROUNDS; i++) {
            int target = nodeChooser.nextInt(addressList.size());
            LOGGER.log(Level.FINEST, "Target index is " + target);
            Sender s = null;
            try {
                LOGGER.log(Level.FINER, "Target is: " + addressList.get(target));
                s = new Sender(addressList.get(target));

                for (int j = 0; j < MSGS_PER_ROUND; j++) {
                    Payload m = new Payload(payloadGen.nextInt());
                    s.send(m);
                    sent.incrementAndGet();
                    sentSummation.addAndGet(m.getData());
                }
            } catch (IllegalStateException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
            finally {
                if (s!=null)
                    s.close();
            }
        }
        LOGGER.log(Level.INFO, "Sent all messages");
        Sender.broadcast(new Done(), addressList);
        synchronized (doneSendingBarrier)
        {
            doneSending = true;
            doneSendingBarrier.notify();
        }
    }

    private static void onDone(InetAddress source)
    {
        int idx = validateHost(source, Process.VALIDATION_MODE.EXCLUDE_DONE);
        if (idx >= 0)
        {
            synchronized (done)
            {
                done.set(idx, true);
            }

            if (allDone()) {
                LOGGER.log(Level.INFO, "Received all messages");
                try
                {
                    synchronized(doneSendingBarrier)
                    {
                        while(!doneSending)
                            doneSendingBarrier.wait();
                    }
                }
                catch(InterruptedException e)
                {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }

                Sender s = null;
                try {
                    s = new Sender(Process.collatorAddress);
                    Summary summary = new Summary(sent.get(), received.get(), sentSummation.get(), receivedSummation.get());
                    s.send(summary);
                    LOGGER.log(Level.INFO,
                            String.format("Sent the summary: Sent %d, Received %d, Sent Summation %d, Received Summation %d",
                                    summary.getSent(), summary.getReceived(), summary.getSentSummation(), summary.getReceivedSummation()));
                } catch (IllegalStateException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                }
                finally {
                    if (s != null)
                        s.close();
                    Process.listener.close();
                }
            }
        }
        else
        {
            LOGGER.log(Level.INFO, "DONE message received from unrecognized source");
        }
    }

    private enum VALIDATION_MODE {EXCLUDE_NONE, EXCLUDE_DONE}

    private static int validateHost(InetAddress addr, Process.VALIDATION_MODE mode)
    {
        String target = addr.getCanonicalHostName();
        for(int i = 0; i < addressList.size(); i++)
        {
            if (mode == Process.VALIDATION_MODE.EXCLUDE_DONE && done.get(i))
                continue;
            String cur = addressList.get(i).getHostString();
            LOGGER.log(Level.FINEST, "Comparing target " + target + " against " + cur);
            if (cur.equals(target))            {
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

                collatorAddress = TokenParser.parseAsAddress(args[1], true);

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
            Process.done = new ArrayList<>();

            LOGGER.log(Level.FINER, "Starting listener thread");
            Process.listener = new Process.ProcessListener(Process.port);
            new Thread(Process.listener).start();
            LOGGER.log(Level.FINER, "Thread started");

            while(Process.done.size() < Process.addressList.size()) {
                Process.done.add(false);
            }
        }

        return success;
    }

    public static void main(String[] args)
    {
        LOGGER.setLevel(Level.ALL);
        if (Process.parseArgs(args))
        {
            Sender s = null;
            try
            {
                LOGGER.log(Level.FINER, "Sending READY message to Collator");
                s = new Sender(Process.collatorAddress);
                s.send(new Ready());
                LOGGER.log(Level.FINER, "READY sent");
            }
            catch (IllegalStateException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
            finally
            {
                if (s!=null)
                    s.close();
            }

        }
        else
        {
            printUsage();
        }
    }

    private static class ProcessListener extends Listener
    {
        private final ExecutorService pool;

        public ProcessListener(int port)
        {
            super(port, true);
            pool = Executors.newFixedThreadPool(Process.addressList.size());
        }

        @Override
        public void handleClient(Socket s)
        {
            pool.execute(new ProcessReceiver(s));
        }

        @Override
        protected boolean closeOther()
        {
            pool.shutdown();
            return true;
        }
    }

    private static class ProcessReceiver extends Receiver implements Runnable
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
                case DONE:
                    consecutivePayloads = 5;
                    Process.onDone(source);
                    break;
                default:
                    throw new UnsupportedOperationException("Invalid message received");
            }
        }

        @Override
        public void run()
        {
            InetAddress clientAddress = super.sock.getInetAddress();
            if (clientAddress != null)
            {
                boolean isValid =  Process.validateHost(clientAddress, VALIDATION_MODE.EXCLUDE_NONE) >= 0 ||
                        clientAddress.getCanonicalHostName().equals(Process.collatorAddress.getHostString()) ;
                if (isValid)
                {
                    LOGGER.log(Level.FINE, "Connection request is valid");
                    LOGGER.log(Level.FINER, "Starting receiver");

                    while(!shouldClose())
                    {
                        try {
                            Message m = receive();
                            handleMessage(m, super.sock.getInetAddress());
                        }
                        catch (IllegalStateException e)
                        {
                            LOGGER.log(Level.SEVERE, e.getMessage());
                            close();
                            break;
                        }
                    }

                    close();
                    LOGGER.log(Level.FINER, "Receiver completed");
                }
                else
                {
                    LOGGER.log(Level.INFO, "Connection request is invalid. Rejecting");
                    try {
                        super.sock.close();
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
}