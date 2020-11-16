package com.jnape.palatable.lambda.effects.benchmarks;

import com.jnape.palatable.lambda.effects.IO;
import com.jnape.palatable.lambda.functions.Fn1;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import static com.jnape.palatable.lambda.effects.IO.io;
import static com.jnape.palatable.lambda.effects.benchmarks.Benchmark.K100;
import static com.jnape.palatable.lambda.effects.benchmarks.Benchmark.runBenchmarks;
import static com.jnape.palatable.lambda.functions.builtin.fn3.Times.times;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

public class IOBenchmark {

    public static void main(String[] args) throws RunnerException {
        runBenchmarks(FlatMapSyncBenchmark.class);
        runBenchmarks(ZipSyncBenchmark.class);
    }

    @BenchmarkMode(Throughput)
    @OutputTimeUnit(MICROSECONDS)
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(5)
    public static class FlatMapSyncBenchmark {

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer leftAssociatedSuspended(State state) {
            return state.leftAssociatedSuspended.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer leftAssociatedValue(State state) {
            return state.leftAssociatedValue.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer rightAssociatedSuspended(State state) {
            return state.rightAssociatedSuspended.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer rightAssociatedValue(State state) {
            return state.rightAssociatedValue.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.State(Scope.Benchmark)
        public static class State {
            IO<Integer> leftAssociatedSuspended;
            IO<Integer> leftAssociatedValue;
            IO<Integer> rightAssociatedSuspended;
            IO<Integer> rightAssociatedValue;

            @Setup(Level.Trial)
            public void doSetup() {
                leftAssociatedSuspended  = times(K100, io -> io.flatMap(x -> io(() -> x + 1)), io(() -> 0));
                leftAssociatedValue      = times(K100, io -> io.flatMap(x -> io(x + 1)), io(0));
                rightAssociatedSuspended = addSuspended(0);
                rightAssociatedValue     = addValue(0);
            }

            private static IO<Integer> addSuspended(Integer x) {
                return x < K100
                       ? io(() -> x + 1).flatMap(State::addSuspended)
                       : io(x);
            }

            private static IO<Integer> addValue(Integer x) {
                return x < K100
                       ? io(x + 1).flatMap(State::addValue)
                       : io(x);
            }
        }
    }

    @BenchmarkMode(Throughput)
    @OutputTimeUnit(MICROSECONDS)
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(5)
    public static class ZipSyncBenchmark {

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer leftLeaningSuspended(State state) {
            return state.leftLeaningSuspended.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer leftLeaningValue(State state) {
            return state.leftLeaningValue.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer rightLeaningSuspended(State state) {
            return state.rightLeaningSuspended.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.Benchmark
        @OperationsPerInvocation(K100)
        public Integer rightLeaningValue(State state) {
            return state.rightLeaningValue.unsafePerformIO();
        }

        @org.openjdk.jmh.annotations.State(Scope.Benchmark)
        public static class State {
            IO<Integer> leftLeaningSuspended;
            IO<Integer> leftLeaningValue;
            IO<Integer> rightLeaningSuspended;
            IO<Integer> rightLeaningValue;

            @Setup(Level.Trial)
            public void doSetup() {
                leftLeaningSuspended  = times(K100, io -> io.zip(io(() -> x -> x + 1)), io(() -> 0));
                leftLeaningValue      = times(K100, io -> io.zip(io(x -> x + 1)), io(0));
                rightLeaningSuspended = times(K100, io -> IO.<Fn1<Integer, Integer>>io(() -> x -> x + 1)
                        .zip(io.flatMap(x -> io(() -> f -> f.apply(x)))), io(() -> 0));
                rightLeaningValue     = times(K100, io -> IO.<Fn1<Integer, Integer>>io(x -> x + 1)
                        .zip(io.flatMap(x -> io(f -> f.apply(x)))), io(0));
            }
        }
    }
}