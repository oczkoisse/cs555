package a4.nodes.server.messages;

import a4.chunker.Slice;
import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

public class RecoveryData extends Notification<ServerMessageType> {

    private String filename;
    private long sequenceNum;
    private Map<Integer, Slice> recoveryData;

    public RecoveryData()
    {
        this.filename = null;
        this.sequenceNum = -1;
        this.recoveryData = null;
    }

    public RecoveryData(String filename, long sequenceNum, List<Integer> failedSlices, List<Slice> correctedSlices)
    {
        if (filename == null)
            throw new NullPointerException("Filename is null");
        if (failedSlices == null || failedSlices.size() == 0)
            throw new IllegalArgumentException("Failed slice list is invalid");
        if (correctedSlices == null || correctedSlices.size() == 0)
            throw new IllegalArgumentException("Corrected slice list is invalid");
        if (correctedSlices.size() != failedSlices.size())
            throw new IllegalArgumentException("Size mismtach between hasReplica slices and corrected slices lists");
        if (sequenceNum < 0)
            throw new IllegalArgumentException("Sequence number is negative");



        this.filename = filename;
        this.sequenceNum = sequenceNum;
        this.recoveryData = new HashMap<>();
        for(int i: failedSlices)
        {
            Object result = recoveryData.putIfAbsent(failedSlices.get(i), correctedSlices.get(i));
            if (result != null)
                throw new IllegalArgumentException("Repeated elements in hasReplica slices list");
        }
    }


    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RECOVERY_DATA;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(filename);
        out.writeLong(sequenceNum);

        out.writeInt(recoveryData.size());
        for(Map.Entry<Integer, Slice> e: recoveryData.entrySet())
        {
            out.writeInt(e.getKey());
            out.writeObject(e.getValue());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.filename = in.readUTF();
        this.sequenceNum = in.readLong();

        if (recoveryData == null)
            recoveryData = new HashMap<>();
        else
            recoveryData.clear();

        int sz = in.readInt();
        for(int i=0; i<sz; i++)
        {
            recoveryData.put(in.readInt(), (Slice) in.readObject());
        }
    }

    public Set<Map.Entry<Integer, Slice>> getRecoveryData()
    {
        return Collections.unmodifiableSet(recoveryData.entrySet());
    }

    public String getFilename()
    {
        return filename;
    }

    public long getSequenceNum()
    {
        return  sequenceNum;
    }
}
