//  Copyright (c) 2024 JC Cormier
//  All rights reserved.
//  SPDX-License-Identifier: MIT
//  For full license text, see LICENSE file in the repo root or https://opensource.org/licenses/MIT

package io.github.asyncomatic.readers.kafka;


import io.github.asyncomatic.workers.Worker;
import io.github.asyncomatic.readers.Reader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class KafkaReader implements Reader {
    public static final String DEFAULT_BOOTSTRAP_SERVERS = "127.0.0.1:9094";
    public static final String DEFAULT_CONSUMER_POLL_DURATION_MAX = "100";
    public static final String DEFAULT_CONSUMER_POLL_RECORDS_MAX = "1";
    public static final String DEFAULT_CONSUMER_GROUP_ID = "aom_default_cg";
    public static final String DEFAULT_CONSUMER_GROUP_INSTANCE_ID = "aom_default_cgi";
    public static final String DEFAULT_CONSUMER_AUTO_OFFSET_RESET = "earliest";

    private final Duration pollInterval;
    private final String topic;

    private final KafkaConsumer<String, String> kafkaConsumer;

    static Logger logger = LoggerFactory.getLogger(KafkaReader.class);

    public KafkaReader(String topic) {
        this.topic = topic;
        this.pollInterval = Duration.ofMillis(Long.parseLong(
                System.getenv().getOrDefault("KAFKA_CONSUMER_POLL_DURATION_MAX",
                        DEFAULT_CONSUMER_POLL_DURATION_MAX)));

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS",
                        DEFAULT_BOOTSTRAP_SERVERS));
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,
                System.getenv().getOrDefault("KAFKA_CONSUMER_GROUP_ID",
                        DEFAULT_CONSUMER_GROUP_ID));
        properties.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,
                System.getenv().getOrDefault("KAFKA_CONSUMER_GROUP_INSTANCE_ID",
                        DEFAULT_CONSUMER_GROUP_INSTANCE_ID));
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                System.getenv().getOrDefault("KAFKA_CONSUMER_AUTO_OFFSET_RESET",
                        DEFAULT_CONSUMER_AUTO_OFFSET_RESET));
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                System.getenv().getOrDefault("KAFKA_CONSUMER_POLL_RECORDS_MAX",
                        DEFAULT_CONSUMER_POLL_RECORDS_MAX));

        kafkaConsumer = new KafkaConsumer<>(properties);
    }

    public void listen(Worker worker) {
        kafkaConsumer.subscribe(List.of(topic));
        do {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(pollInterval);
            for (ConsumerRecord<String, String> record : records) {
                String testJSON = record.value();

                worker.execute(testJSON);
            }
        } while (true);
    }
}
