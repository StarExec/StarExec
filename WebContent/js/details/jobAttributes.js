
"use strict";

jQuery(function($) {
	var currentJobSpaceId=getParameterByName('id');
	var rootJobSpaceId=getParameterByName('id');
	var jobId = $('#data').data('jobid'); // Initialize the explorer list
	var spaceExplorerJsonData = getSpaceExplorerJsonData(jobId);
	var $table = $('attributeTable');

	$('#attributeTotalsTable').dataTable({
		'bSort': true,
		"bPaginate": true
	});

	$table.dataTable({
		'bSort': false,
		'fixedColumns': true,
		'scrollY': '300px',
		'scrollX': '100%',
		'scrollCollapse': true,
		'paging': false
	});

	$(window).resize($table.fnDraw);

	initSpaceExplorer(rootJobSpaceId, currentJobSpaceId, spaceExplorerJsonData);
	setupChangeTimeButton();
});

function getSpaceExplorerJsonData(jobId) {
	var url = starexecRoot+"services/space/" +jobId+ "/jobspaces/true";
	return {
		"ajax" : {
			"url" : url, // Where we will be getting json data from
			"data" : function (n) {
				return {
					id : (n.attr ? n.attr("id") : 0)
				}; // What the default space id should be
			}
		}
	};
}

function setupChangeTimeButton() {
	var isWallclock = true;
	var toggleTime = function() {
		if (isWallclock) {
			$('.changeTime .ui-button-text').html('use wallclock time');
			isWallclock = false;
			$('.cpuSum').show();
			$('.wallclockSum').hide();
		} else {
			isWallclock = true;
			$('.changeTime .ui-button-text').html('use CPU time');
			$('.wallclockSum').show();
			$('.cpuSum').hide();
		}
	}

	$('.cpuSum').hide();

	$(".changeTime")
		.button({
			icons: {
				primary: "ui-icon-refresh"
			}
		})
		.click(toggleTime);
}


function initSpaceExplorer(rootJobSpaceId, currentJobSpaceId, spaceExplorerJsonData) {
	// Set the path to the css theme for the jstree plugin

	$.jstree._themes = starexecRoot+"css/jstree/";

	log('Setting up explore list.');
	$("#exploreList").jstree({
		"json_data" : spaceExplorerJsonData,
		"themes" : {
		"theme" : "default",
		"dots" : true,
		"icons" : true
		},
		"types" : {
			"max_depth" : -2,
			"max_children" : -2,
			"valid_children" : [ "space" ],
			"types" : {
				"space" : {
					"valid_children" : [ "space" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/db.png"
					}
				}
			}
		},
		"ui" : {
			"select_limit" : 1,
			"selected_parent_close" : "select_parent",
			"initially_select" : [ "#"+rootJobSpaceId ]
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// Change the page to the appropriate jobspace.
		var newJobSpaceId = data.rslt.obj.attr("id");
		log("New job space id: "+newJobSpaceId);

		if (newJobSpaceId !== currentJobSpaceId) {
			window.location.href=starexecRoot+'secure/details/jobAttributes.jsp?id='+newJobSpaceId;
		}
	}).on( "click", "a", function (event, data) {
		event.preventDefault();  // This just disable's links in the node title
	});
	log("Initialized exploreList tree.");
}
