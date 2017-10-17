package a3.jobs;

import a3.data.Data;
import a3.io.GenreHotness;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopTenSongs
{
    public static class TopTenSongsMapper extends Mapper<Object, Text, GenreHotness, StringTuple>
    {
        private final GenreHotness genreHotness = new GenreHotness();
        private final StringTuple titleName = new StringTuple();

        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if(d.isValid())
            {
                List<String> genres = d.getArtistTerms();
                Float h = d.getSongHotness();
                String n = d.getArtistName();
                String t = d.getTitle();
                titleName.set(t, n);
                if (h != null)
                {
                    genreHotness.setHotness(h);
                    for(String g: genres)
                    {
                        genreHotness.setGenre(g);
                        context.write(genreHotness, titleName);
                    }

                }
            }
        }
    }

    public static class TopTenSongsCombiner extends Reducer<GenreHotness, StringTuple, GenreHotness, StringTuple>
    {
        private static final int N = 10;

        @Override
        public void reduce(GenreHotness genreHotness, Iterable<StringTuple> titleNames, Context context) throws IOException, InterruptedException
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

    public static class TopTenSongsReducer extends Reducer<GenreHotness, StringTuple, GenreHotness, StringTuple>
    {
        private static final int N = 10;
        private int count = 0;
        @Override
        protected void setup(Context context)
        {
            count = 0;
        }

        @Override
        public void reduce(GenreHotness genreHotness, Iterable<StringTuple> titleNames, Context context) throws IOException, InterruptedException
        {
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

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q4");
            // Current class.
            job.setJarByClass(TopTenArtists.class);
            // Mapper
            job.setMapperClass(TopTenArtists.TopTenArtistsMapper.class);
            // Combiner.
            job.setCombinerClass(TopTenArtists.TopTenArtistsCombiner.class);
            // Reducer
            job.setReducerClass(TopTenArtists.TopTenArtistsReducer.class);

            // How to sort keys before passing to reducer/combiner
            job.setSortComparatorClass(TopTenArtists.DecreasingFloatWritableComparator.class);
            // Need to get tempo from all mappers in order to say which are the top N
            job.setNumReduceTasks(1);

            // Outputs from the Mapper.
            job.setMapOutputKeyClass(FloatWritable.class);
            job.setMapOutputValueClass(Text.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(FloatWritable.class);

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
