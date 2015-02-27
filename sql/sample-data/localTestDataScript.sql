USE starexec; 
-- This file inserts a bit of fake data into the database, making it possible to create jobs.
-- Intended for local test builds where uploads do not work

insert into solvers values (1,1,"test",NOW(),"","",false,1,false,false,1);
insert into configurations values (1,1,"config","",NOW());
insert into solver_assoc values (1,1);
insert into solver_assoc values (2,1);
insert into processors values (1,"proc","","",1,1,1);
insert into processors values (2,"proc","","",1,2,2);
insert into processors values (3,"proc","","",1,3,3);
insert into processors values (8,"proc","","",1,4,3);
insert into processors values (4,"proc","","",2,1,1);
insert into processors values (5,"proc","","",2,2,2);
insert into processors values (6,"proc","","",2,3,3);
insert into processors values (7,"proc","","",2,4,3);
insert into benchmarks values (1,1,"bench",null,NOW(),"","",false,1,false,false);
insert into bench_assoc values(1,1,1);
insert into bench_assoc values(2,1,2);