-- Row count template for one PostgreSQL table.
-- The preflight runner substitutes :target_table.

select
  :'target_table' as table_name,
  count(*) as row_count
from :target_table;
