USE starexec;

-- first clear out the issues from before

delete job_pair_completion from job_pair_completion join job_pair_completion as B on job_pair_completion.pair_id=B.pair_id where job_pair_completion.completion_id<B.completion_id;


-- makes the pair a unique key in the job completion table

alter table job_pair_completion add constraint pair_id unique key (pair_id);