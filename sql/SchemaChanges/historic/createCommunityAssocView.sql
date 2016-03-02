USE starexec;

CREATE VIEW community_assoc AS 
SELECT ancestor AS comm_id, descendant AS space_id FROM closure 
JOIN set_assoc ON set_assoc.child_id=closure.ancestor 
WHERE set_assoc.space_id=1;