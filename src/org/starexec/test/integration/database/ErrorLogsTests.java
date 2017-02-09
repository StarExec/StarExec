package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.data.database.Common;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.sql.SQLException;

/**
 * Created by agieg on 2/9/2017.
 */
public class ErrorLogsTests extends TestSequence {

    final String tableName = "error_logs";

    @Override
    protected String getTestName() {
        return "ErrorLogsTests";
    }

    @Override
    protected void setup() throws Exception {

    }

    @Override
    protected void teardown() throws Exception {

    }

    @StarexecTest
    private void addTest() {
//        try {
//        } catch (SQLException e) {
//            Assert.fail("Caught SQLException:\n"+ Util.getStackTrace(e));
//        }
    }
}
