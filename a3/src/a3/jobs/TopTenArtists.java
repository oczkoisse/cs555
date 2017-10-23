package a3.jobs;

import a3.data.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TopTenArtists
{
    public static class TopTenArtistsMapper extends Mapper<Object, Text, FloatWritable, Text>
    {
        private final FloatWritable tempo = new FloatWritable();
        private final Text artistName = new Text();

        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if(d.isValid())
            {
                Float t = d.getTempo();
                String an = d.getArtistName();
                tempo.set(t);
                artistName.set(an);
                context.write(tempo, artistName);
            }
        }
    }

    public static class TopTenArtistsCombiner extends Reducer<FloatWritable, Text, FloatWritable, Text>
    {
        private static final int N = 10;
        private Set<String> topNArtists = new HashSet<>();

        @Override
        protected void setup(Context context)
        {
            topNArtists.clear();
        }

        @Override
        public void reduce(FloatWritable tempo, Iterable<Text> artists, Context context) throws IOException, InterruptedException
        {
            // We get sorted tempos
            // Make sure no more than N artists with max tempo get out
            if (topNArtists.size() < N) {
                for (Text artist : artists) {
                    String sArtist = artist.toString();
                    if (!topNArtists.contains(sArtist)) {
                        topNArtists.add(sArtist);
                        context.write(tempo, artist);
                        if (topNArtists.size() == N)
                            break;
                    }
                }
            }
        }
    }

    public static class TopTenArtistsReducer extends Reducer<FloatWritable, Text, Text, FloatWritable>
    {
        private static final int N = 10;
        private Set<String> topNArtists = new HashSet<>();

        @Override
        protected void setup(Context context)
        {
            topNArtists.clear();
        }

        @Override
        public void reduce(FloatWritable tempo, Iterable<Text> artists, Context context) throws IOException, InterruptedException
        {
            // We get sorted tempos
            // Make sure no more than N artists with max tempo get out
            if (topNArtists.size() < N) {
                for (Text artist : artists) {
                    String sArtist = artist.toString();
                    if (!topNArtists.contains(sArtist)) {
                        topNArtists.add(sArtist);
                        context.write(artist, tempo);
                        if (topNArtists.size() == N)
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

    private static class DecreasingFloatWritableComparator extends FloatWritable.Comparator
    {
        public DecreasingFloatWritableComparator()
        {
            super();
        }

        @Override
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
        {
            int ret = super.compare(b1, s1, l1, b2, s2, l2);
            return ret == 0 ? ret : -ret;
        }
    }
}
