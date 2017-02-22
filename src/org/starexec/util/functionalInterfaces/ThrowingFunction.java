package org.starexec.util.functionalInterfaces;

/**
 * Created by agieg on 2/10/2017.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, V extends Throwable> {
    R accept(T t) throws V;
}
