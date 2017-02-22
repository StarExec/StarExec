package org.starexec.data.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by agieg on 8/31/2016.
 */
@FunctionalInterface
public interface ConnectionResultsConsumer<T> {
    T query(Connection con, ResultSet results) throws SQLException;
}
