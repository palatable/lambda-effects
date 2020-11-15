package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.functions.Fn0;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public final class AsyncExecutionStrategy<A> implements ExecutionStrategy<A, CompletableFuture<A>> {

    private final Executor executor;

    private AsyncExecutionStrategy(Executor executor) {
        this.executor = executor;
    }

    @Override
    public CompletableFuture<A> execute(Fn0<? extends A> computation) {
        return supplyAsync(computation::apply, executor);
    }

    @Override
    public CompletableFuture<A> execute(A a) {
        return completedFuture(a);
    }

    @Override
    public CompletableFuture<A> execute(IO<A> io) {
        throw new UnsupportedOperationException("nyi");
    }

    public static <A> AsyncExecutionStrategy<A> asyncExecutionStrategy(Executor executor) {
        return new AsyncExecutionStrategy<>(executor);
    }
}
