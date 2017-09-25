package cs555.a2.nodes.discoverer;

import cs555.a2.chord.discovery.DiscoveryService;
import cs555.a2.chord.peer.ID;
import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.hash.Hash;

import java.util.Hashtable;
import java.util.Random;

public class BasicDiscoveryService implements DiscoveryService
{
    private Hashtable<ID, PeerInfo> registeredPeers = new Hashtable<>();
    private Random rnd = new Random();

    @Override
    public PeerInfo getAnyPeer(PeerInfo source)
    {
        if (registeredPeers.size() == 0)
            throw new IllegalStateException("getAnyPeer() called when no nodes are registered with discovery service");

        PeerInfo result;
        do
        {
            // Number of peers are limited to 2^32
            int randomIndex = rnd.nextInt(registeredPeers.size());
            // toArray() may be expensive when the number of peers grow large
            // so the actual number of peers possible may be even lesser
            result = (PeerInfo) registeredPeers.values().toArray()[randomIndex];
        }
        while(registeredPeers.size() > 1 && result.getID().compareTo(source.getID()) == 0);

        return result;
    }

    public boolean register(PeerInfo peerInfo)
    {
        if (!isRegistered(peerInfo)) {
            registeredPeers.put(peerInfo.getID(), peerInfo);
            return true;
        }
        return false;
    }

    public boolean isRegistered(PeerInfo peerInfo)
    {
        return registeredPeers.containsKey(peerInfo.getID());
    }

    public boolean deregister(PeerInfo peerInfo)
    {
        if (isRegistered(peerInfo)) {
            registeredPeers.remove(peerInfo.getID());
            return true;
        }
        return false;
    }
}
