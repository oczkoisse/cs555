package a4.chunker;
import a4.hash.Hash;
import a4.hash.Hasher;
import a4.nodes.client.messages.WriteRequest;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;

public class Chunk implements Externalizable, Iterable<Slice>
{
    private final Hasher hasher = Hasher.getHasherByName(Hash.Name.SHA1);
    private Metadata metadata;
    private List<Slice> sliceList;
    private boolean last;

    public Chunk(Metadata metadata, List<Slice> sliceList, boolean last)
    {
        if (metadata == null)
            throw new NullPointerException("Metadata is null");
        if (sliceList == null)
            throw new NullPointerException("Slice list is null");
        if (sliceList.size() == 0)
            throw new IllegalArgumentException("Slice list size should be greater than 0");

        this.metadata = metadata;
        this.sliceList = sliceList;
        this.last = last;
    }

    /**
     * For instantiating Chunk object from a chunk file stored with {@link #writeToFile()}
     * @param pathToChunkFile
     */
    public Chunk(Path pathToChunkFile) throws IOException, IntegrityCheckFailedException
    {
        List<Integer> failedSlices = new ArrayList<>();
        StringBuilder exMessage = new StringBuilder();
        exMessage.append(String.format("%n"));


        try(FileInputStream fin = new FileInputStream(pathToChunkFile.toString());
            BufferedInputStream bin = new BufferedInputStream(fin);
            DataInputStream din = new DataInputStream(bin)) {

            this.last = din.readBoolean();
            String filename = din.readUTF();
            long seqNum = din.readLong();
            int version = din.readInt();
            long timestamp = din.readLong();
            Date ts = new Date(timestamp);
            this.metadata = new Metadata(filename, seqNum, version, ts);
            System.out.println(this.metadata);
            int sliceCount = din.readInt();
            List<Hash> expectedHashes = new ArrayList<>();
            byte[] hash = new byte[hasher.size()];

            for(int i = 0; i<sliceCount; i++)
            {
                int off = 0;
                while(off < hash.length) {
                    int read = din.read(hash, off, hash.length - off);
                    if (read > 0)
                        off += read;
                    else
                        break;
                }
                expectedHashes.add(new Hash(hash));
            }

            int sliceSizeInBytes = din.readInt();
            Size sliceSize = new Size(sliceSizeInBytes/1024, Size.Unit.K);

            byte[] sliceBuffer = new byte[sliceSizeInBytes];
            this.sliceList = new ArrayList<>();

            for (int curSlice=0; curSlice < sliceCount; curSlice++)
            {
                int off = 0;
                while(off < sliceBuffer.length) {
                    int read = din.read(sliceBuffer, off, sliceBuffer.length - off);
                    if (read > 0)
                        off += read;
                    else
                        break;
                }

                if (curSlice == sliceCount - 1 && din.read() != -1)
                {
                    exMessage.append(String.format("Integrity check failed for slice %d: found more bytes than expected.%n", curSlice));
                    failedSlices.add(curSlice);
                    continue;
                }

                Slice s = null;
                if(off == sliceBuffer.length)
                {
                    s = new Slice(sliceBuffer, sliceSize);
                }
                else
                {
                    s = new Slice(Arrays.copyOf(sliceBuffer, off), sliceSize);
                }
                Hash calculatedHash = s.calculateHash(hasher);
                if(calculatedHash.equals(expectedHashes.get(curSlice)))
                    this.sliceList.add(s);
                else
                {
                    exMessage.append(String.format("Integrity check failed for slice %d: expected %s, got %s.%n", curSlice, expectedHashes.get(curSlice), calculatedHash));
                    failedSlices.add(curSlice);
                }
            }
        }

        if (failedSlices.size() > 0)
        {
            exMessage.append(String.format("Integrity check failed in %s for %d slices.%n", pathToChunkFile, failedSlices.size()));
            throw new IntegrityCheckFailedException(exMessage.toString(), failedSlices, this);
        }
    }

    public Chunk()
    {
        this.metadata = null;
        this.sliceList = null;
        this.last = false;
    }

    public void fixSlice(int index, Slice slice)
    {
        if (index < 0 || index >= sliceList.size())
            throw new IndexOutOfBoundsException("Slice index " + index + " is out of bounds for a chunk with " + sliceList.size() + " slices");
        sliceList.set(index, slice);
    }

    public WriteRequest convertToWriteRequest(int port)
    {
        return new WriteRequest(this.metadata.getFileName().toString(), this.metadata.getSequenceNum(), port);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeBoolean(last);
        out.writeObject(metadata);
        out.writeInt(sliceList.size());
        for(Slice s: sliceList)
            out.writeObject(s);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.last = in.readBoolean();
        this.metadata = (Metadata) in.readObject();
        this.sliceList = new ArrayList<>();
        int sliceCount = in.readInt();
        for (int i=0; i < sliceCount; i++)
            sliceList.add((Slice) in.readObject());
    }

    public void writeToFile() throws IOException
    {
        try(FileOutputStream fout = new FileOutputStream(this.metadata.getStoragePath().toString());
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            DataOutputStream dout = new DataOutputStream(bout)) {
            dout.writeBoolean(last);
            dout.writeUTF(metadata.getFileName().toString());
            dout.writeLong(metadata.getSequenceNum());
            dout.writeInt(metadata.getVersion());
            dout.writeLong(metadata.getTimestamp());
            dout.writeInt(sliceList.size());
            for (Slice s : sliceList) {
                Hash h = s.calculateHash(hasher);
                byte[] bytes = h.asBytes();
                dout.write(bytes);
            }
            boolean sliceSizeWritten = false;
            for (Slice s : sliceList) {
                if (!sliceSizeWritten)
                {
                    dout.writeInt(s.getSliceSize().getByteCount());
                    sliceSizeWritten = true;
                }
                dout.write(s.getSliceData());
            }
        }
    }

    public byte[] toBytes()
    {
        int totalBytes = 0;
        for(Slice s: sliceList)
            totalBytes += s.getSize();

        byte[] bytes = new byte[totalBytes];
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        for(Slice s: sliceList) {
            buf.put(s.getSliceData());
        }
        return buf.array();
    }

    public boolean isLast()
    {
        return last;
    }

    public Metadata getMetadata()
    {
        return this.metadata;
    }

    @Override
    public Iterator<Slice> iterator()
    {
        return sliceList.iterator();
    }
}
