USE starexec;
-- We will be starting to return all archives as simply .zip  files,
-- so we no longer need this in the table
ALTER TABLE users DROP COLUMN pref_archive_type;