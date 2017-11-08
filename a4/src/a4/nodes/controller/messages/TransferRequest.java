package a4.nodes.controller.messages;


import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class TransferRequest implements Message<ControllerMessageType> {
    private String filename;
    private long sequenceNum;
    private InetSocketAddress destination;

    public TransferRequest(String filename, long seqNum, InetSocketAddress destination)
    {
        if (filename == null || seqNum < 0 || destination == null)
            throw new IllegalArgumentException("Invalid arguments passed to TransferRequest");
        this.filename = filename;
        this.sequenceNum = seqNum;
        this.destination = destination;
    }

    public TransferRequest()
    {
        this.filename = null;
        this.sequenceNum = -1;
        this.destination = null;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.TRANSFER_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(filename);
        out.writeLong(sequenceNum);
        out.writeObject(destination);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.filename = in.readUTF();
        this.sequenceNum = in.readLong();
        this.destination = (InetSocketAddress) in.readObject();
    }

    public String getFilename()
    {
        return filename;
    }

    public long getSequenceNum()
    {
        return sequenceNum;
    }

    public InetSocketAddress getDestination() {
        return destination;
    }
}
