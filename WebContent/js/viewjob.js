$(function(){
	/*$.ajax({
		type:'Get',
		dataType: 'json',
		url:'/starexec/services/jobs/all',
		success:function(data) {
		 	populateJobs(data);		// Get the root divisions from the database
		},
		error:function(xhr, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});*/
	
	$('#jobs').flexigrid({
		url: '/starexec/services/jobs/all',
		dataType: 'json',
		colModel : [
			{display: 'ID', name : 'jobid', width : 50, sortable : true, align: 'center', process: onJobClick},
			{display: 'Status', name : 'status', width : 120, sortable : true, align: 'left', process: onJobClick},
			{display: 'Submitted', name : 'submitted', width : 160, sortable : true, align: 'left', process: onJobClick},
			{display: 'Completed', name : 'completed', width : 160, sortable : true, align: 'left', process: onJobClick},
			{display: 'Node', name : 'node', width : 160, sortable : true, align: 'left', process: onJobClick},
			{display: 'Timeout', name : 'timeout', width : 100, sortable : true, align: 'left', process: onJobClick}
			],
		searchitems : [
			{display: 'ID', name : 'jobid', isdefault: true},
			{display: 'Node', name : 'node'},
			{display: 'Submitted', name : 'submitted'},
			{display: 'Completed', name : 'completed'},
			],
		sortname: "jobid",
		sortorder: "desc",
		usepager: true,
		title: 'Jobs',
		useRp: true,
		rp: 15,
		singleSelect: true
	});		
});

function onJobClick(cellDiv, id){
  $(cellDiv).click(function(){
	  alert(id); 
  });
}

function populateJobs(json){	
	$.each(json, function(i, job){			
		$('#jobs').append("<tr><td>" + job.jobid + "</td><td>" + job.status + "</td><td>" + job.submitted + "</td><td>" + job.completed + "</td><td>" + job.node + "</td><td>" + job.timeout + "</td></tr>");
	});
}