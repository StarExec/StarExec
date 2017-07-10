package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Validator;
public class ValidatorTests extends TestSequence {


	@StarexecTest
	private void DescriptionRegexTest() {
		Assert.assertTrue(Validator.isValidPrimDescription(""));
		Assert.assertTrue(Validator.isValidPrimDescription("hello world"));
		Assert.assertTrue(Validator.isValidPrimDescription("This is a sentence, and it has some punctuation."));
		Assert.assertTrue(Validator.isValidPrimDescription("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidPrimDescription(TestUtil.getRandomAlphaString(R.BENCH_DESC_LEN)));
		Assert.assertFalse(Validator.isValidPrimDescription("2017-05-22"));
		Assert.assertFalse(Validator.isValidPrimDescription(TestUtil.getRandomAlphaString(R.BENCH_DESC_LEN+1)));
		Assert.assertFalse(Validator.isValidPrimDescription(null));
		Assert.assertFalse(Validator.isValidPrimDescription("\"\""));
		Assert.assertFalse(Validator.isValidPrimDescription("<script>"));
	}

	@StarexecTest
	private void WebsiteRegexTest() {
		Assert.assertTrue(Validator.isValidWebsite("http://www.uiowa.edu"));
		Assert.assertTrue(Validator.isValidWebsite("https://cs.starexec.uiowa.edu"));
		Assert.assertTrue(Validator.isValidWebsite("http://www.google.com"));

		Assert.assertFalse(Validator.isValidWebsite(null));
		Assert.assertFalse(Validator.isValidWebsite("google.com"));
		Assert.assertFalse(Validator.isValidWebsite("<script></script>"));

	}

	@StarexecTest
	private void QueueNameTest() {
		Assert.assertTrue(Validator.isValidQueueName("hello world"));
		Assert.assertTrue(Validator.isValidQueueName("Mark"));
		Assert.assertTrue(Validator.isValidQueueName("293md03 32idiu"));

		Assert.assertTrue(Validator.isValidQueueName(TestUtil.getRandomAlphaString(R.QUEUE_NAME_LEN)));

		Assert.assertFalse(Validator.isValidQueueName(TestUtil.getRandomAlphaString(R.QUEUE_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidQueueName(""));
		Assert.assertFalse(Validator.isValidQueueName(null));
		Assert.assertFalse(Validator.isValidQueueName("\"\""));
		Assert.assertFalse(Validator.isValidQueueName("<script>"));
	}

	@StarexecTest
	private void JobNameTest() {
		Assert.assertTrue(Validator.isValidJobName("hello world"));
		Assert.assertTrue(Validator.isValidJobName("Mark"));
		Assert.assertTrue(Validator.isValidJobName("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidJobName(TestUtil.getRandomAlphaString(R.JOB_NAME_LEN)));

		Assert.assertFalse(Validator.isValidJobName(TestUtil.getRandomAlphaString(R.JOB_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidJobName(""));
		Assert.assertFalse(Validator.isValidJobName(null));
		Assert.assertFalse(Validator.isValidJobName("\"\""));
		Assert.assertFalse(Validator.isValidJobName("<script>"));
	}

	@StarexecTest
	private void ConfigurationNameTest() {
		Assert.assertTrue(Validator.isValidConfigurationName("hello world"));
		Assert.assertTrue(Validator.isValidConfigurationName("Mark"));
		Assert.assertTrue(Validator.isValidConfigurationName("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidConfigurationName(TestUtil.getRandomAlphaString(R.CONFIGURATION_NAME_LEN)));
		Assert.assertFalse(Validator.isValidConfigurationName(TestUtil.getRandomAlphaString(R.CONFIGURATION_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidConfigurationName(""));
		Assert.assertFalse(Validator.isValidConfigurationName(null));
		Assert.assertFalse(Validator.isValidConfigurationName("\"\""));
		Assert.assertFalse(Validator.isValidConfigurationName("<script>"));
	}

	@StarexecTest
	private void ProcessorNameTest() {
		Assert.assertTrue(Validator.isValidProcessorName("hello world"));
		Assert.assertTrue(Validator.isValidProcessorName("Mark"));
		Assert.assertTrue(Validator.isValidProcessorName("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidProcessorName(TestUtil.getRandomAlphaString(R.PROCESSOR_NAME_LEN)));
		Assert.assertFalse(Validator.isValidProcessorName(TestUtil.getRandomAlphaString(R.PROCESSOR_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidProcessorName(""));
		Assert.assertFalse(Validator.isValidProcessorName(null));
		Assert.assertFalse(Validator.isValidProcessorName("\"\""));
		Assert.assertFalse(Validator.isValidProcessorName("<script>"));
	}

	@StarexecTest
	private void BenchmarkNameTest() {
		Assert.assertTrue(Validator.isValidBenchName("hello world"));
		Assert.assertTrue(Validator.isValidBenchName("Mark"));
		Assert.assertTrue(Validator.isValidBenchName("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidBenchName(TestUtil.getRandomAlphaString(R.BENCH_NAME_LEN)));

		Assert.assertFalse(Validator.isValidBenchName(TestUtil.getRandomAlphaString(R.BENCH_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidBenchName(""));
		Assert.assertFalse(Validator.isValidBenchName(null));
		Assert.assertFalse(Validator.isValidBenchName("\"\""));
		Assert.assertFalse(Validator.isValidBenchName("<script>"));
	}

	@StarexecTest
	private void SolverNameRegexTest() {
		Assert.assertTrue(Validator.isValidSolverName("hello world"));
		Assert.assertTrue(Validator.isValidSolverName("Mark"));
		Assert.assertTrue(Validator.isValidSolverName("293md03 32idiu"));
		Assert.assertTrue(Validator.isValidSolverName(TestUtil.getRandomAlphaString(R.SOLVER_NAME_LEN)));

		Assert.assertFalse(Validator.isValidSolverName(TestUtil.getRandomAlphaString(R.SOLVER_NAME_LEN+1)));
		Assert.assertFalse(Validator.isValidSolverName(""));
		Assert.assertFalse(Validator.isValidSolverName(null));
		Assert.assertFalse(Validator.isValidSolverName("\"\""));
		Assert.assertFalse(Validator.isValidSolverName("<script>"));
	}

	@StarexecTest
	private void BoolRegexTest() {
		Assert.assertTrue(Validator.isValidBool("true"));
		Assert.assertTrue(Validator.isValidBool("false"));
		Assert.assertTrue(Validator.isValidBool("TruE"));
		Assert.assertTrue(Validator.isValidBool("FALsE"));
		Assert.assertFalse(Validator.isValidBool(""));
		Assert.assertFalse(Validator.isValidBool("94"));
		Assert.assertFalse(Validator.isValidBool("adsfc"));
		Assert.assertFalse(Validator.isValidBool("4ks"));
		Assert.assertFalse(Validator.isValidBool("itisfalsethatthisshouldreturntrue"));
		Assert.assertFalse(Validator.isValidBool(null));

	}

	@StarexecTest
	private void InstitutionRegexTest() {
		Assert.assertTrue(Validator.isValidInstitution("The University Of Iowa"));
		Assert.assertTrue(Validator.isValidInstitution("test"));

		StringBuilder sb=new StringBuilder();
		for (int i=0; i<R.INSTITUTION_LEN+2;i++) {
			sb.append("a");
		}
		Assert.assertFalse(Validator.isValidInstitution(sb.toString()));
		Assert.assertFalse(Validator.isValidInstitution(""));
		Assert.assertFalse(Validator.isValidInstitution(null));
		Assert.assertFalse(Validator.isValidInstitution("The University @ Iowa"));
		Assert.assertFalse(Validator.isValidInstitution("<script>"));
	}

	@StarexecTest
	private void IntRegexTest() {
		Assert.assertTrue(Validator.isValidInteger("4"));
		Assert.assertTrue(Validator.isValidInteger("-3835"));
		Assert.assertTrue(Validator.isValidInteger("3420202"));
		Assert.assertTrue(Validator.isValidInteger("0"));
		Assert.assertFalse(Validator.isValidInteger("p"));
		Assert.assertFalse(Validator.isValidInteger("402kj"));
		Assert.assertFalse(Validator.isValidInteger("2378942398743289732"));
		Assert.assertFalse(Validator.isValidInteger(".4"));
		Assert.assertFalse(Validator.isValidInteger("4-4"));
		Assert.assertFalse(Validator.isValidInteger(null));

	}
	@StarexecTest
	private void ArchiveRegexTest() {
		Assert.assertTrue(Validator.isValidArchiveType(".zip"));
		Assert.assertTrue(Validator.isValidArchiveType(".tar"));
		Assert.assertTrue(Validator.isValidArchiveType(".tar.gz"));
		Assert.assertTrue(Validator.isValidArchiveType(".tgz"));
		Assert.assertFalse(Validator.isValidArchiveType(".rar"));
		Assert.assertFalse(Validator.isValidArchiveType(".exe"));
		Assert.assertFalse(Validator.isValidArchiveType(""));
		Assert.assertFalse(Validator.isValidArchiveType("."));
		Assert.assertFalse(Validator.isValidArchiveType(null));
		Assert.assertFalse(Validator.isValidArchiveType("<script>"));

	}
	@StarexecTest
	private void DoubleRegexTest() {
		Assert.assertTrue(Validator.isValidPosDouble("4.0"));
		Assert.assertFalse(Validator.isValidPosDouble("-.9"));
		Assert.assertFalse(Validator.isValidPosDouble("-4"));
		Assert.assertTrue(Validator.isValidPosDouble("493."));
		Assert.assertFalse(Validator.isValidPosDouble("-393.245"));
		Assert.assertTrue(Validator.isValidPosDouble("4022.2935"));
		Assert.assertFalse(Validator.isValidPosDouble("."));
		Assert.assertFalse(Validator.isValidPosDouble("3.52.34"));
		Assert.assertFalse(Validator.isValidPosDouble("-."));
		Assert.assertFalse(Validator.isValidPosDouble("dsd"));
		Assert.assertFalse(Validator.isValidPosDouble("3493.2de"));
		Assert.assertFalse(Validator.isValidPosDouble(null));
	}
	@StarexecTest
	private void EmailRegexTest() {
		Assert.assertTrue(Validator.isValidEmail("test@uiowa.edu"));
		Assert.assertTrue(Validator.isValidEmail("test_two@hotmail.com"));
		Assert.assertTrue(Validator.isValidEmail("AebEdxjkei382@fake.net"));
		Assert.assertFalse(Validator.isValidEmail("testuiowaedu"));
		Assert.assertFalse(Validator.isValidEmail("testuiowa.com"));
		Assert.assertFalse(Validator.isValidEmail("test@uiowanet"));
		Assert.assertFalse(Validator.isValidEmail(null));
		Assert.assertFalse(Validator.isValidEmail("<script>"));


	}
	@StarexecTest
	private void IntegerListTest() {
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"3","1"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"0","132929","3492"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"3"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{}));
		Assert.assertFalse(Validator.isValidIntegerList(new String[]{"3kd"}));
		Assert.assertFalse(Validator.isValidIntegerList(new String[]{"0","132929","3492","d"}));
		Assert.assertFalse(Validator.isValidIntegerList((String[])null));


	}


	@Override
	protected String getTestName() {
		return "ValidatorTests";
	}

	@Override
	protected void setup() throws Exception {

	}

	@Override
	protected void teardown() throws Exception {

	}

}
