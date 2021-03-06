package org.training.hadoop.mapreduce;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class InvertIndex {

  public static class IndexMapper extends Mapper<Object, Text, Text, Text> {
    public void map(Object key,
                    Text value,
                    Context context) throws IOException, InterruptedException {
      //Get the name of the file using context.getInputSplit()method
      String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
      //Split the line in words
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
        //For each word emit word as key and file name as value
        context.write(new Text(itr.nextToken()), new Text(fileName));
      }
    }
  }

  public static class IndexReducer extends Reducer<Text, Text, Text, Text> {
    public void reduce(Text key,
                       Iterable<Text> values,
                       Context context) throws IOException, InterruptedException {
      // Declare the Hash Map to store File name as key to compute
      // and store number of times the filename is occurred for as value
      Map<String, Integer> map = new HashMap<String, Integer>();
      for (Text fileText : values) {
        String file = fileText.toString();
        if (map.containsKey(file)) {
          map.put(file, map.get(file) + 1);
        }
        else {
          map.put(file, 1);
        }
      }
      context.write(key, new Text(map.toString()));
    }
  }


  public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length < 2) {
      System.err.println("Usage: invertindex <in> [<in>...] <out>");
      System.exit(2);
    }
    Job job = Job.getInstance(conf, "Invert Index");

    job.setJarByClass(InvertIndex.class);
    job.setMapperClass(IndexMapper.class);
    job.setReducerClass(IndexReducer.class);

    //Defining the output key and value class for the mapper
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    //Defining the output key and value class for the reducer
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    for (int i = 0; i < otherArgs.length - 1; ++i) {
      FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
    }
    FileOutputFormat.setOutputPath(job,
        new Path(otherArgs[otherArgs.length - 1]));

    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
