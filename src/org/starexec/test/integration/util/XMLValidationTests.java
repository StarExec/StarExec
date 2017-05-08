package org.starexec.test.integration.util;

import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.XMLUtil;

/**
 * Created by agieg on 5/5/2017.
 */
public class XMLValidationTests extends TestSequence {

    public void basicJobXmlValidationTest() {
        try (ResourceLoader loader = new ResourceLoader()) {
        }
    }

    @Override
    protected String getTestName() {
        return "XMLValidationTests";
    }

    @Override
    protected void setup() throws Exception {}

    @Override
    protected void teardown() throws Exception {}
}
