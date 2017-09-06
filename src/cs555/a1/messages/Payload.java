package cs555.a1.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Payload extends Message {

    private int data = 0;

    public Payload()
    {
        super.mType = MessageType.PAYLOAD;
    }

    public Payload(int data)
    {
        this.data = data;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.data = in.readInt();
    }

    public int getData()
    {
        return data;
    }
}
