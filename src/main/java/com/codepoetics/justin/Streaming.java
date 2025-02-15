package com.codepoetics.justin;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streaming {
    private Streaming() { }

    public static <T> Stream<T> over(Consumer<Iteration<T>> producer) {
        var iter = Iterating.over(producer);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iter,
                        Spliterator.IMMUTABLE & Spliterator.NONNULL),
                false).onClose(() -> {
                    try {
                        iter.close();
                    } catch (Exception ignored) {
                        //
                    }
                });
    }
}
