package com.jnape.palatable.lambda.effects;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jnape.palatable.lambda.effects.IO.io;

public class Demo {

    public static void main(String[] args) throws IOException {
        System.in.read();

        IO<Integer> first = run(0, 100_000_000);

        ExecutorService ex = Executors.newFixedThreadPool(10);
        System.out.println(first.unsafeExecute(new AsyncExecutionStrategy<>(ex)).join());

        ex.shutdown();
    }

    public static IO<Integer> run(int x, int max) {
        return x < max
               ? io(() -> {
            if (x % 100_000 == 0) {
                threadLog("flatMap, x:" + x);
            }
            return x + 1;
        }).flatMap(current -> run(current, max))
               : io(x);
    }

    private static void threadLog(Object message) {
        System.out.println(Thread.currentThread() + ": " + message);
    }
}
