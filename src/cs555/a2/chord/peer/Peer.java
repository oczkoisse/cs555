package cs555.a2.chord.peer;

import cs555.a2.chord.peer.messages.*;
import cs555.a2.transport.Message;
import cs555.a2.transport.messenger.Messenger;

public abstract class Peer
{
    private PeerInfo ownInfo;
    private PeerInfo predecessor;
    private FingerTable fingerTable;
    private Messenger messenger;

    public Peer(PeerInfo info)
    {
        this.ownInfo = info;
        this.fingerTable = new FingerTable(info);
        this.predecessor = null;
        this.messenger = new Messenger(info.getListeningAddress().getPort(), 4);
    }

    public PeerInfo lookupSuccessor(ID id)
    {

    }

    public PeerInfo lookupPredecessor(ID id)
    {
        ID ownID = ownInfo.getID();
        ID successorID = getSuccessor().getID();

        // If id is not in interval (ID, successor.ID]
        if(!(id.compareTo(successorID) == 0 || id.inInterval(ownID, getSuccessor().getID())))
        {
            PeerInfo result = getClosestNodePrecedingId(id);
            return result;
        }
        return ownInfo;
    }

    private PeerInfo getClosestNodePrecedingId(ID id)
    {
        // Start at the bottom of the finger table
        int k = fingerTable.size();
        // Continue up until we get to a finger whose successor is in interval (ownID, id)
        // Basically this means it is trying to find a node that is closer to 'id' than it is
        // Starting from the bottom means it is trying to find the closest one among such nodes
        while (k > 0 && !fingerTable.getPeerInfo(--k).getID().inInterval(ownInfo.getID(), id));

        // If we went through all the finger table without finding a closer node
        // then this node is the predecessor of successor of 'id'
        if (k == 0)
            return ownInfo;
        else
            return fingerTable.getPeerInfo(k);
    }

    public PeerInfo getSuccessor()
    {
        return fingerTable.getPeerInfo(0);
    }

    protected void setSuccessor(PeerInfo successor) {
        fingerTable.setPeerInfo(0, successor);
    }

    public PeerInfo getPredecessor()
    {
        return predecessor;
    }

    public void join(PeerInfo anotherPeer)
    {
        messenger.send(new LookupRequest(ownInfo.getID(), ownInfo, true), anotherPeer.getListeningAddress());
    }

    private void handlePredRequestMsg(PredecessorRequest msg)
    {

    }

    public void handleMessage(Message<ChordMessageType> msg)
    {
        switch(msg.getMessageType())
        {
            case PRED_REQUEST:
                handlePredRequestMsg((PredecessorRequest) msg);
                break;
            case PRED_RESULT:
                handlePredResultMsg((PredecessorResult) msg);
                break;
            case LOOKUP_REQUEST:
                handleLookupRequestMsg((LookupRequest) msg);
                break;
            case LOOKUP_RESULT:
                handleLookupResultMsg((LookupResult) msg);
                break;
            case PRED_UPDATE:
                handlePredUpdateMsg((PredecessorUpdate) msg);
                break;
        }
    }

    private void handlePredUpdateMsg(PredecessorUpdate msg)
    {
        PeerInfo latestPredecessor = msg.getLatestPredecessor();
        if (predecessor == null || latestPredecessor.getID().inInterval(predecessor.getID(), ownInfo.getID()))
        {
            predecessor = latestPredecessor;
        }
    }

    private abstract void handleLookupResultMsg(LookupResult msg);

    private void handleLookupRequestMsg(LookupRequest msg)
    {


    }


    private void handlePredResultMsg(PredecessorResult msg)
    {

    }
}
