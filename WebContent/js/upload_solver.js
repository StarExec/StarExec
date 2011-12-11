$(document).ready(function(){
	$('tr:even').addClass('shade');
	
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
				regex: "(\.zip$)|(\.tar(\.gz)?$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex: ".zip, .tar and .tar.gz only"
			}
		}
	});
	
});