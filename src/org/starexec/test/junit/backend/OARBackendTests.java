package org.starexec.test.junit.backend;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.backend.OARBackend;
import org.starexec.util.Util;
import org.testng.Assert;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class})
public class OARBackendTests {
	OARBackend backend = new OARBackend();
	private static String oarnotifyTestString = "admin \n" +
	"priority = 10\n"+
	"scheduler = oar_sched_gantt_with_timesharing_and_fairsharing_and_quotas\n"+
	"state = Active\n"+
"test\n"+
	"priority = 0\n"+
	"scheduler = oar_sched_gantt_with_timesharing\n"+
	"state = Inactive\n"+
"besteffort\n"+
	"priority = 0\n"+
	"scheduler = oar_sched_gantt_with_timesharing_and_fairsharing_and_quotas\n"+
	"state = Active";
	private static Gson gson = new Gson();
	
	private static String oarnodesJSONResults = 
"{\"4\" : {\"network_address\" : \"stardev.cs.uiowa.edu\","+"\"queue\" : \"all\" },\"1\" : {\"network_address\" : \"n001\",\"queue\" : \"test\"}}";
	
	@Test
	public void getQueuesTest() throws IOException {
		PowerMockito.mockStatic(Util.class);
		System.setProperty("line.separator", "\n");
        BDDMockito.given(Util.executeCommand("oarnotify -l")).willReturn(oarnotifyTestString);
        String[] queues = backend.getQueues();
        Assert.assertEquals(queues.length, 2);
        Assert.assertTrue(ArrayUtils.contains(queues, "admin"));
        Assert.assertTrue(ArrayUtils.contains(queues, "besteffort"));
	}
	
	@Test
	public void getNodeQueueAssocTest() {
		JsonObject object = new JsonParser().parse(oarnodesJSONResults).getAsJsonObject();
		for (Entry<String, JsonElement> s : object.entrySet()) {
			System.out.println(s.getValue().getAsJsonObject().get("queue"));
		}
		
	}
}
