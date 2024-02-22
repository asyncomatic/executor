package io.github.asyncomatic.readers;

import io.github.asyncomatic.workers.Worker;

public interface Reader {
    public void listen(Worker worker);

}
