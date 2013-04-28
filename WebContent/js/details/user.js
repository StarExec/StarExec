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
	$('#jobs').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: "/starexec/services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	
	//Initiate solver table
	$('#solvers').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: "/starexec/services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler
    });
    
	
	//Initiate benchmark table
	$('#benchmarks').dataTable( {
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
			width: 'auto'
		});
	});  
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	//var usrId = getParameterByName("id");
	var tableName = $(this).attr('id');
	var usrId = $(this).attr("uid");
	
	$.post(  
			sSource + usrId + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view primitives for this user", 5000);
						break;
					default:
						updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
 						fnCallback(nextDataTablePage);
 						if('j' == tableName[0]){
 							colorizeJobStatistics();
 						} 
 						break;
				}
			},  
			"json"
	).error(function(){
		//alert('Session expired');
		window.location.reload(true); 
	});
}

/**
 * Helper function for fnPaginationHandler; since the proper fieldset to update
 * cannot be reliably found via jQuery DOM navigation from fnPaginationHandler,
 * this method provides manually updates the appropriate fieldset to the new value
 * 
 * @param tableName the name of the table whose fieldset we want to update (not in jQuery id format)
 * @param primCount the new value to update the fieldset with
 * @author Todd Elvers
 */
function updateFieldsetCount(tableName, value){
	switch(tableName[0]){
	case 'j':
		$('#jobExpd').children('span:first-child').text(value);
		break;
	case 's':
		if('o' == tableName[1]) {
			$('#solverExpd').children('span:first-child').text(value);
		} else {
			$('#spaceExpd').children('span:first-child').text(value);
		}
		break;
	case 'b':
		$('#benchExpd').children('span:first-child').text(value);
		break;
	}
}

function colorizeJobStatistics(){
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.asc").heatcolor(
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
	$("#jobs p.desc").heatcolor(
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