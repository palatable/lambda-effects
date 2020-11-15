package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.functions.Fn0;
import com.jnape.palatable.lambda.functions.Fn1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.jnape.palatable.lambda.effects.AsyncExecutionStrategy.asyncExecutionStrategy;
import static com.jnape.palatable.lambda.effects.SyncExecutionStrategy.syncExecutionStrategy;

public interface IO<A> {

    <B> B unsafeExecute(ExecutionStrategy<A, B> executionStrategy);

    default A unsafePerformIO() {
        return unsafeExecute(syncExecutionStrategy());
    }

    //todo: probably deprecate this
    default CompletableFuture<A> unsafePerformAsyncIO(Executor executor) {
        return unsafeExecute(asyncExecutionStrategy(executor));
    }

    //todo: probably deprecate this
    default CompletableFuture<A> unsafePerformAsyncIO() {
        return unsafePerformAsyncIO(ForkJoinPool.commonPool());
    }

    static <A> IO<A> io(A a) {
        return new Value<>(a);
    }

    static <A> IO<A> io(Fn0<? extends A> computation) {
        return new Suspended<>(computation);
    }


    default <B> IO<B> flatMap(Fn1<? super A, ? extends IO<B>> f) {
        return new Sequential<>(this, f);
    }

    final class TrampoliningPlatform implements Platform {

        @Override
        public <A, B> B unsafePerform(IO<A> io, ExecutionStrategy<A, B> executionStrategy) {
            return io.unsafeExecute(executionStrategy);
        }
    }

    final class Value<A> implements IO<A> {

        private final A a;

        public Value(A a) {
            this.a = a;
        }

        @Override
        public <B> B unsafeExecute(ExecutionStrategy<A, B> executionStrategy) {
            return executionStrategy.execute(a);
        }
    }

    final class Suspended<A> implements IO<A> {

        private final Fn0<? extends A> computation;

        public Suspended(Fn0<? extends A> computation) {
            this.computation = computation;
        }

        @Override
        public <B> B unsafeExecute(ExecutionStrategy<A, B> executionStrategy) {
            return executionStrategy.execute(computation);
        }
    }

    final class Sequential<A, B> implements IO<B> {

        private final IO<A>                           io;
        private final Fn1<? super A, ? extends IO<B>> f;

        private Sequential(IO<A> io, Fn1<? super A, ? extends IO<B>> f) {
            this.io = io;
            this.f  = f;
        }

        interface Psi<R, B> {
            <A> R apply(IO<A> io, Fn1<? super A, ? extends IO<B>> f);
        }

        @Override
        public <C> C unsafeExecute(ExecutionStrategy<B, C> executionStrategy) {
            return executionStrategy.execute(this);
        }

        public <R> R interpret(Psi<R, B> psi) {
            return psi.apply(io, f);
        }
    }
}
