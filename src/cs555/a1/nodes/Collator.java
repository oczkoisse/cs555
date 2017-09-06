package cs555.a1.nodes;

import cs555.a1.messages.*;
import cs555.a1.transport.*;
import cs555.a1.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public final class Collator {

    private static final Logger LOGGER = Logger.getLogger(Collator.class.getName());
    private static int port = -1;
    private static List<InetSocketAddress> addressList = null;
    private static List<Summary> summaryList = null;
    private static List<Boolean> ready = null;

    private Collator() {}

    private static boolean allReady()
    {
        synchronized (ready)
        {
            for(boolean i: ready)
                if (!i)
                    return false;
        }
        return true;
    }

    private static boolean allSummarized()
    {
        synchronized(summaryList)
        {
            for(Summary m: summaryList)
                if (m == null)
                    return false;
        }
        return true;
    }

    private enum VALIDATION_MODE {EXCLUDE_NONE, EXCLUDE_READY, EXCLUDE_SUMMARIZED};

    private static int validateHost(InetAddress addr, VALIDATION_MODE mode)
    {
        String target = addr.getCanonicalHostName();
        for(int i = 0; i < addressList.size(); i++)
        {
            if (mode == VALIDATION_MODE.EXCLUDE_READY && ready.get(i))
                continue;
            else if (mode == VALIDATION_MODE.EXCLUDE_SUMMARIZED && summaryList.get(i) != null)
                continue;
            String cur = addressList.get(i).getAddress().getCanonicalHostName();
            LOGGER.log(Level.FINEST, "Comparing target " + target + " against " + cur);
            if (cur.equals(target))
            {
                return i;
            }
        }
        return -1;
    }

    private static void onReady(InetAddress source)
    {
        int idx = validateHost(source, VALIDATION_MODE.EXCLUDE_READY);
        if (idx >= 0)
        {
            synchronized (ready)
            {
                ready.set(idx, true);
            }

            if (allReady()) {
                LOGGER.log(Level.INFO, "Initiating");
                broadcast(new Initiate());
            }
        }
        else
        {
            LOGGER.log(Level.INFO, "READY message received from unrecognized source");
        }
    }

    private static void onSummary(Summary m, InetAddress source)
    {
        int idx = validateHost(source, VALIDATION_MODE.EXCLUDE_SUMMARIZED);
        if (idx >= 0)
        {
            synchronized (summaryList)
            {
                summaryList.set(idx, m);
            }

            if(allSummarized())
            {
                printSummary();
            }
        }
    }

    private static void broadcast(Message m)
    {
        for(InetSocketAddress a: addressList)
        {
            Sender s = null;
            try
            {
                s = new Sender(a);
                s.send(m);
            }
            catch(IllegalStateException e)
            {
                LOGGER.log(Level.WARNING, e.getMessage());
                throw e;
            }
            finally {
                if (s != null)
                    s.close();
            }
        }
    }

    private static void printSummary()
    {
        int sent = 0, received = 0;
        long sentSummation = 0, receivedSummation = 0;
        String formatString = "%1$-20s %2$8d %3$8d %4$16d %5$16d";

        for(int i=0; i<Collator.summaryList.size(); i++)
        {
            System.out.println(String.format(formatString, addressList.get(i).getHostName() + ":" + addressList.get(i).getPort(),
                    summaryList.get(i).getSent(), summaryList.get(i).getReceived(),
                    summaryList.get(i).getSentSummation(), summaryList.get(i).getReceivedSummation()));
            sent += summaryList.get(i).getSent();
            received += summaryList.get(i).getReceived();
            sentSummation += summaryList.get(i).getSentSummation();
            receivedSummation += summaryList.get(i).getReceivedSummation();
        }
        System.out.println(String.format(formatString, "Sum", sent, received, sentSummation, receivedSummation));
    }

    private static void printUsage()
    {
        System.out.println("Usage: java Collator <PORT> <PATH_TO_CONFIG_FILE>");
    }

    public static boolean parseArgs(String[] args)
    {
        int port = -1;
        List<InetSocketAddress> addresses = null;

        boolean success = false;

        if (args.length == 2) {
            try {
                port = TokenParser.parseAsInt(args[0], 0, 65535);
                File f = TokenParser.parseAsPath(args[1]);
                addresses = ConfigReader.read(f.getPath(), false, true);
                if (addresses != null)
                    success = true;
            }
            catch(IllegalArgumentException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return false;
            }
        }

        if (success)
        {
            Collator.port = port;
            Collator.addressList = Collections.unmodifiableList(addresses);
            Collator.ready = new ArrayList<>();
            Collator.summaryList = new ArrayList<>();

            // Default initialize to null for consistent ordering with addressList
            while(Collator.summaryList.size() < Collator.addressList.size()) {
                Collator.ready.add(false);
                Collator.summaryList.add(null);
            }
        }

        return success;
    }

    public static void main(String[] args)
    {
        LOGGER.setLevel(Level.ALL);
        if (Collator.parseArgs(args))
        {
            LOGGER.log(Level.FINER, "Starting listener thread");
            Thread listener = new Thread(new Collator.CollatorListener(Collator.port));
            listener.start();
            LOGGER.log(Level.FINER, "Thread started");
        }
        else
        {
            printUsage();
        }
    }

    private static class CollatorListener extends Listener
    {
        public CollatorListener(int port)
        {
            super(port);
        }

        @Override
        public void handleClient(Socket s)
        {
            LOGGER.log(Level.INFO, "Received a new connection request");
            InetAddress clientAddress = s.getInetAddress();
            if (clientAddress != null)
            {
                boolean isValid = Collator.validateHost(clientAddress, VALIDATION_MODE.EXCLUDE_NONE) >= 0;
                if (isValid)
                {
                    LOGGER.log(Level.FINE, "Connection request is valid");
                    LOGGER.log(Level.FINER, "Starting receiver");

                    Collator.CollatorReceiver r = new CollatorReceiver(s);
                    r.handleMessage(r.receive(), s.getInetAddress());
                    r.close();

                    LOGGER.log(Level.FINER,  "Receiver completed");
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

    private static class CollatorReceiver extends Receiver
    {
        CollatorReceiver(Socket s)
        {
            super(s);
        }

        @Override
        public void handleMessage(Message m, InetAddress source)
        {
            LOGGER.log(Level.FINER, "Received a message");
            LOGGER.log(Level.FINEST, "Message type is " + m.getType());
            switch (m.getType()) {
                case READY:
                    Collator.onReady(source);
                    break;
                case SUMMARY:
                    Collator.onSummary((Summary) m, source);
                    break;
                default:
                    throw new UnsupportedOperationException("Invalid message received");
            }
        }
    }
}