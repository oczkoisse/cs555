package cs555.a2.transport.messenger;

import cs555.a2.transport.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A high-level class for managing all three functions necessary for network communications.
 * It listens on a specified port for new connections, and also accepts asynchronous requests for
 * sending and receiving messages. All messages are sent/received on a one-time basis i.e. connections created are
 * transient and limited to one message per connection.
 */
public class Messenger
{
    private static final Logger LOGGER = Logger.getLogger(Messenger.class.getName());

    // Manager for underlying fixed size thread pool
    private ExecutorService executorService;
    // Completion wrapper around the thread pool to block until events are generated
    private ExecutorCompletionService<Event> executorCompletionService;
    // Listener instance for actually doing the listening for new connections
    private Listener listener;

    private boolean listening;

    /**
     * Creates a new instance for listening on port as given by {@code listeningPort} with
     * {@code nThreadsForMessaging} representing the number of threads that manage sending/receiving requests.
     * Note that this does not actually start listening. Call {@link #listen()} for that.
     * @param listeningPort the port to listen on for incoming connections
     * @param nThreadsForMessaging the number of threads to be reserved for sending/receiving messages
     */
    public Messenger(int listeningPort, int nThreadsForMessaging)
    {
        if (nThreadsForMessaging < 1)
            throw new IllegalArgumentException("Number of threads passed to Messenger must be >= 1");
        // One extra thread for listening
        this.executorService = Executors.newFixedThreadPool(nThreadsForMessaging + 1);
        this.executorCompletionService = new ExecutorCompletionService<>(executorService);
        this.listener = new Listener(listeningPort);
        this.listening = false;
    }

    /**
     * Synchronously sends a message and wraps it into an {@link MessageSent} event for use by thread pool
     * @param msg Message to be sent
     * @param destination IP Address of the destination
     * @return {@link MessageSent} event that may wrap an {@link IOException} on failure
     */
    private static MessageSent trySend(Message msg, InetSocketAddress destination)
    {
        MessageSent ev = new MessageSent();
        try
        {
            Sender.send(msg, destination);
        }
        catch (IOException e)
        {
            ev.setException(e);
        }
        return ev;
    }


    /**
     * Synchronously receives a message and wraps it into an {@link MessageReceived} event for use by thread pool
     * @param source IP Address of the source
     * @return {@link MessageReceived} event that may wrap an {@link IOException} or {@link ClassNotFoundException} on failure
     */
    private static MessageReceived tryReceive(InetSocketAddress source)
    {
        MessageReceived ev = new MessageReceived();
        try
        {
            ev.setMessage(Receiver.receive(source));
        }
        catch (IOException | ClassNotFoundException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    /**
     * Synchronously receives a message and wraps it into an {@link MessageReceived} event for use by thread pool
     * @param sock Connection to the source
     * @return {@link MessageReceived} event that may wrap an {@link IOException} or {@link ClassNotFoundException} on failure
     */
    private static MessageReceived tryReceive(Socket sock)
    {
        MessageReceived ev = new MessageReceived();
        try
        {
            ev.setMessage(Receiver.receive(sock));
        }
        catch (IOException | ClassNotFoundException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    /**
     * Synchronously accepts a connection and wraps it into an {@link ConnectionReceived} event for use by thread pool
     * @return {@link ConnectionReceived} event that may wrap an {@link IOException} on failure
     */
    private ConnectionReceived tryAccept()
    {
        ConnectionReceived ev = new ConnectionReceived();
        try {
            ev.setSocket(listener.accept());
            listen();
        }
        catch (IOException ex)
        {
            ev.setException(ex);
        }
        return ev;
    }

    /**
     * Asynchronously request for sending a message to destination. This will generate a {@link MessageSent}
     * in the future, whether successful or not.
     * @param msg Message to be sent
     * @param destination IP Address of the destination
     */
    public void send(Message msg, InetSocketAddress destination)
    {
        this.executorCompletionService.submit(() -> Messenger.trySend(msg, destination));
    }

    /**
     * Asynchronous request for receiving a message from source. This will generate a {@link MessageReceived}
     * event in the future, whether successful or not.
     * @param source IP Address of the source
     */
    public void receive(InetSocketAddress source)
    {
        this.executorCompletionService.submit(() -> Messenger.tryReceive(source));
    }

    /**
     * Asynchronous request for receiving a message from source. This will generate a {@link MessageReceived}
     * event in the future, whether successful or not.
     * @param sock Socket representing the connection to the source
     */
    public void receive(Socket sock)
    {
        this.executorCompletionService.submit(() -> Messenger.tryReceive(sock));
    }

    /**
     * Begins listening on the port specified when initializing this instance.
     * Once started, further calls have no effect.
     */
    public void listen()
    {
        if (!listening)
        {
            this.executorCompletionService.submit(this::tryAccept);
            listening = true;
        }
    }

    /**
     * Receives the next event generated by {@link Messenger}, blocking if necessary
     * @return the next completed event, or {@link InterruptReceived} event if the next event is interrupted before
     * completion
     * @throws ExecutionException if the next {@link Event} results in an exception being thrown
     */
    public Event getEvent() throws ExecutionException
    {
        try {
            Future<Event> ev = executorCompletionService.take();
            return ev.get();
        }
        catch(InterruptedException e)
        {
            return new InterruptReceived();
        }
    }

    /**
     * Receives the next event generated by {@link Messenger} without blocking
     * @return the next completed event, or {@code null} if no event is completed yet,
     * or {@link InterruptReceived} event if the next event is interrupted before completion
     * @throws ExecutionException if the next {@link Event} results in an exception being thrown
     */
    public Event getEventIfPresent() throws ExecutionException
    {
        try {
            Future<Event> ev = executorCompletionService.poll();
            if (ev != null)
                return ev.get();
            else
                return null;
        }
        catch(InterruptedException ex)
        {
            return new InterruptReceived();
        }
    }

    /**
     * Stop the {@link Messenger}. It does not, however, wait until it actually stops.
     * If waiting is necessary, call {@link #stop(long)} instead.
     * Repeated calls to {@link #stop()} have no additional effect.
     */
    public void stop()
    {
        if (!listener.isClosed())
        {
            try{
                listener.close();
            }
            catch(IOException e)
            {
                LOGGER.log(Level.SEVERE, "Unable to close the Listener on stop()");
            }
        }
        executorService.shutdown();
    }

    /**
     * Stop the {@link Messenger}, waiting at most {@code secondsToWait} seconds for it to actually stop before continuing.
     * If waiting is not needed, call {@link #stop()} instead.
     * Repeated calls to {@link #stop(long)} have no additional effect.
     * @return true if {@link Messenger} actually stopped before {@code secondsToWait} seconds had passed,
     * {@code false} otherwise
     */
    public boolean stop(long secondsToWait) throws InterruptedException
    {
        stop();
        return executorService.awaitTermination(secondsToWait, TimeUnit.SECONDS);
    }
}
