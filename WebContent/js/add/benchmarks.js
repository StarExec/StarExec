$(document).ready(function(){
	initUI();
	attachFormValidation();
	DepEnb=$("#selectDep").attr("default");
	if (DepEnb=="1") {
		$('#depSpaces').show();
		$('#depLinked').show();
		$("#radioDependency").attr("checked", "checked");
	}else{
		
		  $('#depSpaces').hide();
		  $('#depLinked').hide();
		  $("#radioNoDependency").attr("checked", "checked");
	}
	//hide message until upload clicked
	$('#messageField').hide();
});


/**
 * Attach validation to the benchmark upload form
 */
function attachFormValidation(){
	
	// Add 'regex' method to JQuery validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Re-validate the 'file location' field when it loses focus
	$("#benchFile").change(function(){
		 $("#benchFile").blur().focus(); 
    });
	
	// Attach a tooltip to 'benchFile' to display validation errors to the client
	$('#benchFile').qtip(getErrorTooltip());
	
	//initially hide dependency related fields
	  $('#depSpaces').fadeOut(0);
	  $('#depLinked').fadeOut(0);
	
	// Form validation rules/messages
	$("#uploadForm").validate({
		rules: {
			benchFile: {
				required: true,
				regex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			}
		},
		messages: {
			benchFile:{
				required: "please select a file",
				regex: ".zip, .tar and .tar.gz only"
			}
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			if($(error).text().length > 0){
				$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
			}
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			$('#' + $(label).attr('for')).qtip('api').hide();
		}
	});
}


/**
 * Initializes user-interface
 */
function initUI(){
	$('#radioConvert').change(function() {
		  if($('#radioConvert').is(':checked')) {
			  $('#permRow').fadeIn('fast');
		  }
	});
	
	$('#radioDump').change(function() {
		  if($('#radioDump').is(':checked')) {
			  $('#permRow').fadeOut('fast');
		  }
	});
	
	$('#radioDependency').change(function() {
		  if($('#radioDependency').is(':checked')) {
			  $('#depSpaces').fadeIn('fast');
			  $('#depLinked').fadeIn('fast');
		  }
	});
	
	$('#radioNoDependency').change(function() {
		  if($('#radioNoDependency').is(':checked')) {
			  $('#depSpaces').fadeOut('fast');
			  $('#depLinked').fadeOut('fast');
		  }
	});
	
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});
	
	$("#btnUpload").click(function(){		
		$('#messageField').show();
	});
	
}

/**
 * Returns the tooltip configuration used to display error messages to the client
 */
function getErrorTooltip(){
	// Sets up the tooltip look & feel
	$.fn.qtip.styles.errorTooltip = {
			background: '#E1E1E1',
			'padding-left': 15,
			'padding-right': 8,
			'padding-top': 8,
			'padding-bottom': 8,
			color : '#ae0000'
	};
	
	// Return the tooltip configuration using the above style
	return {
		position: {
			corner:{
				target: 'rightMiddle',
				tooltip: 'leftMiddle'
			}
		},
		show: {
			when: false,	// Don't tie the showing of this to any event
			ready: false,	// Don't display tooltip once it has been initialized
			effect: {
				type: 'fade',
				length: 200
			}
		},
		hide: {
			when: false,	// Don't tie the hiding of this to any event
			effect: {
				type: 'fade',
				length: 200
			}
		},
		style: {
			tip: 'leftMiddle',
			name: 'errorTooltip'
		},
		api:{
			onContentUpdate: function(){
				// Fixes the bug where sometimes opacity is < 1
				$('div[qtip="'+this.id+'"]').css('opacity',1);
			}
		}
	};
}