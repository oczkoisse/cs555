package a2.chord.peer.messages;

import a2.chord.peer.PeerInfo;
import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LastGaspPredecessor implements Message<ChordMessageType>
{
    private PeerInfo predecessor;
    private List<DataItem> dataItemList;

    public LastGaspPredecessor()
    {
        this.predecessor = PeerInfo.NULL_PEER;
        this.dataItemList = new ArrayList<>();
    }

    public LastGaspPredecessor(PeerInfo pred, List<DataItem> dataItemList)
    {
        this.predecessor = pred;
        this.dataItemList = dataItemList != null ? dataItemList : new ArrayList<>();
    }


    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LAST_GASP_PRED;
    }


    public PeerInfo getPredecessor()
    {
        return predecessor;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.predecessor == null || this.predecessor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with predecessor set to NULL_PEER");

        out.writeObject(predecessor);
        out.writeInt(dataItemList.size());
        for(DataItem d: dataItemList)
            out.writeObject(d);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.predecessor = (PeerInfo) in.readObject();
        int size = in.readInt();
        for(int i = 0; i < size; i++)
            dataItemList.add((DataItem) in.readObject());
    }

    public List<DataItem> getDataItemList()
    {
        return Collections.unmodifiableList(this.dataItemList);
    }
}
