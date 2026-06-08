/*===================================================================*/
/* table: 配置项  sqlserver  初始化sql                                      */
/*===================================================================*/

-- supfusion_i18n_excel definition
## 1.0.0
create TABLE  supfusion_i18n_excel  (
     id              bigint       NOT NULL primary key,         --  '主键ID',
     status          int       NOT NULL ,         --  '导入/导出状态,1进行中, 2成功, 3失败',
     file_name       varchar(255)       NOT NULL ,         --  '导入/导出文件名',
     error_file      varchar(255)       DEFAULT NULL ,         --  '生成的错误文件名',
     error_message   varchar(255)       DEFAULT NULL ,         --  '错误消息',
     operate_type            varchar(100)       NOT NULL ,         --  '类型,import 或 export',
     error_num       int            DEFAULT 0 ,         --  '错误行数',
     add_num         int            DEFAULT 0 ,         --  '新增行数',
     update_num      int            DEFAULT 0 ,         --  '更新行数',
     all_num         int            DEFAULT 0 ,        --  '总行数'
     valid           tinyint            DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ;                                    -- 'execl导入导出状态实体类';

-- supfusion_i18n_index definition

create TABLE  supfusion_i18n_index  (
     id                   bigint         NOT NULL  primary key,         --  '主键ID',
     module_code          varchar(256)       NOT NULL ,         --  '模块名code',
     module_index_code    varchar(256)       NOT NULL ,         --  '国际化资源索引',
     valid                tinyint         DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ;                                                               -- '索引表';

-- supfusion_i18n_language definition
-- 不允许使用自增
create TABLE    supfusion_i18n_language    (
     id             bigint             NOT NULL  primary key,         --  '主键ID',
     langu_code     varchar(256)       NOT NULL ,         --  '语言code码',
     langu_type     varchar(256)       NOT NULL ,         --  '语言类型(中文描述)',
     langu_name     varchar(256)       NOT NULL ,         --  '语言类型(code自己对应的语言描述)',
     has_used        tinyint         DEFAULT 0 ,         --  '是否启用,0:是 1:否',
     valid          tinyint         DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ; -- ='语言类型表';
-- 初始化数据
insert into supfusion_i18n_language (id,langu_code,langu_type,langu_name,has_used,valid,creator,create_time,create_staff_id,modifier,modify_time,modify_staff_id) values
(1,'zh_CN','中文（简体）','中文（简体）',1,1,null,null,null,null,null,null);
insert into supfusion_i18n_language (id,langu_code,langu_type,langu_name,has_used,valid,creator,create_time,create_staff_id,modifier,modify_time,modify_staff_id) values
(2,'zh_HK','中文（香港）','中文（繁体）',1,1,null,null,null,null,null,null);
insert into supfusion_i18n_language (id,langu_code,langu_type,langu_name,has_used,valid,creator,create_time,create_staff_id,modifier,modify_time,modify_staff_id) values
(3,'en_US','英文（美国）','English',1,1,null,null,null,null,null,null);


-- supfusion_i18n_resource definition

create TABLE    supfusion_i18n_resource    (
     id                     bigint         NOT NULL  primary key,         --  '主键ID',
     i18n_key               varchar(255)       NOT NULL ,         --  '国际化主键',
     i18n_value             varchar(1024)      DEFAULT NULL ,         --  '国际化值',
     langu_code             varchar(255)       NOT NULL ,         --  '语言类型',
     module_code            varchar(255)       NOT NULL ,         --  '模块名',
     module_version_code    varchar(256)       DEFAULT NULL ,         --  '版本号表',
     valid                  tinyint         DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ;                                                                   -- '国际化资源表';
-- 添加索引
create  index index_key on supfusion_i18n_resource(i18n_key);
create  index sup_i18n_re_value_IDX on  supfusion_i18n_resource(i18n_value) ;
create  index index_langu on supfusion_i18n_resource(langu_code);
create  index sup_i18n_re_mo_code_IDX on supfusion_i18n_resource(module_code);
create  index sup_i18n_re_mo_time_IDX on  supfusion_i18n_resource (modify_time );

-- supfusion_i18n_token definition

create TABLE    supfusion_i18n_token    (
     id               bigint       NOT NULL  primary key ,         --  '主键ID',
     module_code      varchar(256)     NOT NULL ,         --  '模块名code',
     has_lock          tinyint       DEFAULT 0 ,         --  '是否持有锁,是否有锁 0:没有 1:有',
     token            varchar(255)     NOT NULL ,         --  'token字段',
     valid            tinyint       DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ;         -- ='Token表';
-- supfusion_i18n_version definition

create TABLE supfusion_i18n_version  (
     id                     bigint         NOT NULL primary key,         --  '主键ID',
     module_code            varchar(256)       NOT NULL ,         --  '模块名code',
     module_version_code    varchar(256)       NOT NULL ,         --  '应用国际化资源版本号',
     valid                  tinyint         DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT 'system' ,         --  '创建者',
     create_time     datetime           DEFAULT getutcdate() ,         --  '创建时间',
     create_staff_id bigint DEFAULT 0 ,  -- '创建者人员ID',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     modify_time     datetime           DEFAULT NULL ,         --  '修改时间',
     modify_staff_id bigint   DEFAULT NULL , -- '修改者人员ID',
) ;                                                                -- '版本号表';
alter table supfusion_i18n_index add unique index tb_ind_mo_code (module_code);
alter table supfusion_i18n_version add unique index tb_ver_mo_code (module_code);

## 1.0.1
ALTER TABLE supfusion_i18n_excel ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_index ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_language ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_resource ADD tenant_id VARCHAR(64) DEFAULT 'dt';
UPDATE supfusion_i18n_language SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_excel SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_index SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_resource SET tenant_id='dt' WHERE tenant_id IS NULL;
ALTER TABLE supfusion_i18n_resource DROP INDEX sup_i18n_re_value_IDX;
ALTER TABLE supfusion_i18n_resource DROP INDEX index_langu;
ALTER TABLE supfusion_i18n_index DROP INDEX tb_ind_mo_code;
ALTER TABLE supfusion_i18n_index add unique index udx_index_code_tenant (module_code, tenant_id);
