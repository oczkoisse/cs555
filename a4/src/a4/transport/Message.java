package a4.transport;


import a4.transport.messenger.Messenger;

/**
 * Base interface for implementing messages for use by {@link Messenger}
 * @param <E> An enumeration representing the types of messages. This is the type returned by {@link #getMessageType()}
 */
public interface Message<E extends Enum<E>> extends java.io.Externalizable
{
    /**
     * Returns the underlying enumeration type of the message.
     * @return
     */
    E getMessageType();

    default Enum isResponseTo()
    {
        return null;
    }

    default Enum isRequestFor()
    {
        return null;
    }
}
