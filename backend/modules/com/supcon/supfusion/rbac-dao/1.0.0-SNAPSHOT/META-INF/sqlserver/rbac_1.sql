## 1.0.0
/*==============================================================*/
/* TABLE: 角色表(RBAC_ROLE)                */
/*==============================================================*/
CREATE TABLE rbac_role (
    ID BIGINT NOT NULL,
    VALID  INT DEFAULT 1,
    VERSION INT DEFAULT 0,
    CID  BIGINT,
    LEAF  TINYINT DEFAULT 0,
    FULL_PATH_NAME  VARCHAR(4000),
    PARENT_ID  BIGINT,
    LAY_NO  BIGINT,
    LAY_REC  VARCHAR(4000),
    UUID  VARCHAR(4000),
    THREE_ROLE_TYPE  INT,
    ROLE_TYPE  VARCHAR(4000),
    SORT  DECIMAL(38),
    DESCRIPTION  VARCHAR(510),
    NAME  VARCHAR(160),
    CODE  VARCHAR(160),
    CREATOR VARCHAR(32) DEFAULT NULL,
    MODIFIER VARCHAR(32) DEFAULT NULL,
    TERMINATOR VARCHAR(32) DEFAULT NULL,
    CREATE_TIME DATETIME,
    MODIFY_TIME DATETIME,
    DELETE_TIME DATETIME,
    CREATE_STAFF_ID BIGINT ,
    MODIFY_STAFF_ID BIGINT ,
    TAG VARCHAR(510),
    primary key (id)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色表', N'user', N'dbo', N'table', N'rbac_role', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_role', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效', N'user', N'dbo', N'table', N'rbac_role', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本信息', N'user', N'dbo', N'table', N'rbac_role', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_role', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否叶子', N'user', N'dbo', N'table', N'rbac_role', N'column', N'LEAF';
EXECUTE sp_addextendedproperty N'MS_Description', '层级全路径', N'user', N'dbo', N'table', N'rbac_role', N'column', N'FULL_PATH_NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '上级节点ID', N'user', N'dbo', N'table', N'rbac_role', N'column', N'PARENT_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '层级', N'user', N'dbo', N'table', N'rbac_role', N'column', N'LAY_NO';
EXECUTE sp_addextendedproperty N'MS_Description', '层级结构', N'user', N'dbo', N'table', N'rbac_role', N'column', N'LAY_REC';
EXECUTE sp_addextendedproperty N'MS_Description', '用于软件公司同步接口', N'user', N'dbo', N'table', N'rbac_role', N'column', N'UUID';
EXECUTE sp_addextendedproperty N'MS_Description', '三员类型:1系统管理员,2安全保密员 ,3安全审计员', N'user', N'dbo', N'table', N'rbac_role', N'column', N'THREE_ROLE_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '角色类型', N'user', N'dbo', N'table', N'rbac_role', N'column', N'ROLE_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '排序', N'user', N'dbo', N'table', N'rbac_role', N'column', N'SORT';
EXECUTE sp_addextendedproperty N'MS_Description', '描述', N'user', N'dbo', N'table', N'rbac_role', N'column', N'DESCRIPTION';
EXECUTE sp_addextendedproperty N'MS_Description', '名称', N'user', N'dbo', N'table', N'rbac_role', N'column', N'NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '编码', N'user', N'dbo', N'table', N'rbac_role', N'column', N'CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_role', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_role', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_role', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_role', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_role', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_role', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_role', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_role', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '标签', N'user', N'dbo', N'table', N'rbac_role', N'column', N'TAG';
create index ROLE_VALID_IDX on rbac_role(VALID);

INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (1,'管理员角色','systemRole','管理员角色','管理员角色',1000,1,1);
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (2,'公司管理员角色','companySystemRole','公司管理员角色','公司管理员角色',1000,1,1);
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (3,'普通用户角色','normalRole','普通用户角色','普通用户角色',1000,1,1);
/*==============================================================*/
/* TABLE:角色用户表(rbac_roleuser)                */
/*==============================================================*/
CREATE TABLE rbac_roleuser(
  ID BIGINT NOT NULL,
  VERSION INT DEFAULT 0 ,
  POSITION_FLAG TINYINT DEFAULT 0,
  ROLE_ID BIGINT,
  USER_ID BIGINT ,
  VALID INT DEFAULT 1 ,
  END_TIME DATETIME,
  START_TIME DATETIME,
  PERSON_NAME VARCHAR(160) ,
  PERSON_CODE VARCHAR(160),
  USER_NAME VARCHAR(160) ,
  TERMINATOR VARCHAR(32) ,
  MODIFIER VARCHAR(32) ,
  CREATOR VARCHAR(32) ,
  CREATE_TIME DATETIME,
  MODIFY_TIME DATETIME,
  DELETE_TIME DATETIME ,
  CREATE_STAFF_ID BIGINT  ,
  MODIFY_STAFF_ID BIGINT  ,
  FROM_POSITION INT DEFAULT 1,
  PRIMARY KEY (ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色用户表', N'user', N'dbo', N'table', N'rbac_roleuser', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本信息', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否仅是岗位带入的角色', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'POSITION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '角色ID', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'ROLE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户ID', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'USER_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '调出时间', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'END_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '调入时间', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'START_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '人员姓名', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'PERSON_NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '人员编号', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'PERSON_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '用户名', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'USER_NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '来源 1 来源于用户 2 来源于岗位 3两者都有', N'user', N'dbo', N'table', N'rbac_roleuser', N'column', N'FROM_POSITION';
create index INDEX_ROLEUSER_ROLE_ID on rbac_roleuser(ROLE_ID);
create index INDEX_ROLEUSER_USER_ID on rbac_roleuser(USER_ID);
INSERT INTO rbac_roleuser(ID, ROLE_ID, USER_ID, PERSON_NAME, PERSON_CODE, USER_NAME,CREATE_STAFF_ID,MODIFY_STAFF_ID) VALUES (1, 1, 1, '默认人员','default', 'admin',1,1);
/*==============================================================*/
/* TABLE:操作表(RBAC_MENUOPERATE)                */
/*==============================================================*/
CREATE TABLE rbac_menuoperate
(
    ID BIGINT NOT NULL ,
    ROW_VERSION BIGINT DEFAULT 0 ,
    DELETE_TIME DATETIME ,
    MODIFY_TIME DATETIME,
    CREATE_TIME DATETIME,
    TERMINATOR VARCHAR(32),
    MODIFIER VARCHAR(32),
    CREATOR VARCHAR(32),
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    VALID INT DEFAULT 1 ,
    CID BIGINT,
    IS_ALLOW_PROXY TINYINT DEFAULT 0,
    IS_HIDDEN TINYINT DEFAULT 0,
    THREE_ROLE TINYINT DEFAULT 0,
    VIEW_CODE VARCHAR(510) ,
    IS_QUERY TINYINT DEFAULT 0,
    IS_ORRELATION TINYINT ,
    ENABLE_DATAPERMISSION TINYINT DEFAULT 0,
    ENABLE_CUSTOMPERMISSION TINYINT DEFAULT 0,
    FOR_FLOW_PERMISSION TINYINT DEFAULT 0,
    ENABLE_NORESTRICT TINYINT DEFAULT 1,
    ENABLE_DEALERPERMISSION TINYINT DEFAULT 0,
    ENABLE_ASSIGNSTAFF TINYINT DEFAULT 0,
    ENABLE_ASSIGNPOS TINYINT DEFAULT 0,
    ENABLE_POSRESTRICT TINYINT DEFAULT 0,
    ENABLE_DEPTRICT TINYINT DEFAULT 0,
    ENABLE_ASSIGNDEPT TINYINT DEFAULT 0,
    ENABLE_GROUPRESTRICT TINYINT DEFAULT 0,
    ENTITY_CODE VARCHAR(510) ,
    IGNORE_PERMISSION TINYINT DEFAULT 0,
    POWER_FLAG TINYINT ,
    FLOW_VERSION VARCHAR(510),
    FLOW_KEY VARCHAR(510) ,
    MSG_ASSEMBLED INT,
    DEPLOYMENT_ID BIGINT,
    MENUOPERATETYPE VARCHAR(510) ,
    MENUINFO_ID BIGINT ,
    ICON_CLS VARCHAR(510),
    MODULE_CODE VARCHAR(510),
    SORT DECIMAL(38) ,
    MEMO VARCHAR(510),
    TARGET VARCHAR(510) ,
    ACTION_URL VARCHAR(510),
    NAMESPACE VARCHAR(510),
    URL VARCHAR(510) ,
    NAME_ZH_CN VARCHAR(510),
    NAME_DISPLAY VARCHAR(510),
    NAME VARCHAR(510),
    CODE VARCHAR(510),
    APP VARCHAR(510),
    DEFAULT_OPERATE INT DEFAULT 0,
    EDITED INT DEFAULT 0 ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '操作表', N'user', N'dbo', N'table', N'rbac_menuoperate', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ROW_VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否允许委托', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'IS_ALLOW_PROXY';
EXECUTE sp_addextendedproperty N'MS_Description', '是否隐藏', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'IS_HIDDEN';
EXECUTE sp_addextendedproperty N'MS_Description', '是否三员菜单', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'THREE_ROLE';
EXECUTE sp_addextendedproperty N'MS_Description', '视图编码', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'VIEW_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否查询操作', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'IS_QUERY';
EXECUTE sp_addextendedproperty N'MS_Description', '该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'IS_ORRELATION';
EXECUTE sp_addextendedproperty N'MS_Description', '启用数据权限', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_DATAPERMISSION';
EXECUTE sp_addextendedproperty N'MS_Description', '启用自定义权限', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_CUSTOMPERMISSION';
EXECUTE sp_addextendedproperty N'MS_Description', '启用业务权限', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'FOR_FLOW_PERMISSION';
EXECUTE sp_addextendedproperty N'MS_Description', '无限制', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_NORESTRICT';
EXECUTE sp_addextendedproperty N'MS_Description', '启用处理人', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_DEALERPERMISSION';
EXECUTE sp_addextendedproperty N'MS_Description', '启用指定人员', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_ASSIGNSTAFF';
EXECUTE sp_addextendedproperty N'MS_Description', '启用指定岗位', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_ASSIGNPOS';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位限制', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_POSRESTRICT';
EXECUTE sp_addextendedproperty N'MS_Description', '部门限制', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_DEPTRICT';
EXECUTE sp_addextendedproperty N'MS_Description', '启用指定部门', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_ASSIGNDEPT';
EXECUTE sp_addextendedproperty N'MS_Description', '启用组限制', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENABLE_GROUPRESTRICT';
EXECUTE sp_addextendedproperty N'MS_Description', '实体编码', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ENTITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '忽视权限', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'IGNORE_PERMISSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否是主列表视图的查询操作', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'POWER_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '工作流版本', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'FLOW_VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '工作流KEY', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'FLOW_KEY';
EXECUTE sp_addextendedproperty N'MS_Description', '操作类型', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MENUOPERATETYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单ID', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MENUINFO_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '图表样式', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ICON_CLS';
EXECUTE sp_addextendedproperty N'MS_Description', '模块信息：BUNDLE的SYMBOLICNAME组成', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MODULE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '排序', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'SORT';
EXECUTE sp_addextendedproperty N'MS_Description', '备注', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'MEMO';
EXECUTE sp_addextendedproperty N'MS_Description', '打开方式', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'TARGET';
EXECUTE sp_addextendedproperty N'MS_Description', 'ACTION', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'ACTION_URL';
EXECUTE sp_addextendedproperty N'MS_Description', '命名空间', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'NAMESPACE';
EXECUTE sp_addextendedproperty N'MS_Description', '地址', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'URL';
EXECUTE sp_addextendedproperty N'MS_Description', '中文名', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'NAME_ZH_CN';
EXECUTE sp_addextendedproperty N'MS_Description', '名称', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '国际化值 存默认名称', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'NAME_DISPLAY';
EXECUTE sp_addextendedproperty N'MS_Description', '编码', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '所属应用名', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'APP';
EXECUTE sp_addextendedproperty N'MS_Description', '默认操作标识，默认操作不可删除', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'DEFAULT_OPERATE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否修改过 修改过的操作升级时不修改', N'user', N'dbo', N'table', N'rbac_menuoperate', N'column', N'EDITED';
create index INDEX_MENUOPERATE_CODE on rbac_menuoperate(CODE);
create index INDEX_MENUOPERATE_NAME on rbac_menuoperate(NAME);
create index IND_MENUOPERATE_URL on rbac_menuoperate(URL);
create index IND_MENUOPERATE_NAMESPACE on rbac_menuoperate(NAMESPACE);
create index IND_MENUOPERATE_MENUINFO_ID on rbac_menuoperate(MENUINFO_ID);
create index IDX_MENUOPERATE_ENTITYCODE on rbac_menuoperate(ENTITY_CODE);
create index BASE_MENUOPERATE_VALID_IDX on rbac_menuoperate(VALID);

/*==============================================================*/
/* TABLE:菜单表(rbac_menuinfo)                */
/*==============================================================*/
CREATE TABLE rbac_menuinfo
(
    ID BIGINT NOT NULL,
    VERSION INT DEFAULT 0 ,
    DELETE_TIME DATETIME DEFAULT GETDATE(),
    MODIFY_TIME DATETIME DEFAULT GETDATE(),
    CREATE_TIME DATETIME DEFAULT GETDATE(),
    TERMINATOR VARCHAR(32) ,
    LEAF INT DEFAULT 0,
    MODIFIER VARCHAR(32),
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT ,
    MODIFY_STAFF_ID BIGINT  ,
    VALID INT DEFAULT 1,
    CID BIGINT,
    SECURITY_CLASS VARCHAR(510),
    ABSOLUTE_HIDDEN TINYINT DEFAULT 0,
    THREE_ROLE TINYINT DEFAULT 0,
    SHOW_TYPE INT,
    REQUEST_TYPE INT,
    HIDDEN_TYPE INT,
    MENU_TYPE INT ,
    IS_HIDE TINYINT DEFAULT 0,
    GROUP_ONLY TINYINT DEFAULT 0,
    ENTITY_CODE VARCHAR(510) ,
    MODULE_CODE VARCHAR(510) ,
    SYSTEM_DEFAULT TINYINT DEFAULT 0,
    CSS_CLASS VARCHAR(510) ,
    SORT DECIMAL(38) ,
    ACTION_URL VARCHAR(510) ,
    NAMESPACE VARCHAR(510),
    URL VARCHAR(510) ,
    TARGET VARCHAR(510) default 'SELF',
    MEMO VARCHAR(510) ,
    NAME VARCHAR(510) ,
    NAME_DISPLAY VARCHAR(510),
    CODE VARCHAR(510),
    APP VARCHAR(510),
    ENABLE TINYINT DEFAULT 1,
    LAY_NO INT ,
    LAY_REC VARCHAR(4000),
    PARENT_ID BIGINT,
    FULL_PATH VARCHAR(4000) ,
    FULL_PATH_NAME VARCHAR(4000),
    SOURCE VARCHAR(510) ,
    EDITED TINYINT DEFAULT 0,
    TYPE INT DEFAULT 0,
    NO_RESTRICT TINYINT DEFAULT 0,
    STATUS INT DEFAULT 0,
    ROUTE VARCHAR(510),
    EXTRA VARCHAR(4000),
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单表', N'user', N'dbo', N'table', N'rbac_menuinfo', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '是否是叶子节点', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'LEAF';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', '密级', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'SECURITY_CLASS';
EXECUTE sp_addextendedproperty N'MS_Description', '绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ABSOLUTE_HIDDEN';
EXECUTE sp_addextendedproperty N'MS_Description', '是否三员菜单', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'THREE_ROLE';
EXECUTE sp_addextendedproperty N'MS_Description', '请求方式0:链接页面，1：链接URL', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'SHOW_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '请求类型', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'REQUEST_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '隐藏类型', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'HIDDEN_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单类型', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MENU_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否隐藏', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'IS_HIDE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否仅集团使用', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'GROUP_ONLY';
EXECUTE sp_addextendedproperty N'MS_Description', '实体编码', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ENTITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '模块信息：BUNDLE的SYMBOLICNAME组成', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MODULE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否默认系统', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'SYSTEM_DEFAULT';
EXECUTE sp_addextendedproperty N'MS_Description', 'CSS_CLASS(菜单样式用)', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CSS_CLASS';
EXECUTE sp_addextendedproperty N'MS_Description', '排序', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'SORT';
EXECUTE sp_addextendedproperty N'MS_Description', 'ACTION', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ACTION_URL';
EXECUTE sp_addextendedproperty N'MS_Description', '命名空间', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'NAMESPACE';
EXECUTE sp_addextendedproperty N'MS_Description', '地址', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'URL';
EXECUTE sp_addextendedproperty N'MS_Description', '打开方式', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'TARGET';
EXECUTE sp_addextendedproperty N'MS_Description', '备注', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'MEMO';
EXECUTE sp_addextendedproperty N'MS_Description', '名称', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '国际化值 存默认名称', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'NAME_DISPLAY';
EXECUTE sp_addextendedproperty N'MS_Description', '编码', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '所属应用名', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'APP';
EXECUTE sp_addextendedproperty N'MS_Description', '是否启用', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ENABLE';
EXECUTE sp_addextendedproperty N'MS_Description', '层级', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'LAY_NO';
EXECUTE sp_addextendedproperty N'MS_Description', '层级结构', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'LAY_REC';
EXECUTE sp_addextendedproperty N'MS_Description', '上级节点ID', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'PARENT_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '层级全路径', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'FULL_PATH';
EXECUTE sp_addextendedproperty N'MS_Description', '层级全路径 菜单名', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'FULL_PATH_NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '来源', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'SOURCE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否修改过 修改过的菜单升级时不修改', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'EDITED';
EXECUTE sp_addextendedproperty N'MS_Description', '资源类型 0是菜单，后续更多的请看枚举类', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否不受权限控制', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'NO_RESTRICT';
EXECUTE sp_addextendedproperty N'MS_Description', '组态期 0，运行期 1', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'STATUS';
EXECUTE sp_addextendedproperty N'MS_Description', '地址', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ROUTE';
EXECUTE sp_addextendedproperty N'MS_Description', '额外信息', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'EXTRA';
create index IDX_MENUINFO_CODE on rbac_menuinfo(CODE);
create index INDEX_MENUINFO_NAME on rbac_menuinfo(NAME);
create index IDX_MENUINFO_MODULECODE on rbac_menuinfo(MODULE_CODE);
create index BASE_MENUINFO_VALID_IDX on rbac_menuinfo(VALID);
ALTER TABLE rbac_menuinfo ADD  DEFAULT 'SELF' FOR TARGET;
ALTER TABLE rbac_menuinfo ADD STATUS int DEFAULT 0 NULL;
ALTER TABLE rbac_menuinfo ADD ROUTE varchar(510) NULL;
ALTER TABLE rbac_menuinfo ADD EXTRA varchar(4000) NULL;
INSERT INTO rbac_menuinfo (ID, VERSION, DELETE_TIME, MODIFY_TIME, CREATE_TIME, TERMINATOR, MODIFIER, CREATOR, VALID, CID, SECURITY_CLASS, ABSOLUTE_HIDDEN, THREE_ROLE, SHOW_TYPE, REQUEST_TYPE, HIDDEN_TYPE, MENU_TYPE, IS_HIDE, GROUP_ONLY, ENTITY_CODE, MODULE_CODE, SYSTEM_DEFAULT, CSS_CLASS, SORT, ACTION_URL, NAMESPACE, URL, TARGET, MEMO, NAME, CODE, LAY_NO, LAY_REC, PARENT_ID, FULL_PATH, SOURCE, LEAF,CREATE_STAFF_ID,MODIFY_STAFF_ID) VALUES (-1, 0, null, null, null, null, null, null, 1, 1000, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '系统默认', 'rbac.MENU_NAME_MENU_LIST', 'menu_list', null, 1, null, null, null, 0,1,1);
--execute sp_rename 'rbac_menuinfo.APP','MODULE_CODE_NO_VERSION';
/*===================================================================*/
/* Table: 标签表(rbac_tag)           */
/*===================================================================*/
create table rbac_tag (
    ID BIGINT NOT NULL ,
    VERSION INT DEFAULT 0 ,
    TYPE VARCHAR(32) DEFAULT NULL,
    NAME VARCHAR(100) NOT NULL,
    CID BIGINT NOT NULL,
    OBJECTID BIGINT NOT NULL ,
    CREATOR VARCHAR(32) DEFAULT NULL ,
    MODIFIER VARCHAR(32) DEFAULT NULL ,
    VALID INT DEFAULT 1 ,
    CREATE_TIME DATETIME ,
    MODIFY_TIME DATETIME ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    PRIMARY KEY (id)
);
EXECUTE sp_addextendedproperty N'MS_Description', '标签表', N'user', N'dbo', N'table', N'rbac_tag', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '标签类型', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '标签名', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'NAME';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', '关联ID', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'OBJECTID';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '逻辑删除,是否有效0:无效 1:有效', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_tag', N'column', N'MODIFY_TIME';
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 1,'管理员角色',1000,1);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 2,'公司管理员角色',1000,2);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 3,'普通用户角色',1000,3);
/*==============================================================*/
/* Table:用户权限表(rbac_userpermission)                */
/*==============================================================*/
CREATE TABLE rbac_userpermission
(
    ID                            BIGINT NOT NULL ,
    VERSION                       INT DEFAULT 0 ,
    USER_ID                       BIGINT ,
    DEAL_STAFF                    BIGINT ,
    CID                           BIGINT ,
    MENUOPERATE_ID                BIGINT ,
    PURVIEW_TYPE                  INT ,
    POSITION_FLAG                 TINYINT DEFAULT 0 ,
    DEPARTMENT_FLAG               TINYINT DEFAULT 0 ,
    GROUP_FLAG                    INT DEFAULT 0,
    ASSIGN_STAFF_FLAG             TINYINT DEFAULT 0 ,
    ASSIGN_POS_FLAG               TINYINT DEFAULT 0 ,
    ASSIGN_DEPT_FLAG             TINYINT DEFAULT 0 ,
    DEALER_PERMISSION_FLAG        TINYINT DEFAULT 0 ,
    NO_RESTRICT_FLAG              TINYINT DEFAULT 0 ,
    ASSIGN_DATAPERMISSION_FLAG    TINYINT DEFAULT 0 ,
    ASSIGN_CUSTOMPERMISSION_FLAG  TINYINT DEFAULT 0 ,
    URL_PATTERN                   VARCHAR(510) ,
    MENUOPERATE_CODE              VARCHAR(510) ,
    DELETE_TIME DATETIME ,
    MODIFY_TIME DATETIME ,
    CREATE_TIME DATETIME ,
    TERMINATOR VARCHAR(32) ,
    MODIFIER VARCHAR(32) ,
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限表', N'user', N'dbo', N'table', N'rbac_userpermission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'USER_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '处理员工ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'DEAL_STAFF';
EXECUTE sp_addextendedproperty N'MS_Description', '公司', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单操作ID', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'MENUOPERATE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '授权方式：角色(0)OR用户(1)', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'PURVIEW_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位限制', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'POSITION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '部门限制', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'DEPARTMENT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '组限制：0 1 2', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'GROUP_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定人员', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ASSIGN_STAFF_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定岗位', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ASSIGN_POS_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定部门', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ASSIGN_DEPT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '处理人权限：0 1', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'DEALER_PERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '无限制', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'NO_RESTRICT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定业务数据权限限制：0 1', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ASSIGN_DATAPERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定自定义权限', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'ASSIGN_CUSTOMPERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', 'URL正则', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'URL_PATTERN';
EXECUTE sp_addextendedproperty N'MS_Description', '冗余操作编码', N'user', N'dbo', N'table', N'rbac_userpermission', N'column', N'MENUOPERATE_CODE';
create index INDEX_UM_MENUOPERATE_ID on rbac_userpermission(MENUOPERATE_ID);
/*==============================================================*/
/* Table:角色权限表(rbac_rolepermission)                */
/*==============================================================*/
CREATE TABLE rbac_rolepermission
(
    ID                            BIGINT NOT NULL,
    CID                           BIGINT,
    VERSION                       INT DEFAULT 0,
    ROLE_ID                       BIGINT ,
    MENUOPERATE_ID                BIGINT ,
    POSITION_FLAG                 TINYINT DEFAULT 0 ,
    DEPARTMENT_FLAG               TINYINT DEFAULT 0 ,
    GROUP_FLAG                    INT DEFAULT 0 ,
    ASSIGN_STAFF_FLAG             TINYINT DEFAULT 0 ,
    ASSIGN_POS_FLAG               TINYINT DEFAULT 0 ,
    ASSIGN_DEPT_FLAG              TINYINT DEFAULT 0 ,
    DEALER_PERMISSION_FLAG        TINYINT DEFAULT 0 ,
    NO_RESTRICT_FLAG              TINYINT DEFAULT 0 ,
    ASSIGN_DATAPERMISSION_FLAG    TINYINT DEFAULT 0 ,
    ASSIGN_CUSTOMPERMISSION_FLAG  TINYINT DEFAULT 0 ,
    URL_PATTERN                   VARCHAR(510) ,
    DELETE_TIME DATETIME ,
    MODIFY_TIME DATETIME ,
    CREATE_TIME DATETIME ,
    TERMINATOR VARCHAR(32) ,
    MODIFIER VARCHAR(32) ,
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限表', N'user', N'dbo', N'table', N'rbac_rolepermission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '删除者', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'TERMINATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '角色ID', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ROLE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单操作ID', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'MENUOPERATE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位限制', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'POSITION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '部门限制', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'DEPARTMENT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '组限制：0 1 2', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'GROUP_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定人员', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ASSIGN_STAFF_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定岗位', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ASSIGN_POS_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定部门', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ASSIGN_DEPT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '处理人权限：0 1', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'DEALER_PERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '无限制', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'NO_RESTRICT_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定业务数据权限限制：0 1', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ASSIGN_DATAPERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '指定自定义权限', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'ASSIGN_CUSTOMPERMISSION_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', 'URL正则', N'user', N'dbo', N'table', N'rbac_rolepermission', N'column', N'URL_PATTERN';
create index INDEX_RM_ROLE_ID on rbac_rolepermission(ROLE_ID);
create index INDEX_RM_MENUOPERATE_ID on rbac_rolepermission(MENUOPERATE_ID);
/*==============================================================*/
/* Table:用户指定岗位表(rbac_userpposition)                */
/*==============================================================*/
CREATE TABLE rbac_userpposition
(
    ID                BIGINT NOT NULL,
    VERSION           INT DEFAULT 0,
    INCLUDE_LOWER     TINYINT DEFAULT 0,
    POSITION_ID       BIGINT ,
    USERPERMISSION_ID BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '用户指定岗位表', N'user', N'dbo', N'table', N'rbac_userpposition', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_userpposition', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_userpposition', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '包含下级', N'user', N'dbo', N'table', N'rbac_userpposition', N'column', N'INCLUDE_LOWER';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位ID', N'user', N'dbo', N'table', N'rbac_userpposition', N'column', N'POSITION_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限ID', N'user', N'dbo', N'table', N'rbac_userpposition', N'column', N'USERPERMISSION_ID';
create index INDEX_USERPPOSITION_UP_ID on rbac_userpposition(USERPERMISSION_ID);
/*==============================================================*/
/* Table:用户指定部门表(rbac_userpdepartment)                */
/*==============================================================*/
CREATE TABLE rbac_userpdepartment
(
    ID                BIGINT NOT NULL ,
    VERSION           INT DEFAULT 0 ,
    INCLUDE_LOWER     TINYINT DEFAULT 0,
    DEPARTMENT_ID       BIGINT,
    USERPERMISSION_ID BIGINT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '用户指定部门表', N'user', N'dbo', N'table', N'rbac_userpdepartment', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_userpdepartment', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_userpdepartment', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '包含下级', N'user', N'dbo', N'table', N'rbac_userpdepartment', N'column', N'INCLUDE_LOWER';
EXECUTE sp_addextendedproperty N'MS_Description', '部门ID', N'user', N'dbo', N'table', N'rbac_userpdepartment', N'column', N'DEPARTMENT_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限ID', N'user', N'dbo', N'table', N'rbac_userpdepartment', N'column', N'USERPERMISSION_ID';
create index INDEX_USERPDEPARTMENT_UP_ID on rbac_userpdepartment(USERPERMISSION_ID);
/*==============================================================*/
/* Table:用户指定人员(rbac_userpstaff)                */
/*==============================================================*/
CREATE TABLE rbac_userpstaff
(
    ID                BIGINT NOT NULL,
    VERSION           INT DEFAULT 0 ,
    STAFF_ID          BIGINT,
    USERPERMISSION_ID BIGINT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '用户指定人员', N'user', N'dbo', N'table', N'rbac_userpstaff', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_userpstaff', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_userpstaff', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '人员ID', N'user', N'dbo', N'table', N'rbac_userpstaff', N'column', N'STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限ID', N'user', N'dbo', N'table', N'rbac_userpstaff', N'column', N'USERPERMISSION_ID';
/*==============================================================*/
/* Table:角色指定岗位表(rbac_rolepposition)                */
/*==============================================================*/
CREATE TABLE rbac_rolepposition
(
    ID                BIGINT NOT NULL ,
    VERSION           INT DEFAULT 0 ,
    INCLUDE_LOWER     TINYINT DEFAULT 0,
    POSITION_ID       BIGINT ,
    ROLEPERMISSION_ID BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色指定岗位表', N'user', N'dbo', N'table', N'rbac_rolepposition', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_rolepposition', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_rolepposition', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '包含下级', N'user', N'dbo', N'table', N'rbac_rolepposition', N'column', N'INCLUDE_LOWER';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位ID', N'user', N'dbo', N'table', N'rbac_rolepposition', N'column', N'POSITION_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限ID', N'user', N'dbo', N'table', N'rbac_rolepposition', N'column', N'ROLEPERMISSION_ID';
create index INDEX_ROLEPPOSITION_UP_ID on rbac_rolepposition(ROLEPERMISSION_ID);
/*==============================================================*/
/* Table:角色指定部门表(rbac_rolepdepartment)                */
/*==============================================================*/
CREATE TABLE rbac_rolepdepartment
(
    ID                BIGINT NOT NULL,
    VERSION           INT DEFAULT 0,
    INCLUDE_LOWER     TINYINT DEFAULT 0,
    DEPARTMENT_ID       BIGINT,
    ROLEPERMISSION_ID BIGINT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色指定部门表', N'user', N'dbo', N'table', N'rbac_rolepdepartment', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_rolepdepartment', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_rolepdepartment', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '包含下级', N'user', N'dbo', N'table', N'rbac_rolepdepartment', N'column', N'INCLUDE_LOWER';
EXECUTE sp_addextendedproperty N'MS_Description', '部门ID', N'user', N'dbo', N'table', N'rbac_rolepdepartment', N'column', N'DEPARTMENT_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限ID', N'user', N'dbo', N'table', N'rbac_rolepdepartment', N'column', N'ROLEPERMISSION_ID';
create index INDEX_ROLEPDEPARTMENT_UP_ID on rbac_rolepdepartment(ROLEPERMISSION_ID);
/*==============================================================*/
/* Table:角色指定人员(rbac_rolepstaff)                */
/*==============================================================*/
CREATE TABLE rbac_rolepstaff
(
    ID                BIGINT NOT NULL ,
    VERSION           INT DEFAULT 0 ,
    STAFF_ID          BIGINT ,
    ROLEPERMISSION_ID BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色指定人员', N'user', N'dbo', N'table', N'rbac_rolepstaff', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_rolepstaff', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_rolepstaff', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '人员ID', N'user', N'dbo', N'table', N'rbac_rolepstaff', N'column', N'STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限ID', N'user', N'dbo', N'table', N'rbac_rolepstaff', N'column', N'ROLEPERMISSION_ID';
/*==============================================================*/
/* Table:业务数据权限定义表(rbac_data_permission)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission
(
    CODE              VARCHAR(1000) NOT NULL,
    EC_ENV            VARCHAR(510) ,
    VERSION           INT DEFAULT 0 ,
    VALID             INT DEFAULT 1,
    DELETE_TIME       DATETIME ,
    MODIFY_TIME       DATETIME ,
    CREATE_TIME       DATETIME ,
    TERMINATOR        VARCHAR(32) ,
    MODIFIER          VARCHAR(32) ,
    CREATOR           VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT,
    ENTITY_CODE       VARCHAR(510),
    MODULE_CODE       VARCHAR(510),
    ORDER_NO          DECIMAL(38) ,
    PROPERTY_CODE     VARCHAR(510) ,
    REF_VIEW_CODE     VARCHAR(510) ,
    IS_TREE           TINYINT DEFAULT 0,
    TARGET_MODEL_CODE VARCHAR(510) ,
    TYPE              VARCHAR(510) ,
    RELATION          VARCHAR(510) ,
    RANK              INT ,
    MODEL_CODE        VARCHAR(510) ,
    PRIMARY KEY(CODE)
);
EXECUTE sp_addextendedproperty N'MS_Description', '业务数据权限定义表', N'user', N'dbo', N'table', N'rbac_data_permission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '模式DEV或PRODUCT 默认PRODUCT', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'EC_ENV';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '实体编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'ENTITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '模块编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'MODULE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '顺序', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'ORDER_NO';
EXECUTE sp_addextendedproperty N'MS_Description', '字段编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'PROPERTY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '关联的参照视图编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'REF_VIEW_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '是否树结构', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'IS_TREE';
EXECUTE sp_addextendedproperty N'MS_Description', '关联模型编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'TARGET_MODEL_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '类型', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '关系', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'RELATION';
EXECUTE sp_addextendedproperty N'MS_Description', '等级', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'RANK';
EXECUTE sp_addextendedproperty N'MS_Description', '模型编码', N'user', N'dbo', N'table', N'rbac_data_permission', N'column', N'MODEL_CODE';
create index DATA_P_VALID_IDX on rbac_data_permission(VALID);
/*==============================================================*/
/* Table:业务数据权限用户权限配置临时表(rbac_data_permission_ushow)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission_ushow
(
    ID                      BIGINT NOT NULL ,
    VERSION                 INT DEFAULT 0,
    DELETE_TIME             DATETIME ,
    MODIFY_TIME             DATETIME ,
    CREATE_TIME             DATETIME ,
    TERMINATOR              VARCHAR(32) ,
    MODIFIER                VARCHAR(32) ,
    CREATOR                 VARCHAR(32) ,
    IS_ASSIGNED             TINYINT DEFAULT 0,
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          INT ,
    OPERATE_ID              BIGINT ,
    VALUE_CODE              VARCHAR(510) ,
    VALUE_TITLE             VARCHAR(510) ,
    VALUE_ID                VARCHAR(510) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    USER_ID                 BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '业务数据权限用户权限配置临时表', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否启用', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'IS_ASSIGNED';
EXECUTE sp_addextendedproperty N'MS_Description', '是否包含上下级', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'IS_INCLUDE_SUB';
EXECUTE sp_addextendedproperty N'MS_Description', '操作ID', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'OPERATE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '编码值', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'VALUE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '标题值', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'VALUE_TITLE';
EXECUTE sp_addextendedproperty N'MS_Description', 'ID值', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'VALUE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '关联特殊权限', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'DATA_PERMISSION_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '关联用户ID', N'user', N'dbo', N'table', N'rbac_data_permission_ushow', N'column', N'USER_ID';
/*==============================================================*/
/* Table:业务数据权限角色权限配置临时表(rbac_data_permission_rshow)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission_rshow
(
    ID                      BIGINT NOT NULL,
    VERSION                 INT DEFAULT 0,
    DELETE_TIME             DATETIME ,
    MODIFY_TIME             DATETIME ,
    CREATE_TIME             DATETIME ,
    DELETE_STAFF_ID         VARCHAR(32) ,
    MODIFY_STAFF_ID         VARCHAR(32) ,
    CREATE_STAFF_ID         VARCHAR(32) ,
    IS_ASSIGNED             TINYINT DEFAULT 0,
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          INT ,
    OPERATE_ID              BIGINT ,
    VALUE_CODE              VARCHAR(510) ,
    VALUE_TITLE             VARCHAR(510) ,
    VALUE_ID                VARCHAR(510) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    ROLE_ID                 BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '业务数据权限角色权限配置临时表', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否启用', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'IS_ASSIGNED';
EXECUTE sp_addextendedproperty N'MS_Description', '是否包含上下级', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'IS_INCLUDE_SUB';
EXECUTE sp_addextendedproperty N'MS_Description', '操作ID', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'OPERATE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '编码值', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'VALUE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '标题值', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'VALUE_TITLE';
EXECUTE sp_addextendedproperty N'MS_Description', 'ID值', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'VALUE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '关联特殊权限', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'DATA_PERMISSION_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '关联角色id', N'user', N'dbo', N'table', N'rbac_data_permission_rshow', N'column', N'ROLE_ID';
/*==============================================================*/
/* Table:业务数据权限用户关联表(rbac_user_datapermission)                */
/*==============================================================*/
CREATE TABLE rbac_user_datapermission
(
    ID                      BIGINT NOT NULL,
    VERSION                 INT DEFAULT 0 ,
    CONFIG_STRING           VARCHAR(8000) ,
    CONTENT                 VARCHAR(4000) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    USERPERMISSION_ID       BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '业务数据权限用户关联表', N'user', N'dbo', N'table', N'rbac_user_datapermission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_user_datapermission', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_user_datapermission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '配置内容', N'user', N'dbo', N'table', N'rbac_user_datapermission', N'column', N'CONFIG_STRING';
EXECUTE sp_addextendedproperty N'MS_Description', 'SQL内容', N'user', N'dbo', N'table', N'rbac_user_datapermission', N'column', N'CONTENT';
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限ID', N'user', N'dbo', N'table', N'rbac_user_datapermission', N'column', N'USERPERMISSION_ID';
/*==============================================================*/
/* Table:业务数据权限角色关联表(rbac_role_datapermission)                */
/*==============================================================*/
CREATE TABLE rbac_role_datapermission
(
    ID                      BIGINT NOT NULL ,
    VERSION                 INT DEFAULT 0 ,
    CONFIG_STRING           VARCHAR(8000),
    CONTENT                 VARCHAR(4000) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    ROLEPERMISSION_ID       BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '业务数据权限角色关联表', N'user', N'dbo', N'table', N'rbac_role_datapermission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_role_datapermission', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_role_datapermission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '配置内容', N'user', N'dbo', N'table', N'rbac_role_datapermission', N'column', N'CONFIG_STRING';
EXECUTE sp_addextendedproperty N'MS_Description', 'SQL内容', N'user', N'dbo', N'table', N'rbac_role_datapermission', N'column', N'CONTENT';
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限ID', N'user', N'dbo', N'table', N'rbac_role_datapermission', N'column', N'ROLEPERMISSION_ID';
/*==============================================================*/
/* Table:自定义权限表(rbac_custom_permission)                */
/*==============================================================*/
CREATE TABLE rbac_custom_permission
(
    CODE              VARCHAR(1000) NOT NULL ,
    EC_ENV            VARCHAR(510) ,
    VERSION           INT DEFAULT 0 ,
    DELETE_TIME       DATETIME ,
    MODIFY_TIME       DATETIME ,
    CREATE_TIME       DATETIME ,
    TERMINATOR        VARCHAR(32) ,
    MODIFIER          VARCHAR(32) ,
    CREATOR           VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    VALID             INT DEFAULT 1 ,
    ENTITY_CODE       VARCHAR(510) ,
    MODULE_CODE       VARCHAR(510) ,
    MEMO              VARCHAR(510) ,
    TITLE             VARCHAR(510),
    CONDITION_SQL     VARCHAR(4000),
    JSON_CONDITION    VARCHAR(4000) ,
    VIEW_CODE         VARCHAR(510) ,
    HAND_WRITING_FLAG INT DEFAULT 0 ,
    PRIMARY KEY(CODE)
);
EXECUTE sp_addextendedproperty N'MS_Description', '自定义权限表', N'user', N'dbo', N'table', N'rbac_custom_permission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '编码', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '模式DEV或PRODUCT 默认PRODUCT', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'EC_ENV';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效数据', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '实体编码', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'ENTITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '模块编码', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'MODULE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '备注', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'MEMO';
EXECUTE sp_addextendedproperty N'MS_Description', '标题', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'TITLE';
EXECUTE sp_addextendedproperty N'MS_Description', '条件SQL', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'CONDITION_SQL';
EXECUTE sp_addextendedproperty N'MS_Description', 'JSON条件', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'JSON_CONDITION';
EXECUTE sp_addextendedproperty N'MS_Description', '视图编码', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'VIEW_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '手写自定义权限', N'user', N'dbo', N'table', N'rbac_custom_permission', N'column', N'HAND_WRITING_FLAG';
create index CUSTOM_P_VALID_IDX on rbac_custom_permission(VALID);
/*==============================================================*/
/* Table:自定义权限用户关联表(rbac_user_custompermission_ref)                */
/*==============================================================*/
CREATE TABLE rbac_user_custompermission_ref
(
    ID                      BIGINT NOT NULL ,
    VERSION                 INT DEFAULT 0 ,
    CUSTOM_PERMISSION_CODE  VARCHAR(510) ,
    USERPERMISSION_ID       BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '自定义权限用户关联表', N'user', N'dbo', N'table', N'rbac_user_custompermission_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_user_custompermission_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_user_custompermission_ref', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '其他限制编码', N'user', N'dbo', N'table', N'rbac_user_custompermission_ref', N'column', N'CUSTOM_PERMISSION_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '用户权限ID', N'user', N'dbo', N'table', N'rbac_user_custompermission_ref', N'column', N'USERPERMISSION_ID';
/*==============================================================*/
/* Table:自定义权限角色关联表(rbac_role_custompermission_ref)                */
/*==============================================================*/
CREATE TABLE rbac_role_custompermission_ref
(
    ID                  BIGINT NOT NULL ,
    VERSION             INT DEFAULT 0 ,
    CUSTOM_PERMISSION_CODE VARCHAR(510) ,
    ROLEPERMISSION_ID   BIGINT ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '自定义权限角色关联表', N'user', N'dbo', N'table', N'rbac_role_custompermission_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_role_custompermission_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_role_custompermission_ref', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '其他限制编码', N'user', N'dbo', N'table', N'rbac_role_custompermission_ref', N'column', N'CUSTOM_PERMISSION_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '角色权限ID', N'user', N'dbo', N'table', N'rbac_role_custompermission_ref', N'column', N'ROLEPERMISSION_ID';
/*==============================================================*/
/* Table:工作流数据权限表(rbac_flow_permission)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission
(
    ID                   BIGINT NOT NULL,
    VERSION              INT DEFAULT 0,
    DELETE_TIME          DATETIME ,
    MODIFY_TIME          DATETIME ,
    CREATE_TIME          DATETIME ,
    TERMINATOR           VARCHAR(32) ,
    MODIFIER             VARCHAR(32) ,
    CREATOR              VARCHAR(32) ,
    CREATE_STAFF_ID BIGINT  ,
    MODIFY_STAFF_ID BIGINT  ,
    ENTITY_CODE          VARCHAR(510) ,
    PURVIEW_DISTRIBUTION INT,
    PURVIEW_STATE        INT,
    MEMO                 VARCHAR(510) ,
    UNLIMITED_POWER      TINYINT DEFAULT 0 ,
    GROUP_POWER_FLAG     INT ,
    ASSIGN_STAFF_FLAG    TINYINT DEFAULT 0,
    ASSIGN_POS_FLAG      TINYINT DEFAULT 0,
    POSITION_POWER_FLAG  TINYINT DEFAULT 0 ,
    FLOW_PERMISSION_TYPE VARCHAR(510) ,
    TYPE_ID              BIGINT ,
    ACTIVITY_CODE        VARCHAR(510) ,
    FLOW_VERSION         VARCHAR(510) ,
    FLOW_KEY             VARCHAR(510) ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '工作流数据权限表', N'user', N'dbo', N'table', N'rbac_flow_permission', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'MODIFIER';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'CREATOR';
EXECUTE sp_addextendedproperty N'MS_Description', '创建时间', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'CREATE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '修改时间', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'MODIFY_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '删除时间', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'DELETE_TIME';
EXECUTE sp_addextendedproperty N'MS_Description', '创建者人员ID', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'CREATE_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '修改者人员ID', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'MODIFY_STAFF_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '是否有效数据', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'VALID';
EXECUTE sp_addextendedproperty N'MS_Description', '实体编码', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'ENTITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '权限分配来源，3工作流分配的权限', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'PURVIEW_DISTRIBUTION';
EXECUTE sp_addextendedproperty N'MS_Description', '权限的来源：1流程,2开始活动', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'PURVIEW_STATE';
EXECUTE sp_addextendedproperty N'MS_Description', '备注', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'MEMO';
EXECUTE sp_addextendedproperty N'MS_Description', '无限制', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'UNLIMITED_POWER';
EXECUTE sp_addextendedproperty N'MS_Description', '组限制 0无组限制，1仅组员可见2仅组长可见', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'GROUP_POWER_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '1:指定人员限制', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'ASSIGN_STAFF_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '1:指定岗位限制', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'ASSIGN_POS_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '1:岗位限制 ', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'POSITION_POWER_FLAG';
EXECUTE sp_addextendedproperty N'MS_Description', '数据类型 枚举：USER,ROLE,WORKGROUP,DEPTMENT,POSITION', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'FLOW_PERMISSION_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '根据数据权限类型对应的ID：// USERID，GROUPID，ROLEID，DEPTID，POSITIONID；', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'TYPE_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '活动编码', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'ACTIVITY_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '流程版本', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'FLOW_VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '流程KEY', N'user', N'dbo', N'table', N'rbac_flow_permission', N'column', N'FLOW_KEY';
create index IDX_DP_TYPEID on rbac_flow_permission(TYPE_ID);
create index IDX_DP_DPTYPE on rbac_flow_permission(FLOW_PERMISSION_TYPE);
create index IDX_DP_ENTITYCODE on rbac_flow_permission(ENTITY_CODE);
/*==============================================================*/
/* Table:工作流数据权限员工关联表(rbac_flow_permission_staff)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission_staff
(
    ID                   BIGINT NOT NULL,
    VERSION              INT DEFAULT 0,
    staff_id             BIGINT,
    flowpermission_id    BIGINT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '工作流数据权限员工关联表', N'user', N'dbo', N'table', N'rbac_flow_permission_staff', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_flow_permission_staff', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '人员ID', N'user', N'dbo', N'table', N'rbac_flow_permission_staff', N'column', N'staff_id';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_flow_permission_staff', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '权限ID', N'user', N'dbo', N'table', N'rbac_flow_permission_staff', N'column', N'flowpermission_id';
/*==============================================================*/
/* Table:工作流数据权限岗位关联表(rbac_flow_permission_position)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission_position
(
    ID                   BIGINT NOT NULL,
    VERSION              INT DEFAULT 0,
    position_id          BIGINT,
    include_lower        tinyint DEFAULT 0,
    flowpermission_id    BIGINT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '工作流数据权限岗位关联表', N'user', N'dbo', N'table', N'rbac_flow_permission_position', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_flow_permission_position', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位ID', N'user', N'dbo', N'table', N'rbac_flow_permission_position', N'column', N'position_id';
EXECUTE sp_addextendedproperty N'MS_Description', '是否包含下级', N'user', N'dbo', N'table', N'rbac_flow_permission_position', N'column', N'include_lower';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_flow_permission_position', N'column', N'VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '权限ID', N'user', N'dbo', N'table', N'rbac_flow_permission_position', N'column', N'flowpermission_id';
/*==============================================================*/
/* Table:菜单操作编码URL关联表(rbac_menuOperateCode_url_ref)                */
/*==============================================================*/
CREATE TABLE rbac_menuOperateCode_url_ref
(
    ID                  BIGINT NOT NULL ,
    MENUOPERATE_CODE    VARCHAR(510) ,
    METHOD_TYPE         INT ,
    URL                 VARCHAR(510) ,
    APP                 VARCHAR(32),
    IS_CUSTOM           TINYINT DEFAULT 0,
    REG_MATCH           TINYINT DEFAULT 0 ,
    IMPORT_TYPE         INT DEFAULT 0,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单操作编码URL关联表', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单操作编码', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'MENUOPERATE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '请求方法，0 GET,1 POST,2 PUT,3 DELETE', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'METHOD_TYPE';
EXECUTE sp_addextendedproperty N'MS_Description', '对应URL', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'URL';
EXECUTE sp_addextendedproperty N'MS_Description', '应用名', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'APP';
EXECUTE sp_addextendedproperty N'MS_Description', '是否自定义操作', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'IS_CUSTOM';
EXECUTE sp_addextendedproperty N'MS_Description', '是否需要正则匹配', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'REG_MATCH';
EXECUTE sp_addextendedproperty N'MS_Description', '导入类型 0 注解导入、1 json文件导入', N'user', N'dbo', N'table', N'rbac_menuOperateCode_url_ref', N'column', N'IMPORT_TYPE';
create index MENU_OPERATE_CODE_IDX on rbac_menuOperateCode_url_ref(MENUOPERATE_CODE);
/*==============================================================*/
/* Table:用户与请求URL关联表(rbac_user_url_ref)                */
/*==============================================================*/
CREATE TABLE rbac_user_url_ref
(
    ID                  BIGINT NOT NULL,
    USER_ID             BIGINT ,
    URL                 VARCHAR(510) ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '用户与请求URL关联表', N'user', N'dbo', N'table', N'rbac_user_url_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_user_url_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '用户ID', N'user', N'dbo', N'table', N'rbac_user_url_ref', N'column', N'USER_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '对应URL', N'user', N'dbo', N'table', N'rbac_user_url_ref', N'column', N'URL';
create index USER_ID_IDX on rbac_user_url_ref(USER_ID);
/*==============================================================*/
/* Table:菜单公司关联表(rbac_menuinfo_company_ref)                */
/*==============================================================*/
CREATE TABLE rbac_menuinfo_company_ref
(
    ID                  BIGINT NOT NULL ,
    MENUINFO_ID         BIGINT ,
    COMPANY_ID          BIGINT ,
    COMPANY_NAME        VARCHAR(510),
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单公司关联表', N'user', N'dbo', N'table', N'rbac_menuinfo_company_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menuinfo_company_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单ID', N'user', N'dbo', N'table', N'rbac_menuinfo_company_ref', N'column', N'MENUINFO_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_menuinfo_company_ref', N'column', N'COMPANY_ID';
EXECUTE sp_addextendedproperty N'MS_Description', '公司名', N'user', N'dbo', N'table', N'rbac_menuinfo_company_ref', N'column', N'COMPANY_NAME';
/*==============================================================*/
/* Table:权限模块初始化版本信息表(rbac_init_verison_info)                */
/*==============================================================*/
CREATE TABLE rbac_init_verison_info
(
    ID                  BIGINT NOT NULL ,
    INIT_VERSION        INT ,
    APP                 VARCHAR(510) ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '权限模块初始化版本信息表', N'user', N'dbo', N'table', N'rbac_init_verison_info', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_init_verison_info', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '初始化脚本版本', N'user', N'dbo', N'table', N'rbac_init_verison_info', N'column', N'INIT_VERSION';
EXECUTE sp_addextendedproperty N'MS_Description', '服务名', N'user', N'dbo', N'table', N'rbac_init_verison_info', N'column', N'APP';
INSERT INTO rbac_init_verison_info (ID,INIT_VERSION) VALUES (1,1);
/*==============================================================*/
/* Table:菜单助记码(rbac_menu_mnecode)                            */
/*==============================================================*/
CREATE TABLE rbac_menu_mnecode
(
    ID                  BIGINT NOT NULL ,
    row_version         BIGINT DEFAULT 0 NOT NULL,
    LANGUAGE            VARCHAR(510) ,
    MENU_INFO           BIGINT ,
    MNE_CODE            VARCHAR(510) ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单助记码', N'user', N'dbo', N'table', N'rbac_menu_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menu_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_menu_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'rbac_menu_mnecode', N'column', N'LANGUAGE';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'rbac_menu_mnecode', N'column', N'MNE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单ID', N'user', N'dbo', N'table', N'rbac_menu_mnecode', N'column', N'MENU_INFO';
/*==============================================================*/
/* Table:角色助记码(rbac_role_mnecode)                            */
/*==============================================================*/
CREATE TABLE rbac_role_mnecode
(
    ID                  BIGINT NOT NULL ,
    row_version         BIGINT DEFAULT 0 NOT NULL,
    ROLE                BIGINT ,
    MNE_CODE            VARCHAR(510) ,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '角色助记码', N'user', N'dbo', N'table', N'rbac_role_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_role_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'rbac_role_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'rbac_role_mnecode', N'column', N'MNE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '角色ID', N'user', N'dbo', N'table', N'rbac_role_mnecode', N'column', N'ROLE';
/*==============================================================*/
/* Table:菜单数据回滚表(rbac_menu_temp)                */
/*==============================================================*/
CREATE TABLE rbac_menu_temp
(
    ID                  BIGINT NOT NULL ,
    UUID                VARCHAR(200) ,
    OLD_DATA            TEXT,
    NEW_DATA            TEXT,
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单数据回滚表', N'user', N'dbo', N'table', N'rbac_menu_temp', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menu_temp', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '回滚标识', N'user', N'dbo', N'table', N'rbac_menu_temp', N'column', N'UUID';
EXECUTE sp_addextendedproperty N'MS_Description', '老数据', N'user', N'dbo', N'table', N'rbac_menu_temp', N'column', N'OLD_DATA';
EXECUTE sp_addextendedproperty N'MS_Description', '新数据', N'user', N'dbo', N'table', N'rbac_menu_temp', N'column', N'NEW_DATA';

DROP VIEW "BASE_DATAPERMISSION";
CREATE VIEW "BASE_DATAPERMISSION" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "ENTITY_CODE", "PURVIEW_DISTRIBUTION", "PURVIEW_STATE", "MEMO", "UNLIMITED_POWER", "GROUP_POWER_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "POSITION_POWER_FLAG", "DATA_PERMISSION_TYPE", "TYPE_ID", "ACTIVITY_CODE", "FLOW_VERSION", "FLOW_KEY") AS
  ( select ID as id, VERSION as version, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time, 1 as valid, ENTITY_CODE as entity_code, PURVIEW_DISTRIBUTION as purview_distribution, PURVIEW_STATE as purview_state, MEMO as memo, UNLIMITED_POWER as unlimited_power, GROUP_POWER_FLAG as group_power_flag, ASSIGN_STAFF_FLAG as assign_staff_flag, ASSIGN_POS_FLAG as assign_pos_flag, POSITION_POWER_FLAG as position_power_flag, FLOW_PERMISSION_TYPE as data_permission_type, TYPE_ID as type_id, ACTIVITY_CODE as activity_code, FLOW_VERSION as flow_version, FLOW_KEY as flow_key from rbac_flow_permission );

DROP VIEW "BASE_DATAPERMISSIONSTAFF";
CREATE VIEW "BASE_DATAPERMISSIONSTAFF" ("ID", "VERSION", "STAFF_ID", "DATAPERMISSION_ID") AS
  ( select id, version, staff_id, FLOWPERMISSION_ID from rbac_flow_permission_staff );

DROP VIEW "BASE_DATAPMSPOSITION";
CREATE VIEW "BASE_DATAPMSPOSITION" ("ID", "VERSION", "POSITION_ID", "INCLUDE_LOWER", "DATAPERMISSION_ID") AS
  ( select id, version, position_id, include_lower, FLOWPERMISSION_ID from rbac_flow_permission_position );

DROP VIEW "BASE_MENUINFO";
CREATE VIEW "BASE_MENUINFO" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "CID", "SECURITY_CLASS", "ABSOLUTE_HIDDEN", "THREE_ROLE", "SHOW_TYPE", "REQUEST_TYPE", "HIDDEN_TYPE", "MENU_TYPE", "IS_HIDE", "GROUP_ONLY", "ENTITY_CODE", "EC_ENTITY_CODE", "MODULE_CODE", "SYSTEM_DEFAULT", "CSS_CLASS", "SORT", "NAMESPACE", "URL", "TARGET", "MEMO", "NAME", "NAME_ZH_CN", "CODE", "LAY_NO", "LAY_REC", "PARENT_ID", "FULL_PATH_NAME", "ACTION", "LEAF", "ST_FLAG", "ST_DIGITALSIGNATURE", "ST_INTRO", "ST_TABTYPEID", "ST_TYPE", "REMOTE_USER_NAME_NAMED", "REMOTE_PASSWORD_NAMED", "REMOTE_ID", "PIMS_MENU_TYPE", "MODULE", "ICON_URL","STATUS") AS
  (
SELECT
	ID AS id,
	VERSION AS version,
	create_staff_id,
	modify_staff_id,
	NULL AS delete_staff_id,
	CREATE_TIME AS create_time,
	MODIFY_TIME AS modify_time,
	DELETE_TIME AS delete_time,
	VALID AS valid,
	CID AS cid,
	SECURITY_CLASS AS security_class,
	ABSOLUTE_HIDDEN AS absolute_hidden,
	THREE_ROLE AS three_role,
	SHOW_TYPE AS show_type,
	REQUEST_TYPE AS request_type,
	HIDDEN_TYPE AS hidden_type,
	MENU_TYPE AS menu_type,
	IS_HIDE AS is_hide,
	GROUP_ONLY AS group_only,
	ENTITY_CODE AS entity_code,
	ENTITY_CODE AS ec_entity_code,
	MODULE_CODE AS module_code,
	SYSTEM_DEFAULT AS system_default,
	CSS_CLASS AS css_class,
	SORT AS sort,
	NAMESPACE AS namespace,
	URL AS url,
	TARGET AS target,
	MEMO AS memo,
	NAME AS name,
	NAME_DISPLAY AS name_zh_cn,
	CODE AS code,
	LAY_NO AS lay_no,
	LAY_REC AS lay_rec,
	PARENT_ID AS parent_id,
	full_path_name AS full_path_name,
	ACTION_URL AS ACTION,
	LEAF AS leaf,
	NULL AS ST_FLAG,
	NULL AS ST_DIGITALSIGNATURE,
	NULL AS ST_INTRO,
	NULL AS ST_TABTYPEID,
	NULL AS ST_TYPE,
	NULL AS REMOTE_USER_NAME_NAMED,
	NULL AS REMOTE_PASSWORD_NAMED,
	NULL AS REMOTE_ID,
	NULL AS PIMS_MENU_TYPE,
	NULL AS MODULE,
	NULL AS ICON_URL,
	STATUS AS status
FROM
	rbac_menuinfo );

DROP VIEW "BASE_MENUOPERATE";
CREATE VIEW "BASE_MENUOPERATE" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "CID", "IS_ALLOW_PROXY", "IS_HIDDEN", "THREE_ROLE", "VIEW_CODE", "IS_QUERY", "IS_ORRELATION", "FOR_DATA_PERMISSION", "ENABLE_NORESTRICT", "ENABLE_OTHERRESTRICT", "ENABLE_SPECIALPERMISSION", "ENABLE_DEALERPERMISSION", "ENABLE_ASSIGNSTAFF", "ENABLE_ASSIGNPOS", "ENABLE_POSRESTRICT", "ENABLE_GROUPRESTRICT", "ENTITY_CODE", "IGNORE_PERMISSION", "POWER_FLAG", "FLOW_VERSION", "FLOW_KEY", "MSG_ASSEMBLED", "DEPLOYMENT_ID", "TYPE", "MENUINFO_ID", "ICON_CLS", "MODULE", "SORT", "MEMO", "TARGET", "NAMESPACE", "URL", "NAME_ZH_CN", "NAME", "CODE", "ACTION", "ST_FLAG", "ST_TYPE", "ST_TABLECODE", "ST_SHOWSTYLE", "ST_OPERATETYPE", "ST_ISVIEW", "ST_ISS2FLOWOPERATE", "ST_ISMAINQUERY", "ST_ISDEFAULT", "ST_FLOWKEY", "ST_DIGITALSIGNATURE", "ST_DEFAULTDISPLAY", "ST_ACTIVITYID", "MENUOPERATE_MAINOPERATECODE", "MENUOPERATE_ISCONTAINER", "MENUOPERATE_ENTRYOPERATECODE","MENUOPERATETYPE") AS
  (
SELECT
	ID AS id,
	ROW_VERSION AS version,
	create_staff_id,
	modify_staff_id,
	NULL AS delete_staff_id,
	CREATE_TIME AS create_time,
	MODIFY_TIME AS modify_time,
	DELETE_TIME AS delete_time,
	VALID AS valid,
	CID AS cid,
	IS_ALLOW_PROXY AS is_allow_proxy,
	IS_HIDDEN AS is_hidden,
	THREE_ROLE AS three_role,
	VIEW_CODE AS view_code,
	IS_QUERY AS is_query,
	IS_ORRELATION AS is_orrelation,
	FOR_FLOW_PERMISSION AS for_data_permission,
	ENABLE_NORESTRICT AS enable_norestrict,
	ENABLE_CUSTOMPERMISSION AS enable_otherrestrict,
	ENABLE_DATAPERMISSION AS enable_specialpermission,
	ENABLE_DEALERPERMISSION AS enable_dealerpermission,
	ENABLE_ASSIGNSTAFF AS enable_assignstaff,
	ENABLE_ASSIGNPOS AS enable_assignpos,
	ENABLE_POSRESTRICT AS enable_posrestrict,
	ENABLE_GROUPRESTRICT AS enable_grouprestrict,
	ENTITY_CODE AS entity_code,
	IGNORE_PERMISSION AS ignore_permission,
	POWER_FLAG AS power_flag,
	FLOW_VERSION AS flow_version,
	FLOW_KEY AS flow_key,
	MSG_ASSEMBLED AS msg_assembled,
	DEPLOYMENT_ID AS deployment_id,
	MENUOPERATETYPE AS TYPE,
	MENUINFO_ID AS menuinfo_id,
	ICON_CLS AS icon_cls,
	MODULE_CODE AS module,
	SORT AS sort,
	MEMO AS memo,
	TARGET AS target,
	NAMESPACE AS namespace,
	URL AS url,
	NAME_ZH_CN AS name_zh_cn,
	NAME AS name,
	CODE AS code,
	ACTION_URL AS ACTION,
	NULL AS ST_FLAG,
	NULL AS ST_TYPE,
	NULL AS ST_TABLECODE,
	NULL AS ST_SHOWSTYLE,
	NULL AS ST_OPERATETYPE,
	NULL AS ST_ISVIEW,
	NULL AS ST_ISS2FLOWOPERATE,
	NULL AS ST_ISMAINQUERY,
	NULL AS ST_ISDEFAULT,
	NULL AS ST_FLOWKEY,
	NULL AS ST_DIGITALSIGNATURE,
	NULL AS ST_DEFAULTDISPLAY,
	NULL AS ST_ACTIVITYID,
	NULL AS MENUOPERATE_MAINOPERATECODE,
	NULL AS MENUOPERATE_ISCONTAINER,
	NULL AS MENUOPERATE_ENTRYOPERATECODE,
	MENUOPERATETYPE AS MENUOPERATETYPE

FROM
	rbac_menuoperate );

DROP VIEW "BASE_ROLE";
CREATE VIEW "BASE_ROLE" ("ID", "VALID", "VERSION", "CID", "LEAF", "FULL_PATH_NAME", "PARENT_ID", "LAY_NO", "LAY_REC", "UUID", "THREE_ROLE_TYPE", "ROLE_TYPE", "SORT", "DESCRIPTION", "NAME", "CODE", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
  (
SELECT
	ID AS id,
	VALID AS valid,
	VERSION AS version,
	CID AS cid,
	LEAF AS leaf,
	FULL_PATH_NAME AS full_path_name,
	PARENT_ID AS parent_id,
	LAY_NO AS lay_no,
	LAY_REC AS lay_rec,
	UUID AS uuid,
	THREE_ROLE_TYPE AS three_role_type,
	ROLE_TYPE AS role_type,
	SORT AS sort,
	DESCRIPTION AS description,
	NAME AS name,
	CODE AS code,
	create_staff_id,
	modify_staff_id,
	NULL AS delete_staff_id,
	CREATE_TIME AS create_time,
	MODIFY_TIME AS modify_time,
	DELETE_TIME AS delete_time
FROM
	rbac_role );

DROP VIEW "BASE_ROLEPERMISSION";
CREATE VIEW "BASE_ROLEPERMISSION" ("ID", "VERSION", "ROLE_ID", "MENUOPERATE_ID", "POSITION_FLAG", "GROUP_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "DEALER_PERMISSION_FLAG", "NO_RESTRICT_FLAG", "ASSIGN_SPECIALPERMISSION_FLAG", "ASSIGN_OTHERRESTRICT_FLAG", "URL_PATTERN", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
  ( select ID as id, VERSION as version, ROLE_ID as role_id, MENUOPERATE_ID as menuoperate_id, POSITION_FLAG as position_flag, GROUP_FLAG as group_flag, ASSIGN_STAFF_FLAG as assign_staff_flag, ASSIGN_POS_FLAG as assign_pos_flag, DEALER_PERMISSION_FLAG as dealer_permission_flag, NO_RESTRICT_FLAG as no_restrict_flag, ASSIGN_DATAPERMISSION_FLAG as assign_specialpermission_flag, ASSIGN_CUSTOMPERMISSION_FLAG as assign_otherrestrict_flag, URL_PATTERN as URL_PATTERN, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time from rbac_rolepermission );

DROP VIEW "BASE_ROLEPPOSITION";
CREATE VIEW "BASE_ROLEPPOSITION" ("ID", "VERSION", "INCLUDE_LOWER", "POSITION_ID", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, INCLUDE_LOWER as include_lower, POSITION_ID as position_id, ROLEPERMISSION_ID as rolepermission_id from rbac_rolepposition );

DROP VIEW "BASE_ROLEPSTAFF";
CREATE VIEW "BASE_ROLEPSTAFF" ("ID", "VERSION", "STAFF_ID", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, STAFF_ID as staff_id, ROLEPERMISSION_ID as rolepermission_id from rbac_rolepstaff );

DROP VIEW "BASE_ROLEUSER";
CREATE VIEW "BASE_ROLEUSER" ("ID", "VERSION", "POSITION_FLAG", "ROLE_ID", "USER_ID", "VALID", "END_TIME", "START_TIME") AS
  (
SELECT
	ID AS id,
	VERSION AS version,
	POSITION_FLAG AS position_flag,
	ROLE_ID AS role_id,
	USER_ID AS user_id,
	VALID AS valid,
	END_TIME AS end_time,
	START_TIME AS start_time
FROM
	rbac_roleuser );

DROP VIEW "BASE_ROLE_OTHERRESTRICT_REF";
CREATE VIEW "BASE_ROLE_OTHERRESTRICT_REF" ("ID", "VERSION", "OTHER_RESTRICT_CODE", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CUSTOM_PERMISSION_CODE as other_restrict_code, ROLEPERMISSION_ID as rolepermission_id from rbac_role_custompermission_ref );

DROP VIEW "BASE_ROLE_SPECIALPERMISSION";
CREATE VIEW "BASE_ROLE_SPECIALPERMISSION" ("ID", "VERSION", "CONFIG_STRING", "CONTENT", "SPECIAL_PERMISSION_CODE", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CONFIG_STRING as config_string, CONTENT as content, DATA_PERMISSION_CODE as special_permission_code, ROLEPERMISSION_ID as rolepermission_id from rbac_role_datapermission );

DROP VIEW "BASE_USERPERMISSION";
CREATE VIEW "BASE_USERPERMISSION" ("ID", "VERSION", "USER_ID", "STAFF_ID", "CID", "MENUOPERATE_ID", "PURVIEW_TYPE", "POSITION_FLAG", "GROUP_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "DEALER_PERMISSION_FLAG", "NO_RESTRICT_FLAG", "ASSIGN_OTHERRESTRICT_FLAG", "ASSIGN_SPECIALPERMISSION_FLAG", "URL_PATTERN", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "ST_STATE", "ST_POWERFLAG", "ST_POWERCODE", "ST_OPERATESTATE", "ST_OPERATEID", "ST_MODULECODE", "ST_MENUID", "ST_MEMO", "ST_FLAG", "DEAL_TIME") AS
  (
SELECT
	ID AS id,
	VERSION AS version,
	USER_ID AS user_id,
	DEAL_STAFF AS staff_id,
	CID AS cid,
	MENUOPERATE_ID AS menuoperate_id,
	PURVIEW_TYPE AS purview_type,
	POSITION_FLAG AS position_flag,
	GROUP_FLAG AS group_flag,
	ASSIGN_STAFF_FLAG AS assign_staff_flag,
	ASSIGN_POS_FLAG AS assign_pos_flag,
	DEALER_PERMISSION_FLAG AS dealer_permission_flag,
	NO_RESTRICT_FLAG AS no_restrict_flag,
	ASSIGN_CUSTOMPERMISSION_FLAG AS assign_otherrestrict_flag,
	ASSIGN_DATAPERMISSION_FLAG AS assign_specialpermission_flag,
	URL_PATTERN AS url_pattern,
	create_staff_id,
	modify_staff_id,
	NULL AS delete_staff_id,
	CREATE_TIME AS create_time,
	MODIFY_TIME AS modify_time,
	DELETE_TIME AS delete_time,
	NULL AS ST_STATE,
	NULL AS ST_POWERFLAG,
	NULL AS ST_POWERCODE,
	NULL AS ST_OPERATESTATE,
	NULL AS ST_OPERATEID,
	NULL AS ST_MODULECODE,
	NULL AS ST_MENUID,
	NULL AS ST_MEMO,
	NULL AS ST_FLAG,
	NULL AS DEAL_TIME
FROM
	rbac_userpermission );

DROP VIEW "BASE_USERPPOSITION";
CREATE VIEW "BASE_USERPPOSITION" ("ID", "VERSION", "INCLUDE_LOWER", "POSITION_ID", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, INCLUDE_LOWER as include_lower, POSITION_ID as position_id, USERPERMISSION_ID as userpermission_id from rbac_userpposition );

DROP VIEW "BASE_USERPSTAFF";
CREATE VIEW "BASE_USERPSTAFF" ("ID", "VERSION", "STAFF_ID", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, STAFF_ID as staff_id, USERPERMISSION_ID as userpermission_id from rbac_userpstaff );

DROP VIEW "BASE_USER_OTHERRESTRICT_REF";
CREATE VIEW "BASE_USER_OTHERRESTRICT_REF" ("ID", "VERSION", "OTHER_RESTRICT_CODE", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CUSTOM_PERMISSION_CODE as other_restrict_code, USERPERMISSION_ID as userpermission_id from rbac_user_custompermission_ref );

DROP VIEW "BASE_USER_SPECIALPERMISSION";
CREATE VIEW "BASE_USER_SPECIALPERMISSION" ("ID", "VERSION", "CONFIG_STRING", "CONTENT", "SPECIAL_PERMISSION_CODE", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CONFIG_STRING as config_string, CONTENT as content, DATA_PERMISSION_CODE as special_permission_code, USERPERMISSION_ID as userpermission_id from rbac_user_datapermission );

DROP VIEW "EC_OTHER_RESTRICT";
CREATE VIEW "EC_OTHER_RESTRICT" ("CODE","HAND_WRITING_FLAG", "EC_ENV", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "ENTITY_CODE", "MODULE_CODE", "MEMO", "TITLE", "CONDITION_SQL", "JSON_CONDITION", "VIEW_CODE") AS
  ( select CODE as code,HAND_WRITING_FLAG AS hand_writing_flag, EC_ENV as ec_env, VERSION as version, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time, VALID as valid, ENTITY_CODE as entity_code, MODULE_CODE as module_code, MEMO as memo, TITLE as title, CONDITION_SQL as condition_sql, JSON_CONDITION as json_condition, VIEW_CODE as view_code from rbac_custom_permission );

DROP VIEW base_role_mnecode;
CREATE VIEW base_role_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.role AS role,
	m.mne_code
FROM
	rbac_role_mnecode m
);
## 1.0.1
/*==============================================================*/
/* Table:菜单APP关联表(rbac_app_ref)                */
/*==============================================================*/
CREATE TABLE rbac_app_ref
(
    ID                  BIGINT NOT NULL,
    menuId              BIGINT,
    appid             VARCHAR(200),
    PRIMARY KEY(ID),
    constraint unique_menu_app unique(menuId,appid)
);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单APP关联表', N'user', N'dbo', N'table', N'rbac_app_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_app_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单ID', N'user', N'dbo', N'table', N'rbac_app_ref', N'column', N'menuId';
EXECUTE sp_addextendedproperty N'MS_Description', 'APP编码', N'user', N'dbo', N'table', N'rbac_app_ref', N'column', N'appid';
/*==============================================================*/
/* Table:APP公司关联表(rbac_app_company_ref)                */
/*==============================================================*/
CREATE TABLE rbac_app_company_ref
(
    ID                  BIGINT NOT NULL,
    CID                 BIGINT,
    APPID               VARCHAR(200),
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', 'APP公司关联表', N'user', N'dbo', N'table', N'rbac_app_company_ref', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_app_company_ref', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '公司ID', N'user', N'dbo', N'table', N'rbac_app_company_ref', N'column', N'CID';
EXECUTE sp_addextendedproperty N'MS_Description', 'APPID', N'user', N'dbo', N'table', N'rbac_app_company_ref', N'column', N'APPID';


ALTER TABLE RBAC_MENUINFO_COMPANY_REF ADD APPID varchar(200) NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '组态期 0，运行期 1', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'STATUS';
EXECUTE sp_addextendedproperty N'MS_Description', '地址', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'ROUTE';
EXECUTE sp_addextendedproperty N'MS_Description', '额外信息', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'EXTRA';

## 1.0.2
execute sp_rename 'rbac_app_ref.ID','id';
execute sp_rename 'rbac_app_company_ref.ID','id';
execute sp_rename 'rbac_app_company_ref.CID','cid';
execute sp_rename 'rbac_app_company_ref.APPID','appid';


## 1.0.6
/*==rbac_menuinfo 冗余字段 appid==*/
ALTER table rbac_menuinfo add appid varchar(200);
EXECUTE sp_addextendedproperty N'MS_Description', '菜单所属app,冗余字段', N'user', N'dbo', N'table', N'rbac_menuinfo', N'column', N'appid';

/*==rbac_menu_app_designer==*/
CREATE TABLE rbac_menu_app_designer
(
    id                  BIGINT NOT NULL,
    appid               VARCHAR(200),
    code               VARCHAR(200),
    parent_code         VARCHAR(200),
    PRIMARY KEY(ID),
    constraint unique_menu_app_designer unique(appid,code)
);
EXECUTE sp_addextendedproperty N'MS_Description', '为方便获取设计器当前节点下的菜单树列表,维护App设计器与菜单的关联关系', N'user', N'dbo', N'table', N'rbac_menu_app_designer', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'rbac_menu_app_designer', N'column', N'id';
EXECUTE sp_addextendedproperty N'MS_Description', 'APPID', N'user', N'dbo', N'table', N'rbac_menu_app_designer', N'column', N'appid';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单code', N'user', N'dbo', N'table', N'rbac_menu_app_designer', N'column', N'code';
EXECUTE sp_addextendedproperty N'MS_Description', '菜单父code', N'user', N'dbo', N'table', N'rbac_menu_app_designer', N'column', N'parent_code';


## 1.0.6
update rm set rm.appid= rar.appid from rbac_menuinfo rm,rbac_app_ref rar where rar.menuId = rm.ID;


## 1.0.7
CREATE TABLE rbac_data_resource_group
(
    id                        BIGINT NOT NULL, --COMMENT '主键ID',
    group_code                VARCHAR(4000) NOT NULL, --COMMENT '资源编码',
    group_name                VARCHAR(4000) NOT NULL, --COMMENT '资源名称',
    resource_url              VARCHAR(4000) NOT NULL, --COMMENT '资源获取地址',
    module_code               VARCHAR(1024) NOT NULL, --COMMENT '模块编码',
    cid                       BIGINT NULL, --COMMENT '公司ID',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               DATETIME NOT NULL, --COMMENT '创建时间',
    create_staff_id           BIGINT NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               DATETIME, --COMMENT '修改时间',
    modify_staff_id           BIGINT, --COMMENT '修改者人员Id',
    PRIMARY KEY (id)
);
INSERT INTO rbac_data_resource_group (id, group_code, group_name, resource_url, module_code, creator, create_staff_id,create_time) VALUES (1, 'oodm-data-group-permission', '数据建模权限分组', '/project/dam/supngin/api/dam/v1/dataGroups', 'oodm', 'system', 0,getdate());


CREATE TABLE rbac_role_data_permission
(
    id                        BIGINT NOT NULL, --COMMENT '主键ID',
    role_id                   BIGINT NOT NULL, --COMMENT '角色ID',
    cid                       BIGINT NOT NULL, --COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL, --COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL, --COMMENT '业务数据名称',
    resource_type             VARCHAR(512), --COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    valid                     INT DEFAULT 1,  --COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               DATETIME NOT NULL, --COMMENT '创建时间',
    create_staff_id           BIGINT NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               DATETIME, --COMMENT '修改时间',
    modify_staff_id           BIGINT, --COMMENT '修改者人员Id',
    PRIMARY KEY (id)
);

CREATE TABLE rbac_role_data_permission_ctrl
(
    id                        BIGINT NOT NULL, --COMMENT '主键ID',
    role_id                   BIGINT NOT NULL, --COMMENT '角色ID',
    cid                       BIGINT NOT NULL, --COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    controlled                INT DEFAULT 1, --COMMENT 'group_code资源是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               DATETIME NOT NULL, --COMMENT '创建时间',
    create_staff_id           BIGINT NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               DATETIME, --COMMENT '修改时间',
    modify_staff_id           BIGINT, --COMMENT '修改者人员Id',
    PRIMARY KEY (id)
);
create UNIQUE INDEX udx_role_data_ctrl on rbac_role_data_permission_ctrl(role_id, cid, group_code);

CREATE TABLE rbac_user_data_permission
(
    id                        BIGINT NOT NULL, --COMMENT '主键ID',
    user_id                   BIGINT NOT NULL, --COMMENT '用户ID',
    cid                       BIGINT NOT NULL, --COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL, --COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL, --COMMENT '业务数据名称',
    resource_type             VARCHAR(512), --COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL ,--COMMENT '资源编码',
    role_id                   BIGINT, --COMMENT '角色ID',
    purview_type              INT NOT NULL, --COMMENT '授权方式：角色(0) 用户(1)',
    valid                     INT DEFAULT 1, --COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               DATETIME NOT NULL, --COMMENT '创建时间',
    create_staff_id           BIGINT NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               DATETIME, --COMMENT '修改时间',
    modify_staff_id           BIGINT, --COMMENT '修改者人员Id',
    PRIMARY KEY (id)
);


CREATE TABLE rbac_user_data_permission_ctrl
(
    id                        BIGINT NOT NULL, --COMMENT '主键ID',
    user_id                   BIGINT NOT NULL, --COMMENT '用户ID',
    cid                       BIGINT NOT NULL, --COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    controlled                INT DEFAULT 1, --COMMENT 'group_code数据分组是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               DATETIME NOT NULL, --COMMENT '创建时间',
    create_staff_id           BIGINT NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               DATETIME, --COMMENT '修改时间',
    modify_staff_id           BIGINT, --COMMENT '修改者人员Id',
    PRIMARY KEY (id)
);
create UNIQUE INDEX udx_user_data_ctrl on rbac_user_data_permission_ctrl(user_id, cid, group_code);

CREATE INDEX COMPANY_REF_MENUINFO_ID_IDX ON rbac_menuinfo_company_ref (MENUINFO_ID);
CREATE INDEX INDEX_USERPERMISSION_MO ON rbac_userpermission (MENUOPERATE_ID);
CREATE INDEX INDEX_MENUOPERATE_CODE_IDX ON rbac_userpermission (MENUOPERATE_CODE);
CREATE INDEX IDX_DP_ACTIVITYCODE ON rbac_flow_permission (ACTIVITY_CODE);
CREATE INDEX INDEX_USER_ID_IDX ON rbac_userpermission (USER_ID);
CREATE INDEX INDEX_CID_IDX ON rbac_userpermission (CID);
CREATE INDEX INDEX_PURVIEW_TYPE_IDX ON rbac_userpermission (PURVIEW_TYPE);
CREATE INDEX INDEX_FLOW_POSITION_ID_IDX ON rbac_flow_permission_position (FLOWPERMISSION_ID);
CREATE INDEX INDEX_FLOW_STAFF_ID_IDX ON rbac_flow_permission_staff (FLOWPERMISSION_ID);
CREATE INDEX INDEX_CUSTOM_UP_ID_IDX ON rbac_user_custompermission_ref (USERPERMISSION_ID);
CREATE INDEX INDEX_DATA_UP_ID_IDX ON rbac_user_datapermission (USERPERMISSION_ID);

## 1.0.8
alter table rbac_menuinfo alter COLUMN URL varchar(2000);
alter table rbac_menuinfo alter COLUMN ROUTE varchar(2000);
alter table rbac_menuoperate alter COLUMN URL varchar(2000);

## 1.0.9
create index idx_rbac_userpermission_1 on rbac_userpermission(user_id,cid);
create index idx_rbac_menuinfo_1 on rbac_menuinfo(lay_no,status);

## 1.0.10
create index idx_rbac_flow_permission_1 on rbac_flow_permission(flow_permission_type, type_id);


## 1.0.11
update rbac_role set create_time=getdate() where  create_time is null;
update rbac_role set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_role ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_role ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_role ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_role ALTER COLUMN modify_time datetime not  NULL;

update rbac_roleuser set create_time=getdate() where  create_time is null;
update rbac_roleuser set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_roleuser ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_roleuser ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_roleuser ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_roleuser ALTER COLUMN modify_time datetime not  NULL;

update rbac_menuoperate set create_time=getdate() where  create_time is null;
update rbac_menuoperate set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_menuoperate ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_menuoperate ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_menuoperate ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_menuoperate ALTER COLUMN modify_time datetime not  NULL;

update rbac_menuinfo set create_time=getdate() where  create_time is null;
update rbac_menuinfo set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_menuinfo ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_menuinfo ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_menuinfo ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_menuinfo ALTER COLUMN modify_time datetime not  NULL;

update rbac_tag set create_time=getdate() where  create_time is null;
update rbac_tag set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_tag ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_tag ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_tag ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_tag ALTER COLUMN modify_time datetime not  NULL;

update rbac_userpermission set create_time=getdate() where  create_time is null;
update rbac_userpermission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_userpermission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_userpermission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_userpermission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_userpermission ALTER COLUMN modify_time datetime not  NULL;

update rbac_rolepermission set create_time=getdate() where  create_time is null;
update rbac_rolepermission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_rolepermission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_rolepermission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_rolepermission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_rolepermission ALTER COLUMN modify_time datetime not  NULL;

update rbac_data_permission set create_time=getdate() where  create_time is null;
update rbac_data_permission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_data_permission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_data_permission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_data_permission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_data_permission ALTER COLUMN modify_time datetime not  NULL;


update rbac_data_permission_ushow set create_time=getdate() where  create_time is null;
update rbac_data_permission_ushow set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_data_permission_ushow ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_data_permission_ushow ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_data_permission_ushow ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_data_permission_ushow ALTER COLUMN modify_time datetime not  NULL;


update rbac_data_permission_rshow set create_time=getdate() where  create_time is null;
update rbac_data_permission_rshow set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_data_permission_rshow ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_data_permission_rshow ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_data_permission_rshow ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_data_permission_rshow ALTER COLUMN modify_time datetime not  NULL;

update rbac_custom_permission set create_time=getdate() where  create_time is null;
update rbac_custom_permission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_custom_permission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_custom_permission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_custom_permission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_custom_permission ALTER COLUMN modify_time datetime not  NULL;

update rbac_flow_permission set create_time=getdate() where  create_time is null;
update rbac_flow_permission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_flow_permission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_flow_permission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_flow_permission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_flow_permission ALTER COLUMN modify_time datetime not  NULL;


update rbac_data_resource_group set create_time=getdate() where  create_time is null;
update rbac_data_resource_group set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_data_resource_group ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_data_resource_group ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_data_resource_group ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_data_resource_group ALTER COLUMN modify_time datetime not  NULL;

update rbac_role_data_permission set create_time=getdate() where  create_time is null;
update rbac_role_data_permission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_role_data_permission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_role_data_permission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_role_data_permission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_role_data_permission ALTER COLUMN modify_time datetime not  NULL;

update rbac_role_data_permission_ctrl set create_time=getdate() where  create_time is null;
update rbac_role_data_permission_ctrl set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_role_data_permission_ctrl ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_role_data_permission_ctrl ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_role_data_permission_ctrl ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_role_data_permission_ctrl ALTER COLUMN modify_time datetime not  NULL;

update rbac_user_data_permission set create_time=getdate() where  create_time is null;
update rbac_user_data_permission set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_user_data_permission ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_user_data_permission ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_user_data_permission ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_user_data_permission ALTER COLUMN modify_time datetime not  NULL;

update rbac_user_data_permission_ctrl set create_time=getdate() where  create_time is null;
update rbac_user_data_permission_ctrl set modify_time =create_time where  modify_time is null;
ALTER TABLE rbac_user_data_permission_ctrl ADD DEFAULT getdate() FOR CREATE_TIME;
ALTER TABLE rbac_user_data_permission_ctrl ALTER COLUMN create_time datetime not  NULL;
ALTER TABLE rbac_user_data_permission_ctrl ADD DEFAULT getdate() FOR modify_time;
ALTER TABLE rbac_user_data_permission_ctrl ALTER COLUMN modify_time datetime not  NULL;

## 1.0.12
delete from rbac_menuoperatecode_url_ref where MENUOPERATE_CODE ='openMsgManager';