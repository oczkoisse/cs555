package cs555.a2.chord;

import cs555.a2.discovery.DiscoveryService;
import cs555.a2.hash.Hash;

import java.util.HashMap;

public abstract class Peer
{
    private PeerInfo predecessor;
    private HashMap<PeerId, PeerInfo> fingerTable;
    private DiscoveryService discoveryService;
    private Hash hash;

    public abstract PeerInfo discover();

    public PeerInfo findSuccessor(PeerId id)
    {
        return null;
    }

    public PeerInfo findPredecessor(PeerId id)
    {
        return null;
    }

    public PeerInfo findClosestFingerPrecedingId(PeerId id)
    {
        return null;
    }
}
