package com.codepoetics.justin;

final class SemaphoredProducerConsumerInterchange<T> implements ProducerConsumerInterchange<T> {

    private volatile boolean consumerIsFinished;
    private final SingleElementBlockingQueue<IterationSignal<T>> queue = new SingleElementBlockingQueue<>();

    @Override
    public void produce(T value) {
        try {
            queue.write(new IterationSignal.Value<>(value));
            if (consumerIsFinished) {
                throw new IterationFinishedException();
            }
        } catch (InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new IterationFinishedException();
        }
    }

    @Override
    public IterationSignal<T> consume() {
        try {
            return queue.read();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new IterationSignal.Finished<>();
        }
    }

    @Override
    public void finishConsuming() {
        consumerIsFinished = true;
        queue.drain();
    }

    @Override
    public void finishProducing() {
        try {
            queue.write(new IterationSignal.Finished<>());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void finishProducingWithException(Exception exception) {
        try {
            queue.write(new IterationSignal.FinishedExceptionally<>(exception));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
