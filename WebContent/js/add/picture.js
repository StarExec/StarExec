$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initializes user-interface 
 */
function initUI(){
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
}

/**
 * Attaches form validation to the picture upload field
 */
function attachFormValidation(){
	
	// Add regular expressions to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});

	// Re-validate the 'picture location' field when it loses focus
	$("#uploadPic").change(function(){
		 $("#uploadPic").blur().focus(); 
    });
	
	// Attach a tooltip to 'uploadPic' to display validation errors to the user
	$('#uploadPic').qtip(getErrorTooltip());
	
	// Form validation rules/messages
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
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			$('#' + $(label).attr('for')).qtip('api').hide();
		}
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
				type: 'grow'
			}
		},
		hide: {
			when: false,	// Don't tie the hiding of this to any event
			effect: 'fade'
		},
		style: {
			tip: 'leftMiddle',
			name: 'errorTooltip'
		},
		api:{
			onContentUpdate: function(){
				// Hide the tooltip initially by not showing the tooltip when it's empty
				if($(this.elements.content).text().length > 1){
					this.show();
					// Fixes the bug where sometimes opacity is < 1
					$('div[qtip="'+this.id+'"]').css('opacity',1);
				}
			}
		}
	};
}