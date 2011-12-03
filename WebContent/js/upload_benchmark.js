// When the document is ready to be executed on
$(document).ready(function(){
	$('#tblUploadBench tbody').children('tr:even').addClass('shade');	
	
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
				regex: "(\.zip$)|(\.tar(\.gz)?$)"
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
});