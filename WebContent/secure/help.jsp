<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		String reference=request.getHeader("referer");
		if (reference!=null) {
			int argIndex=reference.indexOf("?");
			//first, get the referring URL down to just the path without any arguments
			if (argIndex>=0) {
				reference=reference.substring(0,argIndex);
			}
			//next, get rid of the ".jsp" and replace it with ".help"
			reference=reference.substring(0,reference.length()-4)+".help";
			
			reference=reference.substring(reference.indexOf("/secure/")+1);
			reference=Util.docRoot(reference);
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
  <li class="subject"><a href="/${starexecRoot}/secure/help/exploring-spaces.help">Exploring spaces</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/exploring-communities.help">Exploring communities</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/checking-cluster.help">Checking cluster status</a></li>
  <li class="topicHeader">Jobs</li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/running-jobs.help">Running jobs</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/viewing-results.help">Viewing results</a></li>
  <li class="topicHeader">Solvers</li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/uploading-solvers.help">Uploading solvers</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/viewing-solvers.help">Viewing solvers</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/editing-solvers.help">Editing solvers</a></li>
  <li class="topicHeader">Benchmarks</li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/uploading-benchmarks.help">Uploading benchmarks</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/viewing-benchmarks.help">Viewing benchmarks</a></li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/editing-benchmarks.help">Editing benchmarks</a></li>
  <li class="topicHeader">General</li>
  <li class="subject"><a href="/${starexecRoot}/secure/help/input-restrictions.help">Input restrictions</a></li>
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

