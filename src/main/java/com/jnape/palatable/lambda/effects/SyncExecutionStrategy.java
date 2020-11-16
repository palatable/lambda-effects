package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.functions.Fn0;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;

import static com.jnape.palatable.lambda.effects.IO.io;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.recurse;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.terminate;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;

public final class SyncExecutionStrategy<A> implements ExecutionStrategy<A, A> {

    private static final SyncExecutionStrategy<?> INSTANCE = new SyncExecutionStrategy<>();

    private SyncExecutionStrategy() {
    }

    @Override
    public A execute(A a) {
        return a;
    }

    @Override
    public A execute(Fn0<? extends A> computation) {
        return computation.apply();
    }

    @Override
    public A execute(IO<A> io) {
        return trampoline(SyncExecutionStrategy::resume, io);
    }

    private static <A> RecursiveResult<IO<A>, A> resume(IO<A> ioA) {
        if (ioA instanceof IO.Sequential<?, ?>) {
            return recurse(((IO.Sequential<?, A>) ioA).interpret(new IO.Sequential.Psi<>() {
                @Override
                public <Z> IO<A> apply(IO<Z> ioZ, Fn1<? super Z, ? extends IO<A>> f) {
                    if (ioZ instanceof IO.Sequential<?, ?>) {
                        return ((IO.Sequential<?, Z>) ioZ).interpret(new IO.Sequential.Psi<>() {
                            @Override
                            public <Y> IO<A> apply(IO<Y> ioY, Fn1<? super Y, ? extends IO<Z>> g) {
                                return ioY.flatMap(y -> g.apply(y).flatMap(f));
                            }
                        });
                    } else if (ioZ instanceof IO.Parallel<?, ?>) {
                        return ((IO.Parallel<?, Z>) ioZ).interpret(new IO.Parallel.Psi<>() {
                            @Override
                            public <Y> IO<A> apply(IO<Y> ioY, IO<Fn1<? super Y, ? extends Z>> ioG) {
                                return ioY.flatMap(y -> ioG.flatMap(g -> io(g.apply(y)).flatMap(f)));
                            }
                        });
                    }

                    return f.apply(ioZ.unsafePerformIO());
                }
            }));
        } else if (ioA instanceof IO.Parallel<?, ?>) {
            return recurse(((IO.Parallel<?, A>) ioA).interpret(new IO.Parallel.Psi<>() {
                @Override
                public <Z> IO<A> apply(IO<Z> ioZ, IO<Fn1<? super Z, ? extends A>> ioG) {
                    return ioZ.flatMap(z -> ioG.flatMap(g -> io(g.apply(z))));
                }
            }));
        }

        return terminate(ioA.unsafeExecute(syncExecutionStrategy()));
    }

    @SuppressWarnings("unchecked")
    public static <A> SyncExecutionStrategy<A> syncExecutionStrategy() {
        return (SyncExecutionStrategy<A>) INSTANCE;
    }
}
