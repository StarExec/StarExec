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
				regex: "(\.zip$)|(\.tar(\.gz)?$)"
			},
			sn: {
				required: true
			},
			desc: {
				required: true
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex: ".zip, .tar and .tar.gz only"
			},
			sn: {
				required: "input solver name"
			},
			desc: {
				required: "input description"
			}
		}
	});
	
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
});