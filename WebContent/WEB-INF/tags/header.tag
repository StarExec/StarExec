<%@tag description="Standard header content for all starexec pages (not the same as head.tag!)"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header id="pageHeader">
	<a href="/${starexecRoot}/secure/index.jsp"><img src="/${starexecRoot}/images/starlogo.png" alt="Starexec Logo"></a>
	
	<c:if test="${not empty user && user.role != 'unauthorized'}">
		<nav>
			<ul>
				<li class="round" id="helpTab">
					<a id="helpTag" href="/${starexecRoot}/secure/help.jsp" target="_blank">help</a>
				</li>
				<c:if test="${user.role == 'admin'}">
				<li class="round">
					<a href="#">admin</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/user.jsp">users</a></li>  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/cluster.jsp">cluster</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/job.jsp">jobs</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/community.jsp">communities</a></li>
			            <li class="round"><a href="/${starexecRoot}/secure/admin/testing.jsp">Testing</a></li>  
			            <li class="round"><a href="/${starexecRoot}/secure/admin/starexec.jsp">Starexec</a></li>
			           
			        </ul>  
				</li>
				</c:if>
				<li class="round">
					<a href="#">account</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/details/user.jsp?id=${user.id}">profile</a></li>  
			            <li class="round"><a href="#" onclick="javascript:logout();">logout</a></li>  
			        </ul>  
				</li>
				<li class="round">
					<a href="#">spaces</a>  
			        <ul class="subnav round">  
			        	<li class="round"><a href="/${starexecRoot}/secure/explore/spaces.jsp">explore</a></li>			            
			            <li class="round"><a href="/${starexecRoot}/secure/explore/communities.jsp">communities</a></li>    			            
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
					<a href="#">cluster</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/${starexecRoot}/secure/explore/cluster.jsp">status</a></li>  			             
			        </ul>  
				</li>			
			</ul>
		</nav>
	</c:if>
</header>