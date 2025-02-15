# Justin

> I believe in a thing called Loom.
 
Have you ever wished that you could write Pythonic [generator functions](https://wiki.python.org/moin/Generators), or Kotlinesque [sequence blocks](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.sequences/sequence.html), in modern Java? Then it may be..._Justin time_.

With the pattern implemented by _Justin_ you can easily turn a push-based API into a pull-based API, or write sequence generators with complex logic that lazily compute values as a consumer requests them:

```java
private void generateFibonacciSequence(Iteration<Integer> iteration) {
    var a = 0;
    var b = 1;
    while (true) {
        iteration.produce(a);
        var swap = a;
        a = b;
        b = a + swap;
    }
}

@Test
public void consumeFirstTenFibonacciNumbers() {
    assertEquals(List.of(0, 1, 1, 2, 3, 5, 8, 13, 21, 34),
            Streaming.over(this::generateFibonacciSequence).limit(10).toList());
}
```

## Usage

A _producer_ is a `Consumer<Iteration<T>>` function which receives an `Iteration<T>` parameter, and calls `produce()` on it to "yield" values to the consumer. A lambda or a method reference may be used.

Each call to `produce` after the first will block the producer until a consumer has taken the previously-produced value.

Pass a producer to `Iterating.over(producer)` to get a `CloseableIterator<T>` that can access the produced values sequentially in the usual way with `hasNext()` and `next()`

Alternatively, pass it to `Streaming.over(producer)` to get a `Stream<T>`, convenient for transformation and collection using the [Java Streams API](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html).

## How it works

We observe that, in modern Java, [virtual threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html) are implemented using continuations under the hood. This means that we can approximate behaviours that require continuations in other languages by making use of virtual threads (as a proxy for working with the [JVM-internal continuation implementation](https://github.com/openjdk/loom/blob/fibers/src/java.base/share/classes/jdk/internal/vm/Continuation.java) directly).

_Justin_ works by running the "generator" on a virtual thread, and having it "yield" to the consumer by placing a value in a single-element blocking queue.

The consumer, in turn, pulls values from that same queue into `Iterator`, which may be wrapped with a `Stream`, enabling sequential reads of the values so produced.

Users of [goroutines and channels](https://go.dev/tour/concurrency/4) may find this approach familiar.

## Example: breadth-first traversal of a binary tree

Writing an `Iterator` or `Spliterator` to keep track of the progress of a binary tree traversal, enabling a consumer to drive the traversal algorithm to locate and return the next value, is a bit non-trivial.

How much nicer to be able to write the algorithm for a breadth-first traversal in the normal way, and emit values lazily as they are encountered:

```java
default Stream<T> traverseBreadthFirst() {
    return Streaming.over((iteration) -> {
        var queue = new ArrayDeque<Node<T>>();
        queue.add(this);
    
        while (!queue.isEmpty()) {
            var node = queue.removeFirst();
    
            switch (node) {
                case Leaf<T> leaf -> iteration.produce(leaf.value);
                case Branch<T> branch -> {
                    iteration.produce(branch.value);
                    if (branch.left != null) {
                        queue.add(branch.left);
                    }
                    if (branch.right != null) {
                        queue.add(branch.right);
                    }
                }
                default -> {}
            }
        }
    });
}
```

## Consumer-signalled termination

Normally a producer will produce a finite sequence of values then return, in which case a signal is sent to the consumer that there are no more values available. When this happens, the next call to `hasNext()` on the `Iterator` representing the consumer will return `false`; subsequent calls to `next()` will fail with a `NoSuchElementException`.

Sometimes however the producer will produce more values than the consumer is interested in - possibly an infinite sequence - and it will be up to the consumer to signal when it's finished consuming them. Since the producer is running on a virtual thread, we would like it to exit when this happens, rather than remaining blocked on a call to `produce()` forever, waiting for a call to `consume()` that will never come. (Even though virtual threads are cheap, we still don't want to risk unbounded production of continuations that are never cleaned up). 

Here's a simple example, in which the producer emits an infinite stream of integers, and the consumer takes three of them:

```java
assertEquals(List.of(1, 2, 3), Streaming.over((iteration) -> {
    int i = 1;
    while (true) {
        iteration.produce(i++);
    }
}).limit(3).toList());
```

How does the producer know the consumer has finished consuming? The consumer is represented by a `Stream` (as in the above example) or a `CloseableIterator` (if you use `Iterating.over(...)` instead), so we can do one of three things with this object:

1. Explicitly `close()` it.
2. Use it in a [try-with-resources block](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), which ensures it is closed when the block is exited.
3. Wait for the garbage collector to collect it.

In any of these cases, the producer is signalled to stop producing, and an `IterationFinishedException` will be thrown from its next call to `iteration.produce()`. (If you trap it and keep going, that's your lookout.)

## Exception propagation

Exceptions thrown within the producer are propagated to the consumer, and are thrown (wrapped in an unchecked `IterationException`) when a call is made to `Iterator.hasNext()` or `Iterator.next()`.

