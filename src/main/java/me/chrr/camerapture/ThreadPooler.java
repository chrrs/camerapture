package me.chrr.camerapture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadPooler {
    private static final Executor executor = Executors.newCachedThreadPool();

    public static void run(Runnable runnable) {
        executor.execute(runnable);
    }
}
