package cs555.a2.nodes.client.messages;

import cs555.a2.chord.peer.ID;
import cs555.a2.hash.CRC16;
import cs555.a2.hash.Hash;
import cs555.a2.transport.Message;

import java.io.*;
import java.math.BigInteger;

public class DataItem implements Message<ClientMessageType>
{
    private static final File outDir = new File(System.getProperty("java.io.tmpdir"));
    private Hash hash;

    private File pathToFile;
    private ID id;
    private boolean dummy;

    /**
     * Parses a string containing path into a File object after verifying that the file exists and is readable
     * @param pathToFile String containing the path to the file
     * @return A @{link File} object encapsulating the path to a readable file
     * @throws IllegalArgumentException If a file does not exist at the path indicated by {@code pathToFile}, or if it is not readable.
     * @throws NullPointerException If {@code pathToFile} is null
     */
    public static File parseAsPath(String pathToFile)
    {
        if (pathToFile == null)
            throw new NullPointerException("null cannot be parsed as a path");
        File f = new File(pathToFile);
        if (f.exists() && f.isFile())
        {
            if(f.canRead())
                return f;
            else
                throw new IllegalArgumentException(String.format("File %1$s is not readable", pathToFile));
        }
        else
            throw new IllegalArgumentException(String.format("Path %1$s does not exist", pathToFile));
    }


    public DataItem(String pathToFile) throws IOException
    {
        this.pathToFile = parseAsPath(pathToFile);
        this.hash = new CRC16();
        this.id = null;
        this.dummy = false;

        hash.reset();

        byte[] buffer = new byte[1024];

        try(FileInputStream f = new FileInputStream(pathToFile);
            BufferedInputStream bf = new BufferedInputStream(f))
        {
            int bytesRead;
            while ((bytesRead = f.read(buffer)) != -1)
            {
                hash.update(buffer, 0, bytesRead);
            }
        }
        catch(FileNotFoundException ex)
        {
            // Can ignore, already handled by parseAsPath()
        }

        this.id = new ID(new BigInteger(hash.getValue()), hash.size());
        hash.reset();
    }

    public DataItem(String pathToFile, ID id)
    {
        this.pathToFile = new File(pathToFile);
        this.hash = new CRC16();
        this.id = id;
        this.dummy = true;
    }

    public DataItem()
    {
        this.pathToFile = null;
        this.hash = new CRC16();
        this.id = null;
        this.dummy = false;
    }

    @Override
    public ClientMessageType getMessageType()
    {
        return ClientMessageType.DATA_ITEM;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeUTF(pathToFile.getName());
        out.writeObject(this.id);
        out.writeBoolean(dummy);

        if (!dummy) {
            byte[] buffer = new byte[1024];

            try (FileInputStream f = new FileInputStream(pathToFile);
                 BufferedInputStream bf = new BufferedInputStream(f)) {
                int bytesRead;
                while ((bytesRead = f.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            pathToFile.delete();
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        String fileName = in.readUTF();
        this.pathToFile = new File(outDir, fileName);
        this.id = (ID) in.readObject();
        this.dummy = in.readBoolean();

        if (!dummy) {
            byte[] buffer = new byte[1024];

            hash.reset();

            try (FileOutputStream f = new FileOutputStream(pathToFile);
                 BufferedOutputStream bf = new BufferedOutputStream(f)) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    bf.write(buffer, 0, bytesRead);
                    hash.update(buffer, 0, bytesRead);
                }
            }

            assert this.id.compareTo(new ID(new BigInteger(hash.getValue()), hash.size())) == 0;
        }
    }

    public String getFilePath()
    {
        return pathToFile.getPath();
    }

    public ID getID()
    {
        return this.id;
    }
}
