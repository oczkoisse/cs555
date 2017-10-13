package a3.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Writable;

public class FloatTuple implements Writable {
	
	private List<Float> backingList = new ArrayList<>();
	
	public FloatTuple()
	{
	}
	
	/**
	 * A fixed size list of Floats
	 * @param rest one or more Floats with which to initializing this FloatTuple
	 */
	public FloatTuple(float first, float... rest)
	{
		backingList.add(first);

		for(float f: rest)
		    backingList.add(f);
	}
	
	public int size()
	{
		return backingList.size();
	}

	public void readFields(DataInput in) throws IOException {
		int sz = in.readInt();

		for(int i=0; i < sz; i++)
		{
			this.backingList.add(in.readFloat());
		}
	}

	public static FloatTuple read(DataInput in) throws IOException
    {
        FloatTuple ft = new FloatTuple();
        ft.readFields(in);
        return ft;
    }

	public void write(DataOutput out) throws IOException {

	    int sz = this.backingList.size();
	    out.writeInt(sz);

		for(Float e: this.backingList)
		{
			out.writeFloat(e);
		}
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("[");
		for(Float e: backingList)
		{
			s.append(" " + e.floatValue() + ",");
		}
		
		s.setCharAt(s.length() - 1, ' ');
		
		s.append("]");
		
		return s.toString();
	}

	public Float get(int index)
    {
        if (index < 0 || index > backingList.size())
            throw new IndexOutOfBoundsException(index + " out of bounds for list of size " + backingList.size());
        return backingList.get(index);
    }
}