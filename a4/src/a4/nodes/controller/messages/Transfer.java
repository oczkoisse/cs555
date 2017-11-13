package a4.nodes.controller.messages;

import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class Transfer extends Notification<ControllerMessageType> {

    private String filename;
    private long sequenceNum;
    private InetSocketAddress destination;

    public Transfer(String filename, long seqNum, InetSocketAddress destination)
    {
        if (filename == null)
            throw new NullPointerException("Filename is null");
        if (seqNum < 0)
            throw new IllegalArgumentException("Sequence number is negative");
        if (destination == null)
            throw new NullPointerException("Destination is null");
        this.filename = filename;
        this.sequenceNum = seqNum;
        this.destination = destination;
    }

    public Transfer()
    {
        this.filename = null;
        this.sequenceNum = -1;
        this.destination = null;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.TRANSFER;
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
