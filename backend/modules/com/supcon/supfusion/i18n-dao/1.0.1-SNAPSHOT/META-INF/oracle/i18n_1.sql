/*===================================================================*/
/* table: 配置项  oracle  初始化sql                                      */
/*===================================================================*/
-- supfusion_i18n_excel definition
## 1.0.0
CREATE TABLE supfusion_i18n_excel (
    id                NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    status            NUMBER(11, 0) NOT NULL,         --  '导入/导出状态,1进行中, 2成功, 3失败',
    file_name         VARCHAR(255) NOT NULL,         --  '导入/导出文件名',
    error_file        VARCHAR(255) DEFAULT NULL,         --  '生成的错误文件名',
    error_message     VARCHAR(255) DEFAULT NULL,         --  '错误消息',
    operate_type      VARCHAR(100) NOT NULL,         --  '类型,import 或 export',
    error_num         NUMBER(11, 0) DEFAULT 0,         --  '错误行数',
    add_num           NUMBER(11, 0) DEFAULT 0,         --  '新增行数',
    update_num        NUMBER(11, 0) DEFAULT 0,         --  '更新行数',
    all_num           NUMBER(11, 0) DEFAULT 0,         --  '总行数'
    valid             NUMBER(1, 0) DEFAULT 1,          -- '逻辑删除,是否有效0:无效 1:有效',
    creator           VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time       TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id   NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier          VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time       TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id   NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
);                                    -- 'execl导入导出状态实体类';
-- supfusion_i18n_index definition

CREATE TABLE supfusion_i18n_index (
    id                  NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    module_code         VARCHAR(256) NOT NULL,         --  '模块名code',
    module_index_code   VARCHAR(256) NOT NULL,         --  '国际化资源索引',
    valid               NUMBER(1, 0) DEFAULT 1,         --  '逻辑删除,是否有效0:无效 1:有效',
    creator             VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time         TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id     NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier            VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time         TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id     NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
);                                                               -- '索引表';
-- supfusion_i18n_language definition
-- 不允许使用自增

CREATE TABLE supfusion_i18n_language (
    id                NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    langu_code        VARCHAR(256) NOT NULL,         --  '语言code码',
    langu_type        VARCHAR(256) NOT NULL,         --  '语言类型(中文描述)',
    langu_name        VARCHAR(256) NOT NULL,         --  '语言类型(code自己对应的语言描述)',
    has_used          NUMBER(1, 0) DEFAULT 1,         --  '是否启用,1:是 0:否',
    valid             NUMBER(1, 0) DEFAULT 1,         --  '逻辑删除,是否有效0:无效 1:有效',
    creator           VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time       TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id   NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier          VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time       TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id   NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
); 
-- 初始化数据

INSERT INTO supfusion_i18n_language (
    id,
    langu_code,
    langu_type,
    langu_name,
    has_used,
    valid,
    creator,
    create_time,
    create_staff_id,
    modifier,
    modify_time,
    modify_staff_id
) VALUES (
    1,
    'zh_CN',
    '中文（简体）',
    '中文（简体）',
    1,
    1,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
);

INSERT INTO supfusion_i18n_language (
    id,
    langu_code,
    langu_type,
    langu_name,
    has_used,
    valid,
    creator,
    create_time,
    create_staff_id,
    modifier,
    modify_time,
    modify_staff_id
) VALUES (
    2,
    'zh_HK',
    '中文（香港）',
    '中文（繁体）',
    1,
    1,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
);

INSERT INTO supfusion_i18n_language (
    id,
    langu_code,
    langu_type,
    langu_name,
    has_used,
    valid,
    creator,
    create_time,
    create_staff_id,
    modifier,
    modify_time,
    modify_staff_id
) VALUES (
    3,
    'en_US',
    '英文（美国）',
    'English',
    1,
    1,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
);

-- supfusion_i18n_resource definition

CREATE TABLE supfusion_i18n_resource (
    id                    NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    i18n_key              VARCHAR(255) NOT NULL,         --  '国际化主键',
    i18n_value            VARCHAR(1024) DEFAULT NULL,         --  '国际化值',
    langu_code            VARCHAR(255) NOT NULL,         --  '语言类型',
    module_code           VARCHAR(255) NOT NULL,         --  '模块名',
    module_version_code   VARCHAR(256) DEFAULT NULL,         --  '版本号表',
    valid                 NUMBER(1, 0) DEFAULT 1,         --  '逻辑删除,是否有效0:无效 1:有效',
    creator               VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time           TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id       NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier              VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time           TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id       NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
);-- ='语言类型表';-- '国际化资源表';

-- 添加索引

CREATE INDEX sup_i18n_re_key_idx ON
    supfusion_i18n_resource (
        i18n_key
    );

CREATE INDEX sup_i18n_re_value_idx ON
    supfusion_i18n_resource (
        i18n_value
    );

CREATE INDEX sup_i18n_re_langu_idx ON
    supfusion_i18n_resource (
        langu_code
    );

CREATE INDEX sup_i18n_re_mo_code_idx ON
    supfusion_i18n_resource (
        module_code
    );

CREATE INDEX sup_i18n_re_mo_time_idx ON
    supfusion_i18n_resource (
        modify_time
    );

-- supfusion_i18n_token definition

CREATE TABLE supfusion_i18n_token (
    id                NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    module_code       VARCHAR(256) NOT NULL,         --  '模块名code',
    has_lock          NUMBER(1, 0) DEFAULT 0,         --  '是否持有锁,是否有锁 0:没有 1:有',
    token             VARCHAR(255) NOT NULL,         --  'token字段',
    valid             NUMBER(1, 0) DEFAULT 1,         --  '逻辑删除,是否有效0:无效 1:有效',
    creator           VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time       TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id   NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier          VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time       TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id   NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
);         -- ='Token表';
Alter table supfusion_i18n_token add(tenant_id VARCHAR(64) DEFAULT 'dt');
-- supfusion_i18n_version definition

CREATE TABLE supfusion_i18n_version (
    id                    NUMBER(20, 0) NOT NULL PRIMARY KEY,         --  '主键ID',
    module_code           VARCHAR(256) NOT NULL,         --  '模块名code',
    module_version_code   VARCHAR(256) NOT NULL,         --  '应用国际化资源版本号',
    valid                 NUMBER(1, 0) DEFAULT 1,         --  '逻辑删除,是否有效0:无效 1:有效',
    creator               VARCHAR(32) DEFAULT 'system',        -- '创建者',
    create_time           TIMESTAMP DEFAULT sysdate,     -- '创建时间',
    create_staff_id       NUMBER(20, 0) DEFAULT 0,         -- '创建者人员ID',
    modifier              VARCHAR(32) DEFAULT NULL,         -- '修改者',
    modify_time           TIMESTAMP DEFAULT NULL,         -- '修改时间',
    modify_staff_id       NUMBER(20, 0) DEFAULT NULL          -- '修改者人员ID',
);                                                                -- '版本号表';
Alter table supfusion_i18n_version add(tenant_id VARCHAR(64) DEFAULT 'dt');
alter table supfusion_i18n_version  add constraint  tb_ver_mo_code unique (module_code);
alter table supfusion_i18n_index  add constraint  tb_ind_mo_code unique (module_code);

## 1.0.1
ALTER TABLE supfusion_i18n_excel ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_index ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_language ADD tenant_id VARCHAR(64) DEFAULT 'dt';
ALTER TABLE supfusion_i18n_resource ADD tenant_id VARCHAR(64) DEFAULT 'dt';
UPDATE supfusion_i18n_language SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_excel SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_index SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_resource SET tenant_id='dt' WHERE tenant_id IS NULL;
ALTER TABLE supfusion_i18n_resource DROP constraint sup_i18n_re_key_IDX;
ALTER TABLE supfusion_i18n_resource DROP constraint sup_i18n_re_value_idx;
ALTER TABLE supfusion_i18n_resource DROP constraint sup_i18n_re_langu_idx;
ALTER TABLE supfusion_i18n_index DROP constraint tb_ind_mo_code;
ALTER TABLE supfusion_i18n_index ADD constraint udx_index_code_tenant unique (module_code, tenant_id);