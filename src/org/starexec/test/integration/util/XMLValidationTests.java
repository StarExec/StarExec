package org.starexec.test.integration.util;

import static org.junit.Assert.*;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.data.to.enums.JobXmlType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;
import org.starexec.util.XMLUtil;

import java.io.File;
import java.util.List;

/**
 * Created by agieg on 5/5/2017.
 */
public class XMLValidationTests extends TestSequence {
    private User admin = null;
    private Solver solver = null;
    private List<Integer> benchmarkIds = null;
    private ResourceLoader loader = new ResourceLoader();

    public void basicJobXmlValidationTest() {
        try {
            int configId = solver.getConfigurations().get(0).getId();
            File xml = loader.getJoblineTestXMLFile(configId, configId, benchmarkIds.get(0), benchmarkIds.get(1));
            ValidatorStatusCode status = XMLUtil.validateAgainstSchema(xml, JobXmlType.STANDARD.schemaPath);
            assertTrue(status.getMessage(), status.isSuccess());
        } catch (Exception e) {
            fail("Caught Exception: "+ Util.getStackTrace(e));
        }
    }

    @Override
    protected String getTestName() {
        return "XMLValidationTests";
    }

    @Override
    protected void setup() throws Exception {
        admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa", R.ADMIN_ROLE_NAME);
        solver = loader.loadSolverIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
        benchmarkIds = loader.loadBenchmarksIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
    }

    @Override
    protected void teardown() throws Exception {
        loader.deleteAllPrimitives();
    }
}
