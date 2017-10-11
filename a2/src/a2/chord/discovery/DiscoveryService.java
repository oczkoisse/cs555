package a2.chord.discovery;

import a2.chord.peer.PeerInfo;

public interface DiscoveryService
{
    PeerInfo getAnyPeer(PeerInfo source);
}
