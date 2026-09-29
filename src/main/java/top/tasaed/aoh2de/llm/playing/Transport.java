package top.tasaed.aoh2de.llm.playing;

import java.io.IOException;

public interface Transport extends AutoCloseable {
    void start() throws IOException;

    boolean isRunning();

    @Override
    void close();
}
