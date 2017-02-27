// common/defaultSettings global object
var COM_DS = {
	setBenchmarkingFramework: function(benchmarkingFramework) {
		'use strict';
		log('setting benchmarking framework...');
		log('new benchmarking framework is '+benchmarkingFramework);
		$('#editBenchmarkingFramework').val(benchmarkingFramework);
		/*
		var runsolverOptionSelector = '.runsolverOption';
		var benchexecOptionSelector = '.benchexecOption';
		if ( benchmarkingFramework === 'BENCHEXEC' ) {
			$(runsolverOptionSelector).removeAttr('selected');	
			$(benchexecOptionSelector).attr('selected', 'selected')
		} else {
			$(benchexecOptionSelector).removeAttr('selected');	
			$(runsolverOptionSelector).attr('selected', 'selected')
		}*/
	}
}

$(document).ready(function() {
	$("#settingProfile").change(function() {
		
		populateDefaults();
		
	});
	
	$(".clearSolver").click(function() {
		clearSelectedSolver();
	});
	// Set up event handling on the dynamic clear benchmark spans.
	$("#settings").on('click', '.clearBenchmark', function() {
		log('Clear benchmark clicked');
		$(this).closest('tr.defaultBenchmarkRow').remove();
	});
	//sets the default profile if one exists
	if (("#defaultProfile").length>0) {
		var defaultValue=$("#defaultProfile").attr("value");
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
	var benchmarkingFramework = $(profile).find("span.benchmarkingFramework").attr("value");

	COM_DS.setBenchmarkingFramework( benchmarkingFramework );



	setInputToValue("#cpuTimeout",cpuTimeout);
	setInputToValue("#wallclockTimeout",clockTimeout);
	setInputToValue("#maxMem",maxMemory);
	
	setInputToValue("#solver",solverId);


	$('.defaultBenchmarkRow').remove();

	$(profile).find('.defaultBenchmark').each(function() {
		var benchmarkId = $(this).find('.benchId').attr('value');
		var benchmarkName = $(this).find('.benchName').attr('value');
		appendBenchmark(benchmarkId, benchmarkName);
	});

	var benchId=$(profile).find("span.benchId").attr("value");
	//setInputToValue("#benchmark",benchId);
	//setInputToValue("#benchmarkField",benchContents);


	$("#solverNameField").text(solverName);
	
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
	var selectedSettingId=getSelectedSettingId();
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
	$('#settings tbody').append(
		'<tr class="defaultBenchmarkRow">'+
			'<td>default benchmark</td>'+
			'<td class="benchmark" value="'+id+'">'+
				'<p>'+name+'</p>'+
				'<span class="selectPrim clearBenchmark">clear benchmark</span>'+
			'</td>'+
		'</tr>'
	);
}

function useSelectedSolver() {
	var selection=$("#solverList").find("tr.row_selected");
	//nothing is selected
	if (selection.length==0) {
		return;
	}
	var name=$(selection).find("td:first").text();
	var input=selection.find("input");
	var id=input.attr("value");
	setInputToValue("#solver",id);
	$("#solverNameField").text(name);

}
