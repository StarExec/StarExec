<%@tag description="Standard header content for all starexec pages (not the same as head.tag!)"%>
<%@tag import="com.starexec.data.to.*, com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="title" %>
<%@attribute name="css" %>
<%@attribute name="js" %>

<header id="pageHeader">
	<img src="/starexec/images/logo.svg" alt="Starexec Logo">
	<h1>star<span class="font-accent">exec</span></h1>
	<nav>
		<ul>
			<li class="round"><a href="#">dash</a>
			<li class="round"><a href="#">upload</a>
			<li class="round"><a href="#">create</a>
		</ul>
	</nav>
</header>