package org.starexec.test.junit.util;


import org.junit.Assert;
import org.junit.Test;
import org.starexec.util.Hash;

public class HashTests {

	@Test
	public void testGetHex() {
		Assert.assertEquals("", Hash.getHex(new byte[]{}));
		Assert.assertEquals("0a", Hash.getHex(new byte[]{10}));
		Assert.assertEquals("7f0010", Hash.getHex(new byte[]{127, 0, 16}));
	}
}
