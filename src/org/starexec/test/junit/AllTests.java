package org.starexec.test.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.starexec.test.junit.command.HTMLParserTest;
import org.starexec.test.junit.command.JsonHandlerTest;
import org.starexec.test.junit.jobs.LoadBalanceMonitorTests;

@RunWith(Suite.class)
@SuiteClasses({ ComparatorTests.class, UtilTests.class, LoadBalanceMonitorTests.class,
	HTMLParserTest.class, JsonHandlerTest.class})
public class AllTests {

}
