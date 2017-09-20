package cs555.a2.discovery;

import cs555.a2.chord.PeerInfo;

public interface DiscoveryService
{
    boolean register(PeerInfo peerInfo);
    PeerInfo getAnyPeer();
    boolean deregister(PeerInfo peerInfo);
}
