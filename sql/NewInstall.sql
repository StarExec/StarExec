-- Drop and recreate the database to get a fresh slate
DROP DATABASE IF EXISTS starexec;
CREATE DATABASE starexec;

USE starexec;

source new-install/StarSchema.sql
source new-install/MinimalData.sql
source StarProcedures.sql
source StarFunctions.sql