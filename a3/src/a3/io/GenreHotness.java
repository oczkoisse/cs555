package a3.io;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Partitioner;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class GenreHotness implements WritableComparable<GenreHotness>
{
    private Text genre;
    private FloatWritable hotness;

    public GenreHotness()
    {
        this.genre = new Text();
        this.hotness = new FloatWritable(0.0f);
    }

    public GenreHotness(String genre, Float hotness)
    {
        this.genre = new Text(genre);
        this.hotness = new FloatWritable(hotness);
    }

    @Override
    public int compareTo(GenreHotness o)
    {
        int cmp = genre.compareTo(o.genre);
        return cmp == 0 ? -hotness.compareTo(o.hotness) : cmp;
    }

    @Override
    public void write(DataOutput out) throws IOException
    {
        genre.write(out);
        hotness.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException
    {
        this.genre.readFields(in);
        this.hotness.readFields(in);
    }

    public Float getHotness()
    {
        return hotness.get();
    }

    public void setHotness(Float hotness)
    {
        this.hotness.set(hotness);
    }

    public String getGenre()
    {
        return genre.toString();
    }

    public void setGenre(String genre)
    {
        this.genre.set(genre);
    }

    public class Comparator extends WritableComparator
    {
        protected Comparator()
        {
            super(GenreHotness.class, true);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public int compare(WritableComparable w1, WritableComparable w2) {
            GenreHotness k1 = (GenreHotness)w1;
            GenreHotness k2 = (GenreHotness)w2;

            int cmp = k1.genre.compareTo(k2.genre);
            return cmp == 0 ? -k1.hotness.compareTo(k2.hotness) : cmp;
        }
    }

    public class GenrePartitioner extends Partitioner<GenreHotness, Object>
    {
        @Override
        public int getPartition(GenreHotness key, Object val, int numPartitions) {
            int hash = key.getGenre().hashCode();
            int partition = hash % numPartitions;
            return partition;
        }

    }

    public class GenreGroupingComparator extends WritableComparator {
        protected GenreGroupingComparator() {
            super(GenreHotness.class, true);
        }
        @SuppressWarnings("rawtypes")
        @Override
        public int compare(WritableComparable w1, WritableComparable w2) {
            GenreHotness k1 = (GenreHotness)w1;
            GenreHotness k2 = (GenreHotness)w2;

            return k1.genre.compareTo(k2.genre);
        }
    }
}
