package a3.jobs;

import a3.data.Data;
import a3.io.SortedValues;
import a3.io.StringTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.List;

public class TopTenSongs
{
    public static class TopTenSongsMapper extends Mapper<Object, Text, SortedValues, StringTuple>
    {
        private final SortedValues genreHotness = new SortedValues();
        private final StringTuple titleName = new StringTuple();

        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if(d.isValid())
            {
                List<String> genres = d.getPopularArtistTerms();
                Float h = d.getSongHotness();
                String n = d.getArtistName();
                String t = d.getTitle();
                titleName.set(t, n);
                if (h != null && genres != null)
                {
                    genreHotness.setValue(h);
                    for(String g: genres)
                    {
                        genreHotness.setGroup(g);
                        context.write(genreHotness, titleName);
                    }
                }
            }
        }
    }


    public static class TopTenSongsCombiner extends Reducer<SortedValues, StringTuple, SortedValues, StringTuple>
    {
        private static final int N = 10;

        @Override
        public void reduce(SortedValues genreHotness, Iterable<StringTuple> titleNames, Context context) throws IOException, InterruptedException
        {
            int count = 0;
            if (count < N) {
                for (StringTuple titleName : titleNames) {
                    context.write(genreHotness, titleName);
                    count++;
                    if (count == N)
                        break;
                }
            }
        }
    }

    public static class TopTenSongsReducer extends Reducer<SortedValues, StringTuple, NullWritable, Text>
    {
        private static final int N = 10;
        private final Text results = new Text();
        private static final NullWritable nw = NullWritable.get();

        @Override
        protected void setup(Context context)
        {
            results.clear();
        }

        @Override
        public void reduce(SortedValues genreHotness, Iterable<StringTuple> titleNames, Context context) throws IOException, InterruptedException
        {
            int count = 0;
            if (count < N) {
                for (StringTuple titleName : titleNames) {
                    results.set(String.join("\t", genreHotness.getGroup(), genreHotness.getValue().toString(), titleName.toString()));
                    context.write(nw, results);
                    count++;
                    if (count == N)
                        break;
                }
            }
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q5");
            // Current class.
            job.setJarByClass(TopTenSongs.class);
            // Mapper
            job.setMapperClass(TopTenSongs.TopTenSongsMapper.class);
            // Combiner.
            job.setCombinerClass(TopTenSongs.TopTenSongsCombiner.class);
            // Reducer
            job.setReducerClass(TopTenSongs.TopTenSongsReducer.class);

            // How to sort keys before passing to reducer/combiner
            job.setSortComparatorClass(SortedValues.Comparator.class);
            // How to group keys for a single call to reduce()
            job.setGroupingComparatorClass(SortedValues.GroupingComparator.class);
            // How to partition data
            job.setPartitionerClass(SortedValues.Partitioner.class);

            // Outputs from the Mapper.
            job.setMapOutputKeyClass(SortedValues.class);
            job.setMapOutputValueClass(StringTuple.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(NullWritable.class);
            job.setOutputValueClass(Text.class);

            // path to input in HDFS
            FileInputFormat.addInputPath(job, new Path(args[0]));
            // path to output in HDFS
            FileOutputFormat.setOutputPath(job, new Path(args[1]));
            // Block until the job is completed.
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

}
