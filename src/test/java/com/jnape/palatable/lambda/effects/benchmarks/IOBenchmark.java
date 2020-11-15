package com.jnape.palatable.lambda.effects.benchmarks;

import com.jnape.palatable.lambda.effects.IO;
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

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(5)
public class IOBenchmark {

    public static void main(String[] args) throws RunnerException {
        runBenchmarks(IOBenchmark.class);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @OperationsPerInvocation(K100)
    public Integer largeLeftAssociatedSuspendedFlatMap(State state) {
        return state.leftAssociatedSuspendedIO.unsafePerformIO();
    }

    @org.openjdk.jmh.annotations.Benchmark
    @OperationsPerInvocation(K100)
    public Integer largeLeftAssociatedValueFlatMap(State state) {
        return state.leftAssociatedValueIO.unsafePerformIO();
    }

    @org.openjdk.jmh.annotations.Benchmark
    @OperationsPerInvocation(K100)
    public Integer largeRightAssociatedSuspendedFlatMap(State state) {
        return state.rightAssociatedSuspendedIO.unsafePerformIO();
    }

    @org.openjdk.jmh.annotations.Benchmark
    @OperationsPerInvocation(K100)
    public Integer largeRightAssociatedValueFlatMap(State state) {
        return state.rightAssociatedValueIO.unsafePerformIO();
    }

    @org.openjdk.jmh.annotations.State(Scope.Benchmark)
    public static class State {
        IO<Integer> leftAssociatedSuspendedIO;
        IO<Integer> leftAssociatedValueIO;
        IO<Integer> rightAssociatedSuspendedIO;
        IO<Integer> rightAssociatedValueIO;

        @Setup(Level.Trial)
        public void doSetup() {
            leftAssociatedSuspendedIO  = times(K100, io -> io.flatMap(x -> io(() -> x + 1)), io(() -> 0));
            leftAssociatedValueIO      = times(K100, io -> io.flatMap(x -> io(x + 1)), io(0));
            rightAssociatedSuspendedIO = addSuspended(0);
            rightAssociatedValueIO     = addValue(0);
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