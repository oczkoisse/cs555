package cs555.a2.transport.messenger;

public class MessageSent extends Event
{
    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_SENT;
    }
}
