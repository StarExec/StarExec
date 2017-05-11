package org.starexec.test.junit.data.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.mockito.BDDMockito.given;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.app.RESTHelpers;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.to.Benchmark;
import org.starexec.util.Util;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Benchmarks.class})
public class BenchmarkSecurityTests {
    @Before
    public void initialize() {
        PowerMockito.mockStatic(Benchmarks.class);
    }

    @Test
    public void canUserDownloadBenchmarkGetsNullBenchTest() {
		int benchId = 1;
		int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(null);
        assertFalse(BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }
}

