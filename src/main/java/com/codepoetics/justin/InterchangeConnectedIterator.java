package com.codepoetics.justin;

import java.lang.ref.Cleaner;
import java.util.NoSuchElementException;

final class InterchangeConnectedIterator<T> implements CloseableIterator<T> {

    private static final Cleaner CLEANER = Cleaner.create();

    private T buffer = null;
    private final Cleaner.Cleanable cleanable;
    private final ProducerConsumerInterchange<T> state;

    InterchangeConnectedIterator(ProducerConsumerInterchange<T> state, Runnable cleanup) {
        this.state = state;
        this.cleanable = CLEANER.register(this, cleanup);
    }

    @Override
    public boolean hasNext() {
        if (buffer != null) return true;

        return switch (state.consume()) {
            case IterationSignal.Value<T> value -> {
                buffer = value.value();
                yield true;
            }
            case IterationSignal.Finished<T> ignored -> {
                close();
                yield false;
            }
            case IterationSignal.FinishedExceptionally<T> failure -> {
                close();
                throw new IterationException(failure.exception());
            }
        };
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        var taken = buffer;
        buffer = null;
        return taken;
    }

    @Override
    public void close() {
        cleanable.clean();
    }
}
