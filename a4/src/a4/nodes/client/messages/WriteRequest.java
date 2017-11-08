package a4.nodes.client.messages;

import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class WriteRequest implements Message<ClientMessageType> {

    private String filename;
    private long seqNum;
    private int port;

    public WriteRequest(String filename, long seqNum, int port) {
        if (filename == null)
            throw new NullPointerException("Filename cannot be null");
        if (seqNum < 0)
            throw new IllegalArgumentException("Sequence number must be non-negative");
        this.filename = filename;
        this.seqNum = seqNum;
        this.port = port;
    }

    public WriteRequest() {
        this.filename = null;
        this.seqNum = -1;
        this.port = -1;
    }

    @Override
    public ClientMessageType getMessageType() {
        return ClientMessageType.WRITE_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(filename);
        out.writeLong(seqNum);
        out.writeInt(port);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.filename = in.readUTF();
        this.seqNum = in.readLong();
        this.port = in.readInt();
    }

    public String getFilename() {
        return filename;
    }

    public long getSeqNum()
    {
        return seqNum;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public long getUID()
    {
        return 100;
    }
}
