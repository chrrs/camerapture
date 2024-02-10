package me.chrr.camerapture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadManager {
    private static final ThreadManager INSTANCE = new ThreadManager();

    private final Executor executor = Executors.newCachedThreadPool();

    private ThreadManager() {
    }

    public void run(Runnable runnable) {
        executor.execute(runnable);
    }

    public static ThreadManager getInstance() {
        return INSTANCE;
    }
}
