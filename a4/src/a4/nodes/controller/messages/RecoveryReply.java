package a4.nodes.controller.messages;

import a4.nodes.server.messages.ServerMessageType;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class RecoveryReply implements Message<ControllerMessageType> {
    private List<InetSocketAddress> replicas;
    private String filename;
    private long sequenceNum;

    public RecoveryReply()
    {
        this.replicas = null;
        this.filename = null;
        this.sequenceNum = -1;
    }

    public RecoveryReply(String filename, long seqNum, List<InetSocketAddress> replicas)
    {
        if (replicas == null)
            throw new NullPointerException("Replica is null");
        else if(replicas.size() == 0)
            throw new IllegalArgumentException("Replicas size is 0");

        if (seqNum < 0)
            throw new IllegalArgumentException("Sequence number must be non-negative");
        if (filename == null)
            throw new NullPointerException("Filename is null");


        this.replicas = replicas;
        this.filename = filename;
        this.sequenceNum = seqNum;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.RECOVERY_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(replicas.size());
        for(InetSocketAddress addr: replicas)
            out.writeObject(addr);

        out.writeUTF(filename);
        out.writeLong(sequenceNum);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (replicas == null)
            replicas = new ArrayList<>();
        else
            replicas.clear();

        int sz = in.readInt();
        for(int i=0; i<sz; i++)
            this.replicas.add((InetSocketAddress) in.readObject());

        this.filename = in.readUTF();
        this.sequenceNum = in.readLong();
    }

    public List<InetSocketAddress> getReplicas() {
        return this.replicas;
    }

    public String getFileName()
    {
        return this.filename;
    }

    public long getSequenceNum()
    {
        return this.sequenceNum;
    }


    @Override
    public Enum isResponseTo()
    {
        return ServerMessageType.RECOVERY_REQUEST;
    }
}