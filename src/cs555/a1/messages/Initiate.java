package cs555.a1.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Initiate extends Message {

    public Initiate()
    {
        super.mType = MessageType.INITIATE;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {

    }
}
