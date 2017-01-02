package com.lexalytics.kstreamluwak.plus;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryProcessor implements Processor<String, String> {
	public static final Logger logger = LoggerFactory.getLogger(QueryProcessor.class);
	private KeyValueStore<String, String> kvStore;
	private FilteringMonitor monitor;
	private boolean initialized;

	public QueryProcessor(FilteringMonitor monitor) {
		this.monitor = monitor;
		
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		// retrieve the key-value store named "Queries"
		kvStore = (KeyValueStore<String, String>) context.getStateStore("Queries");
		if (kvStore == null) {
			logger.error("Failed to get my kv store");
			return;
		}

		logger.info("Loading queries from kv store");
		KeyValueIterator<String, String> iter = this.kvStore.all();
		while (iter.hasNext()) {
			KeyValue<String, String> entry = iter.next();
			monitor.addQuery(entry.key, entry.value);
		}
		iter.close();
		logger.info("Initialized and ready to go");
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void process(String query_id, String query_query) {
		// save it for later and forward on to the monitor
		kvStore.put(query_id, query_query);
		monitor.addQuery(query_id, query_query);
	}

	@Override
	public void punctuate(long timestamp) {
		// not needed
	}

	@Override
	public void close() {
		// close the key-value store
		kvStore.close();
	}

}
