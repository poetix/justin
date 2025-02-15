package com.codepoetics.justin;

interface ProducerConsumerInterchange<T> extends Iteration<T> {
    void finishProducing();
    void finishProducingWithException(Exception e);

    IterationSignal<T> consume();
    void finishConsuming();
}
