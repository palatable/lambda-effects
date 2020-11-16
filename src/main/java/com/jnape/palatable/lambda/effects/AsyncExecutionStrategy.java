package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.adt.Either;
import com.jnape.palatable.lambda.functions.Fn0;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.jnape.palatable.lambda.adt.Either.left;
import static com.jnape.palatable.lambda.adt.Either.right;
import static com.jnape.palatable.lambda.effects.IO.io;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.recurse;
import static com.jnape.palatable.lambda.functions.recursion.RecursiveResult.terminate;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public final class AsyncExecutionStrategy<A> implements ExecutionStrategy<A, CompletableFuture<A>> {

    private final Executor executor;

    public AsyncExecutionStrategy(Executor executor) {
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
        return new CompletableFuture<>() {{
            unsafePerformAsyncIO(io, executor, this);
        }};
    }

    private void unsafePerformAsyncIO(IO<A> io, Executor ex, CompletableFuture<A> finished) {
        resume(io, ex).whenComplete((rr, t) -> {
            if (t != null)
                finished.completeExceptionally(t);
            else
                rr.match(next -> {
                    unsafePerformAsyncIO(next, ex, finished);
                    return null;
                }, finished::complete);
        });
    }


    private static <A> CompletableFuture<RecursiveResult<IO<A>, A>> resume(IO<A> ioA, Executor executor) {
        if (ioA instanceof IO.Sequential<?, ?>) {
            return ((IO.Sequential<?, A>) ioA).interpret(new IO.Sequential.Psi<>() {
                @Override
                public <Z> CompletableFuture<RecursiveResult<IO<A>, A>> apply(IO<Z> ioZ,
                                                           Fn1<? super Z, ? extends IO<A>> f) {
                    if (ioZ instanceof IO.Sequential<?, ?>) {
                        return completedFuture(recurse(((IO.Sequential<?, Z>) ioZ).interpret(new IO.Sequential.Psi<>() {
                            @Override
                            public <Y> IO<A> apply(IO<Y> ioY, Fn1<? super Y, ? extends IO<Z>> g) {
                                return ioY.flatMap(y -> g.apply(y).flatMap(f));
                            }
                        })));
                    } else if (ioZ instanceof IO.Parallel<?, ?>) {
                        return completedFuture(recurse(((IO.Parallel<?, Z>) ioZ).interpret(new IO.Parallel.Psi<>() {
                            @Override
                            public <Y> IO<A> apply(IO<Y> ioY, IO<Fn1<? super Y, ? extends Z>> ioG) {
                                return ioY.flatMap(y -> ioG.flatMap(g -> io(g.apply(y)).flatMap(f)));
                            }
                        })));
                    }

                    return resume(ioZ, executor).thenApply(rr -> recurse(rr.match(ioZ_ -> ioZ_.flatMap(f), f)));
                }
            });
        } else if (ioA instanceof IO.Parallel<?, ?>) {
            return ((IO.Parallel<?, A>) ioA).interpret(new IO.Parallel.Psi<>() {
                @Override
                public <Z> CompletableFuture<RecursiveResult<IO<A>, A>> apply(IO<Z> ioZ,
                                                                                  IO<Fn1<? super Z, ? extends A>> ioG) {
                    return completedFuture(recurse(ioZ.flatMap(z -> ioG.flatMap(g -> io(g.apply(z))))));
                }
            });
        }

        return ioA.unsafeExecute(new AsyncExecutionStrategy<>(executor)).thenApplyAsync(RecursiveResult::terminate);
    }

    public static <A> AsyncExecutionStrategy<A> asyncExecutionStrategy(Executor executor) {
        return new AsyncExecutionStrategy<>(executor);
    }
}
