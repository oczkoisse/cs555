package cs555.a2.discovery;

import cs555.a2.chord.PeerInfo;

public interface DiscoveryService
{
    PeerInfo getAnyPeer();
}
