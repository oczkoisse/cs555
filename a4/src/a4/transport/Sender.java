package a4.transport;

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
	public static void send(Notification msg, InetSocketAddress destination) throws SenderException
	{
		try (Socket sock = new Socket(destination.getAddress(), destination.getPort());
			 ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream()))
		{
			outs.writeObject(msg);
		}
		catch(IOException ex)
		{
			throw new SenderException(ex);
		}
	}

	/**
	 * Opens a connection to the specified {@code destination} specified as an IP address (host:port),
	 * sends the message, and closes the connection
	 * @param destination {@code Socket} representing the destination to which the message should be sent
	 * @throws IOException if unable to open a socket connection to destination, or
	 * if there is an I/O error while sending the message
	 */
	public static void send(Notification msg, Socket destination) throws SenderException
	{
		try (Socket sock = destination;
			 ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream()))
		{
			outs.writeObject(msg);
		}
		catch(IOException ex)
		{
			throw new SenderException(ex);
		}
	}

	public static Socket sendAndThen(Request request, InetSocketAddress destination) throws SenderException
	{
		try {
			Socket sock = new Socket(destination.getAddress(), destination.getPort());
			ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream());
			outs.writeObject(request);
			return sock;
		}
		catch(IOException ex)
		{
			throw new SenderException(ex);
		}
	}

	public static Socket sendAndThen(Request request, Socket sock) throws SenderException
	{
		try {
			ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream());
			outs.writeObject(request);
			return sock;
		}
		catch(IOException ex)
		{
			throw new SenderException(ex);
		}
	}

    /**
     * Broadcasts a one-time {@code Notification} to all the IP Addresses specified in {@code destinations}.
     * @param destinations A list of {@code InetSocketAddress} representing the destinations to which the message
     *                    should be sent
     */
	public static void broadcast(Notification msg, List<InetSocketAddress> destinations) throws SenderException
	{
		for(InetSocketAddress destination: destinations)
		{
			send(msg, destination);
		}
	}
}
