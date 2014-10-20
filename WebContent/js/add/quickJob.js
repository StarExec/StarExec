
var dialog=null;


$(document).ready(function(){
	
	initUI();
	attachFormValidation();
	
	$('#radioNoPause').attr('checked','checked');
	populateDefaults();
	
	

});

function getMaxCpuTimeout(){
	maxtime=$( "#workerQueue option:selected" ).attr("cpumax");
	return parseInt(maxtime);
}

function getMaxWallTimeout() {
	maxtime=$( "#workerQueue option:selected" ).attr("wallmax");
	return parseInt(maxtime);
}

function getCpuTimeoutErrorMessage() {
	timeout=getMaxCpuTimeout();
	if (isNaN(timeout)) {
		return "please select a queue";
	}
	return timeout+" second max timeout";
}

function getClockTimeoutErrorMessage() {
	timeout=getMaxWallTimeout();
	if (isNaN(timeout)) {
		return "please select a queue";
	}
	return timeout+" second max timeout";
}

/**
 * Attach validation to the job creation form
 */
function attachFormValidation(){
	// Add regular expression capabilities to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	
	
	// Set up form validation
	$("#addForm").validate({
		submitHandler: function(form) {
 			createDialog("Creating your job, please wait. This will take some time for large jobs.");

			form.submit();
		},
		ignore: "", //don't ignore hidden inputs
		rules: {
			name: {
				required: true,
				minlength: 1,
				maxlength: $("#txtJobName").attr("length"),
				regex : getPrimNameRegex()
			},
			desc: {
				required: false,
				maxlength: $("#txtDesc").attr("length"),
				regex: getPrimDescRegex()
			},
			cpuTimeout: {
				required: true,			    
			    max: getMaxCpuTimeout(),
			    min: 1
			},
			wallclockTimeout: {
				required: true,			    
			    max: getMaxWallTimeout(),
			    min: 1
			},
			maxMem: {
				required: true,
				min : 0 
			},
			queue: {
				required: true
			},
			benchName: {
				required:true,
				minlength: 1,
				maxlength: $("#txtBenchName").attr("length"),
				regex : getPrimNameRegex()
			},
			bench: {
				required:true,
				minlength :1
			},
			solver: {
				required:true,
				min: 1
			},
			benchProcess: {
				required: true
			},
			postProcess: {
				required: true
				
			},
			preProcess: {
				required: true
				
			}
		},
		messages: {
			name:{
				required: "enter a job name",
				minlength: "1 character minimum",
				maxlength: $("#txtJobName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			benchName:{
				required: "enter a benchmark name",
				minlength: "1 character minimum",
				maxlength: $("#txtBenchName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			bench: {
				required: "enter a benchmark",
				minlength: "1 character minimum"
			},
			desc: {
				required: "enter a job description",
				maxlength: $("#txtDesc").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			cpuTimeout: {
				required: "enter a timeout",			    
			    max: getCpuTimeoutErrorMessage(),
			    min: "1 second minimum timeout"
			},
			solver: {
				required: "choose a solver",
				min: "choose a solver"
			},
			wallclockTimeout: {
				required: "enter a timeout",			    
			    max: getClockTimeoutErrorMessage(),
			    min: "1 second minimum timeout"
			},
			maxMem: {
				required: "enter a maximum memory",
				max: "100 gigabytes maximum" 
			},
			queue: {
				required: "error - no worker queues"
			},
			benchProcess:  {
				required: "choose a benchmark processor"
			},
			postProcess:  {
				required: "choose a postprocessor"
			},
			preProcess:  {
				required: "choose a preprocessor"
			}
		}
	});
	
	//when we change queues, we need to refresh the validation to use the new timeouts
	$("#workerQueue").change(function() {
		settings = $('#addForm').validate().settings;
		settings.rules.cpuTimeout = {
				required: true,			    
			    max: getMaxCpuTimeout(),
			    min: 1
			};
		
		settings.rules.wallclockTimeout = {
				required: true,			    
			    max: getMaxWallTimeout(),
			    min: 1
			};
		
		settings.messages.cpuTimeout = {
				required: "enter a timeout",			    
			    max: getMaxCpuTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
			};
		
		settings.messages.wallclockTimeout = {
				required: "enter a timeout",			    
			    max: getMaxWallTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
		};
		$("#addForm").valid(); //revalidate now that we have new rules

		
	});
	
	$("#useSolver").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#useSolver").click(function(e) {
		useSelectedSolver();
		e.preventDefault();
	});
};

function useSelectedSolver() {
	selection=$("#solverList").find("tr.row_selected");
	//nothing is selected
	if (selection.length==0) {
		return;
	}
	name=$(selection).find("td:first").text();
	id=$(selection).find("td:nth-child(2)").text();
	setInputToValue("#solver",id);

	$("#solver").siblings("p").children("#solverNameSpan").text(name);

}

function setInputToValue(inputSelector, value) {
	$(inputSelector).attr("value",value);
}

/**
 * Sets all of the fields that have defaults according to the currently selected default setting
 */
function populateDefaults() {
	selectedSettingId=$("#settingProfile option:selected").attr("value");
	
	profile=$(".defaultSettingsProfile[value="+selectedSettingId+"]");
	//first, pull out
	cpuTimeout=$(profile).find("span.cpuTimeout").attr("value");
	clockTimeout=$(profile).find("span.clockTimeout").attr("value");
	maxMemory=$(profile).find("span.maxMemory").attr("value");
	solverId=$(profile).find("span.solverId").attr("value");
	solverName=$(profile).find("span.solverName").attr("value");
	//benchContents=$(profile).find("span.benchContents").attr("value");
	
	preProcessorId=$(profile).find("span.preProcessorId").attr("value");
	postProcessorId=$(profile).find("span.postProcessorId").attr("value");

	benchProcessorId=$(profile).find("span.benchProcessorId").attr("value");
	setInputToValue("#cpuTimeout",cpuTimeout);
	setInputToValue("#wallclockTimeout",clockTimeout);
	setInputToValue("#maxMem",maxMemory);
	setInputToValue("#solver",solverId);
	//setInputToValue("#benchmarkField",benchContents);
	$("#solver").siblings("p").children("#solverNameSpan").text(solverName);
	
	if (stringExists(preProcessorId)) {
		//only set the pre processor if one with this ID actually exists in the dropdown
		if (($('#preProcess > [value='+preProcessorId+']').length > 0)) {
			$("#preProcess").val(preProcessorId);

		}
		
	}
	if (stringExists(postProcessorId)) {
		if (($('#postProcess > [value='+postProcessorId+']').length > 0)) {

			$("#postProcess").val(postProcessorId);
		}
	}
	if (stringExists(benchProcessorId)) {
		if (($('#benchProcess > [value='+benchProcessorId+']').length > 0)) {

			$("#benchProcess").val(benchProcessorId);
		}
	}
}

/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initUI() {
	
	//there must be some bench processor selected, so make sure we are using one
	$("#benchProcess").find("option").first().attr("selected","selected");
	
	$("#dialog-createSettingsProfile").hide();
	
	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(){
		
		history.back(-1);
	});
	
	
	$("#btnSave").button({
		icons: {
			primary: "ui-icon-disk"
		}
	}).click(function() {
		$("#dialog-createSettingsProfile").dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'create': function() {
					$(this).dialog("close");
						$.post(  
							starexecRoot+"secure/add/profile",
							{postp: $("#postProcess").val(), prep: $("#preProcess").val(), benchp: $("#benchProcess").val(),
								solver: $("#solver").val(), name: $("#settingName").val(), cpu: $("#cpuTimeout").val(),
								wall: $("#wallclockTimeout").val(), dep: "false", bench: "", mem: $("#maxMem").val()},
							function(returnCode) {
									//success
							}
						).error(function(xhr, textStatus, errorThrown){
							showMessage('error',"Invalid parameters",5000);
						});
														
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
		
	});
	$("#advancedSettings").expandable(true);
	$("#solverField").expandable(true);
    $('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
		}
    });
    
    $("#settingProfile").change(function() {
		populateDefaults();
	});

    $("#solverList").dataTable({ 
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize
	});
    
    $("#solverList").on("mousedown", "tr",function() {
		if ($(this).hasClass("row_selected")) {
			$(this).removeClass("row_selected");
		} else {
			unselectAll();
			$(this).addClass("row_selected");
		}
	});
}
function unselectAll() {
	$("#solverList").find("tr").removeClass("row_selected");
}

