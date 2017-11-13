package a4.transport.messenger;

import a4.transport.*;

import java.io.*;
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

    private volatile boolean listening;

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

    public Messenger(int nThreadsForSending)
    {
        if (nThreadsForSending < 1)
            throw new IllegalArgumentException("Number of threads passed to Messenger must be >= 1");
        this.executorService = Executors.newFixedThreadPool(nThreadsForSending);
        this.executorCompletionService = new ExecutorCompletionService<>(executorService);
        this.listener = null;
        this.listening = false;
    }

    /**
     * Synchronously sends a @{@link Notification} and wraps it into an {@link NotificationSent} event for use by thread pool
     * @param msg Notification to be sent
     * @param destination IP Address of the destination
     * @return {@link NotificationSent} event that may wrap an {@link SenderException} on failure
     */
    private static NotificationSent trySend(Notification msg, InetSocketAddress destination)
    {
        NotificationSent ev = new NotificationSent(msg, destination);
        try
        {
            Sender.send(msg, destination);
        }
        catch (SenderException ex)
        {
            ev.setException(ex);
        }
        return ev;
    }

    /**
     * Synchronously sends a @{@link Notification} and wraps it into an {@link NotificationSent} event for use by thread pool
     * @param msg Notification to be sent
     * @param destination Socket to the destination
     * @return {@link NotificationSent} event that may wrap an {@link SenderException} on failure
     */
    private static NotificationSent trySend(Notification msg, Socket destination)
    {
        NotificationSent ev = new NotificationSent(msg,
                new InetSocketAddress(destination.getInetAddress(), destination.getPort()));
        try
        {
            Sender.send(msg, destination);
        }
        catch (SenderException ex)
        {
            ev.setException(ex);
        }
        return ev;
    }


    /**
     * Synchronously receives a message and wraps it into an {@link NotificationReceived} event for use by thread pool
     * @param sock Connection to the source
     * @return {@link NotificationReceived} event that may wrap an {@link ReceiverException} on failure
     */
    private MessageReceived tryReceive(Socket sock)
    {
        NotificationReceived nev = new NotificationReceived(sock);
        try
        {
            Message msg = Receiver.receiveAndThen(sock);
            if (msg.isSynchronous())
            {
                RequestReceived rev = new RequestReceived(sock);
                rev.setRequest((Request) msg);
                return rev;
            }
            else
            {
                nev.setNotification((Notification) msg);
                return nev;
            }
        }
        catch (ReceiverException ex)
        {
            nev.setException(ex);
        }
        return nev;
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
            this.executorCompletionService.submit(this::tryAccept);
        }
        catch (IOException ex)
        {
            ev.setException(ex);
        }
        return ev;
    }

    /**
     * Asynchronously request for sending a message to destination. This will generate a {@link NotificationSent}
     * in the future, whether successful or not.
     * @param msg Notification to be sent
     * @param destination IP Address of the destination
     */
    public void send(Notification msg, InetSocketAddress destination)
    {
        if (msg == null)
            throw new NullPointerException("Notification to be sent cannot be null");
        if (destination == null)
            throw new NullPointerException("Destination cannot be null");

        this.executorCompletionService.submit(() -> Messenger.trySend(msg, destination));
    }

    public void send(Notification msg, Socket destination)
    {
        if (msg == null)
            throw new NullPointerException("Notification to be sent cannot be null");
        if (destination == null)
            throw new NullPointerException("Destination cannot be null");

        this.executorCompletionService.submit(() -> Messenger.trySend(msg, destination));
    }

    /**
     * Asynchronous request for receiving a message from source. This will generate a {@link NotificationReceived}
     * event in the future, whether successful or not.
     * @param sock Socket representing the connection to the source
     */
    public void receive(Socket sock)
    {
        if (sock == null)
            throw new NullPointerException("Socket to receive message from cannot be null");

        this.executorCompletionService.submit(() -> this.tryReceive(sock));
    }

    public static Notification request(Request request, Socket sock) throws SenderException, ReceiverException
    {
        if (request == null)
            throw new NullPointerException("Request is null");
        if (sock == null)
            throw new NullPointerException("Socket to receive message from cannot be null");
        Sender.sendAndThen(request, sock);
        return Receiver.receive(sock);
    }

    public static Notification request(Request request, InetSocketAddress destination) throws SenderException, ReceiverException
    {
        if (request == null)
            throw new NullPointerException("Request is null");
        if (destination == null)
            throw new NullPointerException("Destination to receive message from cannot be null");
        Socket sock = Sender.sendAndThen(request, destination);
        return Receiver.receive(sock);
    }

    /**
     * Begins listening on the port specified when initializing this instance.
     * Once started, further calls have no effect.
     */
    public void listen()
    {
        if (listener == null)
            throw new IllegalStateException("listen() called on Messenger instance that can only send messages");

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
                listening = false;
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
