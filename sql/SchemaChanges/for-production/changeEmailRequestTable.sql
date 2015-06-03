CREATE TABLE change_email_requests (
	user_id INT NOT NULL,
	new_email VARCHAR(64) NOT NULL,
	code VARCHAR(36) NOT NULL,
	PRIMARY KEY (user_id),
	UNIQUE KEY (code),
	CONSTRAINT change_email_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

