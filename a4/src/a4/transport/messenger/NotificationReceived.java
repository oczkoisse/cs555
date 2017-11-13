package a4.transport.messenger;

import a4.transport.Notification;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * An {@link Event} representing that a new notification has been received by {@link Messenger}
 */
public class NotificationReceived extends MessageReceived
{
    private Notification msg = null;
    private InetSocketAddress source = null;

    NotificationReceived(InetSocketAddress source)
    {
        if (source == null)
            throw new NullPointerException("Source address is null");
        this.source = source;
    }

    NotificationReceived(Socket sock)
    {
        if (sock == null)
            throw new NullPointerException("Socket is null");
        this.source = new InetSocketAddress(sock.getInetAddress().getHostAddress(), sock.getPort());
    }

    /**
     * Returns the event type
     * @return {@link EventType#NOTIFICATION_RECEIVED}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.NOTIFICATION_RECEIVED;
    }

    boolean setNotification(Notification msg)
    {
        return super.setMessage(msg);
    }

    /**
     * Retrives the received notification
     * @return {@link Notification} representing the received message
     */
    public Notification getNotification()
    {
        return (Notification) super.getMessage();
    }

    public InetSocketAddress getSource()
    {
        return source;
    }
}
