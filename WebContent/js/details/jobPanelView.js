var jobSpaceId; //stores the ID of the job space that is currently selected from the space viewer
var jobId; //the ID of the job being viewed
var panelArray = null;
var useWallclock = true;
var includeUnknown = false;
var stageNumber;
$(document).ready(function() {
	jobId = $("#jobId").attr("value");
	jobSpaceId = $("#spaceId").attr("value");
	stageNumber = $("#stageNumber").attr("value");

	$('#selectStageButton').click(function() {
		var stageToRedirectTo = $('#selectStageInput').val();
		if (isInt(stageToRedirectTo)) {
			window.location.replace(starexecRoot + 'secure/details/jobPanelView.jsp?jobid=' + jobId + '&spaceid=' + jobSpaceId + '&stage=' + stageToRedirectTo);
		} else {
			$('#selectStageError').show();
		}
	});

	//update the tables every 5 seconds
	setInterval(function() {
		refreshPanels();
	}, 5000);
	initUI();
	initializePanels();
});

function isInt(value) {
	var intRegex = /^[1-9]{1}[0-9]*$/;

	return intRegex.test(value);
}

function refreshPanels() {
	for (i = 0; i < panelArray.length; i++) {
		panelArray[i].api().ajax.reload(null, false);
	}
}

function initUI() {
	$("#collapsePanels").button({
		icons: {
			primary: "ui-icon-folder-collapsed"
		}
	});
	$("#openPanels").button({
		icons: {
			primary: "ui-icon-folder-open"
		}
	});
	$(".changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}

	});
	$("#includeUnknown").button({
		icons: {
			primary: "ui-icon-refresh"
		}

	})
	$("#includeUnknown").click(
		function () {
			includeUnknown = !includeUnknown;
			setUnknownButtonText();
		}
	);
	$(".changeTime").click(function() {
		useWallclock = !useWallclock;
		setTimeButtonText();
		refreshPanels();
	});
	$("#pageHeader").hide();
	$("#pageFooter").hide();
	$("#collapsePanels").click(function() {
		$(".panelField").each(function() {
			legend = $(this).children('legend:first');
			isOpen = $(legend).data('open');
			if (isOpen) {
				$(legend).trigger("click");
			}
		});
	});
	$("#openPanels").click(function() {
		$(".panelField").each(function() {
			legend = $(this).children('legend:first');
			isOpen = $(legend).data('open');

			if (!isOpen) {
				$(legend).trigger("click");
			}
		});
	});
}

function getPanelTable(space) {
	spaceName = space.attr("name");
	spaceId = parseInt(space.attr("id"));

	table = "<fieldset class=\"panelField\">" +
		"<legend class=\"panelHeader\">" + spaceName + "</legend>" +
		"<table id=panel" + spaceId + " spaceId=\"" + spaceId + "\" class=\"panel\"><thead>" +
		"<tr class=\"viewSubspace\"><th colspan=\"4\" >Go To Subspace</th></tr>" +
		"<tr><th class=\"solverHead\">solver</th><th class=\"configHead\">config</th> " +
		"<th class=\"solvedHead\">solved</th> <th class=\"timeHead\">time</th> </tr>" +
		"</thead>" +
		"<tbody></tbody> </table></fieldset>";
	return table;

}

function initializePanels() {
	var SOLVER_ID = 0,
		CONFIG_ID = 1,
		SOLVER_NAME = 2,
		CONFIG_NAME = 3,
		SOLVED = 4,
		TIME = 5,
		STAGE = 6,
		WRONG = 7,
		RESOURCED = 8,
		FAILED = 9,
		UNKNOWN = 10,
		INCOMPLETE = 11,
		CONFLICTS = 12;

	var linkTemplate = document.createElement("a");
	linkTemplate.target = "_blank";

	var link = function(url, text) {
		linkTemplate.href = url;
		linkTemplate.textContent = text;
		return linkTemplate.outerHTML;
	};

	/*	var pairsTemplate = ["pairsInSpace.jsp?type=",null,"&sid=",DETAILS_JOB.rootJobSpaceId,"&configid=",null,"&stagenum=",null];
		var getPairsInSpaceLink = function(type, configId, stageNumber) {
			pairsTemplate[1] = type;
			pairsTemplate[5] = configId;
			pairsTemplate[7] = stageNumber;
			return pairsTemplate.join("");
		}
	*/

	var solverTemplate = ["solver.jsp?id=", null];
	var getSolverLink = function(solver) {
		solverTemplate[1] = solver;
		return solverTemplate.join("");
	};

	var configTemplate = ["configuration.jsp?id=", null];
	var getConfigLink = function(config) {
		configTemplate[1] = config;
		return configTemplate.join("");
	};

	var formatSolver = function(row, type, val) {
		var href = getSolverLink(val[SOLVER_ID]);
		return link(href, val[SOLVER_NAME]);
	};
	var formatConfig = function(row, type, val) {
		var href = getConfigLink(val[CONFIG_ID]);
		return link(href, val[CONFIG_NAME]);
	};
	var formatSolved = function(row, type, val) {
		return val[SOLVED];
//		var href = getPairsInSpaceLink("solved", val[CONFIG_ID], val[STAGE]);
		//		return link(href, val[SOLVED]);
	};
	var formatTime = function(row, type, val) {
		return (val[TIME] / 100).toFixed(1);
	};

	var panelTableInitializer = new window.star.DataTableConfig({
		"sDom": 'rt<"clear">',
		"iDisplayLength": 1000, // make sure we show every entry
		"fnServerData": fnShortStatsPaginationHandler,
		"aoColumns": [
			{"mRender": formatSolver},
			{"mRender": formatConfig},
			{"mRender": formatSolved},
			{"mRender": formatTime},
		]
	});

	$.getJSON(starexecRoot + "services/space/" + jobId + "/jobspaces/false?id=" + jobSpaceId,
		function(spaces) {
			panelArray = [];
			for (var i = 0; i < spaces.length; i++) {
				var space = $(spaces[i]);
				var spaceId = parseInt(space.attr("id"));
				var child = getPanelTable(space);

				$("#subspaceSummaryField").append(child);
				panelTableInitializer["sAjaxSource"] = starexecRoot + "services/jobs/solvers/pagination/" + spaceId + "/true/";
				panelArray[i] = $("#panel" + spaceId)
				.dataTable(panelTableInitializer);
			}
			$(".viewSubspace").each(function() {
				$(this).click(function() {
					var jobSpaceId = $(this)
					.parents("table.panel")
					.attr("spaceId");
					var stageToRedirectTo = $('#selectStageInput').val();
					if (isInt(stageToRedirectTo)) {
						window.location.replace(starexecRoot + 'secure/details/jobPanelView.jsp?jobid=' + jobId + '&spaceid=' + jobSpaceId + '&stage=' + stageToRedirectTo);
					} else {
						$('#selectStageError').show();
					}
				});

			});
			$(".panelField").expandable();
		});

}

function fnShortStatsPaginationHandler(sSource, aoData, fnCallback) {
	$.post(
		sSource + useWallclock + '/' + stageNumber + '/' + includeUnknown,
		aoData,
		function(nextDataTablePage) {
			//if the user has clicked on a different space since this was called, we want those results, not these
			s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error populating data table", 5000);
	});
}
