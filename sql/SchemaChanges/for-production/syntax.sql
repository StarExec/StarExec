-- Support syntax highlighting of benchmarks

USE starexec;

-- A table where we can keep track of the different syntax highlighters we
-- support for benchmarks
--   name:  Human readable display name of the syntax
--   class: Class name that will be used in the HTML to denote this syntax
--   js:    Relative path to JavaScript lexer for this syntax
CREATE TABLE syntax (
	id    INT      NOT NULL AUTO_INCREMENT,
	name  CHAR(32) NOT NULL,
	class CHAR(32) NOT NULL,
	js    CHAR(32),
	PRIMARY KEY (id),
	UNIQUE KEY (name)
);

-- Here we add the supported lexers to the DB
-- In general, id is not important and should be set automatically. However, it
-- is important that 1 represent Plain Text (ie: No highlighting) because that
-- will be the default for processors without a syntax set
INSERT INTO syntax (id, name, class, js) VALUES
	(1, 'Plain Text', 'prettyprinted',  NULL            ),
	(2, 'C'         , 'lang-c'       ,  NULL            ),
	(3, 'SMT-LIB'   , 'lang-smtlib'  , 'lib/lang-smtlib'),
	(4, 'TPTP'      , 'lang-tptp'    , 'lib/lang-tptp'  )
;

ALTER TABLE processors ADD syntax_id INT DEFAULT 1;
ALTER TABLE processors ADD CONSTRAINT processors_syntax FOREIGN KEY (syntax_id) REFERENCES syntax(id);
