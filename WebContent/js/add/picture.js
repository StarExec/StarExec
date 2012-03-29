$(document).ready(function(){
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	$("#upForm").validate({
		rules: {
			f: {
				required: true,
				regex: "(\.jpg$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex: ".jpg only"
			}
		}
	});
	
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
});