var jobPairTable;
var qid = 0; // ID of the selected queue, or the queue that owns the selected node
var selectedId = 0;  // ID of the selected primitive

// When the document is ready to be executed on
$(document).ready(function(){
	initDataTables();

	//Set up row click to send to pair details page
	$('#details tbody').on( "click", "a", function(event) {
		event.stopPropogation();
	});

	$("#details tbody").on( "click", "tr", function(event) {
		if (jobPairTable.DataTable().data().length > 0) {
			var pairId = $(this).find('input').val();
			var url = starexecRoot + "secure/details/pair.jsp?id=" + pairId;
			if (event.ctrlKey || event.metaKey) {
				window.open(url, "_blank").focus();
			} else {
				window.location.assign(url);
			}
		}
	});

	// Build left-hand side of page (cluster explorer)
	initClusterExplorer();
	loadQstatOutput();

	$("#refreshQstat")
		.button({
			icons: {
				primary: "ui-icon-refresh"
			}
		})
		.click(
			loadQstatOutput
		)
	;

	$("#refreshLoads")
		.button({
			icons: {
				primary: "ui-icon-refresh"
			}
		})
		.click(
			loadQueueLoads
		)
	;

	$("#qstatField").expandable(true);
	$("#loadsField").expandable(true);
	$("#detailField").expandable(true);

	setInterval(function() {
		jobPairTable.fnDraw(false);
	}, 10000);
});

function initClusterExplorer() {
	// Set the path to the css theme fr the jstree plugin
	$.jstree._themes = starexecRoot+"css/jstree/";

	$("#exploreList").bind("loaded.jstree", function(e, data) {
		// Register a callback for when the jstree has finished loading
		addNodeCountsToTree();
	}).jstree({
		// Initialize the jstree plugin for the explorer list
		"json_data" : {
			"ajax" : {
				"url" : starexecRoot+"services/cluster/queues",	// Where we will be getting json data from
				"data" : function (n) {
					return { id : n.attr ? n.attr("id") : -1 }; // What the default space id should be
				}
			}
		},
		"themes" : {
			"theme" : "default",
			"dots" : true,
			"icons" : true
		},
		"types" : {
			"max_depth" : -2,
			"max_children" : -2,
			"valid_children" : [ "queue" ],
			"types" : {
				"active_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"inactive_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/off.png"
					}
				},
				"enabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"disabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/off.png"
					}
				}
			}
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		id = data.rslt.obj.attr("id");
		parent_node = $.jstree._reference('#exploreList')._get_parent(data.rslt.obj);
		getDetails(id,data.rslt.obj.attr("rel"),parent_node);
	}).on( "click", "a", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
}


function initDataTables() {
	jobPairTable = $('#details').dataTable(new star.DataTableConfig({
		"bServerSide"  : true,
		"bFilter"      : false,
		"sAjaxSource"  : starexecRoot+"services/cluster/",
		"fnServerData" : fnPaginationHandler,
		"columns"      : [
			{"title"   : "Created",
			 "width"   : "12.5%"},
			{"title"   : "Job",
			 "width"   : "12.5%"},
			{"title"   : "User",
			 "width"   : "12.5%"},
			{"title"   : "Benchmark",
			 "width"   : "25%"},
			{"title"   : "Solver",
			 "width"   : "12.5%"},
			{"title"   : "Config",
			 "width"   : "12.5%"},
			{"title"   : "Path",
			 "width"   : "12.5%"},
		]
	}));


	var formatName = function(row, type, val) {
		return star.format.jobLink(val);
	};

	var formatComplete = function(row, type, val) {
		return star.format.heatcolor((val["totalPairs"] - val["pendingPairs"]) * 100 / val["totalPairs"]);
	};

	var formatPending = function(row, type, val) {
		return val["pendingPairs"];
	};

	var formatUser = function(row, type, val) {
		return star.format.userLink(val["user"]);
	};

	var formatTime = function(row, type, val) {
		return star.format.timestamp(val["created"]);
	};

	var $jobs = $("#jobs");
	$jobs.dataTable(new star.DataTableConfig({
		"sServerMethod"  : "GET",
		"bServerSide"    : false,
		"bFilter"        : false,
		"order"          : [
			[4, "desc"],
			[0, "asc"]
		],
		"columns"        : [
			{"title"     : "Job",
			 "className" : "dt-left",
			 "render"    : formatName     },
			{"title"     : "User",
			 "render"    : formatUser     },
			{"title"     : "Pending",
			 "render"    : formatPending,
			 "className" : "dt-right",
			 "width"     : "80px"         },
			{"title"     : "Status",
			 "render"    : formatComplete,
			 "width"     : "60px"         },
			{"title"     : "Created",
			 "className" : "dt-right",
			 "width"     : "8em",
			 "render"    : formatTime     },
		]
	}));
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	var id = $('#exploreList').find('.jstree-clicked').parent().attr("id");
	//If we can't find the id of the queue/node from the DOM, get it from the cookie instead
	if (id == null || typeof id == 'undefined') {
		id = $.cookie("jstree_select");
		// If we also can't find the cookie, then just set the space selected to be the root space
		if (id == null || typeof id == 'undefined') {
			$('#exploreList').jstree('select_node', '#1', true);
			id = 1;
		} else {
			id = id[1];
		}
	}

	//In case the paginate happens before type is set
	if (typeof type == 'undefined') {
		window['type'] = 'queues';
	}
	//we have no pagination for inactive queues
	$.get(
			sSource + window['type'] + "/" + id + "/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					fnCallback(nextDataTablePage);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error populating table",5000);
	});
}

/**
 * Populates the node details panel with information on the given node
 */
function getDetails(id, type, parent_node) {
	var url = '';
	selectedId=id;
	jobPairTable.fnClearTable();	//immediately get rid of the current data, which makes it look more responsive
	if(type == 'active_queue' || type == 'inactive_queue') {
		var $jobs = $("#jobs");
		$("#clusterExpd").html("Enqueued Job Pairs <span> (+)</span>");
		$("#jobsContainer").show();
		url = starexecRoot+"services/cluster/queues/details/" + id;
		qid=id;
		$jobs.dataTable().api().ajax.url( starexecRoot + "services/cluster/queues/jobs/" + id ).load();
		window['type'] = 'queues';
		if (star.JobTableRefresh === undefined) {
			star.JobTableRefresh = window.setInterval($jobs.DataTable().ajax.reload, 10000);
		}
		$("#detailField .expdContainer").css("display", "none");
	} else if(type == 'enabled_node' || type == 'disabled_node') {
		$("#clusterExpd").text("Running Job Pairs");
		$("#jobsContainer").hide();
		url = starexecRoot+"services/cluster/nodes/details/" + id;
		qid=parent_node.attr("id");
		window['type'] = 'nodes';
		if (star.JobTableRefresh !== undefined) {
			window.clearInterval(star.JobTableRefresh);
			delete star.JobTableRefresh;
		}
		$("#detailField .expdContainer").css("display", "block");
	} else  {
		showMessage('error',"Invalid node type",5000);
		return;
	}
	loadQueueLoads();

	$('#loader').show();

	jobPairTable.fnDraw();
	$.get(
		url,
		populateAttributes,
		"json"
	).error(function(){
		showMessage('error',"Internal error getting node details",5000);
	});
}

function loadQstatOutput() {
	$.get(
			starexecRoot+"services/cluster/qstat",
			{},
			function(data){
				$("#qstatOutput").val(data);
			},
			"text"
	);
}

function loadQueueLoads() {
	$.get(
			starexecRoot+"services/cluster/loads/"+qid,
			{},
			function(data){
				$("#loadOutput").val(data);
			},
			"text"
	);
}

/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateAttributes(jsonData) {
	// Populate node details
	$('#workerName').text(jsonData.name.split('.')[0]);
	$('#queueID').text("id = "+selectedId);

	if(jsonData.cpuTimeout != null) {
		//We're dealing with a queue
		$('#activeStatus').hide();

	} else {
		//It is a node
		$('#activeStatus').show();

		if(jsonData.status == 'ACTIVE') {
			$('#activeStatus').text('[ACTIVE]');
			$('#activeStatus').css('color', '#008d03');
		} else {
			$('#activeStatus').text('[INACTIVE]');
			$('#activeStatus').css('color', '#ae0000');
		}
	}

	// Done loading, hide the loader
	$('#loader').hide();
}
