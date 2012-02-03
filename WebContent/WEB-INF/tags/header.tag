<%@tag description="Standard header content for all starexec pages (not the same as head.tag!)"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header id="pageHeader">
	<img src="/starexec/images/logo.svg" alt="Starexec Logo">
	<h1>star<span class="font-accent">exec</span></h1>
	
	<c:if test="${not empty user}">
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
				<li class="round">
					<a href="#">jobs</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="#">mine</a></li>  
			            <li class="round"><a href="#">recent</a></li>  
			        </ul>  
				</li>
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