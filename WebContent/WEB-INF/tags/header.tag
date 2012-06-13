<%@tag description="Standard header content for all starexec pages (not the same as head.tag!)"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header id="pageHeader">
	<a href="/starexec/secure/index.jsp"><img src="/starexec/images/starlogo.png" alt="Starexec Logo"></a>
	
	<c:if test="${not empty user && user.role != 'unauthorized'}">
		<nav>
			<ul>
				<c:if test="${user.role == 'admin'}">
				<li class="round">
					<a href="#">admin</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="#">users</a></li>  
			            <li class="round"><a href="#">cluster</a></li>
			            <li class="round"><a href="#">jobs</a></li>
			            <li class="round"><a href="#">communities</a></li>  
			            <li class="round"><a href="#">other</a></li>
			        </ul>  
				</li>
				</c:if>
				<li class="round">
					<a href="#">account</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/starexec/secure/edit/account.jsp">edit</a></li>  
			            <li class="round"><a href="#" onclick="javascript:logout();">logout</a></li>  
			        </ul>  
				</li>
				<li class="round">
					<a href="#">spaces</a>  
			        <ul class="subnav round">  
			        	<li class="round"><a href="/starexec/secure/explore/spaces.jsp">explore</a></li>			            
			            <li class="round"><a href="/starexec/secure/explore/communities.jsp">communities</a></li>    			            
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
			            <li class="round"><a href="/starexec/secure/explore/cluster.jsp">status</a></li>  			             
			        </ul>  
				</li>			
			</ul>
		</nav>
	</c:if>
</header>