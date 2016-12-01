package org.starexec.data.database;


import java.sql.Connection;


public class PairsRerun {
    public static boolean hasPairBeenRerun(int pairId) {
        return false;
    }

    public static boolean pairHasBeenRerun(Connection con, int pairId) {
        return false;
    }

    public static void markPairAsRerun(int pairId) {

    }

    public static void markPairAsRerun(Connection con, int pairId) {
        
    }
}
