package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.data.database.ErrorLogs;
import org.starexec.data.to.ErrorLog;
import org.starexec.logger.StarLevel;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

//    This would delete all logs in production.
//
//    @StarexecTest
//    private void deleteBeforeTest() {
//        try {
//            int firstId = ErrorLogs.add("test", StarLevel.DEBUG);
//            int secondId = ErrorLogs.add("test", StarLevel.DEBUG);
//            int thirdId = ErrorLogs.add("test", StarLevel.DEBUG);
//
//
//            // This should guarantee that a second passes.
//            Thread.sleep(1500);
//            ErrorLogs.deleteBefore(Timestamp.from(Instant.now()));
//
//            Assert.assertFalse("First log was still in database.", ErrorLogs.getById(firstId).isPresent());
//            Assert.assertFalse("Second log was still in database.", ErrorLogs.getById(secondId).isPresent());
//            Assert.assertFalse("Third log was still in database.", ErrorLogs.getById(thirdId).isPresent());
//        } catch (SQLException e) {
//            Assert.fail("Caught SQLException:\n"+ Util.getStackTrace(e));
//        } catch (InterruptedException e) {
//            Assert.fail("Thread couldn't sleep.\n" + Util.getStackTrace(e));
//        }
//    }

    @StarexecTest
    private void getSinceTest() {
        try {
            Timestamp begin = Timestamp.from(Instant.now());

            Optional<Integer> optFirstId = ErrorLogs.add("test", StarLevel.DEBUG);
            Optional<Integer> optSecondId = ErrorLogs.add("test", StarLevel.DEBUG);
            Optional<Integer> optThirdId = ErrorLogs.add("test", StarLevel.DEBUG);

            Assert.assertTrue(optFirstId.isPresent());
            Assert.assertTrue(optSecondId.isPresent());
            Assert.assertTrue(optThirdId.isPresent());

            int firstId = optFirstId.get();
            int secondId = optSecondId.get();
            int thirdId = optThirdId.get();

            List<ErrorLog> logs = ErrorLogs.getSince(begin);


            // Check that logs we got contain all three logs we added.
            Assert.assertTrue(logs.stream().anyMatch(l -> l.getId() == firstId));
            Assert.assertTrue(logs.stream().anyMatch(l -> l.getId() == secondId));
            Assert.assertTrue(logs.stream().anyMatch(l -> l.getId() == thirdId));

            ErrorLogs.deleteWithId(firstId);
            ErrorLogs.deleteWithId(secondId);
            ErrorLogs.deleteWithId(thirdId);

        } catch (SQLException e) {
            Assert.fail("Caught SQLException:\n"+ Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void getDoesntThrowExceptionTest() {
        try {
            int unlikelyId = 10000000;
            Optional<ErrorLog> optionalLog = ErrorLogs.getById(unlikelyId);
        } catch (SQLException e) {
            Assert.fail("Caught SQLException:\n"+ Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void deleteTest() {
        try {
            Optional<Integer> optId = ErrorLogs.add("test", StarLevel.DEBUG);
            Assert.assertTrue(optId.isPresent()) ;
            ErrorLogs.deleteWithId(optId.get());
            Optional<ErrorLog> log = ErrorLogs.getById(optId.get());

            Assert.assertFalse("Error log was still in database after deletion.",log.isPresent());
        } catch (SQLException e) {
            Assert.fail("Caught SQLException:\n"+ Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void addTest() {
        try {
            StarLevel level = StarLevel.DEBUG;
            String message = "test";
            Optional<Integer> id = ErrorLogs.add(message, level);
            Assert.assertTrue(id.isPresent());
            Optional<ErrorLog> optionalLog = ErrorLogs.getById(id.get());
            Assert.assertTrue("Log did not exist in database.", optionalLog.isPresent());
            ErrorLog log = optionalLog.get();
            Assert.assertEquals(Integer.valueOf(log.getId()), id.get());
            Assert.assertEquals(level, log.getLevel());
            Assert.assertEquals(message, log.getMessage());
            ErrorLogs.deleteWithId(id.get());
        } catch (SQLException e) {
            Assert.fail("Caught SQLException:\n" + Util.getStackTrace(e));
        }
    }
}
