<%@page contentType="text/html" pageEncoding="UTF-8"  import="java.io.*, java.lang.StringBuilder" %>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Version Information" css="public/versionInfo">
	<pre id="infoLoc">
-- Users' disk quotas for benchmark and solver uploads are now enforced.

-- Handle user's trying to register with duplicate e-mail

-- Uploaded description from solvers adjusted so it searches root directory
	
In revision 12725:

Improvements:

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