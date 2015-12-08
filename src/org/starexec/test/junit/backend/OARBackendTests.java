package org.starexec.test.junit.backend;

import java.io.IOException;

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
}
