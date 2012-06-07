<%@page contentType="text/html" pageEncoding="UTF-8"%>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Extra Credit Sheet" css="explore/quickRef">	
	<p> This a quick worksheet for slightly more obscure tasks in StarExec. </p>
	
	<p><b>Downloading a Space Hierarchy XML Representation</b> - click on the 'download space xml' action button and you get the following in your xml file with
	both names and database ids.  You also receive a copy of the xml schema.</p>
	<ul id="qrTable">
		<li>spaces</li>
		<li>solvers</li>
		<li>benchmarks</li>
	</ul>	
	
	<p><b>Uploading a Space Hierarchy XML Representation</b> - click on the 'upload space xml' action button and upload a compressed xml representation
	an xml representation of a space hierarchy.  The  </p>
	<ul id="qrTable">
		<li>spaces - the id you give don't matter, but the name will be the name of the space in starexec </li>
		<li>solvers - the name you give doesn't matter, but the id must correspond to a solver you have access to</li>
		<li>benchmarks - the name you give doesn't matter, but the id must correspond to a benchmark you have access to</li>
	</ul>
	
</star:template>