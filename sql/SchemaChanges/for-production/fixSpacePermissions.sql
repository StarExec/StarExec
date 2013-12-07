USE starexec;

UPDATE permissions JOIN spaces ON spaces.default_permission=permissions.id SET is_leader=0;