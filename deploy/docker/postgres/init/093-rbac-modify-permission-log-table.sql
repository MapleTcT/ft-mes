-- PostgreSQL compatibility for workflow permission audit persistence.
--
-- The recovered configuration runtime persists PermissionModifyLogPO during
-- workflow publishing. Vendor metadata in this package can miss the table,
-- causing /ec/workflow/flowPublish to rollback after JBPM deployment succeeds.

CREATE TABLE IF NOT EXISTS public.rbac_modify_permission_log (
    id bigint PRIMARY KEY,
    cid bigint,
    create_time timestamp without time zone,
    deal_info text,
    deal_time varchar(255),
    deal_type integer,
    dealer varchar(255),
    flow_name varchar(512),
    flow_name_display varchar(512),
    menu_name varchar(512),
    menu_name_inter varchar(512),
    menu_operate_id bigint,
    menu_operate_name varchar(512),
    menu_operate_name_inter varchar(512),
    reason text,
    target_department varchar(512),
    target_name varchar(512),
    target_position varchar(512),
    target_role varchar(512)
);

ALTER TABLE public.rbac_modify_permission_log
    ADD COLUMN IF NOT EXISTS cid bigint,
    ADD COLUMN IF NOT EXISTS create_time timestamp without time zone,
    ADD COLUMN IF NOT EXISTS deal_info text,
    ADD COLUMN IF NOT EXISTS deal_time varchar(255),
    ADD COLUMN IF NOT EXISTS deal_type integer,
    ADD COLUMN IF NOT EXISTS dealer varchar(255),
    ADD COLUMN IF NOT EXISTS flow_name varchar(512),
    ADD COLUMN IF NOT EXISTS flow_name_display varchar(512),
    ADD COLUMN IF NOT EXISTS menu_name varchar(512),
    ADD COLUMN IF NOT EXISTS menu_name_inter varchar(512),
    ADD COLUMN IF NOT EXISTS menu_operate_id bigint,
    ADD COLUMN IF NOT EXISTS menu_operate_name varchar(512),
    ADD COLUMN IF NOT EXISTS menu_operate_name_inter varchar(512),
    ADD COLUMN IF NOT EXISTS reason text,
    ADD COLUMN IF NOT EXISTS target_department varchar(512),
    ADD COLUMN IF NOT EXISTS target_name varchar(512),
    ADD COLUMN IF NOT EXISTS target_position varchar(512),
    ADD COLUMN IF NOT EXISTS target_role varchar(512);

CREATE INDEX IF NOT EXISTS idx_rbac_modify_permission_log_cid
    ON public.rbac_modify_permission_log(cid);

CREATE INDEX IF NOT EXISTS idx_rbac_modify_permission_log_flow_name
    ON public.rbac_modify_permission_log(flow_name);
