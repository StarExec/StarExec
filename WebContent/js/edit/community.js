var leaderTable;

var EDIT_COMMUNITY = {
	benchmarkingFrameworkAttr: 'BENCHMARKING_FRAMEWORK'
};

$(document).ready(function() {
	var id = $('#comId').val();

	var dataTableConfig = new window.star.DataTableConfig();
	leaderTable = $('#leaders').dataTable(dataTableConfig);
	memberTable = $('#Members').dataTable(dataTableConfig);

	$.get(
		starexecRoot + "services/communities/details/" + id,
		function(data) {
			populateDetails(data);
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error getting community details", 5000);
	});

	initUI();
	attachFormValidation();
	attachWebsiteMonitor();
	attachProcessorFormListener();
});

function populateDetails(jsonData) {
	var id = $('#comId').val();

	// Populate leaders table
	$('#leaderField legend')
	.children('span:first-child')
	.text(jsonData.leaders.length);
	leaderTable.fnClearTable();
	$.each(jsonData.leaders, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="' + starexecRoot + 'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="' + starexecRoot + 'images/external.png" /></a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="' + starexecRoot + 'images/external.png" /></a>';
		var deleteUser = '<input type="button" onclick="removeUser(' + user.id + ', ' + id + ')" value="X"/>';
		var demoteUser = '<input type="button" onclick="demoteUser(' + user.id + ', ' + id + ')" value="Demote"/>';

		leaderTable.fnAddData([
			userLink,
			user.institution,
			emailLink,
			deleteUser,
			demoteUser
		]);

	});

	// Populate members table
	$('#memberField legend')
	.children('span:first-child')
	.text(jsonData.space.users.length);
	//memberTable.fnClearTable();
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId = '<input type="hidden" value="' + user.id + '" >';
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="' + starexecRoot + 'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="' + starexecRoot + 'images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="' + starexecRoot + 'images/external.png"/></a>';
		var deleteUser = '<input type="button" onclick="removeUser(' + user.id + ', ' + id + ')" value="X"/>';
		var promoteUser = '<input type="button" onclick="promoteUser(' + user.id + ', ' + id + ')" value="Promote"/>';

		memberTable.fnAddData([
			userLink,
			user.institution,
			emailLink,
			deleteUser,
			promoteUser
		]);

	});
}

function removeUser(userid, id) {
	var idArray = [];
	idArray.push(userid);
	$.post(
		starexecRoot + "services/remove/user/" + id,
		{selectedIds: idArray},
		function(returnCode) {
			parseReturnCode(returnCode);
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error removing user", 5000);
	});
	setTimeout(function() {document.location.reload(true);}, 1000);
}

function promoteUser(userid, id) {
	var idArray = [];
	idArray.push(userid);
	$.post(
		starexecRoot + "services/makeLeader/" + id,
		{selectedIds: idArray},
		function(returnCode) {
			s = parseReturnCode(returnCode);
			if (s) {
				setTimeout(function() {document.location.reload(true);}, 1000);
			}

		},
		"json"
	).error(function() {
		showMessage('error', "Internal error making user a leader", 5000);
	});

}

function demoteUser(userId, id) {
	$.post(
		starexecRoot + "services/demoteLeader/" + id + "/" + userId,
		function(returnCode) {
			s = parseReturnCode(returnCode);
			if (s) {
				setTimeout(function() {document.location.reload(true);}, 1000);
			}
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error demoting user from leader", 5000);
	});
}

/**
 * Monitors the solver's "websites" and updates the server if the client adds/deletes any
 */
function attachWebsiteMonitor() {
	// Handles deleting an existing website
	$("#websiteTable").on("click", ".delWebsite", function() {
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		$('#dialog-confirm-delete-txt')
		.text('Are you sure you want to delete this website?');

		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-confirm-delete').dialog('close');

					$.post(
						starexecRoot + "services/websites/delete/" + id,
						function(returnData) {
							s = parseReturnCode(returnData);
							if (s) {
								parent.remove();
							}
						},
						"json"
					).error(function() {
						showMessage('error',
							"Internal error updating websites",
							5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
	});

	// Handles adding a new website
	$("#addWebsite").click(function() {
		var name = $("#website_name").val();
		var url = $("#website_url").val();

		if (name.trim().length == 0) {
			showMessage('error', 'please enter a website name', 5000);
			return;
		} else if (url.indexOf("http://") != 0) {
			showMessage('error', 'url must start with http://', 5000);
			return;
		} else if (url.trim().length <= 12) {
			showMessage('error', 'the given url is not long enough', 5000);
			return;
		}

		var data = {name: name, url: url};
		$.post(
			starexecRoot + "services/website/add/space/" + $('#comId').val(),
			data,
			function(returnCode) {
				s = parseReturnCode(returnCode);
				if (s) {
					$("#website_name").val("");
					$("#website_url").val("");
					$('#websites li').remove();
					refreshSpaceWebsites();
				}
			},
			"json"
		);

	});
}

function attachProcessorFormListener() {
	$('.radioLocal').click(function() {
		// Set radio local to checked for all types of processors.
		$('.radioLocal').prop('checked', true);
		$('.radioURL').prop('checked', false);
		$('.fileURL').hide();
		$('.processorFileSpan').show();
	});
	$('.radioURL').click(function() {
		log('.radioURL clicked.');
		$('.radioURL').prop('checked', true);
		$('.radioLocal').prop('checked', false);
		$('.processorFileSpan').hide();
		$('.fileURL').show();
	});
}

function initUI() {
	populateDefaultsWithId($("#settingId").attr("value"));

	// Hide #radioURL since choose post processor by URL is not seleted by default.
	$('.fileURL').hide();

	// Make forms editable
	editable("name");
	editable("desc");
	editable("CpuTimeout");
	editable("ClockTimeout");
	editable("MaxMem");
	// Add toggles for the "add new" buttons and hide them by default
	$('#toggleWebsite').click(function() {
		$('#newWebsite').slideToggle('fast');
		togglePlusMinus(this);
	});

	$('#toggleBenchType').click(function() {
		$('#newTypeTbl').slideToggle('fast');
		togglePlusMinus(this);
	});

	$('#togglePostProcessor').click(function() {
		$('#newPostProcessTbl').slideToggle('fast');
		togglePlusMinus(this);
	});

	$('#togglePreProcessor').click(function() {
		$('#newPreProcessTbl').slideToggle('fast');
		togglePlusMinus(this);
	});

	$('#toggleUpdateProcessor').click(function() {
		$('#newUpdateProcessTbl').slideToggle('fast');
		togglePlusMinus(this);
	});

	$('#editPostProcess').change(function() {
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			'PostProcess',
			0);
	});

	$('#editBenchProcess').change(function() {
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			'BenchProcess',
			0);
	});

	$('#editPreProcess').change(function() {
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			'PreProcess',
			0);
	});

	$('#editUpdateProcess').change(function() {
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			'UpdateProcess',
			0);
	});

	$('#editDependenciesEnabled').change(function() {
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			'DependenciesEnabled',
			0);
	});

	$('#editBenchmarkingFramework').change(function() {
		'use strict';
		log('changing benchmarking framework');
		saveChanges($(this).children('option:selected').attr('value'),
			true,
			EDIT_COMMUNITY.benchmarkingFrameworkAttr,
			0);
	});

	$('.deleteBenchmark').click(function() {
		'use strict';
		var settingId = $('#settingId').attr('value');
		var benchId = $(this).attr('value');
		log('About to make delete benchmark request for settingId=' + settingId + ' and benchId=' + benchId);
		$.post(
			starexecRoot + 'services/delete/defaultBenchmark/' + settingId + '/' + benchId,
			'',
			function(returnCode) {
				'use strict';
				var success = parseReturnCode(returnCode);
				if (success) {
					location.reload(true);
				}
			},
			'json'
		);
	});

	$('#newWebsite').hide();
	$('#newTypeTbl').hide();
	$('#newPostProcessTbl').hide();
	$('#newPreProcessTbl').hide();
	$('#newUpdateProcessTbl').hide();

	$('#leaderField').expandable(false);
	$('#memberField').expandable(false);
	$('#websiteField').expandable(true);
	$('#benchmarkField').expandable(true);
	$('#settingsField').expandable(true);
	$('#postProcessorField').expandable(true);
	$('#updateProcessorField').expandable(true);
	$("#preProcessorField").expandable(true);

	$('#addType').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});

	$('#addPostProcessor').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});

	$('#addPreProcessor').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});

	$('#addUpdateProcessor').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});

	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
		}
	});
}

/**
 * Attaches form validation to the pre/post processors and the benchmark type form
 */
function attachFormValidation() {
	// Re-validate the 'post-processor' and benchmark 'processor type' fields when they loses focus
	$("#typeFile").change(function() {
		$("#typeFile").blur().focus();
	});

	$(".processorFile").change(function() {
		$(".processorFile").blur().focus();
	});

	// Adds regular expression handling to JQuery validator
	addValidators();

	var formsToValidate = [
		'#addPostProcessorForm',
		'#addPreProcessorForm',
		'#newTypeForm',
		'#addUpdateProcessorForm'
	];

	$.each(formsToValidate, function(i, selector) {
		$(selector).validate({
			rules: {
				name: {
					required: true,
					maxlength: $("#procName").attr("length"),
					jspregex: "DUMMY REGEX"
				},
				desc: {
					maxlength: $("#procDesc").attr("length"),
					regex: getPrimDescRegex()
				},
				file: {
					required: true
				}
			},
			messages: {
				name: {
					required: "enter a processor name",
					maxlength: $("#procName")
					.attr("length") + " characters maximum",
					jspregex: "invalid character(s)"
				},
				desc: {
					required: "enter a processor description",
					maxlength: $("#procDesc")
					.attr("length") + " characters maximum",
					regex: "invalid character(s)"
				},
				file: {
					required: "choose a file"
				}
			}
		});
	});
}

/**
 * Refreshes the space's websites by clearing them from the DOM, querying for them, and
 * then re-populating the DOM with the new data
 */
function refreshSpaceWebsites() {
	location.reload();
}

/**
 * Processes website data by adding a delete button to the HTML and inject that into the DOM

 function processWebsiteData(jsonData) {
	// Injects the clickable delete button that's always present
	$('#websiteTable tr').remove();
	$.each(jsonData, function(i, site) {
		$('#websiteTable').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src=starexecRoot+"images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
}*/

function editable(attribute) {
	$('#edit' + attribute).click(function() {
		var old = $(this).html();

		if (attribute == "desc") {
			$(this)
			.after('<td><textarea>' + old + '</textarea>&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>')
			.remove();
		} else if (attribute == "name") {
			$(this)
			.after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>')
			.remove();
		} else if (attribute == "CpuTimeout") {
			$(this)
			.after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>')
			.remove();
		} else if (attribute == "ClockTimeout") {
			$(this)
			.after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>')
			.remove();
		} else if (attribute == "MaxMem") {
			$(this)
			.after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>')
			.remove();
		}

		$('#save' + attribute)
		.click(function() {saveChanges(this, true, attribute, old);});
		$('#cancel' + attribute)
		.click(function() {saveChanges(this, false, attribute, old);});

		$('#save' + attribute).button({
			icons: {
				secondary: "ui-icon-check"
			}
		});

		$('#cancel' + attribute).button({
			icons: {
				secondary: "ui-icon-close"
			}
		});
	});

	$('#edit' + attribute).css('cursor', 'pointer');
}

function saveChanges(obj, save, attr, old) {
	if (true == save) {
		var newVal;
		//since the description is in a textarea, we need to case switch on it to pull
		//from the correct object
		if (attr == 'desc') {
			newVal = $(obj).siblings('textarea:first').val();

			if (newVal.length > $('#descRow').attr('length')) {
				showMessage('error', $('#descRow')
				.attr('length') + " characters maximum", 5000);
				return;
			}
		} else if (attr == 'name') {
			newVal = $(obj).siblings('input:first').val();
			if (newVal.length > $('#nameRow').attr('length')) {
				showMessage('error', $('#nameRow')
				.attr('length') + " characters maximum", 5000);
				return;
			}
		} else if (attr == "PostProcess" || attr == "PreProcess" || attr == "BenchProcess" || attr == "UpdateProcess" || attr === EDIT_COMMUNITY.benchmarkingFrameworkAttr) {
			newVal = obj;
		} else if (attr == "CpuTimeout") {
			newVal = $(obj).siblings('input:first').val();
		} else if (attr == "ClockTimeout") {
			newVal = $(obj).siblings('input:first').val();
		} else if (attr == "DependenciesEnabled") {
			newVal = obj;
		} else if (attr = "MaxMem") {
			newVal = $(obj).siblings('input:first').val();
		}

		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;

		//these attributes are of the community itself
		if (attr == "name" || attr == "desc") {
			$.post(
				starexecRoot + "services/edit/space/" + attr + "/" + getParameterByName(
				"cid"),
				{val: newVal},
				function(returnCode) {
					s = parseReturnCode(returnCode);
					if (s) {
						// Hide the input box and replace it with the table cell
						$(obj)
						.parent()
						.after('<td id="edit' + attr + '">' + newVal + '</td>')
						.remove();
						// Make the value editable again
						editable(attr);
					} else {
						$(obj)
						.parent()
						.after('<td id="edit' + attr + '">' + old + '</td>')
						.remove();
						// Make the value editable again
						editable(attr);
					}
				},
				"json"
			).error(function() {
				showMessage('error', "Internal error updating field", 5000);
			});

			//every other attribute is of the DefaultSettings profile the community has
		} else {
			log('making post to changing default settings for community');
			$.post(
				starexecRoot + "services/edit/defaultSettings/" + attr + "/" + $(
				"#settingId").attr("value"),
				{val: newVal},
				function(returnCode) {
					s = parseReturnCode(returnCode);
					if (s) {
						// Hide the input box and replace it with the table cell
						$(obj)
						.parent()
						.after('<td id="edit' + attr + '">' + newVal + '</td>')
						.remove();
						// Make the value editable again
						editable(attr);
					} else {
						$(obj)
						.parent()
						.after('<td id="edit' + attr + '">' + old + '</td>')
						.remove();
						// Make the value editable again
						editable(attr);
					}
				},
				"json"
			).error(function() {
				showMessage('error', "Internal error updating field", 5000);
			});
		}

	} else {
		// Hide the input box and replace it with the table cell
		$(obj)
		.parent()
		.after('<td id="edit' + attr + '">' + old + '</td>')
		.remove();
		// Make the value editable again
		editable(attr);
	}
}

/**
 * Toggles the plus-minus state of the "+ add new" website button
 */
function togglePlusMinus(addSiteButton) {
	if ($(addSiteButton).text()[0] == "+") {
		$(addSiteButton).text("- add new");
	} else {
		$(addSiteButton).text("+ add new");
	}
}
