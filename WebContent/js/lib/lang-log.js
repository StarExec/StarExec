PR['registerLangHandler'](
	PR['createSimpleLexer'](
		[
		],
		[
			[PR['PR_COMMENT'], /\d{2}\/\d{2}\/\d{2}\s\d{2}:\d{2}:\d{2}\s(?:AM|PM)\s\w{3}:/],
			['warn', /.*Operation not permitted/],
			['warn', /job error:.*/],
			['warn', /runsolver output was not valid/],
			[PR['PR_PLAIN'], /^\w+/]
		]
	), ['log']
);
