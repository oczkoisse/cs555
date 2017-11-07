package a4.chunker;

import a4.hash.Hash;
import a4.hash.Hasher;

import java.io.*;
import java.util.Arrays;

class Slice implements Externalizable
{
    private Size sliceSize;
    private byte[] sliceData;

    // Meant only for writing to ObjectOutput
    public Slice()
    {
        this.sliceSize = null;
        this.sliceData = null;
    }

    public Slice(byte[] sliceData, Size sliceSize)
    {
        if (sliceSize == null)
            throw new NullPointerException("null Size passed as slice size");

        if (sliceData.length == 0 || sliceData.length > sliceSize.getByteCount())
            throw new IllegalArgumentException("Slice data length " + sliceData.length + " is invalid for a slice of size " + sliceSize);

        this.sliceData = Arrays.copyOf(sliceData, sliceData.length);
        this.sliceSize = sliceSize;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(sliceSize);
        out.writeInt(sliceData.length);
        out.write(sliceData, 0, sliceData.length);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.sliceSize = (Size) in.readObject();
        this.sliceData = new byte[in.readInt()];
        int bytesRead = 0;
        while(sliceData.length - bytesRead > 0)
            bytesRead += in.read(sliceData, bytesRead, sliceData.length - bytesRead);
        assert bytesRead == sliceData.length;
    }

    public Size getSliceSize()
    {
        return sliceSize;
    }

    public int getSize()
    {
        return sliceData.length;
    }

    public Hash calculateHash(Hasher hasher)
    {
        if (hasher == null)
            throw new NullPointerException("null Hasher passed");

        hasher.reset();
        hasher.update(sliceData, 0, sliceData.length);
        return hasher.getValue();
    }

    byte[] getSliceData()
    {
        return sliceData;
    }
}