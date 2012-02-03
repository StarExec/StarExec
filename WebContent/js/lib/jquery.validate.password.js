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
	
	var LOWER = /[a-z]/,
		UPPER = /[A-Z]/,
		DIGIT = /[0-9]/,
		PUNCT = /[~!@#$%\^&*()\-_=+]/,
		SPECIAL = /[^a-zA-Z0-9~!@#$%\^&*()\-_=+]/;
		
		
	function rating(rate, message) {
		return {
			rate: rate,
			messageKey: message
		};
	}
	
	function uncapitalize(str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
	
	$.validator.passwordRating = function(password) {
		var lower = LOWER.test(password),
			upper = UPPER.test(uncapitalize(password)),
			digit = DIGIT.test(password),
			punct = PUNCT.test(password),
			special = SPECIAL.test(password);
		
		if(special)
			return rating(0, "illegal");
		if (!password || password.length < 6)
			return rating(1, "too-short");
		if (password.length > 16)
			return rating(2, "too-long");
		if (lower && upper && digit && punct)
			return rating(6, "strong");
		if (lower && digit && punct || upper && digit && punct)
			return rating(5, "good");
		if (lower && digit || upper && digit || lower && punct || upper && punct || digit && punct)
			return rating(4, "weak");
		if (lower || upper || digits || punct)
			return rating(3, "very-weak");
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
		// update message for this field
		if($(element).attr("id") == "password"){
			var meter = $(".password-meter", element.form);		
			meter.find(".password-meter-bar").removeClass().addClass("password-meter-bar").addClass("password-meter-" + rating.messageKey);
			meter.find(".password-meter-message")
			.removeClass()
			.addClass("password-meter-message")
			.addClass("password-meter-message-" + rating.messageKey)
			.text($.validator.passwordRating.messages[rating.messageKey]);
			return rating.rate > 4;
		}
		
		// Prevents confirm_password's validation messages from being suppressed
		return true;
	}, "&nbsp;");
	
	// manually add class rule
	$.validator.classRuleSettings.password = { password: true };
	
})(jQuery);
