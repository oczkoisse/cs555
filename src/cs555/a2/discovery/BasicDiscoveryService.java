package cs555.a2.discovery;

import cs555.a2.chord.PeerInfo;
import cs555.a2.hash.Hash;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Random;

public class BasicDiscoveryService implements DiscoveryService
{
    private Hashtable<BigInteger, PeerInfo> registeredPeers = new Hashtable<>();
    private Random rnd = new Random();
    private Hash hash;

    public BasicDiscoveryService(Hash hash)
    {
        this.hash = hash;
    }

    @Override
    public boolean register(PeerInfo peerInfo)
    {
        if (!isRegistered(peerInfo)) {
            registeredPeers.put(peerInfo.getId(), peerInfo);
            return true;
        }
        return false;
    }

    private boolean isRegistered(PeerInfo peerInfo)
    {
        return registeredPeers.containsKey(peerInfo.getId());
    }

    @Override
    public PeerInfo getAnyPeer()
    {
        // Number of peers are limited to 2^32
        int randomIndex = rnd.nextInt(registeredPeers.size());
        // toArray() may be expensive when the number of peers grow large
        // so the actual number of peers possible may be even lesser
        return (PeerInfo) registeredPeers.values().toArray()[randomIndex];
    }

    @Override
    public boolean deregister(PeerInfo peerInfo)
    {
        if (isRegistered(peerInfo)) {
            registeredPeers.remove(peerInfo.getId());
            return true;
        }
        return false;
    }
}
