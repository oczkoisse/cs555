package cs555.a1.messages;

public abstract class Message implements java.io.Externalizable {
    protected  MessageType mType;

    public MessageType getType()
    {
        return mType;
    }
}
