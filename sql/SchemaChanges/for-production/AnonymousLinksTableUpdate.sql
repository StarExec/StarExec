USE starexec;

DROP TABLE anonymous_links;

CREATE TABLE anonymous_links (
	unique_id VARCHAR(36) NOT NULL,
	primitive_id INT NOT NULL,
	primitive_type ENUM('solver', 'bench', 'job') NOT NULL,
	primitives_to_anonymize ENUM('all', 'allButBench', 'none') NOT NULL,
	date_created DATE NOT NULL,

	PRIMARY KEY (unique_id),
	UNIQUE KEY (primitive_id, primitive_type, primitives_to_anonymize)
);
