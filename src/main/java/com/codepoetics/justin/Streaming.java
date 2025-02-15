package com.codepoetics.justin;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streaming {
    private Streaming() { }

    public static <T> Stream<T> over(Consumer<Iteration<T>> source) {
        var iter = Iterating.over(source);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iter,
                        Spliterator.IMMUTABLE & Spliterator.NONNULL),
                false).onClose(() -> {
                    try {
                        ((AutoCloseable) iter).close();
                    } catch (Exception ignored) {
                        //
                    }
                });
    }
}
