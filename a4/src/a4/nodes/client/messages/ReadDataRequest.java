package a4.nodes.client.messages;


import a4.nodes.server.messages.ServerMessageType;
import a4.transport.Message;
import a4.transport.Request;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ReadDataRequest extends Request<ClientMessageType> {

    private String filename;
    private long seqNum;

    public ReadDataRequest(String filename, long seqNum)
    {
        if (filename == null)
            throw new NullPointerException("Filename is null");
        if (seqNum < 0)
            throw new IllegalArgumentException("Sequence number is negative");

        this.filename = filename;
        this.seqNum = seqNum;
    }

    public ReadDataRequest()
    {
        this.filename = null;
        this.seqNum = -1;
    }

    @Override
    public ClientMessageType getMessageType() {
        return ClientMessageType.READ_DATA_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(filename);
        out.writeLong(seqNum);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.filename = in.readUTF();
        this.seqNum = in.readLong();
    }

    public String getFilename()
    {
        return filename;
    }

    public long getSeqNum()
    {
        return seqNum;
    }
}
