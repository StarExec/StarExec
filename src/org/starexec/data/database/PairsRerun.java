package org.starexec.data.database;


import java.sql.Connection;
import java.sql.SQLException;


/**
 * Responsible for issuing queries and updates to the pairs_rerun table.
 */
public class PairsRerun {
    /**
     * Checks whether or not a pair has been rerun.
     * @param pairId the id of the pair to check.
     * @return true if the pair has been rerun, false otherwise.
     * @throws SQLException on database error.
     */
    public static boolean hasPairBeenRerun(int pairId) throws SQLException {
        return false;
    }

    /**
     * Checks whether or not a pair has been rerun.
     * @param con an open database connection to use for the procedure.
     * @param pairId the id of the pair to check.
     * @return true if the pair has been rerun, false otherwise.
     * @throws SQLException on database error.
     */
    public static boolean pairHasBeenRerun(Connection con, int pairId) throws SQLException {
        return false;
    }

    /**
     * Marks a pair as having been rerun in the pairs_rerun table.
     * @param pairId the id of the pair to mark as having been rerun.
     * @throws SQLException on database error.
     */
    public static void markPairAsRerun(int pairId) throws SQLException {

    }
    /**
     * Marks a pair as having been rerun in the pairs_rerun table.
     * @param con an open database connection to use for the procedure.
     * @param pairId the id of the pair to mark as having been rerun.
     * @throws SQLException on database error.
     */
    public static void markPairAsRerun(Connection con, int pairId) throws SQLException {

    }

    // Currently only used for tests.
    public static void unmarkPairAsRerun(int pairId) throws SQLException {

    }
}
