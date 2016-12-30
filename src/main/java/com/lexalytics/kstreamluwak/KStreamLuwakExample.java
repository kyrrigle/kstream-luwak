package com.lexalytics.kstreamluwak;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Demonstrates, using the high-level KStream DSL, how to use Luwak
 */
public class KStreamLuwakExample {

    public static void main(String[] args) throws Exception {
    	org.apache.log4j.BasicConfigurator.configure();
    	Logger.getRootLogger().setLevel(Level.INFO);
    	
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-luwak-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "localhost:2181");
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KStreamBuilder builder = new KStreamBuilder();

        KStream<String, String> source = builder.stream("luwak-monitor-input");
        
        FilteringMonitor monitor = new FilteringMonitor();

        KStream<String, String> filtered_output = source.filter(monitor);

        filtered_output.to(Serdes.String(), Serdes.String(), "luwak-monitor-output");

        KafkaStreams streams = new KafkaStreams(builder, props);
        streams.start();

        while (!monitor.isDone()) {
        	Thread.sleep(1000L);
        }

        streams.close();
    }
}

