package cs555.a2.chord.peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

final class FingerTable
{
    private List<ID> fingers;
    private HashMap<ID, PeerInfo> fingerToPeerMap;
    private final ID ownID;

    private static final BigInteger TWO = BigInteger.valueOf(2);

    public FingerTable(ID ownID)
    {
        if (ownID == null)
            throw new IllegalArgumentException("ID passed to FingerTable can't be null");

        this.ownID = ownID;
        // the following is just the initial capacity
        this.fingers = new ArrayList<>(ownID.sizeInBits());
        this.fingerToPeerMap = new HashMap<>();

        // Cache fingers to avoid calculating again
        for (int k = 0; k < ownID.sizeInBits(); k++)
            this.fingers.add(calculateFinger(k));

        this.fingers = Collections.unmodifiableList(this.fingers);

        for (int k = 0; k < this.fingers.size(); k++)
            this.fingerToPeerMap.put(this.fingers.get(k), PeerInfo.NULL_PEER);
    }

    public int size()
    {
        return fingers.size();
    }

    public ID getFinger(int k)
    {
        return fingers.get(k);
    }

    public synchronized PeerInfo getPeerInfo(int k)
    {
        return fingerToPeerMap.get(fingers.get(k));
    }

    public synchronized void setPeerInfo(int k, PeerInfo peerInfo)
    {
        fingerToPeerMap.replace(getFinger(k), peerInfo);
    }

    public synchronized void setPeerInfo(ID finger, PeerInfo peerInfo)
    {
        fingerToPeerMap.replace(finger, peerInfo);
    }

    private ID calculateFinger(int k)
    {
        if (k >= 0 && k < ownID.sizeInBits())
            return ownID.addModulo(TWO.pow(k));
        else
            throw new IllegalArgumentException("Finger " + k + " is not defined");
    }

}
