package com.jnape.palatable.lambda.effects;

import static com.jnape.palatable.lambda.effects.IO.io;
import static com.jnape.palatable.lambda.functions.builtin.fn3.Times.times;

public class Demo {

    public static void main(String[] args) {

//        IO<Integer> times = times(10_000_000, io -> io.flatMap(x -> io(() -> x + 1)), io(() -> 0));
//        System.out.println("starting");
//        times.unsafePerformIO();
//        System.out.println("done");

        times(10_000, io -> io.flatMap(x -> io(x + 1)), io(()-> 0))
                .unsafePerformIO();
    }
}
