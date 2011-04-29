$(function(){
	$.ajax({
		type:'Get',
		dataType: 'json',
		url:'/starexec/services/jobs/all',
		success:function(data) {
		 	populateJobs(data);		// Get the root divisions from the database
		},
		error:function(xhr, textStatus, errorThrown) {
			alert(errorThrown);
		}
	});
});

function populateJobs(json){	
	$.each(json, function(i, job){			
		$('#jobs').append("<tr><td>" + job.jobid + "</td><td>" + job.status + "</td><td>" + job.submitted + "</td><td>" + job.completed + "</td><td>" + job.node + "</td><td>" + job.timeout + "</td></tr>");
	});
}