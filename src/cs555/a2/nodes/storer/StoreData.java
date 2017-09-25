package cs555.a2.nodes.storer;

import cs555.a2.transport.messenger.Messenger;

import java.util.logging.Logger;

public class StoreData
{
    private static final Logger LOGGER = Logger.getLogger(StoreData.class.getName());
    private Messenger messenger = new Messenger(1);

    public static void printUsage()
    {
        System.out.println("Usage: " + StoreData.class.getCanonicalName() + " <DiscoveryHost:Port>");
    }

    public static void main(String args[])
    {

    }
}
