package com.codepoetics.justin;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class IteratingTest {

    @Test
    public void producerBlocksUntilConsumerConsumes() throws InterruptedException {
        AtomicBoolean step1 = new AtomicBoolean(false);
        Iterator<Integer> iter = Iterating.over((iteration) -> {
            iteration.produce(1);
            step1.set(true);
        });

        Thread.sleep(100);
        assertFalse(step1.get());

        assertEquals(1, iter.next());

        Thread.sleep(100);
        assertTrue(step1.get());
    }

    @Test
    public void hasNextIsFalseAfterAllElementsConsumed() {
        Iterator<Integer> iter = Iterating.over((iteration) -> {
            iteration.produce(1);
            iteration.produce(2);
        });

        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void hasNextPropagatesException() {
        Iterator<Integer> iter = Iterating.over((iteration) -> {
            throw new IllegalStateException("Out of cheese");
        });

        assertThrows(IterationException.class, iter::hasNext);
    }


}
