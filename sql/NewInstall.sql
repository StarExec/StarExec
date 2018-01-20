-- Drop and recreate the database to get a fresh slate
DROP DATABASE IF EXISTS @DB.Name@;
CREATE DATABASE @DB.Name@;

USE @DB.Name@;

source new-install/StarSchema.sql
source new-install/MinimalData.sql
source StarProcedures.sql
source StarFunctions.sql
