package org.starexec.util.functionalInterfaces;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingConsumer<T, U extends Throwable> {
    void accept(T t) throws U;
}
