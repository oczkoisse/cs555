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

    public PeerInfo getPeerInfo(int k)
    {
        synchronized (fingerToPeerMap)
        {
            return fingerToPeerMap.get(fingers.get(k));
        }
    }

    public void setPeerInfo(int k, PeerInfo peerInfo)
    {
        synchronized (fingerToPeerMap)
        {
            // Rest is an optimization to speed up stabilization
            if (fingerToPeerMap.replace(getFinger(k), peerInfo) != null && peerInfo != PeerInfo.NULL_PEER)
            {
                ID id = peerInfo.getID();
                for(int i = k+1; i < size(); i++)
                {
                    if(getFinger(i).inInterval(ownID, id) || getFinger(i).compareTo(id) == 0)
                        setPeerInfo(i, peerInfo);
                }
            }
        }
    }

    public void setPeerInfo(ID finger, PeerInfo peerInfo)
    {
        int idx = fingers.indexOf(finger);
        if (idx >= 0)
            setPeerInfo(idx, peerInfo);
    }

    public void setSuccessor(PeerInfo successor) {
        synchronized (fingerToPeerMap)
        {
            setPeerInfo(0, successor);
        }
    }

    public PeerInfo getSuccessor()
    {
        return getPeerInfo(0);
    }


    private ID calculateFinger(int k)
    {
        if (k >= 0 && k < ownID.sizeInBits())
            return ownID.addModulo(TWO.pow(k));
        else
            throw new IllegalArgumentException("Finger " + k + " is not defined");
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%n"));
        final String formatString = "%1$3d (%2$" + (ownID.sizeInBits()/4 + 1) + "s): %3$s (%4$s)%n";
        synchronized (fingerToPeerMap)
        {
            for(int k=0; k<size(); k++)
            {
                stringBuilder.append(String.format(formatString, k, getFinger(k), getPeerInfo(k).getName(), getPeerInfo(k).getID()));
            }
        }
        return stringBuilder.toString();
    }

}
