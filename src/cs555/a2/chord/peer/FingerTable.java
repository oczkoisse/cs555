package cs555.a2.chord.peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class FingerTable
{
    private final List<ID> fingers;
    private HashMap<ID, PeerInfo> fingerToPeerMap;
    private final PeerInfo owner;

    private static final BigInteger TWO = BigInteger.valueOf(2);

    public FingerTable(PeerInfo owner)
    {
        this.owner = owner;
        this.fingers = new ArrayList<>(owner.getID().sizeInBits());

        // Cache fingers to avoid calculating again
        for (int k = 0; k < this.fingers.size(); k++)
            this.fingers.add(calculateFinger(k));

        for (int k = 0; k < this.fingers.size(); k++)
            this.fingerToPeerMap.put(this.fingers.get(k), owner);
    }

    public int size()
    {
        return fingers.size();
    }

    public ID getFinger(int k)
    {
        return fingers.get(k);
    }

    public PeerInfo getPeerInfo(int k)
    {
        return fingerToPeerMap.get(fingers.get(k));
    }

    public void setPeerInfo(int k, PeerInfo peerInfo)
    {
        fingerToPeerMap.replace(getFinger(k), peerInfo);
    }

    private ID calculateFinger(int k)
    {
        if (k >= 0 && k < this.size())
            return owner.getID().addModulo(TWO.pow(k));
        else
            throw new IllegalArgumentException("Finger " + k + " is not defined");
    }

}
