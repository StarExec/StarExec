package org.starexec.test.integration.servlets;


import org.starexec.data.to.User;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BenchmarkUploaderTests extends TestSequence {

    class BenchmarkUploaderWrapper {
        public File getDirectoryForBenchmarkUpload(int userId, String name) throws FileNotFoundException {
            return BenchmarkUploader.getDirectoryForBenchmarkUpload(userId, name);
        }
    }

    public String getTestName() {
        return "BenchmarkUploaderTests";
    }

    @StarexecTest
    public void getBenchmarkDirectoryTest() {
        try (ResourceLoader loader = new ResourceLoader()) {
            User user = loader.loadUserIntoDatabase();
            File dir = BenchmarkUploader.getDirectoryForBenchmarkUpload(user.getId(), null);

            assertTrue("Directory could not be deleted.", dir.delete());

        } catch (FileNotFoundException e) {
            fail("Directory was not created: " + Util.getStackTrace(e));
        }
    }

    public void setup() {

    }

    public void teardown() {

    }

}
