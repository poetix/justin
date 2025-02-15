package com.codepoetics.justin;

sealed interface IterationSignal<T> permits IterationSignal.Value, IterationSignal.Finished, IterationSignal.FinishedExceptionally {

    record Value<T>(T value) implements IterationSignal<T> {
    }

    record Finished<T>() implements IterationSignal<T> {
    }

    record FinishedExceptionally<T>(Exception exception) implements IterationSignal<T> {
    }
}
