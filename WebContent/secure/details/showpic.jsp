<%
	try {
		String imgurl = request.getParameter("imgurl").toString();
		request.setAttribute("imgurl", imgurl);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	}
%>

<img src= ${imgurl} width = 300>