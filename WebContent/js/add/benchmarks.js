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
	
	$("#radioLocal").attr("checked", "checked");
	$("#benchFile").show();
	$("#fileURL").hide();
	
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
	
	$("#radioLocal").change(function() {
		if ($("#radioLocal").is(":checked")) {
			$("#fileURL").stop(true,true,true);
			$("#fileURL").fadeOut('fast', function() {
				if ($("#radioLocal").is(":checked")) {
					$("#benchFile").fadeIn('fast');
					$("#uploadForm").validate().element("#fileURL");
				}
				
			});
			
			
		}
	});
	$("#radioURL").change(function() {
		if ($("#radioURL").is(":checked")) {
			$("#benchFile").stop(true,true,true);
			$("#benchFile").fadeOut('fast', function() {
				if ($("#radioURL").is(":checked")) {
					$("#fileURL").fadeIn('fast');
					$("#uploadForm").validate().element("#benchFile");
				}
				
			});
			
		}
	});
	
	// Re-validate the 'file location' field when it loses focus
	$("#benchFile").change(function(){
		 $("#benchFile").blur().focus(); 
    });
	

	
	//initially hide dependency related fields
	  $('#depSpaces').fadeOut(0);
	  $('#depLinked').fadeOut(0);
	
	// Form validation rules/messages
	$("#uploadForm").validate({
		rules: {
			benchFile: {
				required: "#radioLocal:checked",
				regex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			url : {
				required: "#radioURL:checked",
				regex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			}
		},
		messages: {
			benchFile:{
				required: "please select a file",
				regex: ".zip, .tar and .tar.gz only"
			},
			url : {
				required: "please enter a URL",
				regex: "URL must be .zip, .tar, or .tar.gz"	
			}
		},
		submitHandler: function(form) {
			createDialog("Uploading benchmarks to server, please wait. This will take some time for large archives.");
			form.submit();
			
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
	
	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(e){
		e.preventDefault();
		history.back(-1);
	});
	
}
