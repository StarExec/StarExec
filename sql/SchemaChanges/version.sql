-- Add version information for DB Schema
-- Minor version is incremented on each change
-- Major version is only incremented when a change may require manual
--     intervention and cannot be applied automatically

ALTER TABLE system_flags
ADD major_version INT UNSIGNED;

ALTER TABLE system_flags
ADD minor_version INT UNSIGNED;

UPDATE system_flags SET major_version=1, minor_version=1;
