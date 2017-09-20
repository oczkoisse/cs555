package cs555.a2.transport.events;

public class MessageSent extends Event
{
    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_SENT;
    }
}
