$(function(){
	$('#jobs').flexigrid({
		url: '/starexec/services/jobs/all',
		dataType: 'json',
		colModel : [
			{display: 'ID', name : 'jobid', width : 50, sortable : true, align: 'center', process: onJobClick},
			{display: 'Status', name : 'status', width : 80, sortable : true, align: 'left', process: onJobClick},
			{display: 'Runtime', name : 'runtime', width : 80, sortable : true, align: 'left', process: onJobClick},
			{display: 'Submitted', name : 'submitted', width : 150, sortable : true, align: 'left', process: onJobClick},
			{display: 'Completed', name : 'completed', width : 150, sortable : true, align: 'left', process: onJobClick},
			{display: 'Node', name : 'node', width : 140, sortable : true, align: 'left', process: onJobClick},
			{display: 'Timeout', name : 'timeout', width : 90, sortable : true, align: 'left', process: onJobClick}
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
	
	$('.flexigrid').attr('id', 'jobGrid');	// Give the main grid a new id
	$('#btnBack').hide();
});

function onJobClick(cellDiv, id){
  $(cellDiv).click(function(){
	  // When a row is clicked, get the job pair data related to the job and display it
	  var pairTable = $(document.createElement('table'));
	  $(pairTable).hide();
	  
	  $('#jobGrid').before(pairTable);	  	 	  
	  $('#jobGrid').fadeOut(200, function(){
		  $(pairTable).flexigrid({
				url: '/starexec/services/jobs/pairs/' + id,
				dataType: 'json',
				colModel : [
					{display: 'ID', name : 'jpid', width : 50, sortable : true, align: 'center'},
					{display: 'Status', name : 'status', width : 60, sortable : true, align: 'left'},
					{display: 'Result', name : 'result', width : 50, sortable : true, align: 'left'},
					{display: 'Solver', name : 'solver', width : 80, sortable : true, align: 'left'},
					{display: 'Config', name : 'config', width : 80, sortable : true, align: 'left'},
					{display: 'Bechmark', name : 'benchmark', width : 100, sortable : true, align: 'left'}	,			
					{display: 'Run Time', name : 'runtime', width : 80, sortable : true, align: 'left'},
					{display: 'Start Time', name : 'startime', width : 140, sortable : true, align: 'left'},
					{display: 'End Time', name : 'endtime', width : 140, sortable : true, align: 'left'},
					{display: 'Node', name : 'node', width : 70, sortable : true, align: 'left'}
					],
				searchitems : [
					{display: 'ID', name : 'jpid', isdefault: true},
					{display: 'Status', name : 'status'},
					{display: 'Result', name : 'result'},
					{display: 'Solver', name : 'solver'},
					{display: 'Bechmark', name : 'benchmark'},
					{display: 'Run Time', name : 'runtime'},
					{display: 'Start Time', name : 'startime'},
					{display: 'End Time', name : 'endtime'},
					{display: 'Node', name : 'node'},
					],
				sortname: "jpid",
				sortorder: "desc",
				usepager: true,
				title: 'Job pairs for job #' + id,
				useRp: true,
				rp: 15,
				singleSelect: true
			});	
		  $(pairTable).fadeIn(400, function(){});
		  $('#btnBack').fadeIn(400, function(){});
	  });
  });
}

function showJobs(){
	$('.flexigrid:not(:last)').remove();
	$('#jobGrid').fadeIn(400, function(){});
	$('#btnBack').hide();
}

function populateJobs(json){	
	$.each(json, function(i, job){			
		$('#jobs').append("<tr><td>" + job.jobid + "</td><td>" + job.status + "</td><td>" + job.submitted + "</td><td>" + job.completed + "</td><td>" + job.node + "</td><td>" + job.timeout + "</td></tr>");
	});
}