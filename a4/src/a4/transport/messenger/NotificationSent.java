package a4.transport.messenger;

import a4.transport.Notification;

import java.net.InetSocketAddress;

/**
 * An {@link Event} representing that a Notification has been sent by {@link Messenger}
 */
public class NotificationSent extends Event
{
    private Notification msg = null;
    private InetSocketAddress destination = null;

    NotificationSent(Notification msg, InetSocketAddress destination)
    {
        if (msg == null)
            throw new NullPointerException("Notification is null");
        if (destination == null)
            throw new NullPointerException("Destination is null");
        this.msg = msg;
        this.destination = destination;
    }

    /**
     * Retrives the sent notification
     * @return {@link Notification} representing the sent notification
     */
    public Notification getNotification()
    {
        return msg;
    }

    public InetSocketAddress getDestination()
    {
        return destination;
    }

    /**
     * Returns the event type
     * @return {@link EventType#NOTIFICATION_SENT}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.NOTIFICATION_SENT;
    }

}
