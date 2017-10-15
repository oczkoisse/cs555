package a3.jobs;

import a3.data.Data;
import a3.io.StringTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.*;

public class ArtistGenre
{
    public static class ArtistGenreMapper extends Mapper<Object, Text, Text, StringTuple>
    {

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (!d.isHeader()) {
                List<String> genres = d.getArtistTerms();
                if(genres != null && genres.size() > 0)
                    context.write(new Text(d.getArtistName()), new StringTuple(genres));
            }
        }
    }

    public static class ArtistGenreReducer extends Reducer<Text, StringTuple, Text, Text>
    {
        @Override
        protected void reduce(Text artist, Iterable<StringTuple> genres, Context context) throws IOException, InterruptedException
        {
            String mostCommonGenre = null;

            HashMap<String, Integer> counts = new HashMap<>();

            for(StringTuple genreTuple: genres)
            {
                for(int i=0; i<genreTuple.size(); i++) {
                    String genre = genreTuple.get(i);
                    counts.putIfAbsent(genre, 0);
                    counts.replace(genre, counts.get(genre) + 1);
                }
            }

            Set<String> mostCommonGenres = new HashSet<>();
            Integer mostCommonGenreCount = Integer.MIN_VALUE;
            for(Map.Entry<String,Integer> entry : counts.entrySet()) {
                if(entry.getValue() > mostCommonGenreCount) {
                    mostCommonGenres.clear();
                    mostCommonGenres.add(entry.getKey());
                    mostCommonGenreCount = entry.getValue();
                }
                else if(entry.getValue().equals(mostCommonGenreCount))
                {
                    mostCommonGenres.add(entry.getKey());
                }
            }

            if (mostCommonGenres.size() > 0) {
                mostCommonGenre = String.join(", ", mostCommonGenres);
                mostCommonGenre += " (" + mostCommonGenreCount + ")";
            }

            if (mostCommonGenre != null)
                context.write(artist, new Text(mostCommonGenre));
        }
    }

    /**
    public static class ArtistGenreCombiner extends Reducer<Text, StringTuple, Text, StringTuple>
    {
        @Override
        protected void reduce(Text artist, Iterable<StringTuple> genres, Context context) throws IOException, InterruptedException
        {
            HashMap<String, Integer> counts = new HashMap<>();

            for(StringTuple genreTuple: genres)
            {
                for(int i=0; i<genreTuple.size(); i++) {
                    String genre = genreTuple.get(i);
                    counts.putIfAbsent(genre, 0);
                    counts.replace(genre, counts.get(genre) + 1);
                }
            }

            Set<String> mostCommonGenres = new HashSet<>();
            Integer mostCommonGenreCount = Integer.MIN_VALUE;
            for(Map.Entry<String,Integer> entry : counts.entrySet()) {
                if(entry.getValue() > mostCommonGenreCount) {
                    mostCommonGenres.clear();
                    mostCommonGenres.add(entry.getKey());
                    mostCommonGenreCount = entry.getValue();
                }
                else if(entry.getValue().equals(mostCommonGenreCount))
                {
                    mostCommonGenres.add(entry.getKey());
                }
            }


            StringTuple s = new StringTuple(mostCommonGenres.toArray(new String[mostCommonGenres.size()]));
            for(int i=0; i<mostCommonGenreCount; i++)
                context.write(artist, s);
        }
    }
    */
    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q1");
            // Current class.
            job.setJarByClass(ArtistGenre.class);
            // Mapper
            job.setMapperClass(ArtistGenreMapper.class);
            // Combiner.
            //job.setCombinerClass(ArtistGenreCombiner.class);
            // Reducer
            job.setReducerClass(ArtistGenreReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(StringTuple.class);


            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
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
