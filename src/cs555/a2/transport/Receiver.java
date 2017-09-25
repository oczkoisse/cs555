package cs555.a2.transport;

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
     * @throws IOException if unable to open a socket connection to source, or
     * if there is an I/O error while receiving the message
     * @throws ClassNotFoundException if unable to resolve the received message as a valid {@code Message} instance
     */
    public  static Message receive(InetSocketAddress source) throws IOException, ClassNotFoundException
    {
        try (Socket sock = new Socket(source.getAddress(), source.getPort());
             ObjectInputStream ins = new ObjectInputStream(sock.getInputStream()))
        {
            return (Message) ins.readObject();
        }
    }

    /**
     * Receive a message from the connection represented by the {@code Socket} instance, and closes the connection
     * @param sock {@code Socket} representing a source from which to receive the message
     * @return {@code Message} denoting the received message
     * @throws IOException if there is an I/O error while receiving the message
     * @throws ClassNotFoundException if unable to resolve the received message as a valid {@code Message} instance
     */
    public static Message receive(Socket sock) throws IOException, ClassNotFoundException
    {
        try (Socket s = sock;
             ObjectInputStream ins = new ObjectInputStream(sock.getInputStream()))
        {
            return (Message) ins.readObject();
        }
    }
}
