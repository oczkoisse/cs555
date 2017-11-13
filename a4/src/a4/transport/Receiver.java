package a4.transport;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Packages the functionality to receive a one-time message. As such, it always tries to close the
 * connection as soon as the message is successfully received.
 */
public final class Receiver
{
    private Receiver() {}

    /**
     * Opens a connection to the specified {@code source} specified as an IP address (host:port),
     * receives the message, and closes the connection
     * @param source {@code InetSocketAddress} representing a source from which to receive the message
     * @return {@code Message} denoting the received message
     */
    public  static Notification receive(InetSocketAddress source) throws ReceiverException
    {
        try (Socket sock = new Socket(source.getAddress(), source.getPort());
             ObjectInputStream ins = new ObjectInputStream(sock.getInputStream()))
        {
            return (Notification) ins.readObject();
        }
        catch(IOException | ClassNotFoundException ex)
        {
            throw new ReceiverException(ex);
        }
    }

    /**
     * Receive a message from the connection represented by the {@code Socket} instance, and closes the connection
     * @param sock {@code Socket} representing a source from which to receive the message
     * @return {@code Message} denoting the received message
     */
    public static Notification receive(Socket sock) throws ReceiverException
    {
        try (Socket s = sock;
             ObjectInputStream ins = new ObjectInputStream(s.getInputStream()))
        {
            return (Notification) ins.readObject();
        }
        catch(IOException | ClassNotFoundException ex)
        {
            throw new ReceiverException(ex);
        }
    }

    public static Message receiveAndThen(Socket sock) throws ReceiverException
    {
        try
        {
            ObjectInputStream ins = new ObjectInputStream(sock.getInputStream());
            return (Message) ins.readObject();
        }
        catch(IOException | ClassNotFoundException ex)
        {
            throw new ReceiverException(ex);
        }
    }

}
