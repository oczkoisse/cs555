package cs555.a2.nodes.discoverer;

import cs555.a2.discovery.BasicDiscoveryService;
import cs555.a2.hash.Hash;
import cs555.a2.transport.messenger.Messenger;

public class Discoverer extends BasicDiscoveryService
{
    private Messenger messenger;

    public Discoverer(Hash hash, int listeningPort)
    {
        super(hash);
        this.messenger = new Messenger(listeningPort, 1);
    }
}
