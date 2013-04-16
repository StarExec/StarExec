#!/bin/bash

/home/starexec/scripts/starsql <<EOF
select first_name from job_pairs , jobs , users where job_pairs.job_id = jobs.id and jobs.user_id = users.id and status_code = 4;
EOF