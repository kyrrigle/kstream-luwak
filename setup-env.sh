

if [ -n "$1" ]; then
    export CONFLUENT_HOME="$1"
fi

if [ -z "$CONFLUENT_HOME" ]; then
    echo "CONFLUENT_HOME env is required"
elif [ ! -x $CONFLUENT_HOME/bin/kafka-server-start ]; then
    echo "This '$CONFLUENT_HOME' doesn't look like a confluent install?"
else
    alias kt="$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181"
    alias kcp="$CONFLUENT_HOME/bin/kafka-console-producer --broker-list localhost:9092"
    alias kcc="$CONFLUENT_HOME/bin/kafka-console-consumer --zookeeper localhost:2181"

    function reset-demo {
        echo "Resetting demo"
        create-topics
        echo "Performing application reset"
        $CONFLUENT_HOME/bin/kafka-streams-application-reset \
            --application-id kstream-luwak-plus-example-docs \
            --input-topics luwak-monitor-input
        $CONFLUENT_HOME/bin/kafka-streams-application-reset \
            --application-id kstream-luwak-plus-example-q-Matts-MacBook-Pro.local \
            --input-topics luwak-monitor-queries-topic


    }

    function create-topics {
        if [ -z "$(kt --list --topic luwak-monitor-input )" ]; then
            kt --create --replication-factor 1 --partitions 10 --topic luwak-monitor-input
        fi
        if [ -z "$(kt --list --topic luwak-monitor-output )" ]; then
            kt --create --replication-factor 1 --partitions 10 --topic luwak-monitor-output
        fi
        if [ -z "$(kt --list --topic luwak-monitor-queries-topic )" ]; then
            kt --create --replication-factor 1 --partitions 1 --topic luwak-monitor-queries-topic --config cleanup.policy=compact
        fi
    }

    function delete-topics {
        kt --delete --topic luwak-monitor-input
        kt --delete --topic luwak-monitor-output
        kt --delete --topic luwak-monitor-queries-topic
    }
fi
