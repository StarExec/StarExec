// When the document is ready to be executed on
$(document).ready(function(){
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
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
		}
	});
	
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
    }});
});