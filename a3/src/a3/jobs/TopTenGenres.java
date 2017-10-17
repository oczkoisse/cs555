package a3.jobs;

import a3.data.Data;
import a3.io.StringTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopTenGenres
{
    public static class GenresCountMapper extends Mapper<Object, Text, Text, IntWritable>
    {
        private static final IntWritable ONE = new IntWritable(1);
        private final Text genre = new Text();

        @Override
        protected void setup(Context context)
        {
            genre.clear();
        }

        @Override
        public void map(Object o, Text content, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(content.toString());
            if (d.isValid())
            {
                List<String> genres = d.getArtistTerms();
                for(String g: genres)
                {
                    genre.set(g);
                    context.write(genre, ONE);
                }
            }
        }
    }

    public static class GenresCountReducer extends Reducer<Text, IntWritable, Text, IntWritable>
    {
        private final IntWritable totalCount = new IntWritable(0);

        @Override
        protected void setup(Context context)
        {
            totalCount.set(0);
        }

        @Override
        public void reduce(Text genre, Iterable<IntWritable> counts, Context context) throws IOException, InterruptedException
        {
            int tc = 0;
            for(IntWritable c: counts)
                tc += c.get();

            totalCount.set(tc);

            context.write(genre, totalCount);
        }
    }

    public static class TopTenGenresMapper extends Mapper<Object, Text, LongWritable, Text>
    {
        private LongWritable count = new LongWritable(0);
        private Text genre = new Text();

        @Override
        protected void setup(Context context)
        {
            count.set(0);
            genre.clear();
        }

        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            String[] parts = contents.toString().split("\\t");
            genre.set(parts[0]);
            count.set(Long.parseLong(parts[1]));
            context.write(count, genre);
        }
    }

    public static class TopTenGenresCombiner extends Reducer<LongWritable, Text, LongWritable, Text>
    {
        private static final int N = 10;
        private Set<String> topNGenres = new HashSet<>();

        @Override
        protected void setup(Context context)
        {
            topNGenres.clear();
        }

        @Override
        public void reduce(LongWritable count, Iterable<Text> genres, Context context) throws IOException, InterruptedException
        {
            // We get sorted tempos
            // Make sure no more than N artists with max genre get out
            if (topNGenres.size() < N) {
                for (Text artist : genres) {
                    String sArtist = artist.toString();
                    if (!topNGenres.contains(sArtist)) {
                        topNGenres.add(sArtist);
                        context.write(count, artist);
                        if (topNGenres.size() == N)
                            break;
                    }
                }
            }
        }
    }

    public static class TopTenGenresReducer extends Reducer<LongWritable, Text, Text, LongWritable>
    {
        private static final int N = 10;
        private Set<String> topNGenres = new HashSet<>();

        @Override
        protected void setup(Context context)
        {
            topNGenres.clear();
        }

        @Override
        public void reduce(LongWritable count, Iterable<Text> genres, Context context) throws IOException, InterruptedException
        {
            // We get sorted tempos
            // Make sure no more than N artists with max tempo get out
            if (topNGenres.size() < N) {
                for (Text artist : genres) {
                    String sArtist = artist.toString();
                    if (!topNGenres.contains(sArtist)) {
                        topNGenres.add(sArtist);
                        context.write(artist, count);
                        if (topNGenres.size() == N)
                            break;
                    }
                }
            }
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q8part1");
            // Current class.
            job.setJarByClass(TopTenGenres.class);
            // Mapper
            job.setMapperClass(TopTenGenres.GenresCountMapper.class);
            // Combiner.
            job.setCombinerClass(TopTenGenres.GenresCountReducer.class);
            // Reducer
            job.setReducerClass(TopTenGenres.GenresCountReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(IntWritable.class);

            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(IntWritable.class);

            // path to input in HDFS
            FileInputFormat.addInputPath(job, new Path(args[0]));
            // path to output in HDFS
            FileOutputFormat.setOutputPath(job, new Path("/home/inter"));
            // Block until the job is completed.
            if (job.waitForCompletion(true))
            {
                Configuration conf2 = new Configuration();
                // Give the MapRed job a name. You'll see this name in the Yarn webapp.
                Job job2 = Job.getInstance(conf2, "q8part2");
                // Current class.
                job2.setJarByClass(TopTenGenres.class);
                // Mapper
                job2.setMapperClass(TopTenGenres.TopTenGenresMapper.class);
                // Combiner
                job2.setCombinerClass(TopTenGenres.TopTenGenresCombiner.class);
                // Reducer
                job2.setReducerClass(TopTenGenres.TopTenGenresReducer.class);

                // How to sort keys before passing to reducer/combiner
                job2.setSortComparatorClass(LongWritable.DecreasingComparator.class);
                // Need to get genre counts from all mappers in order to say which are the top N
                job2.setNumReduceTasks(1);

                // Outputs from the Mapper.
                job2.setMapOutputKeyClass(LongWritable.class);
                job2.setMapOutputValueClass(Text.class);
                // Outputs from Reducer. It is sufficient to set only the following two properties
                // if the Mapper and Reducer has same key and value types. It is set separately for
                // elaboration.
                job2.setOutputKeyClass(Text.class);
                job2.setOutputValueClass(LongWritable.class);

                job2.setNumReduceTasks(1);

                // path to input in HDFS
                FileInputFormat.addInputPath(job2, new Path("/home/inter"));
                // path to output in HDFS
                FileOutputFormat.setOutputPath(job2, new Path(args[1]));
                // Block until the job is completed.
                System.exit(job2.waitForCompletion(true) ? 0 : 1);
            }
            else
                System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
}
