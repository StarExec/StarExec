<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="constants.*, data.*, data.to.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>

<script type="text/javascript">
	$(document).ready(function(){
		$.ajax( {
			type:'Get',
			dataType: 'json',
			url:'/starexec/services/levels/root',
			success:function(data) {
			 	populateRoots(data);
			},
			error:function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});		
	});
	
	function populateRoots(json){
		$.each(json, function(i, level){
			$('#levels').append("<li onclick='getSublevel(this," + level.id + ")'>" + level.name + "</li>");			
		});
	}
	
	function getSublevel(element, levelId){
		$.ajax( {
			type:'Get',
			dataType: 'json',
			url:'/starexec/services/levels/sublevels/' + levelId,
			success:function(data) {				
				var li = $(document.createElement('li'));
				var ul = $(document.createElement('ul'));
				$.each(data, function(i, level){
					$(ul).append("<li onclick='getSublevel(this," + level.id + ")'>" + level.name + "</li>");			
				});				
				$(li).append(ul);
				$(element).after(li);
			},
			error:function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});	
	}
</script>

</head>
<body>
	<form id="upForm" enctype="multipart/form-data" action="UploadSolver" method="POST">
		<h2>Solver Upload</h2>
		<label>Solver ZIP</label>
		<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>											
		<input type="submit"/>
	</form>
	
	<h2>Select solveable groups</h2>
	<ul style="margin-top:10px; list-style-type:none;" id="levels">

	</ul>
</body>
</html>