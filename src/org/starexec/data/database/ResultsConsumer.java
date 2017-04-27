package org.starexec.data.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by agieg on 8/30/2016.
 */
@FunctionalInterface
public 	interface ResultsConsumer<T> {
    T query(ResultSet results) throws SQLException;
}
