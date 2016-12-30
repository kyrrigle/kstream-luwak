package com.lexalytics.kstreamluwak;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.streams.kstream.Predicate;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.luwak.*;
import uk.co.flax.luwak.matchers.SimpleMatcher;
import uk.co.flax.luwak.presearcher.TermFilteredPresearcher;
import uk.co.flax.luwak.queryparsers.LuceneQueryParser;

public class FilteringMonitor implements Predicate<String, String> {

	public static final Logger logger = LoggerFactory.getLogger(FilteringMonitor.class);
	private Monitor monitor;
	private String defaultField = "text";
	private boolean done = false;

	public FilteringMonitor()  throws IOException {
		monitor = new Monitor(new LuceneQueryParser(defaultField), new TermFilteredPresearcher());
	}	

	/* (non-Javadoc)
	 * @see org.apache.kafka.streams.kstream.Predicate#test(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean test(String key, String value) {
		if (value.startsWith("q ")) {
			String[] parts = value.split(" ", 3);
			if (parts.length != 3) {
				logger.error("Unexpected query {}", value);
				return false;
			}
			String query_id = parts[1];
			String query_query = parts[2];
			MonitorQuery mq = new MonitorQuery(query_id, query_query);
			try {
				List<QueryError> errors = monitor.update(mq);
				if (errors.size() > 0) {
					logger.error("There were errors with this query {}", value);
				}
			} catch (IOException e) {
				logger.error("Failed to update monitor", e);
			}
			logger.info("Added query {} = {}", query_id, query_query);
			return false;
		}
		else if (value.startsWith("d ")) {
			String[] parts = value.split(" ", 3);
			if (parts.length != 3) {
				logger.error("Unexpected document {}", value);
				return false;
			}
			String doc_id = parts[1];
			String doc_text = parts[2];
			InputDocument doc = InputDocument.builder(doc_id)
					.addField(defaultField, doc_text, new StandardAnalyzer())
					.build();
			try {
				Matches<QueryMatch> matches = monitor.match(doc, SimpleMatcher.FACTORY);
				int matchCount = matches.getMatchCount(doc_id);
				if (matchCount > 0) {
					logger.info("Document {} matched {} queries", doc_id, matchCount);
					return true;
				}
			} catch (IOException e) {
				logger.error("Failed to match document", e);
			}
			return false;
		}
		else if (value.equalsIgnoreCase("quit")) {
			done = true;
			logger.info("Looks like we're done");
			try {
				monitor.close();
			} catch (IOException e) {
				logger.error("Failed to close cleanly", e);
			}
			return false;
		}
		logger.warn("I don't know what to do with " + value);
		return false;
	}

	public boolean isDone() {
		return done;
	}

	public void close() {
		done = true;
		try {
			monitor.close();
		} catch (IOException e) {
			logger.error("Failed to close cleanly", e);
		}
		
	}

}
