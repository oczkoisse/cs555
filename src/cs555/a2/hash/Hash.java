package cs555.a2.hash;

public abstract class Hash
{
    public abstract void update(int b);
    public abstract void update(byte[] data, int offset, int len);
    public abstract void reset();
    public abstract byte[] getValue();
}
