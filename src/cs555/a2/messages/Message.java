package cs555.a2.messages;

public abstract class Message implements java.io.Externalizable {
    protected  MessageType mType;

    public MessageType getType()
    {
        return mType;
    }
}