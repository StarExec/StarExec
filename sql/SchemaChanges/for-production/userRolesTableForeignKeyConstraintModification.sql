ALTER TABLE user_roles DROP FOREIGN KEY user_roles_email;
ALTER TABLE user_roles ADD CONSTRAINT user_roles_email FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE ON UPDATE CASCADE;
