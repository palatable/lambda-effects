package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.functions.Fn0;
import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.effects.SyncExecutionStrategy.syncExecutionStrategy;

public interface IO<A> {

    <B> B unsafeExecute(ExecutionStrategy<A, B> executionStrategy);

    default A unsafePerformIO() {
        return unsafeExecute(syncExecutionStrategy());
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

    default <B> IO<B> zip(IO<Fn1<? super A, ? extends B>> ioF) {
        return new Parallel<>(this, ioF);
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

        private final IO<A>                           ioA;
        private final Fn1<? super A, ? extends IO<B>> f;

        private Sequential(IO<A> ioA, Fn1<? super A, ? extends IO<B>> f) {
            this.ioA = ioA;
            this.f   = f;
        }

        interface Psi<R, B> {
            <A> R apply(IO<A> io, Fn1<? super A, ? extends IO<B>> f);
        }

        @Override
        public <C> C unsafeExecute(ExecutionStrategy<B, C> executionStrategy) {
            return executionStrategy.execute(this);
        }

        public <R> R interpret(Psi<R, B> psi) {
            return psi.apply(ioA, f);
        }
    }

    final class Parallel<A, B> implements IO<B> {

        private final IO<A>                           ioA;
        private final IO<Fn1<? super A, ? extends B>> ioF;

        private Parallel(IO<A> ioA, IO<Fn1<? super A, ? extends B>> ioF) {
            this.ioA = ioA;
            this.ioF = ioF;
        }

        interface Psi<R, B> {
            <A> R apply(IO<A> ioA, IO<Fn1<? super A, ? extends B>> ioF);
        }

        public <R> R interpret(Parallel.Psi<R, B> psi) {
            return psi.apply(ioA, ioF);
        }

        @Override
        public <C> C unsafeExecute(ExecutionStrategy<B, C> executionStrategy) {
            return executionStrategy.execute(this);
        }
    }
}
