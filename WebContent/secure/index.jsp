<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="starexec preview">	
	<style type="text/css">
		#content a:link {
			text-decoration: underline;
		}
		#content ul {
			margin-left: 40px;
		}
		#content ul li {
			padding: 3px;
		}
	</style>		
	<p>this is an alpha release of starexec. please note this is a limited preview, and as with all new software we will be prone to bugs and unfinished features. all user-generated content may be cleared at any time, but your credentials will remain in tact during the preview. you can help us improve starexec's quality by reporting bugs directly to the developers as well as any general feedback on functionality and design by selecting 'give feedback' below. you can return to this page by clicking the starexec logo at the top of any page.</p>
	<br/><br/>		
	<ul>
		<li>
			<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Feedback">give feedback</a>
		</li>
		<li>
			<a href="mailto:${contactEmail}?subject=[Starexec ${buildVersion}] Bug Report">report bug</a>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/Home">public dev wiki</a>
		</li>
		<li>
			<a href="http://wiki.uiowa.edu/display/stardev/User+Guide">user guide</a>
		</li>
	</ul>	
	<br/><br/>
	<p>this build was last updated on ${buildDate}</p>
</star:template>