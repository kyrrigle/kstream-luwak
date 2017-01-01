package com.lexalytics.kstreamluwak.plus;

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

	@Override
	public boolean test(String doc_id, String doc_text) {
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

	public void addQuery(String query_id, String query_query) {
		MonitorQuery mq = new MonitorQuery(query_id, query_query);
		try {
			List<QueryError> errors = monitor.update(mq);
			if (errors.size() > 0) {
				logger.error("There were errors with this query {}", query_query);
			}
		} catch (IOException e) {
			logger.error("Failed to update monitor", e);
		}
		logger.info("Added query {} = {}", query_id, query_query);	
	}

}
