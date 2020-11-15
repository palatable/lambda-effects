package com.jnape.palatable.lambda.effects;
public interface Platform {

    <A, B> B unsafePerform(IO<A> io, ExecutionStrategy<A, B> executionStrategy);

}
