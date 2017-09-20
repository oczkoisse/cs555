package cs555.a2.transport;

import cs555.a2.transport.events.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Sender
{
	public static void send(Message msg, InetSocketAddress destination) throws IOException
	{
		try (Socket sock = new Socket(destination.getAddress(), destination.getPort());
			 ObjectOutputStream outs = new ObjectOutputStream(sock.getOutputStream()))
		{
			outs.writeObject(msg);
		}
		catch(IOException e)
		{
		    throw e;
		}
	}

	public static void broadcast(Message msg, List<InetSocketAddress> destinations) throws IOException
	{
		for(InetSocketAddress destination: destinations)
		{
			send(msg, destination);
		}
	}
}
