package a4.chunker;

import a2.hash.Hash;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

class Slice implements Externalizable
{
    private Size sliceSize;
    private byte[] sliceData;

    // Meant only for writing to DataOutput
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
        out.write(sliceData.length);
        out.write(sliceData);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.sliceSize = (Size) in.readObject();
        this.sliceData = new byte[in.readInt()];
        in.read(this.sliceData);
    }

    public Size getSliceSize()
    {
        return sliceSize;
    }

    public int getSize()
    {
        return sliceData.length;
    }

    public byte[] calculateHash(Hash hasher)
    {
        if (hasher == null)
            throw new NullPointerException("null Hash passed");

        hasher.reset();
        hasher.update(sliceData, 0, sliceData.length);
        return hasher.getValue();
    }
}