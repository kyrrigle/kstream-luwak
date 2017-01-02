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

        $ export CONFLUENT_HOME=/path/to/confluent-3.0.0

2. Build the demo project

        $ mvm clean install
    
3. Prepare our data
  
        $ $CONFLUENT_HOME/bin kafka-topics --create \
                  --zookeeper localhost:2181 \
                  --replication-factor 1 \
                  --partitions 1 \
                  --topic luwak-monitor-input
                  
        $ cat ./test-data.txt | $CONFLUENT_HOME/bin/kafka-console-producer \
                  --broker-list localhost:9092 \
                  --topic luwak-monitor-input

4. Run the demo

        $ mvn exec:java -Dexec.args="basic"
        ...
        754 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Added query query1 = foo AND bar
        763 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Added query query2 = bar AND baz
        788 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc1 matched 1 queries
        898 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc2 matched 1 queries
        900 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Document doc3 matched 2 queries
        901 [StreamThread-1] INFO com.lexalytics.kstreamluwak.FilteringMonitor  - Looks like we're done

5. Check the output

        $ $CONFLUENT_HOME/bin/kafka-console-consumer \
                 --zookeeper localhost:2181 \
                 --topic luwak-monitor-output \
                 --from-beginning
        d doc1 this document contains the words foo and bar only
        d doc2 this document, on the other hand, mentions bar and baz.
        d doc3 this one goes nuts and mentions foo, bar and baz -- all three!

    and viola doc3 didn't show up in the output topic!

##Plus Example
In addition to the above "basic" there is a "plus" version that introduces a second input topic for the queries and leaves the input queue for documents only.  Additionaly now the topic items need to have proper keys which are used as the corresponding id for luwak.

For this version I wanted to be able to scale out the search part while having a single set of queries that will persist between runs (and be restored when a new instance starts up).  The best way I could find to achieve this was to have a second Kafka Stream application for the queries that starts up with a node-specific application id (currently appending the hostname) and a single topic partition. This application uses a [state store](http://docs.confluent.io/3.0.0/streams/developer-guide.html#defining-a-state-store) to keep track of the queries seen.  Not sure this is the *best* way to achieve this but it seems to work. YMMV

To run this demo you can also use some bash commandline helpers defined in the set-env.sh script.  First source the file, giving it the path to your confluent install directory:

    $ . setup-env.sh /Users/mking/confluent-3.1.1

Now you can create the topics and load the test data:

    $ create-topics
    Created topic "luwak-monitor-input".
    Created topic "luwak-monitor-output".
    Created topic "luwak-monitor-queries-topic".
    $ cat queries.txt | kcp --topic luwak-monitor-queries-topic --property parse.key=true --property key.separator=,
    $ cat docs.txt | kcp --topic luwak-monitor-input --property parse.key=true --property key.separator=,

And run the plus version of the code:

    $ mvn exec:java -Dexec.args="plus"
    ...
    1554 [StreamThread-3] INFO com.lexalytics.kstreamluwak.plus.FilteringMonitor  - Document doc1 matched 1 queries
    1554 [StreamThread-2] INFO com.lexalytics.kstreamluwak.plus.FilteringMonitor  - Document doc2 matched 1 queries
    1668 [StreamThread-2] INFO com.lexalytics.kstreamluwak.plus.FilteringMonitor  - Document doc3 matched 2 queries

Lastly check the output queue:

    $ kcc --topic luwak-monitor-output --from-beginning --property print.key=true --property key.separator=,
    Using the ConsoleConsumer with old consumer is deprecated and will be removed in a future major release. Consider using the new consumer by passing [bootstrap-server] instead of [zookeeper].
    doc1,this document contains the words foo and bar only
    doc2,this document, on the other hand, mentions bar and baz.
    doc3,this one goes nuts and mentions foo, bar and baz -- all three!
