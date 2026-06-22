-- PostgreSQL compatibility for recovered ADP/MES legacy native SQL.
--
-- Several recovered service methods still issue Oracle/MySQL-style numeric
-- boolean assignments, for example:
--   UPDATE WOM_TASK_ACT_ITEMSS SET CAN_BE_USED = 0 WHERE VALID = 1 ...
--
-- PostgreSQL can explicitly cast 0/1 to boolean and can compare boolean = 1 in
-- this runtime, but it does not allow assignment to boolean columns without an
-- assignment cast. Keep the table columns as real PostgreSQL booleans and allow
-- the legacy assignment form to execute.

UPDATE pg_catalog.pg_cast
   SET castcontext = 'a'
 WHERE castsource = 'integer'::regtype
   AND casttarget = 'boolean'::regtype
   AND castcontext = 'e';
