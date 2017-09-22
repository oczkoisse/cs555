package cs555.a2.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Allows to listen for incoming connections. This is meant as a base class for creating higher
 * level functionality (for example, a threaded listener).
 */
public class Listener  {

	private ServerSocket sock;
	/**
	 * Create a Listener instance to listen on {@code port}, reusing the port if {@code reuse} is {@code true}
	 * @param port port number to listen on for incoming connections
	 * @param reuse if {@code true}, the port is reused
	 */
	public Listener(int port, boolean reuse)
	{
		try
		{
			sock = new ServerSocket(port);
			if (reuse)
				sock.setReuseAddress(reuse);
		}
		catch(IOException e)
		{
			sock = null;
		}
	}

	/**
	 * Create a Listener instance to listen on {@code port} by reusing the port if needed
	 * @param port port number to listen on for incoming connections
	 * @throws IllegalStateException if the {@code Listener} is unable to initialize the server socket
	 */
	public Listener(int port)
	{
		this(port, false);
	}

	/**
	 * Create a Listener instance to listen on any available port, reusing it if needed
	 * @throws IllegalStateException if the {@code Listener} is unable to initialize the server socket
	 */
	public Listener()
	{
		this(0);
	}

	/**
	 * Accept a new connection. Waits until a new connection is received.
	 * @return If successful, a {@code Socket} representing the new connection. Otherwise, {@code null} is returned
	 * if {@coee Listener} is closed while still waiting.
	 * @throws IllegalStateException if {@link Listener} is unable to initialize the server socket
	 * @throws IOException if an I/O error occurs when waiting for a connection
	 */
	public Socket accept() throws IOException
    {
    	if (sock == null)
    		throw new IllegalStateException("accept() called on an uninitialized Listener");
    	try
		{
			return sock.accept();
		}
		catch(SocketException e)
		{
			// Means the Listener is closed
			return null;
		}
    }


	/**
	 * Closes the {@code Listener} instance.
	 * @throws IOException if an I/O error occurs when closing the socket.
	 */
	public void close() throws IOException
    {
        if (sock != null && !sock.isClosed())
            sock.close();
    }

	/**
	 * Check if the {@code Listener} is already closed
	 * @return {@code true} if already closed, else {@code false}
	 */
	public boolean isClosed()
    {
    	return sock == null || sock.isClosed();
    }
}
