package io.github.asyncomatic.services;

import io.github.asyncomatic.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ExecutorService {
    private static final String DEFAULT_SCHEDULER_SCHEME = "http://";
    private static final String DEFAULT_SCHEDULER_HOST = "127.0.0.1";
    private static final String DEFAULT_SCHEDULER_PORT = ":8080";
    private static final String DEFAULT_SCHEDULER_ROUTE = "/jobs";

    private static final String DEFAULT_KAFKA_TOPIC = "aom.default.topic";

    static Logger logger = LoggerFactory.getLogger(ExecutorService.class);

    public static void main(String[] args) {

        logger.info("Starting ExecutorService");
        try {
            String scheme = System.getenv().getOrDefault("SCHEDULER_SCHEME", DEFAULT_SCHEDULER_SCHEME);
            String host = System.getenv().getOrDefault("SCHEDULER_HOST", DEFAULT_SCHEDULER_HOST);
            String port = System.getenv().getOrDefault("SCHEDULER_PORT", DEFAULT_SCHEDULER_PORT);
            String route = System.getenv().getOrDefault("SCHEDULER_ROUTE", DEFAULT_SCHEDULER_ROUTE);
            URI schedulerURI = URI.create(scheme + host + port + route);

            String queueTopic = System.getenv().getOrDefault("KAFKA_TOPIC", DEFAULT_KAFKA_TOPIC);

            Executor testExecutor = new Executor(queueTopic, schedulerURI);
            testExecutor.start();

        }catch(Exception e) {
            logger.error("Failed starting ExecutorService");
            logger.error(e.toString());

            System.exit(1);
        }
    }
}
