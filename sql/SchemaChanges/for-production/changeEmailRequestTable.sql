CREATE TABLE change_email_requests (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	new_email VARCHAR(64) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT change_email_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

