## 1.0.0
/*===================================================================*/
/* Table: 授权信息表(license_info)                */
/*===================================================================*/
create TABLE LICENSE_INFO (
    ID              bigint      NOT NULL       ,-- COMMENT '主键ID',
    MODULE_CODE    VARCHAR(200)      NOT NULL    ,-- COMMENT '主键ID',
    LICENSE_KEY    VARCHAR(256)    NOT NULL   ,--     COMMENT '授权KEY',
    VALUE           VARCHAR(50)     NOT NULL       ,-- COMMENT '授权值  0已授权 -1未授权 -2未授权 但在试用期 可用',
    DESCRIPTION       VARCHAR(256)    DEFAULT NULL    ,--    COMMENT '描述',
    APPLICATION_NAME       VARCHAR(256)    DEFAULT NULL    ,--    COMMENT '应用模块名称',
    APPLICATION_TYPE       VARCHAR(256)    DEFAULT NULL    ,--    COMMENT '应用模块型号',
    TIME            VARCHAR(256)    NOT NULL   ,-- COMMENT 'APP启动时的时间',
    HASH_CODE       VARCHAR(256)    NOT NULL   ,-- COMMENT 'MODULECODE LICENSEKEY VALUE TIME SALT MD5加密后',
     VALID             TINYINT       DEFAULT 1 ,                    -- COMMENT '是否删除 0 删除 1 未删除',
    CREATOR           VARCHAR(32)      DEFAULT NULL ,              -- COMMENT '创建者',
    MODIFIER          VARCHAR(32)      DEFAULT NULL ,              -- COMMENT '修改者',
    CREATE_TIME       DATETIME        DEFAULT GETUTCDATE()   NULL, -- COMMENT '创建时间',
    MODIFY_TIME       DATETIME ,                                   -- COMMENT '修改时间',
    CREATE_STAFF_ID   BIGINT      DEFAULT NULL, --COMMENT '创建者人员ID',
    MODIFY_STAFF_ID   BIGINT      DEFAULT NULL, --COMMENT '修改者人员ID',
     PRIMARY KEY (ID)
) ;
create UNIQUE INDEX MODULE_KEY ON  LICENSE_INFO(MODULE_CODE);
EXECUTE sp_addextendedproperty N'MS_Description', '授权信息表', N'user', N'dbo', N'table', N'license_info', NULL, NULL;

