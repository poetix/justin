package com.codepoetics.justin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FibonacciExampleTest {

    private void generateFibonacciSequence(Iteration<Integer> iteration) {
        var a = 0;
        var b = 1;
        while (true) {
            iteration.produce(a);
            var swap = a;
            a = b;
            b = a + swap;
        }
    }

    @Test
    public void consumeFirstTenFibonacciNumbers() {
        assertEquals(List.of(0, 1, 1, 2, 3, 5, 8, 13, 21, 34),
                Streaming.over(this::generateFibonacciSequence).limit(10).toList());
    }
}
