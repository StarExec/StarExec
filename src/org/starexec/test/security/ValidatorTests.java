package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.command.R;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.util.Validator;
public class ValidatorTests extends TestSequence {
	
	
	@Test
	private void DescriptionRegexTest() {
		Assert.assertTrue(Validator.isValidPrimDescription(""));
		Assert.assertTrue(Validator.isValidPrimDescription("hello world"));
		Assert.assertTrue(Validator.isValidPrimDescription("This is a sentence, and it has some punctuation."));
		Assert.assertTrue(Validator.isValidPrimDescription("293md03 32idiu"));		
		
		Assert.assertFalse(Validator.isValidPrimDescription(null));
		Assert.assertFalse(Validator.isValidPrimDescription("\"\""));
		Assert.assertFalse(Validator.isValidPrimDescription("<script>"));
	}
	
	@Test
	private void WebsiteRegexTest() {
		Assert.assertTrue(Validator.isValidWebsite("http://www.uiowa.edu"));
		Assert.assertTrue(Validator.isValidWebsite("https://cs.starexec.uiowa.edu"));
		Assert.assertTrue(Validator.isValidWebsite("http://www.google.com"));
		
		Assert.assertFalse(Validator.isValidWebsite(null));
		Assert.assertFalse(Validator.isValidWebsite("google.com"));
		Assert.assertFalse(Validator.isValidWebsite("<script></script>"));

	}
	
	@Test
	private void NameRegexTest() {
		Assert.assertTrue(Validator.isValidPrimName("hello world"));
		Assert.assertTrue(Validator.isValidPrimName("Mark"));
		Assert.assertTrue(Validator.isValidPrimName("293md03 32idiu"));		

		Assert.assertFalse(Validator.isValidPrimName(""));
		Assert.assertFalse(Validator.isValidPrimName(null));
		Assert.assertFalse(Validator.isValidPrimName("\"\""));
		Assert.assertFalse(Validator.isValidPrimName("<script>"));
		
	}
	
	@Test
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
	
	@Test 
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
	
	@Test
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
	@Test
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
	@Test
	private void DoubleRegexTest() {
		Assert.assertTrue(Validator.isValidDouble("4.0"));
		Assert.assertTrue(Validator.isValidDouble("-.9"));
		Assert.assertTrue(Validator.isValidDouble("-4"));
		Assert.assertTrue(Validator.isValidDouble("493."));
		Assert.assertTrue(Validator.isValidDouble("-393.245"));
		Assert.assertTrue(Validator.isValidDouble("4022.2935"));
		Assert.assertFalse(Validator.isValidDouble("."));
		Assert.assertFalse(Validator.isValidDouble("3.52.34"));
		Assert.assertFalse(Validator.isValidDouble("-."));
		Assert.assertFalse(Validator.isValidDouble("dsd"));
		Assert.assertFalse(Validator.isValidDouble("3493.2de"));
		Assert.assertFalse(Validator.isValidDouble(null));
	}
	@Test
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
	@Test
	private void IntegerListTest() {
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"3","1"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"0","132929","3492"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{"3"}));
		Assert.assertTrue(Validator.isValidIntegerList(new String[]{}));
		Assert.assertFalse(Validator.isValidIntegerList(new String[]{"3kd"}));
		Assert.assertFalse(Validator.isValidIntegerList(new String[]{"0","132929","3492","d"}));
		Assert.assertFalse(Validator.isValidIntegerList(null));
		

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
