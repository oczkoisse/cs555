package cs555.a2.hash;

import java.util.Date;
import cs555.a2.util.ByteConverter;

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
        Date d = new Date();
        long timestamp = d.getTime();

        reset();

        byte[] bytes = ByteConverter.convert(timestamp);
        update(bytes, 0, bytes.length);
        byte[] hashed =  getValue();

        reset();

        return hashed;
    }
}
