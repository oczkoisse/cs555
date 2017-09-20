package cs555.a2.chord;

import java.math.BigInteger;
import java.util.HashMap;

public abstract class Peer
{
    private PeerInfo predecessor;
    private HashMap<BigInteger, PeerInfo> fingerTable;

    public abstract PeerInfo discover();



}
