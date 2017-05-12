package org.starexec.test.junit.data.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.Benchmark;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Benchmarks.class, Permissions.class, GeneralSecurity.class})
public class BenchmarkSecurityTests {

    @Before
    public void initialize() {
        PowerMockito.mockStatic(Benchmarks.class);
        PowerMockito.mockStatic(Permissions.class);
        PowerMockito.mockStatic(GeneralSecurity.class);
    }

    @Test
    public void canUserDownloadBenchmarkGetsNullBenchTest() {
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(null);
        assertFalse(BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserCantSeeBenchTest() {
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(mock(Benchmark.class));
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(false);
        assertFalse(BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserHasAdminPrivileges() {
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(bench.isDownloadable()).willReturn(false);
        // Then benchmark is not the users benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        // The user has admin privileges.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(true);

        assertTrue("The user is an admin so should be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserIsOwner() {
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(bench.isDownloadable()).willReturn(false);
        // Then benchmark is not the users benchmark
        given(bench.getUserId()).willReturn(userId);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        // The user has admin privileges.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);

        assertTrue("The user is owner so should be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenBenchmarkIsDownloadable() {
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(bench.isDownloadable()).willReturn(true);
        // Then benchmark is not the users benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        // The user has admin privileges.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);

        assertTrue("The benchmark is downloadable so user should be able to download.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }
    @Test
    public void canUserDownloadBenchmarkWhenUserIsNotAdminBenchIsNotDownloadableAndUserIsNotOwner() {
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(bench.isDownloadable()).willReturn(false);
        // Then benchmark is not the users benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        // The user has admin privileges.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);

        assertFalse("The user should not be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserGetAnonymousLinkIfUserOwnsBenchmarkOrIsAdmin() {
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);

        assertTrue("Admins and owners should be able to get the anon link.",
                BenchmarkSecurity.canUserGetAnonymousLink(benchId, userId).isSuccess());
    }

    @Test
    public void canUserGetAnonymousLinkIfUserDoesNotOwnBenchmarkOrIsAdmin() {
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(false);

        assertFalse("Only admins and owners should be able to get the anon link.",
                BenchmarkSecurity.canUserGetAnonymousLink(benchId, userId).isSuccess());
    }
}

