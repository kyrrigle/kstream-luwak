# Kafka Streams - Luwak Proof of Concept

Inspired by the [Samza-Luwak Proof of Concept](https://github.com/romseygeek/samza-luwak) here's a version that uses [Kafka Streams](http://docs.confluent.io/3.0.0/streams/)

The demo is really simple, using a simple filter DSL to filter an input topic only letting items through that match 1 or more query using Luwak.

                        +----------+
                        |          |
    +-------------+     |  luwak   |     +----------------+
    | input topic | --> |  filter  | --> | filtered topic |
    +-------------+     |          |     +----------------+
                        +----------+

The input topic takes the same input strings that the Samza-Luwak demo did, mixing queries with the documents and a special pill to shut the demo down:

    q query1 foo AND bar
    q query2 bar AND baz
    d doc1 this document contains the words foo and bar only
    d doc2 this document, on the other hand, mentions bar and baz.
    d doc3 this one goes nuts and mentions foo, bar and baz -- all three!
    d doc4 finally, this one mentions none of those words.
    quit

To run the demo:

1. Follow the [Kafka Stream Quickstart](http://docs.confluent.io/3.0.0/streams/quickstart.html#start-the-kafka-cluster) to start zookeeper and kafka.  Stop before preparing the data.  I'm going to set an environment variable for the directory where I unpacked confluent-3.0.0

        # export CONFLUENT_HOME=/path/to/confluent-3.0.0

2. Build the demo project

        # mvm clean install
    
3. Prepare our data
  
        # $CONFLUENT_HOME/bin kafka-topics --create \
                  --zookeeper localhost:2181 \
                  --replication-factor 1 \
                  --partitions 1 \
                  --topic luwak-monitor-input
                  
        # cat ./test-data.txt | $CONFLUENT_HOME/bin/kafka-console-producer \
                  --broker-list localhost:9092 \
                  --topic luwak-monitor-input

4. Run the demo

        # mvn exec:java
        ...
        754 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Added query query1 = foo AND bar
        763 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Added query query2 = bar AND baz
        788 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc1 matched 1 queries
        898 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc2 matched 1 queries
        900 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc3 matched 2 queries
        901 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Looks like we're done

5. Check the output

        # $CONFLUENT_HOME/bin/kafka-console-consumer \
                 --zookeeper localhost:2181 \
                 --topic luwak-monitor-output \
                 --from-beginning
        d doc1 this document contains the words foo and bar only
        d doc2 this document, on the other hand, mentions bar and baz.
        d doc3 this one goes nuts and mentions foo, bar and baz -- all three!

    and viola doc3 didn't show up in the output topic!
