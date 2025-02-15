package com.codepoetics.justin;

import java.util.Iterator;
import java.util.function.Consumer;

public final class Iterating {

    private Iterating() { }

    public static <T> CloseableIterator<T> over(Consumer<Iteration<T>> producer) {
        ProducerConsumerInterchange<T> interchange = new SemaphoredProducerConsumerInterchange<>();

        var producerThread = Thread.startVirtualThread(() -> {
            try {
                producer.accept(interchange);
                interchange.finishProducing();
            } catch (IterationFinishedException e) {
                // Ignore: this was just used to end execution of the iteration
            } catch (Exception e) {
                interchange.finishProducingWithException(e);
            }
        });

        return new InterchangeConnectedIterator<>(interchange, () -> {
            interchange.finishConsuming();
            producerThread.interrupt();
        });
    }

}
