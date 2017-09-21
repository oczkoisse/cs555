package cs555.a2.chord.messages;

import cs555.a2.transport.messenger.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LookupQuery implements Message<ChordMessageType>
{
    private String id;

    public LookupQuery(String id)
    {
        this.id = id;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeUTF(id);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.id = in.readUTF();
    }

    public String getId()
    {
        return id;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LOOKUP_QUERY;
    }

    public  LookupQuery()
    {
        this.id =  "";
    }
}
