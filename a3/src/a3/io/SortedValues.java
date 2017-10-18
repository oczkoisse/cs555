package a3.io;

import org.apache.hadoop.io.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SortedValues implements WritableComparable<SortedValues>
{
    private Text group;
    private DoubleWritable value;

    public SortedValues()
    {
        this.group = new Text();
        this.value = new DoubleWritable(0.0);
    }

    public SortedValues(String group, Float value)
    {
        this.group = new Text(group);
        this.value = new DoubleWritable(value);
    }

    @Override
    public int compareTo(SortedValues o)
    {
        int cmp = group.compareTo(o.group);
        return cmp == 0 ? -value.compareTo(o.value) : cmp;
    }

    @Override
    public void write(DataOutput out) throws IOException
    {
        group.write(out);
        value.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException
    {
        this.group.readFields(in);
        this.value.readFields(in);
    }

    public Double getValue()
    {
        return value.get();
    }

    public void setValue(Float value)
    {
        this.value.set(value);
    }

    public String getGroup()
    {
        return group.toString();
    }

    public void setGroup(String group)
    {
        this.group.set(group);
    }

    public static class Comparator extends WritableComparator
    {
        protected Comparator()
        {
            super(SortedValues.class, true);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public int compare(WritableComparable w1, WritableComparable w2) {
            SortedValues k1 = (SortedValues)w1;
            SortedValues k2 = (SortedValues)w2;

            int cmp = k1.group.compareTo(k2.group);
            return cmp == 0 ? -k1.value.compareTo(k2.value) : cmp;
        }
    }

    public static class Partitioner extends org.apache.hadoop.mapreduce.Partitioner<SortedValues, Object>
    {
        private Text group = new Text();
        @Override
        public int getPartition(SortedValues key, Object val, int numPartitions) {
            group.set(key.getGroup());
            int hash = group.hashCode();
            int partition = hash % numPartitions;
            return partition;
        }

    }

    public static class GroupingComparator extends WritableComparator {
        protected GroupingComparator() {
            super(SortedValues.class, true);
        }
        @SuppressWarnings("rawtypes")
        @Override
        public int compare(WritableComparable w1, WritableComparable w2) {
            SortedValues k1 = (SortedValues)w1;
            SortedValues k2 = (SortedValues)w2;

            return k1.group.compareTo(k2.group);
        }
    }
}
