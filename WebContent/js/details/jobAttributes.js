var rootJobSpaceId;
var spaceExplorerJsonData;
var jobId;
var attributeDataTable;
var openAjaxRequests = [];
var jobSpaceId;
var attributeTotalsTable;

$(document).ready(function(){
    jobSpaceId=getParameterByName('id');
    rootJobSpaceId=getParameterByName('id');
    jsTree=makeSpaceTree("#exploreList");
    // Initialize the jstree plugin for the explorer list
    jobId = $('#data').data('jobid');
    spaceExplorerJsonData = getSpaceExplorerJsonData();
    initSpaceExplorer();
    //initDataTables();
	setupChangeTimeButton();
});

function getSpaceExplorerJsonData() {
    'use strict';
    var spaceExplorerJsonData = {};
    var url = starexecRoot+"services/space/" +jobId+ "/jobspaces/true";
    spaceExplorerJsonData = {
        "ajax" : {
            "url" : url, // Where we will be getting json data from
            "data" : function (n) {
                return {
                    id : (n.attr ? n.attr("id") : 0)
                }; // What the default space id should be
            }
        }
    };
    return spaceExplorerJsonData;
}
function setupChangeTimeButton() {
	$(".changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	
	});

	var isWallclock = true;
	$('.cpuSum').hide();


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


	$(".changeTime").click(toggleTime);
		
}


function initSpaceExplorer() {
    // Set the path to the css theme for the jstree plugin

    $.jstree._themes = starexecRoot+"css/jstree/";
    var id;




    // Initialize the jstree plugin for the explorer list
    /*$("#exploreList").bind("loaded.jstree", function() {
        log("exploreList tree has finished loading.");
        $("#exploreList").jstree("select_node", ".rootNode");
    })*/
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
            killAjaxRequests();
            // When a node is clicked, get its ID and display the info in the details pane
            id = data.rslt.obj.attr("id");
			console.log('Changing jobspace to '+id);
            jobSpaceId = id;
            name = data.rslt.obj.attr("name");
            console.log(data.rslt.obj);
            maxStages = data.rslt.obj.attr("maxStages");
            setMaxStagesDropdown(parseInt(maxStages));
            $('#spaceId').text("id: " + id);
            reloadTables(id);
    }).on( "click", "a", function (event, data) {
        event.preventDefault();  // This just disable's links in the node title
    });
    log("Initialized exploreList tree.");

}

function reloadTables(id) {
	if (attributeDataTable != null) {
		attributeDataTable.fnDestroy();
	}
	if (attributeTotalsTable != null) {
		attributeTotalsTable.fnDestroy();
	}
	console.log('Reloading tables using jobspaceId='+id);
	
	// Recreate the headers since these can be different depending on the jobspace.
    $('#attributeTable').remove();
    $('legend').after('<table id="attributeTable"></table>');
    $('#attributeTable').append('<thead></thead>');
    $('#attributeTable thead').append('<tr></tr>');
    $('#attributeTable tr').append('<th>solver</th>');
    $('#attributeTable tr').append('<th>config</th>');
    $.post(starexecRoot+"services/jobs/attributes/header/"+jobSpaceId,
            {},
            populateTableHeaders,
            "json");
}

function populateTableHeaders(headers) {
    console.log("headers");
    console.log(headers);
    for(var h in headers) {
		console.log(h);
        $('#attributeTable tr').append('<th>' + headers[h] + '</th>');
    }
    initDataTables();
}

function killAjaxRequests() {
    for (var i = 0; i < openAjaxRequests.length; i++) {
        openAjaxRequests[i].abort();
    }
    openAjaxRequests = []
}
function totalsPaginationHandler(sSource, aoData, fnCallback) {
    $.post(
        sSource + "jobs/attributes/totals/"+jobSpaceId,
        aoData,
        function(nextDataTablePage){
            var s=parseReturnCode(nextDataTablePage);
            if (s) {
                fnCallback(nextDataTablePage);
            }
        },
        "json"
    )
}


function initDataTables() {
    attributeDataTable = $('#attributeTable').dataTable( {
        "sDom"          :getDataTablesDom(),
        "iDisplayStart" : 0,
        "iDisplayLength" : defaultPageSize,
        "bServerSide"       : false,
        "sAjaxSource"       : starexecRoot+"services/",
        "sServerMethod"     : 'POST',
        "fnServerData"      : fnPaginationHandler
    });

    attributeTotalsTable = $('#attributeTotalsTable').dataTable({
        "sDom"          :getDataTablesDom(),
        "iDisplayStart" : 0,
        "iDisplayLength" : defaultPageSize,
        "bServerSide"       : false,
        "sAjaxSource"       : starexecRoot+"services/",
        "sServerMethod"     : 'POST',
        "fnServerData"      : totalsPaginationHandler
    });

}



function fnPaginationHandler(sSource, aoData, fnCallback) {
    $.post(
            sSource + "jobs/attributes/"+jobSpaceId,
            aoData,
            function(nextDataTablePage){
                var s=parseReturnCode(nextDataTablePage);
                if (s) {
                    fnCallback(nextDataTablePage);
                }
            },
            "json"
            )
}
