package com.lexalytics.kstreamluwak.plus;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.processor.TopologyBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.log4j.Level;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.slf4j.Logger;

/**
 * Demonstrates, using the high-level KStream DSL, how to use Luwak
 */
public class KStreamLuwakPlusExample {
	public static final Logger logger = LoggerFactory.getLogger(KStreamLuwakPlusExample.class);
	private static FilteringMonitor monitor;
	private static QueryProcessor queryProcessor;

	public static KafkaStreams loadQueries(Properties props) {
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		String[] p = processName.split("@");
		String hostName = p[1];
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-luwak-plus-example-q-" + hostName); // needs to be unique to node
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1);
        // setting offset reset to earliest so we'll get the whole table when we first start up
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        KStreamBuilder builder = new KStreamBuilder();

        StateStoreSupplier queryStore = Stores.create("Queries")
                .withStringKeys()
                .withStringValues()
                .persistent()
                .build();

        queryProcessor = new QueryProcessor(monitor);
        builder.addSource("Source", "luwak-monitor-queries-topic")
        	.addProcessor("Process", () -> queryProcessor, "Source")
        	.addStateStore(queryStore, "Process");
        
        KafkaStreams streams = new KafkaStreams(builder, props);
        return streams;
	}
	
    public static void main(String[] args) throws Exception {
    	ArgumentParser parser = ArgumentParsers.newArgumentParser("Checksum")
                .defaultHelp(true)
                .description("Calculate checksum of given files.");
    	parser.addArgument("--reset").action(Arguments.storeTrue());
    	Namespace ns = parser.parseArgs(args);

    	boolean clientReset = ns.getBoolean("reset");
    	
    	org.apache.log4j.BasicConfigurator.configure();
    	org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    	// Create the monitor
        monitor = new FilteringMonitor();

    	// Common properties
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "localhost:2181");
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Set up the query processing
        KafkaStreams q_streams = loadQueries(props);

        // Continue on with document processing
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-luwak-plus-example-docs");
 
        KStreamBuilder builder = new KStreamBuilder();

        KStream<String, String> source = builder.stream("luwak-monitor-input");
        source.filter(monitor).to(Serdes.String(), Serdes.String(), "luwak-monitor-output");

        KafkaStreams streams = new KafkaStreams(builder, props);

        if (clientReset) {
        	logger.info("Client reset requested");
        	streams.cleanUp();
        	q_streams.cleanUp();
        }
        q_streams.start();
        
        // wait for the query processor to set itself up
        while (!queryProcessor.isInitialized()) {
        	Thread.sleep(1000L);
        }
        
        streams.start();

        while (!monitor.isDone()) {
        	Thread.sleep(1000L);
        }

        streams.close();
    }
}

