$(document).ready(function() {
	$("#settingProfile").change(function() {
		
		populateDefaults();
		
	});
	
	$(".clearSolver").click(function() {
		clearSelectedSolver();
	});
	$(".clearBenchmark").click(function() {
		clearSelectedBenchmark();
	});
	//sets the default profile if one exists
	if (("#defaultProfile").length>0) {
		defaultValue=$("#defaultProfile").attr("value");
		if (parseInt(defaultValue)>0) {
			if ($('#settingProfile > [value='+defaultValue+']').length > 0) {
				$("#settingProfile").val(defaultValue);
			}
		}
		
	} 
	populateDefaults();	

	
});

function getSelectedSettingType() {
	
	return $("#settingProfile option:selected").attr("type");

}

function getSelectedSettingId() {
	return $("#settingProfile option:selected").attr("value");
	
}

function populateDefaultsWithId(selectedSettingId) {
	
	if ($(".defaultSettingsProfile[value="+selectedSettingId+"]").length<=0) {
		return; //couldn't find the profile, so nothing to populate
	}
	profile=$(".defaultSettingsProfile[value="+selectedSettingId+"]");
	//first, pull out
	cpuTimeout=$(profile).find("span.cpuTimeout").attr("value");
	clockTimeout=$(profile).find("span.clockTimeout").attr("value");
	maxMemory=$(profile).find("span.maxMemory").attr("value");
	solverId=$(profile).find("span.solverId").attr("value");
	solverName=$(profile).find("span.solverName").attr("value");
	benchName=$(profile).find("span.benchName").attr("value");
	benchId=$(profile).find("span.benchId").attr("value");
	preProcessorId=$(profile).find("span.preProcessorId").attr("value");
	postProcessorId=$(profile).find("span.postProcessorId").attr("value");
	deps = $(profile).find("span.dependency").attr("value");
	benchProcessorId=$(profile).find("span.benchProcessorId").attr("value");
	setInputToValue("#cpuTimeout",cpuTimeout);
	setInputToValue("#wallclockTimeout",clockTimeout);
	setInputToValue("#maxMem",maxMemory);
	
	setInputToValue("#solver",solverId);
	setInputToValue("#benchmark",benchId);
	
	//setInputToValue("#benchmarkField",benchContents);
	$("#solverNameField").text(solverName);
	$("#benchmarkNameField").text(benchName);
	
	$(".dependencySetting").val(deps);
	if (stringExists(preProcessorId)) {
		//only set the pre processor if one with this ID actually exists in the dropdown
		if (($('.preProcessSetting > [value='+preProcessorId+']').length > 0)) {
			$(".preProcessSetting").val(preProcessorId);
		}
	}
	
	if (stringExists(postProcessorId)) {
		if (($('.postProcessSetting > [value='+postProcessorId+']').length > 0)) {
			$(".postProcessSetting").val(postProcessorId);
		}
	}
	if (stringExists(benchProcessorId)) {
		if (($('.benchProcessSetting > [value='+benchProcessorId+']').length > 0)) {

			$(".benchProcessSetting").val(benchProcessorId);
		}
	}
	
	
}

/**
 * Sets all of the fields that have defaults according to the currently selected default setting
 */
function populateDefaults() {
	selectedSettingId=getSelectedSettingId();
	if (!stringExists(selectedSettingId)) {
		return; //no setting selected.
	}
	
	if (getSelectedSettingType()=="COMMUNITY") {
	    $("#saveProfile").hide();
	    $("#deleteProfile").hide();

	} else {
		 $("#saveProfile").show();
		 $("#deleteProfile").show();
	}
	
	populateDefaultsWithId(selectedSettingId);

	
}

function clearSelectedSolver() {
	setInputToValue("#solver","-1");
	$("#solverNameField").text("None");
}

function clearSelectedBenchmark() {
	setInputToValue("#benchmark","-1");
	$("#benchmarkNameField").text("None");
}

function useSelectedBenchmark() {
	selection=$("#benchmarkList").find("tr.row_selected");
	//nothing is selected
	if (selection.length==0) {
		return;
	}
	name=$(selection).find("td:first").text();
	input=selection.find("input");
	id=input.attr("value");
	setInputToValue("#benchmark",id);

	$("#benchmarkNameField").text(name);

}

function useSelectedSolver() {
	selection=$("#solverList").find("tr.row_selected");
	//nothing is selected
	if (selection.length==0) {
		return;
	}
	name=$(selection).find("td:first").text();
	input=selection.find("input");
	id=input.attr("value");
	setInputToValue("#solver",id);
	$("#solverNameField").text(name);

}