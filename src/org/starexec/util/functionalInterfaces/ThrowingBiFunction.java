package org.starexec.util.functionalInterfaces;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, V extends Throwable> {
    R accept(T t, U u) throws V;
}
