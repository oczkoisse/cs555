package cs555.a2.transport;

import cs555.a2.transport.messenger.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * {@code Sender} packages the functionality to send a one-time message. As such, it always tries to close the
 * connection as soon as the message is successfully sent.
 */
public final class Sender
{

    private Sender() {}

    /**
     * Opens a connection to the specified {@code destination} specified as an IP address (host:port),
     * sends the message, and closes the connection
     * @param destination {@code InetSocketAddress} representing the destination to which the message should be sent
     * @throws IOException if unable to open a socket connection to destination, or
     * if there is an I/O error while sending the message
     */
	public static void send(Message msg, InetSocketAddress destination) throws IOException
	{
		try (Socket sock = new Socket(destination.getAddress(), destination.getPort());
			 ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream()))
		{
			outs.writeObject(msg);
		}
	}

    /**
     * Broadcasts a one-time {@code Message} to all the IP Addresses specified in {@code destinations}.
     * Uses {@link #send(Message, InetSocketAddress)} to send these messages.
     * @param destinations A list of {@code InetSocketAddress} representing the destinations to which the message
     *                    should be sent
     * @throws IOException if unable to open a socket connection to any of the destinations, or
     * if there is an I/O error while sending the message to any of them
     */
	public static void broadcast(Message msg, List<InetSocketAddress> destinations) throws IOException
	{
		for(InetSocketAddress destination: destinations)
		{
			send(msg, destination);
		}
	}
}
