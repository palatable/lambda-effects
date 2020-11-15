package com.jnape.palatable.lambda.effects.benchmarks;

import org.openjdk.jmh.runner.RunnerException;

public final class AllBenchmarks {
    private AllBenchmarks() {
    }

    public static void main(String[] args) throws RunnerException {
        IOBenchmark.main(args);
    }
}
