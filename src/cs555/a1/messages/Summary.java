package cs555.a1.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Summary extends Message {

    private int sent = 0;
    private int received = 0;
    private long sentSummation = 0;
    private long receivedSummation = 0;

    public Summary()
    {
        super.mType = MessageType.SUMMARY;
    }

    public Summary(int sent, int received, long sentSummation, long receivedSummation)
    {
        this.sent = sent;
        this.received = received;
        this.sentSummation = sentSummation;
        this.receivedSummation = receivedSummation;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(sent);
        out.writeInt(received);
        out.writeLong(sentSummation);
        out.writeLong(receivedSummation);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.sent = in.readInt();
        this.received = in.readInt();
        this.sentSummation = in.readLong();
        this.receivedSummation = in.readLong();
    }

    public int getSent()
    {
        return sent;
    }

    public int getReceived()
    {
        return received;
    }

    public long getSentSummation()
    {
        return sentSummation;
    }

    public long getReceivedSummation()
    {
        return receivedSummation;
    }
}
