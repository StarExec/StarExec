<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		
		String reference=request.getParameter("ref");
		if (reference==null) {
			//there was no ref parameter, so try and use the actual referer
			reference=request.getHeader("referer");
			int argIndex=reference.indexOf("?");
			//first, get the referring URL down to just the path without any arguments
			if (argIndex>=0) {
				reference=reference.substring(0,argIndex);
			}
			//next, get rid of the ".jsp" and replace it with ".help"
			reference=reference.substring(0,reference.length()-4)+".help";
			
			reference=reference.substring(reference.indexOf("/secure/")+1);
			
			reference=Util.docRoot(reference);

	
		}
		if (reference!=null) {
			request.setAttribute("ref",reference);
			
		}
	} catch (Exception e) {
		//if we can't find the referrer, that's fine. Just load up the main help page
	}
	
%>
<star:template title="Help Pages" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, help/help, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/table, help/help">			

<span id="reference" href="${ref}"></span>

<div id="helpTopics">
 <ul id="topicList">
  <li class="topicHeader">Exploring</li>
  <li class="subject"><a href="/${starexecRoot}/secure/explore/spaces.help">Exploring spaces</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/explore/communities.help">Exploring communities</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/explore/cluster.help">Checking cluster status</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/explore/statistics.help">Viewing community statistics</a></li>
  <li class="topicHeader">Benchmarks</li>
  <li class="subject"><a href="/${starexecRoot}/secure/add/benchmarks.help">Adding benchmarks</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/details/benchmark.help">Accessing benchmarks</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/edit/benchmark.help">Editing benchmarks</a></li>
  <li class="topicHeader">Solvers</li>
  <li class="subject"><a href="/${starexecRoot}/secure/add/solver.help">Adding solvers</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/details/solver.help">Viewing solvers</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/edit/solver.help">Editing solvers</a></li>
  <li class="topicHeader">Jobs</li>
  <li class="subject"><a href="/${starexecRoot}/secure/add/job.help">Running jobs</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/details/job.help">Viewing results</a></li>
  <li class="topicHeader">Users</li>
  <li class="subject"><a href="/${starexecRoot}/secure/edit/account.help">Managing your account</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/edit/spacePermissions.help">Editing permissions</a></li>
  <li class="topicHeader">General</li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/input-restrictions.help">Input restrictions</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/linking-copying.help">Linking and copying</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/removing-recycling-deleting.help">Removing, recycling, and deleting primitives</a></li>
</ul>
</div>
	
<div id="detailPanel">	
 <p>These are the StarExec help pages.
 </p>
 <p>Please select a topic from the left hand side.
 </p>
 <br>
 <p>You can also check our 
 <a href="/${starexecRoot}/public/quickReference.jsp">Quick Reference</a> guide.
</p>		
</div>
	
</star:template>

