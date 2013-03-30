/*
 * jQuery validate.password plug-in 1.0
 *
 * http://bassistance.de/jquery-plugins/jquery-plugin-validate.password/
 *
 * Copyright (c) 2009 Jörn Zaefferer
 *
 * $Id$
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 */

/*
 * Modified by Todd Elvers
 */
(function($) {
	
	// Regexes used to calculate password strength
	var LOWER = /[a-z]/,
		UPPER = /[A-Z]/,
		DIGIT = /[0-9]/,
		PUNCT = /[~!@#$%\^&*()\-_=+]/,
		SPECIAL = /[^a-zA-Z0-9~!@#$%\^&*()\-_=+]/;
		
	// Reference to the password strength meter in the DOM
	var passwordStrengthMeter = null;

	/**
	 * Sets the passwordStrengthMeter's pointer to the password strength
	 * meter DOM element and hides it
	 */
	$.validator.passwordStrengthMeter = function(domElement){
		passwordStrengthMeter = domElement;
		$(passwordStrengthMeter).hide();
	};
	
	function rating(rate, message) {
		return {
			rate: rate,
			messageKey: message
		};
	}
	
	function uncapitalize(str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
	
	/**
	 * Returns a password strength value and message
	 */
	$.validator.passwordRating = function(password) {
		var lower = LOWER.test(password),
			upper = UPPER.test(uncapitalize(password)),
			digit = DIGIT.test(password),
			punct = PUNCT.test(password),
			special = SPECIAL.test(password);
		
		if(special){
			return rating(0, "illegal");
		}
		if (!password || password.length < 5){
			return rating(1, "too-short");
		}
		
		
		if (password.length > $("#password").attr("length")){
			return rating(2, "too-long");
		}
		if (lower && upper && digit && punct){
			return rating(6, "strong");
		}
		if (lower && digit && punct || upper && digit && punct || upper && lower && punct || upper && lower && digit){
			return rating(5, "good");
		}
		if (lower && digit || upper && digit || lower && punct || upper && punct){
			return rating(4, "weak");
		}
		if (lower || upper || digits || punct){
			return rating(3, "very-weak");
		}
	};
	
	$.validator.passwordRating.messages = {
		"illegal"  : "Illegal Characters",
		"too-short": "Too Short",
		"too-long" : "Too Long",
		"very-weak": "Very weak",
		"weak"     : "Weak",
		"good"     : "Good",
		"strong"   : "Strong"
	};
	
	$.validator.addMethod("password", function(value, element) {
		// use untrimmed value
		var password = element.value;
	
		var rating = $.validator.passwordRating(password);
		
		// If the password strength meter's DOM element is known, make sure it's visible
		if(null != passwordStrengthMeter && false == $(passwordStrengthMeter).is(':visible')){
			$(passwordStrengthMeter).show();
		}
		
		// update message for this field
		if($(element).attr("id") == "password"){
			var meter = $(".password-meter", element.form);		
			meter.find(".password-meter-bar").removeClass().addClass("password-meter-bar").addClass("password-meter-" + rating.messageKey);
			meter.find(".password-meter-message")
			.removeClass()
			.addClass("password-meter-message")
			.addClass("password-meter-message-" + rating.messageKey)
			.text($.validator.passwordRating.messages[rating.messageKey]);
			
			// Valid passwords = passwords of strength 4 or more
			return rating.rate > 3;
		}
		
		// Prevents confirm_password's validation messages from being suppressed
		return true;
	}, "&nbsp;");
	
	// manually add class rule
	$.validator.classRuleSettings.password = { password: true };
	
})(jQuery);
