package com.codepoetics.justin;

final class RelayingProducerConsumerInterchange<T> implements ProducerConsumerInterchange<T> {

    private volatile boolean consumerIsFinished;
    private final SingleElementBlockingRelay<IterationSignal<T>> relay = new SingleElementBlockingRelay<>();

    @Override
    public void produce(T value) {
        try {
            relay.write(new IterationSignal.Value<>(value));
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
            return relay.read();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new IterationSignal.Finished<>();
        }
    }

    @Override
    public void finishConsuming() {
        consumerIsFinished = true;
        relay.drain();
    }

    @Override
    public void finishProducing() {
        try {
            relay.write(new IterationSignal.Finished<>());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void finishProducingWithException(Exception exception) {
        try {
            relay.write(new IterationSignal.FinishedExceptionally<>(exception));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
