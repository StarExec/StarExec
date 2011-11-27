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
			            <li class="round"><a href="#">remove user</a></li>  
			            <li class="round"><a href="#">kill job</a></li>
			            <li class="round"><a href="#">cluster</a></li>  
			            <li class="round"><a href="#">take offline</a></li>
			        </ul>  
				</li>
				</c:if>
				<li class="round">
					<a href="#">account</a>  
			        <ul class="subnav round">  
			            <li class="round"><a href="/starexec/secure/edit_account.jsp">edit</a></li>  
			            <li class="round"><a href="#" onclick="javascript:logout();">logout</a></li>  
			        </ul>  
				</li>
				<li class="round">
					<a href="#">spaces</a>  
			        <ul class="subnav round">  
			        	<li class="round"><a href="/starexec/secure/space_explorer.jsp">explore</a></li>			            
			            <li class="round"><a href="/starexec/secure/community/view.jsp">communities</a></li>    			            
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
			            <li class="round"><a href="#">status</a></li>  
			            <li class="round"><a href="#">availability</a></li>  
			        </ul>  
				</li>			
			</ul>
		</nav>
	</c:if>
</header>