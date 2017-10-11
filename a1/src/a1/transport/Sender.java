package a1.transport;

import java.net.*;
import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import a1.messages.Message;

public final class Sender
{
	private final static Logger LOGGER = Logger.getLogger(Sender.class.getName());

	private Socket sock;
	private ObjectOutputStream outs;
	
	public Sender(InetSocketAddress addr)
	{
		try
		{
            Socket s;
            ObjectOutputStream o;
			s = new Socket(addr.getAddress(), addr.getPort());
            o = new ObjectOutputStream(s.getOutputStream());
            this.sock = s;
            this.outs = o;
		}
		catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new IllegalStateException("Unable to open the socket connection");
        }
	}

	public void send(Message m)
	{
		try
		{
			outs.writeObject(m);
		}
		catch(IOException e)
		{
			LOGGER.log(Level.SEVERE, e.toString(), e);
			throw new IllegalStateException("Failed to write to socket");
		}
	}

	public static void broadcast(Message m, List<InetSocketAddress> addList)
	{
		for(InetSocketAddress a: addList)
		{
			Sender s = null;
			try
			{
				s = new Sender(a);
				s.send(m);
			}
			catch(IllegalStateException e)
			{
				LOGGER.log(Level.WARNING, e.getMessage());
				throw e;
			}
			finally
			{
				if (s!= null)
					s.close();
			}
		}
	}

	public void close()
	{
		try
		{
			outs.close();
			sock.close();
		}
		catch(IOException e)
		{
			LOGGER.log(Level.SEVERE, e.toString(), e);
			throw new IllegalStateException("Unable to close");
		}
	}

}
