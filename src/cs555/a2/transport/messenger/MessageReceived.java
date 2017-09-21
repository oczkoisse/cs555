package cs555.a2.transport.messenger;

public class MessageReceived extends Event
{
    private Message msg = null;

    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_RECEIVED;
    }

    public Message getMessage()
    {
        return msg;
    }

    public boolean setMessage(Message msg)
    {
        if (msg != null && this.msg == null) {
            this.msg = msg;
            return true;
        }
        return false;
    }
}
