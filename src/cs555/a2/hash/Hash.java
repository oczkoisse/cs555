package cs555.a2.hash;

public interface Hash
{
    void update(int b);
    void update(byte[] data, int offset, int len);
    void reset();
    byte[] getValue();
    int size();

    default int sizeInBits()
    {
        return size() * Byte.SIZE;
    }
}
