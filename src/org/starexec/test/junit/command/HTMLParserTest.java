package org.starexec.test.junit.command;

import org.apache.http.Header;
import org.junit.Assert;
import org.junit.Test;
import org.starexec.command.HTMLParser;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public class HTMLParserTest {

	private Header getMockHeader(String name, String value) {
		Header h = mock(Header.class);
		when(h.getName()).thenReturn(name);
		when(h.getValue()).thenReturn(value);
		return h;
	}
	@Test
	public void testExtractCookie() {
		Header h = getMockHeader("Set-Cookie", "first=fake cookie;second=another cookie");
		Header [] headers = new Header[3];
		headers[0] = getMockHeader("a", "b");
		headers[1] = h;
		headers [2] = getMockHeader("c", "d");
		Assert.assertEquals("fake cookie",HTMLParser.extractCookie(headers, "first"));
		Assert.assertEquals("another cookie",HTMLParser.extractCookie(headers, "second"));
		Assert.assertEquals(null, HTMLParser.extractCookie(headers, "third"));
		Assert.assertEquals(null, HTMLParser.extractCookie(null, "first"));
	}
	
	@Test
	public void testExtractMultipartCookie() {
		Header h = getMockHeader("Set-Cookie", "first=a,b,c;second=d");
		Header [] headers = new Header[3];
		headers[0] = getMockHeader("a", "b");
		headers[1] = h;
		headers [2] = getMockHeader("c", "d");
		Assert.assertArrayEquals(new String[] {"a","b","c"},HTMLParser.extractMultipartCookie(headers, "first"));
		Assert.assertArrayEquals(new String[] {"d"},HTMLParser.extractMultipartCookie(headers, "second"));
		Assert.assertArrayEquals(null, HTMLParser.extractMultipartCookie(headers, "third"));
		Assert.assertArrayEquals(null, HTMLParser.extractMultipartCookie(null, "first"));
	}
	
	@Test
	public void testURLEncode() {
		HashMap<String, String> params = new HashMap<String,String>();
		String baseURL = "http://www.test.com/";
		
		Assert.assertEquals(baseURL, HTMLParser.URLEncode(baseURL, params));
		params.put("name", "value");
		Assert.assertEquals(baseURL+"?name=value", HTMLParser.URLEncode(baseURL, params));
		params.put("next", "param");
		String url=HTMLParser.URLEncode(baseURL, params);
		Assert.assertTrue(url.contains("name=value") && url.contains("next=param"));
		Assert.assertTrue(url.contains("&name") ^ url.contains("&next"));
	}
	
	@Test
	public void testExtractIDFromJson() {
		Assert.assertEquals(null, HTMLParser.extractIDFromJson(""));
		Assert.assertEquals(null, HTMLParser.extractIDFromJson(null));
		Assert.assertEquals(null, HTMLParser.extractIDFromJson("< name=one >"));
		Assert.assertEquals(null, HTMLParser.extractIDFromJson("<value=\"1\">< type=\"hidden\" >"));
		
		Assert.assertEquals(3, (int)HTMLParser.extractIDFromJson("<input value=\"1\"><input value=\"3\"type=\"hidden\" >"));

	}
}
