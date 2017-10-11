package a2.util;

import java.nio.ByteBuffer;

/**
 * Utility class to convert primitive integer types to a byte array
 */
public class ByteConverter
{
    private final static ByteBuffer converter = ByteBuffer.allocate(Long.BYTES);

    private ByteConverter() {}

    /**
     * Converts a {@code long} to a {@code byte[]} array
     * @param val the value to convert
     * @return {@code byte[]} array of length {@link Long#BYTES}
     */
    public static byte[] convert(long val)
    {
        converter.clear();
        converter.putLong(val);
        converter.position(0);
        byte[] out = new byte[Long.BYTES];
        converter.get(out);
        return out;
    }

    /**
     * Converts a {@code int} to a {@code byte[]} array
     * @param val the value to convert
     * @return {@code byte[]} array of length {@link Integer#BYTES}
     */
    public static byte[] convert(int val)
    {
        converter.clear();
        converter.putInt(val);
        converter.position(0);
        byte[] out = new byte[Integer.BYTES];
        converter.get(out);
        return out;
    }

    /**
     * Converts a {@code short} to a {@code byte[]} array
     * @param val the value to convert
     * @return {@code byte[]} array of length {@link Short#BYTES}
     */
    public static byte[] convert(short val)
    {
        converter.clear();
        converter.putShort(val);
        converter.position(0);
        byte[] out = new byte[Short.BYTES];
        converter.get(out);
        return out;
    }
}
