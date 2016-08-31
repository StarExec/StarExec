package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by agieg on 8/30/2016.
 */
@FunctionalInterface
public 	interface QueryProducer<T> {
    T query(CallableStatement procedure, ResultSet results) throws SQLException;
}
