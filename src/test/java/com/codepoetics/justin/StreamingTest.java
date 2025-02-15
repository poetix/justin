package com.codepoetics.justin;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class StreamingTest {

    @Test
    public void nothingProduced() {
        assertEquals(emptyList(), Streaming.over(iteration -> {}).toList());
    }

    @Test
    public void allEmittedItemsAreCollected() {
        assertEquals(List.of("one", "two", "three"),
                Streaming.over((iteration) -> {
                    iteration.produce("one");
                    iteration.produce("two");
                    iteration.produce("three");
                }).toList());
    }

    @Test
    public void autoCloses() {
        AtomicBoolean wasExplicitlyFinished = new AtomicBoolean(false);

        try (var stream = Streaming.over((iteration) -> {
            int i = 1;
            while (true) {
                try {
                    iteration.produce(i++);
                } catch (IterationFinishedException e) {
                    wasExplicitlyFinished.set(true);
                    throw e;
                }
            }
        })) {
            assertEquals(List.of(1, 2, 3), stream.limit(3).toList());
        }

        assertTimeout(Duration.ofSeconds(1), () -> {
            while (!wasExplicitlyFinished.get()) {
                Thread.sleep(100);
            }
        });
    }

    @Test
    public void closesOnFinalisation() {
        AtomicBoolean wasExplicitlyFinished = new AtomicBoolean(false);

        assertEquals(List.of(1, 2, 3), Streaming.over((iteration) -> {
            int i = 1;
            while (true) {
                try {
                    iteration.produce(i++);
                } catch (IterationFinishedException e) {
                    wasExplicitlyFinished.set(true);
                    throw e;
                }
            }
        }).limit(3).toList());

        System.gc();
        assertTimeout(Duration.ofSeconds(1), () -> {
            while (!wasExplicitlyFinished.get()) {
                Thread.sleep(100);
            }
        });
    }

    @Test
    public void exceptionInProducerPropagatesToConsumer() {
        assertThrows(IterationException.class, () ->
            Streaming.over(iteration -> {
                iteration.produce("one");
                throw new RuntimeException("boo!");
            }).toList()
        );
    }

}
