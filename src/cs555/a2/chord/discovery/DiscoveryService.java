package cs555.a2.chord.discovery;

import cs555.a2.chord.peer.PeerInfo;

public interface DiscoveryService
{
    PeerInfo getAnyPeer(PeerInfo source);
}
