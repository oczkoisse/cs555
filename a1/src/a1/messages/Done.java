package a1.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Done extends Message {

    public Done()
    {
        super.mType = MessageType.DONE;
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
