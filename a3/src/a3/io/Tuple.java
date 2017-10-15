package a3.io;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class implementing the notion of a homogeneous tuple
 * @param <T> any {@link Object} that must be defined to read/write over {@link DataInput}/{@link DataOutput} streams
 *          respectively.
 */
public abstract class Tuple<T> implements Writable
{
    /**
     * A list of elements, possibly with {@code null} values
     */
    private List<T> backingList;

    /**
     * An empty tuple, usually for purposes of {@link #readFields(DataInput)}
     */
    public Tuple()
    {
        this.backingList = new ArrayList<T>();
    }

    /**
     * A fixed size list of {@link T}
     * @param elements zero or more elements to initialize this Tuple. {@code null}s are accepted
     */
    public Tuple(T... elements)
    {
        this();
        set(elements);
    }

    public Tuple(List<T> elements)
    {
        this();
        set(elements);
    }

    /**
     * Returns the number of elements in the {@link Tuple}, some or all of them being {@code null}
     * @return the number of elements in the {@link Tuple}
     */
    public final int size()
    {
        return backingList.size();
    }

    /**
     * Read an element from the input stream. The purpose of this method is to provide a translation of certain
     * values of {@link T} to {@code null} values
     * @param in the stream to read from
     * @return the read element
     * @throws IOException if there is an error in reading over stream
     */
    protected abstract T readElement(DataInput in) throws IOException;

    /**
     * Writes an element to the output stream. The purpose of this method is to provide a translation of {@code null}
     * values in the tuple to a certain value of {@link T} that can be written over the stream
     * @param out the output stream
     * @param e the element to be written
     * @throws IOException if there is an error in writing to output stream
     */
    protected abstract void writeElement(DataOutput out, T e) throws IOException;

    /**
     * Initializes the {@link Tuple} by reading its elements from input stream using {@link #readElement(DataInput)}
     * @param in the input stream
     * @throws IOException if there is an error while reading from the input stream
     */
    @Override
    public final void readFields(DataInput in) throws IOException
    {
        // VERY IMPORTANT
        // Clear the internal list to allow repeated calls to readFields
        clear();
        int sz = in.readInt();

        for(int i=0; i < sz; i++)
        {
            this.backingList.add(readElement(in));
        }
    }

    /**
     * Writes all elements in the tuple to output stream using {@link #writeElement(DataOutput, Object)}.
     * @param out the output stream
     * @throws IOException if there is an error in writing to output stream
     */
    @Override
    public final void write(DataOutput out) throws IOException {

        int tupleSize = this.backingList.size();
        out.writeInt(tupleSize);

        for(T e: this.backingList)
        {
            writeElement(out, e);
        }
    }

    public final void set(T... elements)
    {
        clear();
        Collections.addAll(this.backingList, elements);
    }

    public final void set(List<T> elements)
    {
        this.backingList.clear();
        this.backingList.addAll(elements);
    }

    public final void set(int index, T element)
    {
        checkIndex(index);
        this.backingList.set(index, element);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append("(");
        for(T e: backingList)
        {
            if (e == null)
                s.append(" null,");
            else
                s.append(" " + e.toString() + ",");
        }

        if (s.charAt(s.length() - 1) == ',')
            s.setCharAt(s.length() - 1, ' ');

        s.append(")");

        return s.toString();
    }

    private void checkIndex(int index)
    {
        if (index < 0 || index > backingList.size())
            throw new IndexOutOfBoundsException(index + " out of bounds for a tuple of size " + backingList.size());
    }

    /**
     * Get an element of the tuple
     * @param index should be non-negative and less than the size of tuple as given by {@link #size()}
     * @return the element at index {@code index}, possibly {@code null}
     */
    public final T get(int index)
    {
        checkIndex(index);
        return backingList.get(index);
    }

    public void clear()
    {
        this.backingList.clear();
    }
}
