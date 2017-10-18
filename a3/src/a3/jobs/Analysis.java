package a3.jobs;

import a3.data.Data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

// Genre popularity over decades
public class Analysis {

    public static Byte encodeYear(int year)
    {
        if (year == 0)
            return -1;
        else if (year >= 2000 && year <= 2010)
            return 10;
        else
        {
            int lastDigit = year % 10;
            int decadeDigit = ((year - lastDigit) / 10) % 10;
            return (byte)decadeDigit;
        }
    }

    public static String yearToDecade(int encodedYear)
    {
        if (encodedYear > 10 || encodedYear < -1)
            throw new IllegalArgumentException(String.format("Passed year (%d) doesn't seem to be properly encoded", encodedYear));

        switch(encodedYear)
        {
            case -1:
                return "Unknown";
            case 10:
                return "2000s";
            default:
                return String.format("19%d0s", encodedYear);
        }
    }

    public static class GenreCountsPerDecadeMapper extends Mapper<Object, Text, Text, IntWritable>
    {
        private final Text decadeGenre = new Text();
        private final IntWritable ONE = new IntWritable(1);

        @Override
        protected void setup(Context context)
        {
            decadeGenre.clear();
        }

        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (d.isValid())
            {
                int year = d.getYear();
                if (year == 0)
                    return;
                int encodedYear = encodeYear(year);
                String decade = yearToDecade(encodedYear);
                for(String g: d.getArtistTerms())
                {
                    decadeGenre.set(decade+";"+g);
                    context.write(decadeGenre, ONE);
                }
            }
        }
    }

    public static class GenreCountsPerDecadeReducer extends Reducer<Text, IntWritable, Text, IntWritable>
    {
        private IntWritable count = new IntWritable(0);

        @Override
        protected void setup(Context context)
        {
            count.set(0);
        }

        @Override
        public void reduce(Text decadeYear, Iterable<IntWritable> counts, Context context) throws IOException, InterruptedException
        {
            int totalCount = 0;
            for(IntWritable count: counts)
            {
                totalCount += count.get();
            }
            count.set(totalCount);
            context.write(decadeYear, count);
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "analysis");
            // Current class.
            job.setJarByClass(Analysis.class);
            // Mapper
            job.setMapperClass(Analysis.GenreCountsPerDecadeMapper.class);
            // Reducer
            job.setReducerClass(Analysis.GenreCountsPerDecadeReducer.class);
            // Combiner
            job.setCombinerClass(Analysis.GenreCountsPerDecadeReducer.class);
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
