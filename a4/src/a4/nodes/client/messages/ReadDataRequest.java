package a4.nodes.client.messages;


import a4.nodes.server.messages.ServerMessageType;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ReadDataRequest implements Message<ClientMessageType> {

    private String filename;
    private long seqNum;
    private int port;

    public ReadDataRequest(String filename, long seqNum, int port)
    {
        if (filename == null)
            throw new NullPointerException("Filename is null");
        if (seqNum < 0)
            throw new IllegalArgumentException("Sequence number is negative");

        this.filename = filename;
        this.seqNum = seqNum;
        this.port = port;
    }

    public ReadDataRequest()
    {
        this.filename = null;
        this.seqNum = -1;
        this.port = -1;
    }

    @Override
    public ClientMessageType getMessageType() {
        return ClientMessageType.READ_DATA_REQUEST;
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

    public String getFilename()
    {
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
    public Enum isRequestFor()
    {
        return ServerMessageType.READ_DATA;
    }
}
