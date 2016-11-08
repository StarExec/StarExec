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
	var profile=$(".defaultSettingsProfile[value="+selectedSettingId+"]");
	//first, pull out
	var cpuTimeout=$(profile).find("span.cpuTimeout").attr("value");
	var clockTimeout=$(profile).find("span.clockTimeout").attr("value");
	var maxMemory=$(profile).find("span.maxMemory").attr("value");
	solverId=$(profile).find("span.solverId").attr("value");
	var solverName=$(profile).find("span.solverName").attr("value");
	var benchName=$(profile).find("span.benchName").attr("value");
	var preProcessorId=$(profile).find("span.preProcessorId").attr("value");
	var postProcessorId=$(profile).find("span.postProcessorId").attr("value");
	var deps = $(profile).find("span.dependency").attr("value");
	var benchProcessorId=$(profile).find("span.benchProcessorId").attr("value");
	setInputToValue("#cpuTimeout",cpuTimeout);
	setInputToValue("#wallclockTimeout",clockTimeout);
	setInputToValue("#maxMem",maxMemory);
	
	setInputToValue("#solver",solverId);

	$(profile).find('.benchId').each(function(i, ele) {
		var benchmarkName = $(ele)
		appendBenchmark()
	});

	var benchId=$(profile).find("span.benchId").attr("value");
	//setInputToValue("#benchmark",benchId);
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
	//setInputToValue("#benchmark","-1");
	//$("#benchmarkNameField").text("None");
}

function useSelectedBenchmark() {
	var selection=$("#benchmarkList").find("tr.row_selected");
	//nothing is selected
	if (selection.length==0) {
		return;
	}
	var name=$(selection).find("td:first").text();
	var input=selection.find("input");
	var id=input.attr("value");

	appendBenchmark(id, name);


}

function appendBenchmark(id, name) {
	$('.selectedDefaultBenchmarks').append(
		'<td class="benchmark"><p class="benchmarkNameField"></p><span class="selectPrim clearBenchmark">clear benchmark</span></td>'
	);

	// Get the benchmark row we just appended.
	var newBenchRow = $('.selectedDefaultBenchmarks').last();

	// Set an id and name on it.
	$(newBenchRow).val(id);
	$(newBenchRow).find('.benchmarkNameField').text(name);
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