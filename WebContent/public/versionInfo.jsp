<%@page contentType="text/html" pageEncoding="UTF-8"  import="java.io.*, java.lang.StringBuilder" %>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Version Information" css="public/versionInfo">
	<pre id="infoLoc">

In revision 12929:

-- Job output (downloaded from the job details page) is structured now
   to reflect the structure of the space, when running a whole space
   hierarchy as a job.  Paths within the space are also included in
   the CSV output now, for job information (again, from the job details
   page).

-- owners of benchmarks can always download them now.

-- benchmark names are validated on upload.

-- starexec_description.txt files are created when downloading spaces.

-- Numerous fixes and improvements to StarexecCommand, the browser-free
   client tool for accessing StarExec (download with the link at the
   bottom of the StarExec frame).  The most important is the new polljob
   and related commands, that let you pull job information and job output
   as it becomes available on StarExec.

-- Round-robin execution is now supported for jobs.  With this option,
   you will see all your subspaces progress in parallel when a job
   is running.

-- We now correctly distinguish memouts, cpu timeouts, and wallclock
   timeouts.

-- Solvers can be downloaded for re-upload now: the downloaded archive 
   does not have a top-level directory for the solver, so it matches
   the format StarExec expects on upload.

-- quotas are being calculated and enforced.

In revision 12782:

-- We are now validating the names of benchmark before adding the
   benchmarks to the DB.  I fixed an issue where our name validator
   was too strict and where unvalidated names were not being 
   reported properly to the user.  This fixes an issue Geoff was
   having uploading TPTP axioms.

In revision 12770:

-- Should be able to delete benchmark validators now.

-- Fixed bug introduced in 12755, where the postprocessor was
   not getting invoked correctly on job output.

-- updated docs for StarexecCommand.

-- jobs will begin execution right when entered (this behavior
   changed in the previous release, when fair scheduling was added).

In revision 12755:

-- The names of solvers can contain spaces (' ').  

-- small bug with the edit processors page (from edit community) is fixed.

-- scheduling jobs is fair now for each queue, and work is dispatched
   to queues independently (before, a job could incorrectly be blocked
   waiting for another queue to clear).

-- initial version of StarexecCommand is posted (more documentation coming
   soon from Eric).

-- Handle user's trying to register with duplicate e-mail

-- back/cancel buttons have been added to some pages

-- The root directory of an archive is searched for starexec_description.txt
   when a solver is uploaded and the user requests that the description is
   pulled from the archive.
	
In revision 12730:

Improvements:

-- owners can always download their own benchmarks, regardless of downloadable status.

-- benchmark names are now validated on upload.

-- default post-processors for jobs chosen correctly on job creation page.

-- workaround for problem selecting dependency space out of a large number
   of spaces on the job creation page.

In revision 12647:

Bug fixes:

-- the previous release was not configured correctly, so job results
   were not getting sent back to the database after job pairs completed
   running.  This is fixed now.

Improvements:

-- the CSV spreadsheets for job information now contain the attributes
   computed by the postprocessors for the job pairs.  

In revision 12601:

Bug fixes:

-- Dedicated queues like smteval.q are now getting recognized
   correctly.

-- No more "Session expired" alerts and unusable pages.

-- We will not insert redundant rows into the job_attributes table, which
   was leading to exceptions from the SQL layer.  This should fix
   a bug that was plaguing Geoff.

-- only the user can see their solvers, jobs, and benchmarks on his
   user details page, fixing a bug reported by Harald.

Improvements:

-- The job details pages now have summaries of results by solver.
   
-- A progress dialog is now shown during uploads and downloads.

-- The names of downloaded archives are still long and messy,
   but the directory names in those archives are not (requested by Harald).

-- The unit "s" has been dropped from the job information CSV (requested
   by Harald).

-- The runsolver tool has been updated, and it will now be given the --add-eof
   flag when executed (requested by Geoff).

-- Uploading benchmarks now extracts description for space (starexec_description.txt) and sets it.

-- Implemented back buttons for facilitated navigation.

-- The solver details page warns user if solver is uploaded without configuration.

</pre>
</star:template>