USE starexec;

ALTER TABLE user_assoc
DROP FOREIGN KEY user_assoc_ibfk_4;

ALTER TABLE user_assoc
DROP COLUMN proxy;