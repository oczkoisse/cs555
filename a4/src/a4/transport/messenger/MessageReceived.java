package a4.transport.messenger;

import a4.transport.Message;

public abstract class MessageReceived extends Event {

    private Message msg;

    protected boolean setMessage(Message msg)
    {
        if (msg == null)
            throw new NullPointerException("Message is null");
        if (this.msg == null)
        {
            this.msg = msg;
            return true;
        }
        return false;
    }

    /**
     * Retrieves the received message
     * @return {@link Message} representing the received message
     */
    protected Message getMessage()
    {
        if (this.msg == null)
            throw new IllegalStateException("getNotification() called with no set message");
        return msg;
    }
}
