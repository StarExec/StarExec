package org.starexec.test.junit.backend;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.backend.OARBackend;
import org.starexec.util.Util;
import org.testng.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
	
	private static String oarnodesJSONResults = 
"{\"4\" : {\"network_address\" : \"stardev.cs.uiowa.edu\","+"\"queue\" : \"all\" },\"1\" : {\"network_address\" : \"n001\",\"queue\" : \"test\"}}";
	
	
	private static String submitScriptResults = "preline\nOAR_JOB_ID=23\npostline";
	
	private static String oarstatJSONResults = "{ \"8\" : { \"types\" : [], \"Job_Id\" : \"8\" }, \"1\" : {\"Job_Id\" : \"1\"}}";
	@Before
	public void initialize() {
		PowerMockito.mockStatic(Util.class);
		System.setProperty("line.separator", "\n");
	}
	
	@Test
	public void getQueuesTest() throws IOException {
        BDDMockito.given(Util.executeCommand("oarnotify -l")).willReturn(oarnotifyTestString);
        String[] queues = backend.getQueues();
        Assert.assertEquals(queues.length, 2);
        Assert.assertTrue(ArrayUtils.contains(queues, "admin"));
        Assert.assertTrue(ArrayUtils.contains(queues, "besteffort"));
	}
	
	@Test
	public void getNodeQueueAssocTest() throws IOException {
		BDDMockito.given(Util.executeCommand("oarnodes -J")).willReturn(oarnodesJSONResults);
		Map<String, String> nodesToQueues = backend.getNodeQueueAssociations();
		Assert.assertEquals(2,nodesToQueues.size());
		Assert.assertEquals("all", nodesToQueues.get("stardev.cs.uiowa.edu"));
		Assert.assertEquals("test", nodesToQueues.get("n001"));
	}
	
	@Test
	public void getActiveExecutionIdsTest() throws IOException {
		BDDMockito.given(Util.executeCommand("oarstat -J")).willReturn(oarstatJSONResults);
		Set<Integer> ans = backend.getActiveExecutionIds();
		Assert.assertTrue(ans.contains(8));
		Assert.assertTrue(ans.contains(1));
		Assert.assertTrue(ans.size()==2);
	}
	
	@Test
	public void submitScriptGetIdTest() throws IOException {
		BDDMockito.given(Util.executeCommand(new String[] {"oarsub","-O", "","-E","","-d","",
				"-l","/cpuset=1","-S",""})).willReturn(submitScriptResults);
		int id = backend.submitScript("", "", "");
		Assert.assertEquals(id, 23);
	}
}
