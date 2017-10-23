package a2.hash;

import java.util.Date;
import a2.util.ByteConverter;

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

    default byte[] randomHash()
    {
        reset();

        Date d = new Date();
        long timestamp = d.getTime();
        byte[] bytes = ByteConverter.convert(timestamp);
        update(bytes, 0, bytes.length);
        return getValue();
    }
}
