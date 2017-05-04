package org.starexec.util.functionalInterfaces;

@FunctionalInterface
public interface ThrowingConsumer<T, U extends Throwable> {
    void accept(T t) throws U;
}
