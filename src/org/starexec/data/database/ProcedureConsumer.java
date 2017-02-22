package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by agieg on 8/31/2016.
 */
@FunctionalInterface
public interface ProcedureConsumer {
    void setupProcedure(CallableStatement procedure) throws SQLException;
}
