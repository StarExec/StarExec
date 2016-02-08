USE starexec;

CREATE TABLE anonymous_links (
	unique_id VARCHAR(36) NOT NULL,
	primitive_id INT NOT NULL,
	primitive_type VARCHAR(36),
	hide_primitive_name BOOLEAN,

	PRIMARY KEY (unique_id),
	UNIQUE KEY (primitive_id, primitive_type, hide_primitive_name)
);
