package org.starexec.test.integration.database;

import org.junit.Assert;
import org.junit.Test;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.PairsRerun;
import org.starexec.data.to.*;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PairsRerunTests extends TestSequence {

    private Job job;

    @StarexecTest
    private void MarkPairAsRerunTest() {
        JobPair jobPair = job.getJobPairs().get(0);
        Assert.assertNotNull( "Job pair was null so job was not setup correctly.", jobPair);
        try {
            PairsRerun.markPairAsRerun(jobPair.getId());
            Assert.assertTrue((PairsRerun.hasPairBeenRerun(jobPair.getId())));

            // Cleanup...
            PairsRerun.unmarkPairAsRerun(jobPair.getId());
        } catch (SQLException e) {
            Assert.fail("Test threw SQLException: "+ Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void PairNotRerunTest() {
        JobPair jobPair = job.getJobPairs().get(0);
        Assert.assertNotNull( "Job pair was null so job was not setup correctly.", jobPair);
        try {
            Assert.assertFalse((PairsRerun.hasPairBeenRerun(jobPair.getId())));
        } catch (SQLException e) {
            Assert.fail("Test threw SQLException: "+ Util.getStackTrace(e));
        }
    }

    @Override
    protected String getTestName() {
        return "PairsRerunTests";
    }

    @Override
    protected void setup() throws Exception {


        User user=loader.loadUserIntoDatabase();
        Space space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
        Solver solver=loader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
        Processor postProc=loader.loadProcessorIntoDatabase("postproc.zip", Processor.ProcessorType.POST, Communities.getTestCommunity().getId());
        List<Integer> benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());

        int wallclockTimeout=100;
        int cpuTimeout=100;
        int gbMemory=1;
        List<Integer> solverIds=new ArrayList<Integer>();
        solverIds.add(solver.getId());

        job=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
        Assert.assertNotNull(Jobs.get(job.getId()));
    }

    @Override
    protected void teardown() throws Exception {
        loader.deleteAllPrimitives();
    }



}
