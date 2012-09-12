var jobTable;
$(document).ready(function(){
	// Hide loading images by default
	$('legend img').hide();
	
	$('.popoutLink').button({
		icons: {
			secondary: "ui-icon-newwin"
    }});
	
	$('#downLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
	
	$('#editLink').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#returnLink, #returnLinkMargin').button({
		icons: {
			secondary: "ui-icon-arrowreturnthick-1-w"
		}});
	
	$('img').click(function(event){
		PopUp($(this).attr('enlarge'));
	});
	
	//Initiate job table
	$('#usrJobsTable').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: "/starexec/services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
});

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto',
		});
	});  
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	var usrId = getParameterByName("id");
	
	$.post(  
			sSource + usrId + "/jobs/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					default:
						// Replace the current page with the newly received page
						fnCallback(nextDataTablePage);
						$('#jobExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
						colorizeJobStatistics();
						break;
				}
			},  
			"json"
	).error(function(){
		alert('Session expired');
		window.location.reload(true); 
	});
}

function colorizeJobStatistics(){
	// Colorize the statistics in the job table for completed pairs
	$("#usrJobsTable p.asc").heatcolor(
			function() {
				// Return the floating point value of the stat
				return eval($(this).text());
			},
			{ 
				maxval: 1,
				minval: 0,
				colorStyle: 'greentored',
				lightness: 0 
			}
	);
	
	// Colorize the statistics in the job table (for pending and error which use reverse color schemes)
	$("#usrJobsTable p.desc").heatcolor(
			function() {
				return eval($(this).text());
			},
			{ 
				maxval: 1,
				minval: 0,
				colorStyle: 'greentored',
				reverseOrder: true,
				lightness: 0 
			}
	);
}