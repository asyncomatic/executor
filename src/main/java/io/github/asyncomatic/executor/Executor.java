package io.github.asyncomatic.executor;

import io.github.asyncomatic.constants.Constants;
import io.github.asyncomatic.readers.Reader;
import io.github.asyncomatic.workers.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class Executor {
    private Reader reader;
    private Worker worker;

    static Logger logger = LoggerFactory.getLogger(Executor.class);

    public Executor(String queueTopic, URI schedulerURI) {
        try {
            String worker_class = System.getenv().getOrDefault("AOM_WORKER_CLASS", Constants.DEFAULT_WORKER_CLASS);
            worker = (Worker) Class.forName(worker_class).getConstructor(URI.class).newInstance(schedulerURI);

            String queue_class = System.getenv().getOrDefault("AOM_QUEUE_READER_CLASS", Constants.DEFAULT_READER_CLASS);
            reader = (Reader) Class.forName(queue_class).getConstructor(String.class).newInstance(queueTopic);

        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        reader.listen(worker);
    }

}
