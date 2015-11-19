package org.starexec.test.junit.command;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.starexec.command.JsonHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import com.google.gson.JsonElement;

public class JsonHandlerTest {

	@Test
	public void testGetJsonString() throws Exception {
		HttpResponse response = Mockito.mock(HttpResponse.class);
		HttpEntity mockEntity = Mockito.mock(HttpEntity.class);
		Mockito.when(response.getEntity()).thenReturn(mockEntity);
		Mockito.when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream("hello!".getBytes("UTF-8")));
		JsonElement e = JsonHandler.getJsonString(response);
		assertEquals("\"hello!\"", e.toString());
	}
}
