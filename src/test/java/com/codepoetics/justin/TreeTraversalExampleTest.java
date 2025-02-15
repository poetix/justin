package com.codepoetics.justin;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeTraversalExampleTest {

    sealed interface Node<T extends Comparable<T>> permits Node.Empty, Node.Leaf, Node.Branch {

        Empty<?> EMPTY = new Empty<>();

        @SuppressWarnings("unchecked")
        static <T extends Comparable<T>> Node<T> empty() {
            return (Empty<T>) EMPTY;
        }

        @SafeVarargs
        static <T extends Comparable<T>> Node<T> of(T...items) {
            return Arrays.stream(items).reduce(
                    empty(),
                    Node::add,
                    (a, b) -> { throw new UnsupportedOperationException(); }
            );
        }

        default Node<T> add(T value) {
            return switch (this) {
                case Empty<T> ignored -> new Leaf<>(value);
                case Leaf<T> leaf -> switch(value.compareTo(leaf.value)) {
                    case 0 -> this;
                    case 1 -> new Branch<>(leaf.value, empty(), new Leaf<>(value));
                    default -> new Branch<>(leaf.value, new Leaf<>(value), empty());
                };
                case Branch<T> branch -> switch(value.compareTo(branch.value)) {
                    case 0 -> this;
                    case 1 -> new Branch<>(
                            branch.value,
                            branch.left,
                            branch.right.add(value)
                    );
                    default -> new Branch<>(
                            branch.value,
                            branch.left.add(value),
                            branch.right
                    );
                };
            };
        }

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

        default Stream<T> traverseDepthFirst() {
            return Streaming.over((iteration) -> {
                var stack = new ArrayDeque<Node<T>>();
                var current = this;
                stack.push(current);

                while (!stack.isEmpty()) {
                    while (current instanceof Node.Branch<T>) {
                        current = ((Branch<T>) current).left;
                        stack.push(current);
                    }

                    current = stack.pop();

                    switch (current) {
                        case Leaf<T> leaf -> {
                            iteration.produce(leaf.value);
                        }
                        case Branch<T> branch -> {
                            iteration.produce(branch.value);
                            current = branch.right;
                            stack.push(current);
                        }
                        default -> {}
                    }
                }
            });
        }

        record Empty<T extends Comparable<T>>() implements Node<T> {}
        record Leaf<T extends Comparable<T>>(T value) implements Node<T> {}
        record Branch<T extends Comparable<T>>(T value, Node<T> left, Node<T> right) implements Node<T> {}
    }


    @Test
    public void treeTraversalExample() {
        /*
        Builds this tree:

                    100
              50          150
           25    75   125     175
         */
        var node = Node.of(100, 50, 150, 25, 75, 125, 175);

        assertEquals(List.of(100, 50, 150, 25, 75, 125, 175), node.traverseBreadthFirst().toList());
        assertEquals(List.of(25, 50, 75, 100, 125, 150, 175), node.traverseDepthFirst().toList());
    }
}
