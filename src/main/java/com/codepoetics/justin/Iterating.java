package com.codepoetics.justin;

import java.util.Iterator;
import java.util.function.Consumer;

public final class Iterating {

    private Iterating() { }

    public static <T> Iterator<T> over(Consumer<Iteration<T>> source) {
        ProducerConsumerInterchange<T> state = new SemaphoredProducerConsumerInterchange<>();

        var producerThread = Thread.startVirtualThread(() -> {
            try {
                source.accept(state);
                state.finishProducing();
            } catch (IterationFinishedException e) {
                // Ignore: this was just used to end execution of the iteration
            } catch (Exception e) {
                state.finishProducingWithException(e);
            }
        });

        return new InterchangeConnectedIterator<>(state, () -> {
            state.finishConsuming();
            producerThread.interrupt();
        });
    }

}
