#!/bin/bash

/home/starexec/scripts/starsql <<EOF
select job_id from job_pairs where status_code = 4;
EOF