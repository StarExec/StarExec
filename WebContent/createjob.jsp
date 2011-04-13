<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.CREATE_JOB %></title>
<%@ include file="includes/jQuery.html" %>

<style type="text/css">
ul {
	list-style-type:none;
}
</style>

<script type="text/javascript">
	$(document).ready(function(){
		$.ajax({
			type:'Get',
			dataType: 'json',
			url:'/starexec/services/solvers/all',
			success:function(data) {
			 	populateSolvers(data);		// Get the root divisions from the database
			},
			error:function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});	
		
		$.ajax({
			type:'Get',
			dataType: 'json',
			url:'/starexec/services/benchmarks/all',
			success:function(data) {				
				populateBenchmarks(data);		// Get the root divisions from the database
			},
			error:function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});	
	});
	
	function populateSolvers(json){
		// For each json solver return from the webservice, shove it into the DOM
		$.each(json, function(i, solver){			
			$('#solvers').append("<li><input name='solver' class='solChk' type='checkbox' value='" + solver.id + "'/> <span> " + solver.name + "</span></li>");
		});
	}
	
	function populateBenchmarks(json){
		// For each json solver return from the webservice, shove it into the DOM
		$.each(json, function(i, bench){			
			$('#bench').append("<li><input name ='bench' class='benchChk' type='checkbox' value='" + bench.id + "'/> <span> " + bench.fileName + "</span></li>");
		});
	}
	
	/*
	function doSubmit(){		
		valList = extractSelected($('#levels'));		// Extract the top-most selected checkbox values from the levels list
		$('form').attr('action', "UploadSolver?<%=P.SUPPORT_DIV%>=" + valList.join(','));	// Set the form to submit to the UploadSolver servlet with the selected values

		if(valList.length)								// If we had at least one supported level selected...
			return true;
		else {											// Else show an error and return false
			alert("The solver must support at least one division!");
			return false;
		}
	}
	
	function extractSelected(ulist){												// Extracts selected checkbox values from a list
		var localList = [];															// Create a list for my subtree...
		$(ulist).children('li').each(function(index, li){							// For each list item in my immediate children
			if($(li).children("input[type=checkbox]:first").is(':checked'))			// If the listitem contains a checked checkbox...
				localList.push($(li).children("input[type=checkbox]:first").val()); // Add that value to the list and don't bother evaluating my subtrees because I'm selected, therefore they must be as well
			else if($(li).children("ul").length) 									// Else, traverse my subtree recursively and find selected checkbox values				
				localList = localList.concat(extractSelected($(li).children("ul:first")));		// Traverse my subtree and append the results to my list
		});
		
		return localList;	// Return the resulting list
	}
	
	function toggleCheck(element){				
		if($(element).is(":checked")){		// If I'm checked
			$(element).parent().find("input[type=checkbox]").attr('checked', true);				// Check any child checkboxes
		} else {
			//$(element).parent().find("ul input[type=checkbox]").attr('checked', false);		// Uncheck my child checkboxes			
			$(element).parents("li").children("input[type=checkbox]").attr('checked', false);	// Uncheck all my ancestors
		}
	}
	
	function populateRoots(json){
		// For each json level return from the webservice, shove it into the DOM
		$.each(json, function(i, level){			
			$('#levels').append("<li><input class='lvlChk' onclick='toggleCheck(this)' type='checkbox' value='" + level.id + "'/> <span onclick='getSublevel(this, " + level.id + ")'> " + level.name + "</span></li>");
		});
	}
	
	function getSublevel(element, levelId){
		if($(element).parent().has("ul").length > 0){	// If the element already fetched a list
			$(element).siblings("ul").toggle();			// Toggle it
			return;										// Don't call the database again
		}
			
		$.ajax( {
			type:'Get',
			dataType: 'json',
			url:'/starexec/services/levels/sublevels/' + levelId,	// Call the webservice to get my direct children
			success:function(data) {								
				var ul = $(document.createElement('ul'));			// Create a new list
				$.each(data, function(i, level){					// For each sublevel returned from the service, insert it into the DOM
					$(ul).append("<li><input class='lvlChk' onclick='toggleCheck(this)' type='checkbox' value='" + level.id + "'/> <span onclick='getSublevel(this, " + level.id + ")'> " + level.name + "</span></li>");
				});				
				
				$(element).after(ul);	// Insert the list after the element that was clicked 
				
				if($(element).siblings("input[type=checkbox]").is(":checked")){				// If the element that was clicked was checked...
					$(element).parent().find("input[type=checkbox]").attr('checked', true);	// Then check all of the sublevels I just added
				}
			},
			error:function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});			
	}
	*/
</script>

</head>
<body>
	<form id="upForm" action="SubmitJob" method="POST">
		<h1>Job Creator</h1>
		<h2>Select Solvers</h2>
		<ul style="margin-top:10px;" id="solvers">		
		</ul>
		
		<h2>Select Benchmarks</h2>
		<ul style="margin-top:10px;" id="bench">
		</ul>
															
		<input type="submit"/>		
	</form>	
	
</body>
</html>