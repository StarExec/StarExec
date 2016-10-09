package org.starexec.test.junit.backend;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.starexec.backend.GridEngineBackend;
import org.starexec.exceptions.StarExecException;
import org.starexec.util.Util;
import org.testng.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class})
public class GridEngineBackendTests {
	GridEngineBackend backend = new GridEngineBackend();
	private static String testSGEOutputString = "job-ID  prior   name       user         state submit/start at     queue                          slots ja-task-ID \n"+ 
"-----------------------------------------------------------------------------------------------------------------\n" +
"    998 0.55500 job_768.ba tomcat       qw    12/03/2015 13:18:55                                    1        \n" +
"    999 0.55500 job_769.ba tomcat       qw    12/03/2015 13:18:55                                    1        \n" +
"   1000 0.55500 job_770.ba tomcat       qw    12/03/2015 13:18:55                                    1        ";
 
	@Test
	public void getActiveExecutionIdsTest() throws IOException {
		PowerMockito.mockStatic(Util.class);
		System.setProperty("line.separator", "\n");
        BDDMockito.given(Util.executeCommand("qstat -s a")).willReturn(testSGEOutputString);
		Set<Integer> ids = backend.getActiveExecutionIds();
		Assert.assertTrue(ids.size()==3);
		Assert.assertTrue(ids.contains(998));
		Assert.assertTrue(ids.contains(999));
		Assert.assertTrue(ids.contains(1000));
	}

	@Test
	public void getSlotsInQueueTest() {
		String testQueueName = "all.q";
		String testCommand = GridEngineBackend.QUEUE_GET_SLOTS_PATTERN.replace(GridEngineBackend.QUEUE_NAME_PATTERN, testQueueName);
		PowerMockito.mockStatic(Util.class);
		try {
			BDDMockito.given(Util.executeCommand(testCommand)).willReturn("2");
			Integer slots = backend.getSlotsInQueue(testQueueName);
			Assert.assertEquals(slots, new Integer(2));
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		} catch (StarExecException e) {
			Assert.fail("Caught StarExecException: " + Util.getStackTrace(e));
		}
	}
}
