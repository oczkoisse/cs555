package a4.nodes.client;

import a4.chunker.Chunk;
import a4.chunker.Chunker;
import a4.chunker.Size;
import a4.nodes.client.messages.WriteData;
import a4.nodes.controller.messages.WriteReply;
import a4.transport.messenger.Event;
import a4.transport.messenger.Messenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final Size chunkSize = new Size(64, Size.Unit.K);
    private static final Size sliceSize = new Size(8, Size.Unit.K);

    private final InetSocketAddress controllerAddress;
    private final Messenger messenger;
    private volatile boolean isRunning = true;

    public Client(int listeningPort, String controllerHost, int controllerPort) {
        this.controllerAddress = new InetSocketAddress(controllerHost, controllerPort);
        this.messenger = new Messenger(listeningPort, 4);
    }

    private void listen() {
        messenger.listen();
        LOGGER.log(Level.INFO, "Begin listening for new connections...");
    }


    @Override
    public void run() {
        listen();

        while (isRunning) {
            try {
                LOGGER.log(Level.FINE, "Waiting for next event");
                Event ev = messenger.getEvent();
                LOGGER.log(Level.FINE, "Received an event");
                if (!ev.causedException())
                    handleEvent(ev);
                else {
                    LOGGER.log(Level.SEVERE, "Received event caused an exception: " + ev.getException().getMessage());
                    handleFailedEvent(ev);
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred while executing the event");
                ex.printStackTrace();
            }
        }
    }

    private void handleFailedEvent(Event ev) {

    }

    private void handleEvent(Event ev) {

    }

    private static void printUsage() {
        System.out.println("Usage: " + Client.class.getCanonicalName() + " <ListeningPort> <ControllerHost> <ControllerPort>");
    }

    public void writeFile(Path pathToFile) {
        if (Files.isReadable(pathToFile)) {
            try(Chunker chunker = new Chunker(pathToFile, chunkSize, sliceSize))
            {
                for(Chunk c: chunker)
                {
                    WriteReply writeReply = (WriteReply) Messenger.ask(c.convertToWriteRequest(), controllerAddress);
                    WriteData writeData = new WriteData(c, writeReply);
                    this.messenger.send(writeData, writeData.getForwardingAddress());
                }
            }
            catch(IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }

        } else {
            LOGGER.log(Level.INFO, pathToFile.toString() + " is not readable.");
        }
    }

    public void readFile(String fileName)
    {

    }

    public void stop()
    {
        isRunning = false;
        this.messenger.stop();
    }


    public static void main(String[] args)
    {
        try {
            int ownPort = Integer.parseInt(args[0]);
            String controllerHost = args[1];
            int controllerPort = Integer.parseInt(args[2]);
            Client c = new Client(ownPort, controllerHost, controllerPort);
            new Thread(c).start();

            try(InputStreamReader in = new InputStreamReader(System.in);
                BufferedReader bin = new BufferedReader(in))
            {
                while(true) {
                    System.out.println("Enter the operation [r (default) | w] and file name to read/file path to write: ");
                    String command = bin.readLine();
                    String[] commands = command.split("\\s*,\\s*");
                    if (commands.length == 2) {
                        if (commands[0].equalsIgnoreCase("r")) {
                            c.readFile(commands[0]);
                        } else if (commands[0].equalsIgnoreCase("w")) {
                            try {
                                Path pathToFile = Paths.get(commands[1]);


                            } catch(InvalidPathException ex)
                            {
                                System.out.println("Path is invalid: " + ex.getInput());
                            }
                        } else {
                            System.out.println("Operation is invalid");
                        }
                    } else {
                        System.out.println("Command is invalid");
                    }
                }
            }
            catch(IOException ex)
            {
                System.out.println(ex.getMessage());
                c.stop();
            }
        }
        catch(NumberFormatException | IndexOutOfBoundsException ex)
        {
            printUsage();
        }
    }
}
