package org.starexec.util.functionalInterfaces;

import jdk.nashorn.internal.objects.annotations.Function;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, V extends Throwable> {
    R accept(T t, U u) throws V;
}
