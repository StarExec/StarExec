<%@tag description="Standard header content for all starexec pages (not the same as head.tag!)"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header id="pageHeader">
	<a href="/${starexecRoot}/secure/index.jsp"><img src="/${starexecRoot}/images/starlogo.png" alt="StarExec Logo"></a>
	
	<c:if test="${not empty user && user.role != 'unauthorized' && user.role != 'suspended'}">
		<nav>
			<ul>
				<c:if test="${user.role == 'admin'}">
				<li class="round">
					<a href="#">Admin</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/user.jsp">Users</a></li>  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/cluster.jsp">Cluster</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/job.jsp">Jobs</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/community.jsp">Communities</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/testing.jsp">Testing</a></li>  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/starexec.jsp">StarExec</a></li>
			           
			        </ul>  
				</li>
				</c:if>
				<li class="round">
					<a href="#">Account</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/details/user.jsp?id=${user.id}">Profile</a></li>  
			            <li class="round"><a href="#" onclick="javascript:logout();">Logout</a></li>  
			        </ul>  
				</li>
				<li class="round">
					<a href="#">Spaces</a>  
			        <ul class="subnav round">  
			        	<li class="round"><a href="/${starexecRoot}/secure/explore/spaces.jsp">Explore</a></li>			            
			            <li class="round"><a href="/${starexecRoot}/secure/explore/communities.jsp">Communities</a></li>
						<li class="round"><a href="/${starexecRoot}/secure/explore/statistics.jsp">Statistics</a></li> 		            
						<li class="round"><a href="/${starexecRoot}/secure/explore/reports.jsp">Reports</a></li> 		            
			        </ul>  
				</li>
				<!-- <li class="round">
					<a href="#">jobs</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="#">mine</a></li>  
			            <li class="round"><a href="#">recent</a></li>  
			        </ul>  
				</li>-->
				<li class="round">
					<a href="#">Cluster</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/explore/cluster.jsp">Status</a></li>  			             
			        </ul>  
				</li>			
				<li class="round" id="helpTab"><a id="helpTag" href="/${starexecRoot}/secure/help.jsp">Help</a></li>
			</ul>
		</nav>
	</c:if>
</header>
