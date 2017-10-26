package org.starexec.test.junit;

import org.junit.Assert;
import org.junit.Test;
import org.starexec.util.Util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class UtilTests {

	@Test
	public void BytesToGigabytesTest() {
		Assert.assertEquals(1, Util.bytesToGigabytes(1073741824),.005);
		Assert.assertEquals(0,Util.bytesToGigabytes(0),.005);
	}

	@Test
	public void GetExtensionTest() {
		Assert.assertEquals("zip",Util.getFileExtension("this/is/a/fake.zip"));
		Assert.assertEquals("test",Util.getFileExtension("fake.test"));
	}

	@Test
	public void GetTempPasswordTest() {
		int index=0;
		while (index<10) {
			index++;
			String pass=Util.getTempPassword();
			Assert.assertNotNull(pass);
			Assert.assertEquals(pass.length(),Util.clamp(6, 20, pass.length()));
		}
	}

	@Test
	public void ToIntegerListTest() {
		List<Integer> ints=Util.toIntegerList(new String[]{"11","2","321"});
		Assert.assertEquals(3,ints.size());
		Assert.assertEquals(11,(int)ints.get(0));
		Assert.assertEquals(2,(int)ints.get(1));
		Assert.assertEquals(321,(int)ints.get(2));

		ints=Util.toIntegerList(new String[]{});
		Assert.assertEquals(0,ints.size());
	}

	@Test
	public void intClampTest() {
		Assert.assertEquals(1,Util.clamp(0, 10, 1));
		Assert.assertEquals(13,Util.clamp(13, 25, 7));
		Assert.assertEquals(30,Util.clamp(4, 30, 31));
		Assert.assertEquals(10,Util.clamp(10, 10, 10));
	}

	@Test
	public void longClampTest() {
		Assert.assertEquals(1,Util.clamp(0L, 10L, 1L));
		Assert.assertEquals(13,Util.clamp(13L, 25L, 7L));
		Assert.assertEquals(30,Util.clamp(4L, 30L, 31L));
		Assert.assertEquals(10,Util.clamp(10L, 10L, 10L));
	}

	@Test
	public void isNullOrEmptyTest() {
		Assert.assertTrue(Util.isNullOrEmpty(null));
		Assert.assertTrue(Util.isNullOrEmpty(""));
		Assert.assertFalse(Util.isNullOrEmpty("a"));
		Assert.assertFalse(Util.isNullOrEmpty("another test"));
		Assert.assertFalse(Util.isNullOrEmpty("null"));
		Assert.assertFalse(Util.isNullOrEmpty("empty"));
	}

	@Test
	public void BytesToMegabytesTest() {
		Assert.assertEquals(1, Util.bytesToMegabytes(1048576));
		Assert.assertEquals(1, Util.bytesToMegabytes(1048577));
		Assert.assertEquals(3, Util.bytesToMegabytes(4048576));
		Assert.assertEquals(0, Util.bytesToMegabytes(3));
	}

	@Test
	public void getColorFromStringTest() {
		Color c = Util.getColorFromString("black");
		Assert.assertEquals(0, c.getGreen());
		Assert.assertEquals(0, c.getBlue());
		Assert.assertEquals(0, c.getRed());
	}

	@Test
	public void getNullColorFromStringTest() {
		Color c = Util.getColorFromString("fakecolor");
		Assert.assertNull(c);
	}

	@Test
	public void isBinaryFile() throws IOException {
		File binaryFile = new File("upload-test/solvers/always-sat-solver.zip");
		Assert.assertTrue(binaryFile.exists());
		Assert.assertTrue(Util.isBinaryFile(binaryFile));

		File textFile = new File("build.xml");
		Assert.assertTrue(textFile.exists());
		Assert.assertFalse(Util.isBinaryFile(textFile));
	}
}
