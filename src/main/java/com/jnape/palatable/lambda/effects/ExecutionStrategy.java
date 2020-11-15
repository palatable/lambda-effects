package com.jnape.palatable.lambda.effects;

import com.jnape.palatable.lambda.functions.Fn0;

public interface ExecutionStrategy<A, B> {

    B execute(A a);

    B execute(Fn0<? extends A> computation);

    B execute(IO<A> io);
}
