package org.starexec.test.junit.data.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.data.database.*;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.util.Validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Benchmarks.class,
        Permissions.class,
        GeneralSecurity.class,
        Validator.class,
        Processors.class,
        ProcessorSecurity.class,
        Users.class,
        Uploads.class
})
public class BenchmarkSecurityTests {

    @Before
    public void initialize() {
        PowerMockito.mockStatic(Benchmarks.class);
        PowerMockito.mockStatic(Permissions.class);
        PowerMockito.mockStatic(GeneralSecurity.class);
        PowerMockito.mockStatic(Validator.class);
        PowerMockito.mockStatic(Processors.class);
        PowerMockito.mockStatic(ProcessorSecurity.class);
        PowerMockito.mockStatic((Users.class));
        PowerMockito.mockStatic((Uploads.class));
    }

    @Test
    public void canUserDownloadBenchmarkGetsNullBenchTest() {
        // given
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(null);

        // when / then
        assertFalse(BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserCantSeeBenchTest() {
        // given
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(mock(Benchmark.class));
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(false);

        // when / then
        assertFalse(BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserHasAdminPrivileges() {
        // given
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

        // when / then
        assertTrue("The user is an admin so should be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenUserIsOwner() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(bench.isDownloadable()).willReturn(false);
        // Then benchmark is not the users benchmark
        given(bench.getUserId()).willReturn(userId);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        // The user has admin privileges.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);

        // when / then
        assertTrue("The user is owner so should be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserDownloadBenchmarkWhenBenchmarkIsDownloadable() {
        // given
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

        // when / then
        assertTrue("The benchmark is downloadable so user should be able to download.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }
    @Test
    public void canUserDownloadBenchmarkWhenUserIsNotAdminBenchIsNotDownloadableAndUserIsNotOwner() {
        // given
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

        // when / then
        assertFalse("The user should not be able to download the benchmark.",
                BenchmarkSecurity.canUserDownloadBenchmark(benchId, userId).isSuccess());
    }

    @Test
    public void canUserGetAnonymousLinkIfUserOwnsBenchmarkOrIsAdmin() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);

        // when / then
        assertTrue("Admins and owners should be able to get the anon link.",
                BenchmarkSecurity.canUserGetAnonymousLink(benchId, userId).isSuccess());
    }

    @Test
    public void canUserGetAnonymousLinkIfUserDoesNotOwnBenchmarkOrIsAdmin() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(false);

        // when / then
        assertFalse("Only admins and owners should be able to get the anon link.",
                BenchmarkSecurity.canUserGetAnonymousLink(benchId, userId).isSuccess());
    }

    @Test
    public void canUserSeeBenchmarkContentsWhenBenchIsNull() {
        // given
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.get(benchId)).willReturn(null);
        // when / then
        assertFalse("Should be false for null benchmark.", BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());
    }

    @Test
    public void userCanSeeBenchmarkContentsIfUserIsOwner() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        // User owns this benchmark.
        given(bench.getUserId()).willReturn(userId);
        // user is not admin
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);
        given(Benchmarks.get(benchId)).willReturn(bench);

        // when / then
        assertTrue("Should be true since user owns the benchmark",
                BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());
    }

    @Test
    public void userCanSeeBenchmarkContentsIfUserIsAdmin() {
        // given
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        // User does not own this benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        // user is admin
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(true);
        given(Benchmarks.get(benchId)).willReturn(bench);

        // when / then
        assertTrue("Should be true since user is admin", BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());
    }

    @Test
    public void userCanSeeBenchmarkContentsIfBenchIsDownloadableAndUserCanSeeBench() {
        // given
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        // User does not own this benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        // bench is downloadable
        given(bench.isDownloadable()).willReturn(true);
        given(bench.getId()).willReturn(benchId);
        // user is not admin.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        given(Benchmarks.get(benchId)).willReturn(bench);

        // when / then
        assertTrue("Should be true since bench is downloadable and user can see the benchmark.",
                BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());

    }

    @Test
    public void userCannotSeeBenchmarkContentsIfBenchIsNotDownloadable() {
        // given
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        // User does not own this benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        // bench is downloadable
        given(bench.isDownloadable()).willReturn(false);
        // user is not admin.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(true);
        given(Benchmarks.get(benchId)).willReturn(bench);

        // when / then
        assertFalse("Should be false since bench is not downloadable.",
                BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());
    }

    @Test
    public void userCannotSeeBenchmarkContentsIfUserCannotSeeBench() {

        // given
        int benchId = 1;
        int userId = 2;
        int otherUserId = 3;
        Benchmark bench = mock(Benchmark.class);
        // User does not own this benchmark
        given(bench.getUserId()).willReturn(otherUserId);
        // bench is downloadable
        given(bench.isDownloadable()).willReturn(true);
        // user is not admin.
        given(GeneralSecurity.hasAdminReadPrivileges(userId)).willReturn(false);
        given(Permissions.canUserSeeBench(benchId, userId)).willReturn(false);
        given(Benchmarks.get(benchId)).willReturn(bench);

        // when / then
        assertFalse("Should be true false since user cant see bench.",
                BenchmarkSecurity.canUserSeeBenchmarkContents(benchId, userId).isSuccess());
    }

    @Test
    public void userCannotDeleteOrRecycleBenchIfBenchCannotBeFound() {
        // given
        int benchId = 1;
        int userId = 2;
        given(Benchmarks.getIncludeDeletedAndRecycled(benchId,false)).willReturn(null);

        // when / then
        assertFalse("Benchmark cannot be found so should not be deletable.",
                BenchmarkSecurity.canUserDeleteBench(benchId, userId).isSuccess());
        assertFalse("Benchmark cannot be found so should not be recyclable.",
                BenchmarkSecurity.canUserRecycleBench(benchId, userId).isSuccess());
    }

    @Test
    public void userCannotDeleteOrRecycleBenchIfTheyDoNotOwnBenchOrAreAdmin() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.getIncludeDeletedAndRecycled(benchId,false)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(false);

        // when / then
        assertFalse("User does not own bench and is not admin so should not be deletable.",
                BenchmarkSecurity.canUserDeleteBench(benchId, userId).isSuccess());
        assertFalse("User does not own bench and is not admin so should not be recyclable.",
                BenchmarkSecurity.canUserRecycleBench(benchId, userId).isSuccess());
    }

    @Test
    public void userCanDeleteOrRecycleBenchIfTheyOwnBenchOrAreAdmin() {
        // given
        int benchId = 1;
        int userId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(Benchmarks.getIncludeDeletedAndRecycled(benchId,false)).willReturn(bench);
        given(Benchmarks.get(benchId)).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);

        // when / then
        assertTrue("User owns bench or is admin so should be deletable.",
                BenchmarkSecurity.canUserDeleteBench(benchId, userId).isSuccess());
        assertTrue("User owns bench or is admin so should be recyclable.",
                BenchmarkSecurity.canUserRecycleBench(benchId, userId).isSuccess());
    }

    @Test
    public void userCantRestoreBenchmarkIfBenchmarkCantBeFound() {
        // given
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(null);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRestoreBenchmark(1, 2);

        // then
        assertFalse("User cant restore benchmark if benchmark was not found.", status.isSuccess());
    }

    @Test
    public void userCantRestoreBenchmarkIfBenchmarkNotRecycled() {
        // given
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(mock(Benchmark.class));
        given(Benchmarks.isBenchmarkRecycled(anyInt())).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRestoreBenchmark(1, 2);

        // then
        assertFalse("User cant restore benchmark if benchmark has not been recycled.", status.isSuccess());
    }

    @Test
    public void userCantRestoreBenchmarkIfUserDoesNotOwnBenchmarkAndIsNotAdmin() {
        // given
        int userId = 2;
        Benchmark b = mock(Benchmark.class);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(b);
        given(Benchmarks.isBenchmarkRecycled(anyInt())).willReturn(true);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(b, userId)).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRestoreBenchmark(1, userId);

        // then
        assertFalse("User cant restore benchmark if user does not own benchmark and is not admin.", status.isSuccess());
    }

    @Test
    public void userCanRestoreBenchmarkIfUserOwnsBenchmarkOrIsAdmin() {
        // given
        int userId = 2;
        Benchmark b = mock(Benchmark.class);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(b);
        given(Benchmarks.isBenchmarkRecycled(anyInt())).willReturn(true);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(b, userId)).willReturn(true);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRestoreBenchmark(1, userId);

        // then
        assertTrue("User can restore benchmark if user owns benchmark or is admin.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfNewNameIsNotValid() {
        // given
        given(Validator.isValidBenchName(anyString())).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, 3);

        // then
        assertFalse("Should not be able to edit benchmark if new name is invalid.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfDescriptionIsNotValid() {
        // given
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, 3);

        // then
        assertFalse("Should not be able to edit benchmark if description is invalid.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfBenchmarkCantBeFound() {
        // given
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(null);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, 3);

        // then
        assertFalse("Should not be able to edit benchmark if benchmark cannot be found.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfUserIsNotOwnerOrAdmin() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertFalse("Should not be able to edit benchmark if user is not owner or admin.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfBenchmarkIsDeleted() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);
        given(Benchmarks.isBenchmarkDeleted(anyInt())).willReturn(true);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertFalse("Should not be able to edit benchmark if it is deleted.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfBenchTypeCannotBeFound() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);
        given(Benchmarks.isBenchmarkDeleted(anyInt())).willReturn(false);
        given(Processors.get(anyInt())).willReturn(null);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertFalse("Should not be able to edit benchmark if processor cannot be found.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfProcessorIsNotBenchType() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        Processor proc = mock(Processor.class);
        // Processor is not BENCH type so test should fail.
        given(proc.getType()).willReturn(ProcessorType.POST);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);
        given(Benchmarks.isBenchmarkDeleted(anyInt())).willReturn(false);
        given(Processors.get(anyInt())).willReturn(proc);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertFalse("Should not be able to edit benchmark if processor is not bench type.", status.isSuccess());
    }

    @Test
    public void userCantEditBenchmarkIfUserCantSeeProcessor() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        Processor proc = mock(Processor.class);
        given(proc.getType()).willReturn(ProcessorType.BENCH);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);
        given(Benchmarks.isBenchmarkDeleted(anyInt())).willReturn(false);
        given(Processors.get(anyInt())).willReturn(proc);
        given(ProcessorSecurity.canUserSeeProcessor(anyInt(), anyInt())).willReturn(new ValidatorStatusCode(false));

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertFalse("Should not be able to edit benchmark if processor is not bench type.", status.isSuccess());
    }

    @Test
    public void userCanEditBenchmarkIfUserCanSeeProcessor() {
        // given
        final int userId = 3;
        Benchmark bench = mock(Benchmark.class);
        Processor proc = mock(Processor.class);
        given(proc.getType()).willReturn(ProcessorType.BENCH);
        given(Validator.isValidBenchName(anyString())).willReturn(true);
        given(Validator.isValidPrimDescription(anyString())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(bench);
        given(BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId)).willReturn(true);
        given(Benchmarks.isBenchmarkDeleted(anyInt())).willReturn(false);
        given(Processors.get(anyInt())).willReturn(proc);
        given(ProcessorSecurity.canUserSeeProcessor(anyInt(), anyInt())).willReturn(new ValidatorStatusCode(true));

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserEditBenchmark(1, "name", "description", 2, userId);

        // then
        assertTrue("Should not be able to edit benchmark if processor is not bench type.", status.isSuccess());
    }

    @Test
    public void userOwnsBenchOrIsAdminFalseIfBenchIsNull() {
        // given
        Benchmark bench = null;

        // when
        boolean success = BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, 1);

        // then
        assertFalse("Bench is null so nobobody should own it.", success);
    }

    @Test
    public void userOwnsBenchOrIsAdminFalseIfUserDoesNotOwnBenchAndIsNotAdmin() {
        // given
        int userId = 1;
        int otherUserId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(bench.getUserId()).willReturn(otherUserId);
        given(GeneralSecurity.hasAdminWritePrivileges(anyInt())).willReturn(false);

        // when
        boolean success = BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId);

        // then
        assertFalse("User does not own benchmark and is not admin so should be false.", success);
    }

    @Test
    public void userOwnsBenchOrIsAdminTrueIfUserOwnsBench() {
        // given
        int userId = 1;
        Benchmark bench = mock(Benchmark.class);
        given(bench.getUserId()).willReturn(userId);
        given(GeneralSecurity.hasAdminWritePrivileges(anyInt())).willReturn(false);

        // when
        boolean success = BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId);

        // then
        assertTrue("User owns benchmark so should be true.", success);
    }

    @Test
    public void userOwnsBenchOrIsAdminTrueIfUserIsAdmin() {
        // given
        int userId = 1;
        int otherUserId = 2;
        Benchmark bench = mock(Benchmark.class);
        given(bench.getUserId()).willReturn(otherUserId);
        given(GeneralSecurity.hasAdminWritePrivileges(anyInt())).willReturn(true);

        // when
        boolean success = BenchmarkSecurity.userOwnsBenchOrIsAdmin(bench, userId);

        // then
        assertTrue("User has admin privileges so should be true.", success);
    }

    @Test
    public void userCantRecycleOrphanedBenchmarksIfUserCantBeFound() {
        // given
        given(Users.get(anyInt())).willReturn(null);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRecycleOrphanedBenchmarks(1, 2);

        // then
        assertFalse("User can't recycle orphaned benchmarks if owner cant be found.", status.isSuccess());
    }

    @Test
    public void userCantRecycleOrphanedBenchmarksIfUserIsNotAdminOrOwner() {
        // given
        int owner = 1;
        int userId = 2;
        given(Users.get(anyInt())).willReturn(mock(User.class));
        given(GeneralSecurity.hasAdminWritePrivileges(userId)).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRecycleOrphanedBenchmarks(owner, userId);

        // then
        assertFalse("User is neither owner or admin so cannot recycle orphaned benchmarks.", status.isSuccess());
    }

    @Test
    public void userCanRecycleOrphanedBenchmarksIfAdmin() {
        // given
        int owner = 1;
        int userId = 2;
        given(Users.get(anyInt())).willReturn(mock(User.class));

        // user is admin
        given(GeneralSecurity.hasAdminWritePrivileges(userId)).willReturn(true);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRecycleOrphanedBenchmarks(owner, userId);

        // then
        assertTrue("User is admin so should be able to recycle orphaned benchmarks.", status.isSuccess());
    }

    @Test
    public void userCanRecycleOrphanedBenchmarksIfOwner() {
        // given

        // user is owner
        int owner = 1;
        int userId = owner;
        given(Users.get(anyInt())).willReturn(mock(User.class));
        // user is not admin
        given(GeneralSecurity.hasAdminWritePrivileges(userId)).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canUserRecycleOrphanedBenchmarks(owner, userId);

        // then
        assertTrue("User is owner so should be able to recycle orphaned benchmarks.", status.isSuccess());
    }

    @Test
    public void userCantGetBenchmarkJsonIfUserCantSeeBenchmark() {
        // given
        given(Permissions.canUserSeeBench(anyInt(), anyInt())).willReturn(false);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canGetJsonBenchmark(1, 2);

        // then
        assertFalse("User cannot see benchmark so they cannot get the JSON.", status.isSuccess());
    }

    @Test
    public void userCantGetBenchmarkJsonIfBenchmarkCantBeFound() {
        // given
        given(Permissions.canUserSeeBench(anyInt(), anyInt())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(null);

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canGetJsonBenchmark(1, 2);

        // then
        assertFalse("Benchmark cant be found so user cannot get the JSON.", status.isSuccess());
    }

    @Test
    public void userCanGetBenchmarkJsonIfUserCanSeeBenchmarkAndItCanBeFound() {
        // given
        given(Permissions.canUserSeeBench(anyInt(), anyInt())).willReturn(true);
        given(Benchmarks.getIncludeDeletedAndRecycled(anyInt(), anyBoolean())).willReturn(mock(Benchmark.class));

        // when
        ValidatorStatusCode status = BenchmarkSecurity.canGetJsonBenchmark(1, 2);

        // then
        assertTrue("User should be able to get Benchmark JSON since they can see it.", status.isSuccess());
    }

    @Test
    public void userCantSeeBenchmarkUploadStatusIfItCantBeFound() {
        // given
        given(Uploads.getBenchmarkStatus(anyInt())).willReturn(null);

        // when
        boolean success = BenchmarkSecurity.canUserSeeBenchmarkStatus(1, 2);

        // then
        assertFalse("User should not be able to see benchmark upload status if it cannot be found.", success);
    }

    @Test
    public void userCanSeeBenchmarkUploadStatusIfUserHasAdminPrivileges() {
        // given
        given(Uploads.getBenchmarkStatus(anyInt())).willReturn(mock(BenchmarkUploadStatus.class));
        given(GeneralSecurity.hasAdminReadPrivileges(anyInt())).willReturn(true);

        // when
        boolean success = BenchmarkSecurity.canUserSeeBenchmarkStatus(1, 2);

        // then
        assertTrue("User is admin so should be able to see bench upload status.", success);
    }

    @Test
    public void userCantSeeBenchmarkUploadStatusIfUserIsNotOwner() {
        // given
        int userId = 2;
        int owner = 3;
        BenchmarkUploadStatus uploadStatus = mock(BenchmarkUploadStatus.class);
        given(uploadStatus.getUserId()).willReturn(owner);
        given(Uploads.getBenchmarkStatus(anyInt())).willReturn(uploadStatus);
        // User is not admin.
        given(GeneralSecurity.hasAdminReadPrivileges(anyInt())).willReturn(false);

        // when
        boolean success = BenchmarkSecurity.canUserSeeBenchmarkStatus(1, userId);

        // then
        assertFalse("User is not owner so should not be able to see upload status.", success);
    }

    @Test
    public void userCanSeeBenchmarkUploadStatusIfUserIsOwner() {
        // given
        int userId = 2;
        int owner = userId;
        BenchmarkUploadStatus uploadStatus = mock(BenchmarkUploadStatus.class);
        given(uploadStatus.getUserId()).willReturn(owner);
        given(Uploads.getBenchmarkStatus(anyInt())).willReturn(uploadStatus);
        // User is not admin.
        given(GeneralSecurity.hasAdminReadPrivileges(anyInt())).willReturn(false);

        // when
        boolean success = BenchmarkSecurity.canUserSeeBenchmarkStatus(1, userId);

        // then
        assertTrue("User is owner so should be able to see upload status.", success);
    }
}
