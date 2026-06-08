## 1.0.0
/*==============================================================*/
/* TABLE: 角色表(RBAC_ROLE)                */
/*==============================================================*/
CREATE TABLE rbac_role (
    ID NUMBER(20, 0) NOT NULL,
    VALID  NUMBER(10, 0) DEFAULT 1,
    VERSION NUMBER(10, 0) DEFAULT 0,
    CID  NUMBER(20, 0),
    LEAF  NUMBER(1, 0) DEFAULT 0,
    FULL_PATH_NAME  VARCHAR(4000),
    PARENT_ID  NUMBER(20, 0),
    LAY_NO  NUMBER(20, 0),
    LAY_REC  VARCHAR(4000),
    UUID  VARCHAR(4000),
    THREE_ROLE_TYPE  NUMBER(10, 0),
    ROLE_TYPE  VARCHAR(4000),
    SORT  FLOAT(53),
    DESCRIPTION  VARCHAR(510),
    NAME  VARCHAR(160),
    CODE  VARCHAR(160),
    CREATOR VARCHAR(32) DEFAULT NULL,
    MODIFIER VARCHAR(32) DEFAULT NULL,
    TERMINATOR VARCHAR(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    DELETE_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20, 0) ,
    MODIFY_STAFF_ID NUMBER(20, 0) ,
    TAG VARCHAR(510),
    primary key (id)
);
COMMENT ON TABLE rbac_role IS '角色表';
COMMENT ON COLUMN rbac_role.ID IS '主键ID';
COMMENT ON COLUMN rbac_role.VALID IS '是否有效';
COMMENT ON COLUMN rbac_role.VERSION IS '版本信息';
COMMENT ON COLUMN rbac_role.CID IS '公司ID';
COMMENT ON COLUMN rbac_role.LEAF IS '是否叶子';
COMMENT ON COLUMN rbac_role.FULL_PATH_NAME IS '层级全路径';
COMMENT ON COLUMN rbac_role.PARENT_ID IS '上级节点ID';
COMMENT ON COLUMN rbac_role.LAY_NO IS '层级';
COMMENT ON COLUMN rbac_role.LAY_REC IS '层级结构';
COMMENT ON COLUMN rbac_role.UUID IS '用于软件公司同步接口';
COMMENT ON COLUMN rbac_role.THREE_ROLE_TYPE IS '三员类型:1系统管理员,2安全保密员 ,3安全审计员';
COMMENT ON COLUMN rbac_role.ROLE_TYPE IS '角色类型';
COMMENT ON COLUMN rbac_role.SORT IS '排序';
COMMENT ON COLUMN rbac_role.DESCRIPTION IS '描述';
COMMENT ON COLUMN rbac_role.NAME IS '名称';
COMMENT ON COLUMN rbac_role.CODE IS '编码';
COMMENT ON COLUMN rbac_role.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_role.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_role.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_role.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_role.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_role.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_role.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_role.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_role.TAG IS '标签';
create index ROLE_VALID_IDX on rbac_role(VALID);

INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (1,'管理员角色','systemRole','管理员角色','管理员角色',1000,1,1);
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (2,'公司管理员角色','companySystemRole','公司管理员角色','公司管理员角色',1000,1,1);
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID,CREATE_STAFF_ID,MODIFY_STAFF_ID) values (3,'普通用户角色','normalRole','普通用户角色','普通用户角色',1000,1,1);
/*==============================================================*/
/* TABLE:角色用户表(rbac_roleuser)                */
/*==============================================================*/
CREATE TABLE rbac_roleuser(
  ID NUMBER(20, 0) NOT NULL,
  VERSION NUMBER(10, 0) DEFAULT 0 ,
  POSITION_FLAG NUMBER(1,0) DEFAULT 0,
  ROLE_ID NUMBER(20, 0),
  USER_ID NUMBER(20, 0) ,
  VALID NUMBER(10, 0) DEFAULT 1 ,
  END_TIME TIMESTAMP(6) ,
  START_TIME TIMESTAMP(6),
  PERSON_NAME VARCHAR(160) ,
  PERSON_CODE VARCHAR(160),
  USER_NAME VARCHAR(160) ,
  TERMINATOR VARCHAR(32) ,
  MODIFIER VARCHAR(32) ,
  CREATOR VARCHAR(32) ,
  CREATE_TIME TIMESTAMP,
  MODIFY_TIME TIMESTAMP,
  DELETE_TIME TIMESTAMP ,
  CREATE_STAFF_ID NUMBER(20, 0)  ,
  MODIFY_STAFF_ID NUMBER(20, 0)  ,
  FROM_POSITION NUMBER(2, 0) DEFAULT 1,
  PRIMARY KEY (ID)
);
COMMENT ON TABLE rbac_roleuser IS '角色用户表';
COMMENT ON COLUMN rbac_roleuser.ID IS '主键ID';
COMMENT ON COLUMN rbac_roleuser.VERSION IS '版本信息';
COMMENT ON COLUMN rbac_roleuser.POSITION_FLAG IS '是否仅是岗位带入的角色';
COMMENT ON COLUMN rbac_roleuser.ROLE_ID IS '角色ID';
COMMENT ON COLUMN rbac_roleuser.USER_ID IS '用户ID';
COMMENT ON COLUMN rbac_roleuser.VALID IS '是否有效';
COMMENT ON COLUMN rbac_roleuser.END_TIME IS '调出时间';
COMMENT ON COLUMN rbac_roleuser.START_TIME IS '调入时间';
COMMENT ON COLUMN rbac_roleuser.PERSON_NAME IS '人员姓名';
COMMENT ON COLUMN rbac_roleuser.PERSON_CODE IS '人员编号';
COMMENT ON COLUMN rbac_roleuser.USER_NAME IS '用户名';
COMMENT ON COLUMN rbac_roleuser.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_roleuser.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_roleuser.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_roleuser.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_roleuser.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_roleuser.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_roleuser.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_roleuser.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_roleuser.FROM_POSITION IS '来源 1 来源于用户 2 来源于岗位 3两者都有';
create index INDEX_ROLEUSER_ROLE_ID on rbac_roleuser(ROLE_ID);
create index INDEX_ROLEUSER_USER_ID on rbac_roleuser(USER_ID);
INSERT INTO rbac_roleuser(ID, ROLE_ID, USER_ID, PERSON_NAME, PERSON_CODE, USER_NAME,CREATE_STAFF_ID,MODIFY_STAFF_ID)
VALUES (1, 1, 1, '默认人员','default', 'admin',1,1);
/*==============================================================*/
/* TABLE:操作表(RBAC_MENUOPERATE)                */
/*==============================================================*/
CREATE TABLE rbac_menuoperate
(
    ID NUMBER(20, 0) NOT NULL ,
    ROW_VERSION NUMBER(20) DEFAULT 0 ,
    DELETE_TIME TIMESTAMP ,
    MODIFY_TIME TIMESTAMP,
    CREATE_TIME TIMESTAMP,
    TERMINATOR VARCHAR(32),
    MODIFIER VARCHAR(32),
    CREATOR VARCHAR(32),
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    VALID NUMBER(10, 0) DEFAULT 1 ,
    CID NUMBER(20, 0),
    IS_ALLOW_PROXY NUMBER(1, 0) DEFAULT 0,
    IS_HIDDEN NUMBER(1, 0) DEFAULT 0,
    THREE_ROLE NUMBER(1, 0) DEFAULT 0,
    VIEW_CODE VARCHAR(510) ,
    IS_QUERY NUMBER(1, 0) DEFAULT 0,
    IS_ORRELATION NUMBER(1, 0),
    ENABLE_DATAPERMISSION NUMBER(1, 0) DEFAULT 0,
    ENABLE_CUSTOMPERMISSION NUMBER(1, 0) DEFAULT 0,
    FOR_FLOW_PERMISSION NUMBER(1, 0) DEFAULT 0,
    ENABLE_NORESTRICT NUMBER(1, 0) DEFAULT 1,
    ENABLE_DEALERPERMISSION NUMBER(1, 0) DEFAULT 0,
    ENABLE_ASSIGNSTAFF NUMBER(1, 0) DEFAULT 0,
    ENABLE_ASSIGNPOS NUMBER(1, 0) DEFAULT 0,
    ENABLE_POSRESTRICT NUMBER(1, 0) DEFAULT 0,
    ENABLE_DEPTRICT NUMBER(1, 0) DEFAULT 0,
    ENABLE_ASSIGNDEPT NUMBER(1, 0) DEFAULT 0,
    ENABLE_GROUPRESTRICT NUMBER(1, 0) DEFAULT 0,
    ENTITY_CODE VARCHAR(510) ,
    IGNORE_PERMISSION NUMBER(1, 0) DEFAULT 0,
    POWER_FLAG NUMBER(1, 0) DEFAULT 0,
    FLOW_VERSION VARCHAR(510),
    FLOW_KEY VARCHAR(510) ,
    MSG_ASSEMBLED NUMBER(10, 0),
    DEPLOYMENT_ID NUMBER(20, 0),
    MENUOPERATETYPE VARCHAR(510) ,
    MENUINFO_ID NUMBER(20, 0) ,
    ICON_CLS VARCHAR(510),
    MODULE_CODE VARCHAR(510),
    SORT FLOAT(53) ,
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
    DEFAULT_OPERATE NUMBER(10, 0) DEFAULT 0,
    EDITED NUMBER(1, 0) DEFAULT 0 ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menuoperate IS '操作表';
COMMENT ON COLUMN rbac_menuoperate.ID IS '主键ID';
COMMENT ON COLUMN rbac_menuoperate.ROW_VERSION IS '版本';
COMMENT ON COLUMN rbac_menuoperate.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_menuoperate.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_menuoperate.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_menuoperate.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_menuoperate.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_menuoperate.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_menuoperate.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_menuoperate.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_menuoperate.VALID IS '是否有效';
COMMENT ON COLUMN rbac_menuoperate.CID IS '公司ID';
COMMENT ON COLUMN rbac_menuoperate.IS_ALLOW_PROXY IS '是否允许委托';
COMMENT ON COLUMN rbac_menuoperate.IS_HIDDEN IS '是否隐藏';
COMMENT ON COLUMN rbac_menuoperate.THREE_ROLE IS '是否三员菜单';
COMMENT ON COLUMN rbac_menuoperate.VIEW_CODE IS '视图编码';
COMMENT ON COLUMN rbac_menuoperate.IS_QUERY IS '是否查询操作';
COMMENT ON COLUMN rbac_menuoperate.IS_ORRELATION IS '该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_DATAPERMISSION IS '启用数据权限';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_CUSTOMPERMISSION IS '启用自定义权限';
COMMENT ON COLUMN rbac_menuoperate.FOR_FLOW_PERMISSION IS '启用业务权限';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_NORESTRICT IS '无限制';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_DEALERPERMISSION IS '启用处理人';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_ASSIGNSTAFF IS '启用指定人员';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_ASSIGNPOS IS '启用指定岗位';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_POSRESTRICT IS '岗位限制';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_DEPTRICT IS '部门限制';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_ASSIGNDEPT IS '启用指定部门';
COMMENT ON COLUMN rbac_menuoperate.ENABLE_GROUPRESTRICT IS '启用组限制';
COMMENT ON COLUMN rbac_menuoperate.ENTITY_CODE IS '实体编码';
COMMENT ON COLUMN rbac_menuoperate.IGNORE_PERMISSION IS '忽视权限';
COMMENT ON COLUMN rbac_menuoperate.POWER_FLAG IS '是否是主列表视图的查询操作';
COMMENT ON COLUMN rbac_menuoperate.FLOW_VERSION IS '工作流版本';
COMMENT ON COLUMN rbac_menuoperate.FLOW_KEY IS '工作流KEY';
COMMENT ON COLUMN rbac_menuoperate.MENUOPERATETYPE IS '操作类型';
COMMENT ON COLUMN rbac_menuoperate.MENUINFO_ID IS '菜单ID';
COMMENT ON COLUMN rbac_menuoperate.ICON_CLS IS '图表样式';
COMMENT ON COLUMN rbac_menuoperate.MODULE_CODE IS '模块信息：BUNDLE的SYMBOLICNAME组成';
COMMENT ON COLUMN rbac_menuoperate.SORT IS '排序';
COMMENT ON COLUMN rbac_menuoperate.MEMO IS '备注';
COMMENT ON COLUMN rbac_menuoperate.TARGET IS '打开方式';
COMMENT ON COLUMN rbac_menuoperate.ACTION_URL IS 'ACTION';
COMMENT ON COLUMN rbac_menuoperate.NAMESPACE IS '命名空间';
COMMENT ON COLUMN rbac_menuoperate.URL IS '地址';
COMMENT ON COLUMN rbac_menuoperate.NAME_ZH_CN IS '中文名';
COMMENT ON COLUMN rbac_menuoperate.NAME IS '名称';
COMMENT ON COLUMN rbac_menuoperate.NAME_DISPLAY IS '国际化值 存默认名称';
COMMENT ON COLUMN rbac_menuoperate.CODE IS '编码';
COMMENT ON COLUMN rbac_menuoperate.APP IS '所属应用名';
COMMENT ON COLUMN rbac_menuoperate.DEFAULT_OPERATE IS '默认操作标识，默认操作不可删除';
COMMENT ON COLUMN rbac_menuoperate.EDITED IS '是否修改过 修改过的操作升级时不修改';
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
    ID NUMBER(20, 0) NOT NULL,
    VERSION NUMBER(10, 0) DEFAULT 0 ,
    DELETE_TIME TIMESTAMP default sysdate,
    MODIFY_TIME TIMESTAMP default sysdate,
    CREATE_TIME TIMESTAMP default sysdate,
    TERMINATOR VARCHAR(32) ,
    LEAF NUMBER(10, 0) DEFAULT 0,
    MODIFIER VARCHAR(32),
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0) ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    VALID NUMBER(10, 0) DEFAULT 1,
    CID NUMBER(20, 0),
    SECURITY_CLASS VARCHAR(510),
    ABSOLUTE_HIDDEN NUMBER(1, 0) DEFAULT 0,
    THREE_ROLE NUMBER(1, 0) DEFAULT 0,
    SHOW_TYPE NUMBER(10, 0),
    REQUEST_TYPE NUMBER(10, 0),
    HIDDEN_TYPE NUMBER(10, 0),
    MENU_TYPE NUMBER(10, 0) ,
    IS_HIDE NUMBER(1, 0) DEFAULT 0,
    GROUP_ONLY NUMBER(1, 0) DEFAULT 0,
    ENTITY_CODE VARCHAR(510) ,
    MODULE_CODE VARCHAR(510) ,
    SYSTEM_DEFAULT NUMBER(1, 0) DEFAULT 0,
    CSS_CLASS VARCHAR(510) ,
    SORT FLOAT(53) ,
    ACTION_URL VARCHAR(510) ,
    NAMESPACE VARCHAR(510),
    URL VARCHAR(510) ,
    TARGET VARCHAR(510) default 'SELF',
    MEMO VARCHAR(510) ,
    NAME VARCHAR(510) ,
    NAME_DISPLAY VARCHAR(510),
    CODE VARCHAR(510),
    APP VARCHAR(510),
    ENABLE NUMBER(1, 0) DEFAULT 1,
    LAY_NO NUMBER(10, 0) ,
    LAY_REC VARCHAR(4000),
    PARENT_ID NUMBER(20, 0),
    FULL_PATH VARCHAR(4000) ,
    FULL_PATH_NAME VARCHAR(4000),
    SOURCE VARCHAR(510) ,
    EDITED NUMBER(1, 0) DEFAULT 0,
    TYPE NUMBER(10, 0) DEFAULT 0,
    NO_RESTRICT NUMBER(1, 0) DEFAULT 0,
    STATUS NUMBER(1, 0) DEFAULT 0,
    ROUTE VARCHAR(510),
    EXTRA VARCHAR(4000),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menuinfo IS '菜单表';
COMMENT ON COLUMN rbac_menuinfo.ID IS '主键ID';
COMMENT ON COLUMN rbac_menuinfo.VERSION IS '版本';
COMMENT ON COLUMN rbac_menuinfo.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_menuinfo.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_menuinfo.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_menuinfo.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_menuinfo.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_menuinfo.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_menuinfo.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_menuinfo.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_menuinfo.LEAF IS '是否是叶子节点';
COMMENT ON COLUMN rbac_menuinfo.VALID IS '是否有效';
COMMENT ON COLUMN rbac_menuinfo.CID IS '公司ID';
COMMENT ON COLUMN rbac_menuinfo.SECURITY_CLASS IS '密级';
COMMENT ON COLUMN rbac_menuinfo.ABSOLUTE_HIDDEN IS '绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏';
COMMENT ON COLUMN rbac_menuinfo.THREE_ROLE IS '是否三员菜单';
COMMENT ON COLUMN rbac_menuinfo.SHOW_TYPE IS '请求方式0:链接页面，1：链接URL';
COMMENT ON COLUMN rbac_menuinfo.REQUEST_TYPE IS '请求类型';
COMMENT ON COLUMN rbac_menuinfo.HIDDEN_TYPE IS '隐藏类型';
COMMENT ON COLUMN rbac_menuinfo.MENU_TYPE IS '菜单类型';
COMMENT ON COLUMN rbac_menuinfo.IS_HIDE IS '是否隐藏';
COMMENT ON COLUMN rbac_menuinfo.GROUP_ONLY IS '是否仅集团使用';
COMMENT ON COLUMN rbac_menuinfo.ENTITY_CODE IS '实体编码';
COMMENT ON COLUMN rbac_menuinfo.MODULE_CODE IS '模块信息：BUNDLE的SYMBOLICNAME组成';
COMMENT ON COLUMN rbac_menuinfo.SYSTEM_DEFAULT IS '是否默认系统';
COMMENT ON COLUMN rbac_menuinfo.CSS_CLASS IS 'CSS_CLASS(菜单样式用)';
COMMENT ON COLUMN rbac_menuinfo.SORT IS '排序';
COMMENT ON COLUMN rbac_menuinfo.ACTION_URL IS 'ACTION';
COMMENT ON COLUMN rbac_menuinfo.NAMESPACE IS '命名空间';
COMMENT ON COLUMN rbac_menuinfo.URL IS '地址';
COMMENT ON COLUMN rbac_menuinfo.TARGET IS '打开方式';
COMMENT ON COLUMN rbac_menuinfo.MEMO IS '备注';
COMMENT ON COLUMN rbac_menuinfo.NAME IS '名称';
COMMENT ON COLUMN rbac_menuinfo.NAME_DISPLAY IS '国际化值 存默认名称';
COMMENT ON COLUMN rbac_menuinfo.CODE IS '编码';
COMMENT ON COLUMN rbac_menuinfo.APP IS '所属应用名';
COMMENT ON COLUMN rbac_menuinfo.ENABLE IS '是否启用';
COMMENT ON COLUMN rbac_menuinfo.LAY_NO IS '层级';
COMMENT ON COLUMN rbac_menuinfo.LAY_REC IS '层级结构';
COMMENT ON COLUMN rbac_menuinfo.PARENT_ID IS '上级节点ID';
COMMENT ON COLUMN rbac_menuinfo.FULL_PATH IS '层级全路径';
COMMENT ON COLUMN rbac_menuinfo.FULL_PATH_NAME IS '层级全路径 菜单名';
COMMENT ON COLUMN rbac_menuinfo.SOURCE IS '来源';
COMMENT ON COLUMN rbac_menuinfo.EDITED IS '是否修改过 修改过的菜单升级时不修改';
COMMENT ON COLUMN rbac_menuinfo.TYPE IS '资源类型 0是菜单，后续更多的请看枚举类';
COMMENT ON COLUMN rbac_menuinfo.NO_RESTRICT IS '是否不受权限控制';
COMMENT ON COLUMN rbac_menuinfo.STATUS IS '组态期 0，运行期 1';
COMMENT ON COLUMN rbac_menuinfo.ROUTE IS '地址';
COMMENT ON COLUMN rbac_menuinfo.EXTRA IS '额外信息';
create index IDX_MENUINFO_CODE on rbac_menuinfo(CODE);
create index INDEX_MENUINFO_NAME on rbac_menuinfo(NAME);
create index IDX_MENUINFO_MODULECODE on rbac_menuinfo(MODULE_CODE);
create index BASE_MENUINFO_VALID_IDX on rbac_menuinfo(VALID);
INSERT INTO rbac_menuinfo (ID, VERSION, DELETE_TIME, MODIFY_TIME, CREATE_TIME, TERMINATOR, MODIFIER, CREATOR, VALID, CID, SECURITY_CLASS, ABSOLUTE_HIDDEN, THREE_ROLE, SHOW_TYPE, REQUEST_TYPE, HIDDEN_TYPE, MENU_TYPE, IS_HIDE, GROUP_ONLY, ENTITY_CODE, MODULE_CODE, SYSTEM_DEFAULT, CSS_CLASS, SORT, ACTION_URL, NAMESPACE, URL, TARGET, MEMO, NAME, CODE, LAY_NO, LAY_REC, PARENT_ID, FULL_PATH, SOURCE, LEAF,CREATE_STAFF_ID,MODIFY_STAFF_ID) VALUES (-1, 0, null, null, null, null, null, null, 1, 1000, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '系统默认', 'rbac.MENU_NAME_MENU_LIST', 'menu_list', null, 1, null, null, null, 0,1,1);
--alter table rbac_menuinfo rename column APP to MODULE_CODE_NO_VERSION;
/*===================================================================*/
/* Table: 标签表(rbac_tag)           */
/*===================================================================*/
create table rbac_tag (
    ID NUMBER(20, 0) NOT NULL ,
    VERSION NUMBER(10, 0) DEFAULT 0 ,
    TYPE VARCHAR(32) DEFAULT NULL,
    NAME VARCHAR(100) NOT NULL,
    CID NUMBER(20, 0) NOT NULL,
    OBJECTID NUMBER(20, 0) NOT NULL ,
    CREATOR VARCHAR(32) DEFAULT NULL ,
    MODIFIER VARCHAR(32) DEFAULT NULL ,
    VALID NUMBER(10, 0) DEFAULT 1 ,
    CREATE_TIME TIMESTAMP ,
    MODIFY_TIME TIMESTAMP ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    PRIMARY KEY (id)
);
COMMENT ON TABLE rbac_tag IS '标签表';
COMMENT ON COLUMN rbac_tag.ID IS '主键ID';
COMMENT ON COLUMN rbac_tag.VERSION IS '版本';
COMMENT ON COLUMN rbac_tag.TYPE IS '标签类型';
COMMENT ON COLUMN rbac_tag.NAME IS '标签名';
COMMENT ON COLUMN rbac_tag.CID IS '公司ID';
COMMENT ON COLUMN rbac_tag.OBJECTID IS '关联ID';
COMMENT ON COLUMN rbac_tag.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_tag.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_tag.VALID IS '逻辑删除,是否有效0:无效 1:有效';
COMMENT ON COLUMN rbac_tag.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_tag.MODIFY_TIME IS '修改时间';
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 1,'管理员角色',1000,1);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 2,'公司管理员角色',1000,2);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID) VALUES( 3,'普通用户角色',1000,3);
/*==============================================================*/
/* Table:用户权限表(rbac_userpermission)                */
/*==============================================================*/
CREATE TABLE rbac_userpermission
(
    ID                            NUMBER(20, 0) NOT NULL ,
    VERSION                       NUMBER(10, 0) DEFAULT 0 ,
    USER_ID                       NUMBER(20, 0) ,
    DEAL_STAFF                    NUMBER(20, 0) ,
    CID                           NUMBER(20, 0) ,
    MENUOPERATE_ID                NUMBER(20, 0) ,
    PURVIEW_TYPE                  NUMBER(10, 0) ,
    POSITION_FLAG                 NUMBER(1, 0) DEFAULT 0 ,
    DEPARTMENT_FLAG               NUMBER(1, 0) DEFAULT 0 ,
    GROUP_FLAG                    NUMBER(10, 0) DEFAULT 0,
    ASSIGN_STAFF_FLAG             NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_POS_FLAG               NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_DEPT_FLAG             NUMBER(1, 0) DEFAULT 0 ,
    DEALER_PERMISSION_FLAG        NUMBER(1, 0) DEFAULT 0 ,
    NO_RESTRICT_FLAG              NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_DATAPERMISSION_FLAG    NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_CUSTOMPERMISSION_FLAG  NUMBER(1, 0) DEFAULT 0 ,
    URL_PATTERN                   VARCHAR(510) ,
    MENUOPERATE_CODE              VARCHAR(510) ,
    DELETE_TIME TIMESTAMP ,
    MODIFY_TIME TIMESTAMP ,
    CREATE_TIME TIMESTAMP ,
    TERMINATOR VARCHAR(32) ,
    MODIFIER VARCHAR(32) ,
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_userpermission IS '用户权限表';
COMMENT ON COLUMN rbac_userpermission.ID IS '主键ID';
COMMENT ON COLUMN rbac_userpermission.VERSION IS '版本';
COMMENT ON COLUMN rbac_userpermission.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_userpermission.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_userpermission.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_userpermission.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_userpermission.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_userpermission.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_userpermission.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_userpermission.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_userpermission.USER_ID IS '用户ID';
COMMENT ON COLUMN rbac_userpermission.DEAL_STAFF IS '处理员工ID';
COMMENT ON COLUMN rbac_userpermission.CID IS '公司';
COMMENT ON COLUMN rbac_userpermission.MENUOPERATE_ID IS '菜单操作ID';
COMMENT ON COLUMN rbac_userpermission.PURVIEW_TYPE IS '授权方式：角色(0)OR用户(1)';
COMMENT ON COLUMN rbac_userpermission.POSITION_FLAG IS '岗位限制';
COMMENT ON COLUMN rbac_userpermission.DEPARTMENT_FLAG IS '部门限制';
COMMENT ON COLUMN rbac_userpermission.GROUP_FLAG IS '组限制：0 1 2';
COMMENT ON COLUMN rbac_userpermission.ASSIGN_STAFF_FLAG IS '指定人员';
COMMENT ON COLUMN rbac_userpermission.ASSIGN_POS_FLAG IS '指定岗位';
COMMENT ON COLUMN rbac_userpermission.ASSIGN_DEPT_FLAG IS '指定部门';
COMMENT ON COLUMN rbac_userpermission.DEALER_PERMISSION_FLAG IS '处理人权限：0 1';
COMMENT ON COLUMN rbac_userpermission.NO_RESTRICT_FLAG IS '无限制';
COMMENT ON COLUMN rbac_userpermission.ASSIGN_DATAPERMISSION_FLAG IS '指定业务数据权限限制：0 1';
COMMENT ON COLUMN rbac_userpermission.ASSIGN_CUSTOMPERMISSION_FLAG IS '指定自定义权限';
COMMENT ON COLUMN rbac_userpermission.URL_PATTERN IS 'URL正则';
COMMENT ON COLUMN rbac_userpermission.MENUOPERATE_CODE IS '冗余操作编码';
/*==============================================================*/
/* Table:角色权限表(rbac_rolepermission)                */
/*==============================================================*/
CREATE TABLE rbac_rolepermission
(
    ID                            NUMBER(20, 0) NOT NULL,
    CID                           NUMBER(20, 0),
    VERSION                       NUMBER(10, 0) DEFAULT 0,
    ROLE_ID                       NUMBER(20, 0) ,
    MENUOPERATE_ID                NUMBER(20, 0) ,
    POSITION_FLAG                 NUMBER(1, 0) DEFAULT 0 ,
    DEPARTMENT_FLAG               NUMBER(1, 0) DEFAULT 0 ,
    GROUP_FLAG                    NUMBER(10, 0) DEFAULT 0 ,
    ASSIGN_STAFF_FLAG             NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_POS_FLAG               NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_DEPT_FLAG             NUMBER(1, 0) DEFAULT 0 ,
    DEALER_PERMISSION_FLAG        NUMBER(1, 0) DEFAULT 0 ,
    NO_RESTRICT_FLAG              NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_DATAPERMISSION_FLAG    NUMBER(1, 0) DEFAULT 0 ,
    ASSIGN_CUSTOMPERMISSION_FLAG  NUMBER(1, 0) DEFAULT 0 ,
    URL_PATTERN                   VARCHAR(510) ,
    DELETE_TIME TIMESTAMP ,
    MODIFY_TIME TIMESTAMP ,
    CREATE_TIME TIMESTAMP ,
    TERMINATOR VARCHAR(32) ,
    MODIFIER VARCHAR(32) ,
    CREATOR VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_rolepermission IS '角色权限表';
COMMENT ON COLUMN rbac_rolepermission.ID IS '主键ID';
COMMENT ON COLUMN rbac_rolepermission.VERSION IS '版本';
COMMENT ON COLUMN rbac_rolepermission.TERMINATOR IS '删除者';
COMMENT ON COLUMN rbac_rolepermission.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_rolepermission.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_rolepermission.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_rolepermission.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_rolepermission.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_rolepermission.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_rolepermission.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_rolepermission.ROLE_ID IS '角色ID';
COMMENT ON COLUMN rbac_rolepermission.MENUOPERATE_ID IS '菜单操作ID';
COMMENT ON COLUMN rbac_rolepermission.POSITION_FLAG IS '岗位限制';
COMMENT ON COLUMN rbac_rolepermission.DEPARTMENT_FLAG IS '部门限制';
COMMENT ON COLUMN rbac_rolepermission.GROUP_FLAG IS '组限制：0 1 2';
COMMENT ON COLUMN rbac_rolepermission.ASSIGN_STAFF_FLAG IS '指定人员';
COMMENT ON COLUMN rbac_rolepermission.ASSIGN_POS_FLAG IS '指定岗位';
COMMENT ON COLUMN rbac_rolepermission.ASSIGN_DEPT_FLAG IS '指定部门';
COMMENT ON COLUMN rbac_rolepermission.DEALER_PERMISSION_FLAG IS '处理人权限：0 1';
COMMENT ON COLUMN rbac_rolepermission.NO_RESTRICT_FLAG IS '无限制';
COMMENT ON COLUMN rbac_rolepermission.ASSIGN_DATAPERMISSION_FLAG IS '指定业务数据权限限制：0 1';
COMMENT ON COLUMN rbac_rolepermission.ASSIGN_CUSTOMPERMISSION_FLAG IS '指定自定义权限';
COMMENT ON COLUMN rbac_rolepermission.URL_PATTERN IS 'URL正则';
create index INDEX_RM_ROLE_ID on rbac_rolepermission(ROLE_ID);
create index INDEX_RM_MENUOPERATE_ID on rbac_rolepermission(MENUOPERATE_ID);
/*==============================================================*/
/* Table:用户指定岗位表(rbac_userpposition)                */
/*==============================================================*/
CREATE TABLE rbac_userpposition
(
    ID                NUMBER(20, 0) NOT NULL,
    VERSION           NUMBER(10, 0) DEFAULT 0,
    INCLUDE_LOWER     NUMBER(1, 0) DEFAULT 0,
    POSITION_ID       NUMBER(20, 0) ,
    USERPERMISSION_ID NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_userpposition IS '用户指定岗位表';
COMMENT ON COLUMN rbac_userpposition.ID IS '主键ID';
COMMENT ON COLUMN rbac_userpposition.VERSION IS '版本';
COMMENT ON COLUMN rbac_userpposition.INCLUDE_LOWER IS '包含下级';
COMMENT ON COLUMN rbac_userpposition.POSITION_ID IS '岗位ID';
COMMENT ON COLUMN rbac_userpposition.USERPERMISSION_ID IS '用户权限ID';
create index INDEX_USERPPOSITION_UP_ID on rbac_userpposition(USERPERMISSION_ID);
/*==============================================================*/
/* Table:用户指定部门表(rbac_userpdepartment)                */
/*==============================================================*/
CREATE TABLE rbac_userpdepartment
(
    ID                NUMBER(20, 0) NOT NULL ,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    INCLUDE_LOWER     NUMBER(1, 0) DEFAULT 0,
    DEPARTMENT_ID       NUMBER(20, 0),
    USERPERMISSION_ID NUMBER(20, 0),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_userpdepartment IS '用户指定部门表';
COMMENT ON COLUMN rbac_userpdepartment.ID IS '主键ID';
COMMENT ON COLUMN rbac_userpdepartment.VERSION IS '版本';
COMMENT ON COLUMN rbac_userpdepartment.INCLUDE_LOWER IS '包含下级';
COMMENT ON COLUMN rbac_userpdepartment.DEPARTMENT_ID IS '部门ID';
COMMENT ON COLUMN rbac_userpdepartment.USERPERMISSION_ID IS '用户权限ID';
create index INDEX_USERPDEPARTMENT_UP_ID on rbac_userpdepartment(USERPERMISSION_ID);
/*==============================================================*/
/* Table:用户指定人员(rbac_userpstaff)                */
/*==============================================================*/
CREATE TABLE rbac_userpstaff
(
    ID                NUMBER(20, 0) NOT NULL,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    STAFF_ID          NUMBER(20, 0),
    USERPERMISSION_ID NUMBER(20, 0),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_userpstaff IS '用户指定人员';
COMMENT ON COLUMN rbac_userpstaff.ID IS '主键ID';
COMMENT ON COLUMN rbac_userpstaff.VERSION IS '版本';
COMMENT ON COLUMN rbac_userpstaff.STAFF_ID IS '人员ID';
COMMENT ON COLUMN rbac_userpstaff.USERPERMISSION_ID IS '用户权限ID';
/*==============================================================*/
/* Table:角色指定岗位表(rbac_rolepposition)                */
/*==============================================================*/
CREATE TABLE rbac_rolepposition
(
    ID                NUMBER(20, 0) NOT NULL ,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    INCLUDE_LOWER     NUMBER(1, 0) DEFAULT 0,
    POSITION_ID       NUMBER(20, 0) ,
    ROLEPERMISSION_ID NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_rolepposition IS '角色指定岗位表';
COMMENT ON COLUMN rbac_rolepposition.ID IS '主键ID';
COMMENT ON COLUMN rbac_rolepposition.VERSION IS '版本';
COMMENT ON COLUMN rbac_rolepposition.INCLUDE_LOWER IS '包含下级';
COMMENT ON COLUMN rbac_rolepposition.POSITION_ID IS '岗位ID';
COMMENT ON COLUMN rbac_rolepposition.ROLEPERMISSION_ID IS '角色权限ID';
create index INDEX_ROLEPPOSITION_UP_ID on rbac_rolepposition(ROLEPERMISSION_ID);
/*==============================================================*/
/* Table:角色指定部门表(rbac_rolepdepartment)                */
/*==============================================================*/
CREATE TABLE rbac_rolepdepartment
(
    ID                NUMBER(20, 0) NOT NULL,
    VERSION           NUMBER(10, 0) DEFAULT 0,
    INCLUDE_LOWER     NUMBER(1, 0) DEFAULT 0,
    DEPARTMENT_ID       NUMBER(20, 0),
    ROLEPERMISSION_ID NUMBER(20, 0),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_rolepdepartment IS '角色指定部门表';
COMMENT ON COLUMN rbac_rolepdepartment.ID IS '主键ID';
COMMENT ON COLUMN rbac_rolepdepartment.VERSION IS '版本';
COMMENT ON COLUMN rbac_rolepdepartment.INCLUDE_LOWER IS '包含下级';
COMMENT ON COLUMN rbac_rolepdepartment.DEPARTMENT_ID IS '部门ID';
COMMENT ON COLUMN rbac_rolepdepartment.ROLEPERMISSION_ID IS '角色权限ID';
create index INDEX_ROLEPDEPARTMENT_UP_ID on rbac_rolepdepartment(ROLEPERMISSION_ID);
/*==============================================================*/
/* Table:角色指定人员(rbac_rolepstaff)                */
/*==============================================================*/
CREATE TABLE rbac_rolepstaff
(
    ID                NUMBER(20, 0) NOT NULL ,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    STAFF_ID          NUMBER(20, 0) ,
    ROLEPERMISSION_ID NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_rolepstaff IS '角色指定人员';
COMMENT ON COLUMN rbac_rolepstaff.ID IS '主键ID';
COMMENT ON COLUMN rbac_rolepstaff.VERSION IS '版本';
COMMENT ON COLUMN rbac_rolepstaff.STAFF_ID IS '人员ID';
COMMENT ON COLUMN rbac_rolepstaff.ROLEPERMISSION_ID IS '角色权限ID';
/*==============================================================*/
/* Table:业务数据权限定义表(rbac_data_permission)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission
(
    CODE              VARCHAR(1000) NOT NULL,
    EC_ENV            VARCHAR(510) ,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    VALID             NUMBER(10, 0) DEFAULT 1,
    DELETE_TIME       TIMESTAMP ,
    MODIFY_TIME       TIMESTAMP ,
    CREATE_TIME       TIMESTAMP ,
    TERMINATOR        VARCHAR(32) ,
    MODIFIER          VARCHAR(32) ,
    CREATOR           VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0),
    ENTITY_CODE       VARCHAR(510),
    MODULE_CODE       VARCHAR(510),
    ORDER_NO          FLOAT(53) ,
    PROPERTY_CODE     VARCHAR(510) ,
    REF_VIEW_CODE     VARCHAR(510) ,
    IS_TREE           NUMBER(1, 0) DEFAULT 0,
    TARGET_MODEL_CODE VARCHAR(510) ,
    TYPE              VARCHAR(510) ,
    RELATION          VARCHAR(510) ,
    RANK              NUMBER(10, 0) ,
    MODEL_CODE        VARCHAR(510) ,
    PRIMARY KEY(CODE)
);
COMMENT ON TABLE rbac_data_permission IS '业务数据权限定义表';
COMMENT ON COLUMN rbac_data_permission.CODE IS '编码';
COMMENT ON COLUMN rbac_data_permission.EC_ENV IS '模式DEV或PRODUCT 默认PRODUCT';
COMMENT ON COLUMN rbac_data_permission.VERSION IS '版本';
COMMENT ON COLUMN rbac_data_permission.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_data_permission.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_data_permission.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_data_permission.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_data_permission.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_data_permission.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_data_permission.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_data_permission.ENTITY_CODE IS '实体编码';
COMMENT ON COLUMN rbac_data_permission.MODULE_CODE IS '模块编码';
COMMENT ON COLUMN rbac_data_permission.ORDER_NO IS '顺序';
COMMENT ON COLUMN rbac_data_permission.PROPERTY_CODE IS '字段编码';
COMMENT ON COLUMN rbac_data_permission.REF_VIEW_CODE IS '关联的参照视图编码';
COMMENT ON COLUMN rbac_data_permission.IS_TREE IS '是否树结构';
COMMENT ON COLUMN rbac_data_permission.TARGET_MODEL_CODE IS '关联模型编码';
COMMENT ON COLUMN rbac_data_permission.TYPE IS '类型';
COMMENT ON COLUMN rbac_data_permission.RELATION IS '关系';
COMMENT ON COLUMN rbac_data_permission.RANK IS '等级';
COMMENT ON COLUMN rbac_data_permission.MODEL_CODE IS '模型编码';
create index DATA_P_VALID_IDX on rbac_data_permission(VALID);
/*==============================================================*/
/* Table:业务数据权限用户权限配置临时表(rbac_data_permission_ushow)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission_ushow
(
    ID                      NUMBER(20, 0) NOT NULL ,
    VERSION                 NUMBER(10, 0) DEFAULT 0,
    DELETE_TIME             TIMESTAMP ,
    MODIFY_TIME             TIMESTAMP ,
    CREATE_TIME             TIMESTAMP ,
    TERMINATOR              VARCHAR(32) ,
    MODIFIER                VARCHAR(32) ,
    CREATOR                 VARCHAR(32) ,
    IS_ASSIGNED             NUMBER(1, 0) DEFAULT 0,
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          NUMBER(10, 0) ,
    OPERATE_ID              NUMBER(20, 0) ,
    VALUE_CODE              VARCHAR(510) ,
    VALUE_TITLE             VARCHAR(510) ,
    VALUE_ID                VARCHAR(510) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    USER_ID                 NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_data_permission_ushow IS '业务数据权限用户权限配置临时表';
COMMENT ON COLUMN rbac_data_permission_ushow.ID IS '主键ID';
COMMENT ON COLUMN rbac_data_permission_ushow.VERSION IS '版本';
COMMENT ON COLUMN rbac_data_permission_ushow.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_data_permission_ushow.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_data_permission_ushow.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_data_permission_ushow.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_data_permission_ushow.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_data_permission_ushow.IS_ASSIGNED IS '是否启用';
COMMENT ON COLUMN rbac_data_permission_ushow.IS_INCLUDE_SUB IS '是否包含上下级';
COMMENT ON COLUMN rbac_data_permission_ushow.OPERATE_ID IS '操作ID';
COMMENT ON COLUMN rbac_data_permission_ushow.VALUE_CODE IS '编码值';
COMMENT ON COLUMN rbac_data_permission_ushow.VALUE_TITLE IS '标题值';
COMMENT ON COLUMN rbac_data_permission_ushow.VALUE_ID IS 'ID值';
COMMENT ON COLUMN rbac_data_permission_ushow.DATA_PERMISSION_CODE IS '关联特殊权限';
COMMENT ON COLUMN rbac_data_permission_ushow.USER_ID IS '关联用户ID';
/*==============================================================*/
/* Table:业务数据权限角色权限配置临时表(rbac_data_permission_rshow)                */
/*==============================================================*/
CREATE TABLE rbac_data_permission_rshow
(
    ID                      NUMBER(20, 0) NOT NULL,
    VERSION                 NUMBER(10, 0) DEFAULT 0,
    DELETE_TIME             TIMESTAMP ,
    MODIFY_TIME             TIMESTAMP ,
    CREATE_TIME             TIMESTAMP ,
    DELETE_STAFF_ID         VARCHAR(32) ,
    MODIFY_STAFF_ID         VARCHAR(32) ,
    CREATE_STAFF_ID         VARCHAR(32) ,
    IS_ASSIGNED             NUMBER(1, 0) DEFAULT 0,
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          NUMBER(10, 0) ,
    OPERATE_ID              NUMBER(20, 0) ,
    VALUE_CODE              VARCHAR(510) ,
    VALUE_TITLE             VARCHAR(510) ,
    VALUE_ID                VARCHAR(510) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    ROLE_ID                 NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_data_permission_rshow IS '业务数据权限角色权限配置临时表';
COMMENT ON COLUMN rbac_data_permission_rshow.ID IS '主键ID';
COMMENT ON COLUMN rbac_data_permission_rshow.VERSION IS '版本';
COMMENT ON COLUMN rbac_data_permission_rshow.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_data_permission_rshow.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_data_permission_rshow.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_data_permission_rshow.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_data_permission_rshow.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_data_permission_rshow.IS_ASSIGNED IS '是否启用';
COMMENT ON COLUMN rbac_data_permission_rshow.IS_INCLUDE_SUB IS '是否包含上下级';
COMMENT ON COLUMN rbac_data_permission_rshow.OPERATE_ID IS '操作ID';
COMMENT ON COLUMN rbac_data_permission_rshow.VALUE_CODE IS '编码值';
COMMENT ON COLUMN rbac_data_permission_rshow.VALUE_TITLE IS '标题值';
COMMENT ON COLUMN rbac_data_permission_rshow.VALUE_ID IS 'ID值';
COMMENT ON COLUMN rbac_data_permission_rshow.DATA_PERMISSION_CODE IS '关联特殊权限';
COMMENT ON COLUMN rbac_data_permission_rshow.ROLE_ID IS '关联角色id';
/*==============================================================*/
/* Table:业务数据权限用户关联表(rbac_user_datapermission)                */
/*==============================================================*/
CREATE TABLE rbac_user_datapermission
(
    ID                      NUMBER(20, 0) NOT NULL,
    VERSION                 NUMBER(10, 0) DEFAULT 0 ,
    CONFIG_STRING           BLOB ,
    CONTENT                 VARCHAR(4000) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    USERPERMISSION_ID       NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_user_datapermission IS '业务数据权限用户关联表';
COMMENT ON COLUMN rbac_user_datapermission.ID IS '主键ID';
COMMENT ON COLUMN rbac_user_datapermission.VERSION IS '版本';
COMMENT ON COLUMN rbac_user_datapermission.CONFIG_STRING IS '配置内容';
COMMENT ON COLUMN rbac_user_datapermission.CONTENT IS 'SQL内容';
COMMENT ON COLUMN rbac_user_datapermission.USERPERMISSION_ID IS '用户权限ID';
/*==============================================================*/
/* Table:业务数据权限角色关联表(rbac_role_datapermission)                */
/*==============================================================*/
CREATE TABLE rbac_role_datapermission
(
    ID                      NUMBER(20, 0) NOT NULL ,
    VERSION                 NUMBER(10, 0) DEFAULT 0 ,
    CONFIG_STRING           BLOB,
    CONTENT                 VARCHAR(4000) ,
    DATA_PERMISSION_CODE    VARCHAR(510) ,
    ROLEPERMISSION_ID       NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_role_datapermission IS '业务数据权限角色关联表';
COMMENT ON COLUMN rbac_role_datapermission.ID IS '主键ID';
COMMENT ON COLUMN rbac_role_datapermission.VERSION IS '版本';
COMMENT ON COLUMN rbac_role_datapermission.CONFIG_STRING IS '配置内容';
COMMENT ON COLUMN rbac_role_datapermission.CONTENT IS 'SQL内容';
COMMENT ON COLUMN rbac_role_datapermission.ROLEPERMISSION_ID IS '角色权限ID';
/*==============================================================*/
/* Table:自定义权限表(rbac_custom_permission)                */
/*==============================================================*/
CREATE TABLE rbac_custom_permission
(
    CODE              VARCHAR(1000) NOT NULL ,
    EC_ENV            VARCHAR(510) ,
    VERSION           NUMBER(10, 0) DEFAULT 0 ,
    DELETE_TIME       TIMESTAMP ,
    MODIFY_TIME       TIMESTAMP ,
    CREATE_TIME       TIMESTAMP ,
    TERMINATOR        VARCHAR(32) ,
    MODIFIER          VARCHAR(32) ,
    CREATOR           VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    VALID             NUMBER(10, 0) DEFAULT 1 ,
    ENTITY_CODE       VARCHAR(510) ,
    MODULE_CODE       VARCHAR(510) ,
    MEMO              VARCHAR(510) ,
    TITLE             VARCHAR(510),
    CONDITION_SQL     VARCHAR(4000),
    JSON_CONDITION    VARCHAR(4000) ,
    VIEW_CODE         VARCHAR(510) ,
    HAND_WRITING_FLAG NUMBER(1,0) DEFAULT 0 ,
    PRIMARY KEY(CODE)
);
COMMENT ON TABLE rbac_custom_permission IS '自定义权限表';
COMMENT ON COLUMN rbac_custom_permission.CODE IS '编码';
COMMENT ON COLUMN rbac_custom_permission.EC_ENV IS '模式DEV或PRODUCT 默认PRODUCT';
COMMENT ON COLUMN rbac_custom_permission.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_custom_permission.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_custom_permission.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_custom_permission.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_custom_permission.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_custom_permission.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_custom_permission.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_custom_permission.VERSION IS '版本';
COMMENT ON COLUMN rbac_custom_permission.VALID IS '是否有效数据';
COMMENT ON COLUMN rbac_custom_permission.ENTITY_CODE IS '实体编码';
COMMENT ON COLUMN rbac_custom_permission.MODULE_CODE IS '模块编码';
COMMENT ON COLUMN rbac_custom_permission.MEMO IS '备注';
COMMENT ON COLUMN rbac_custom_permission.TITLE IS '标题';
COMMENT ON COLUMN rbac_custom_permission.CONDITION_SQL IS '条件SQL';
COMMENT ON COLUMN rbac_custom_permission.JSON_CONDITION IS 'JSON条件';
COMMENT ON COLUMN rbac_custom_permission.VIEW_CODE IS '视图编码';
COMMENT ON COLUMN rbac_custom_permission.HAND_WRITING_FLAG IS '手写自定义权限';
create index CUSTOM_P_VALID_IDX on rbac_custom_permission(VALID);
/*==============================================================*/
/* Table:自定义权限用户关联表(rbac_user_custompermission_ref)                */
/*==============================================================*/
CREATE TABLE rbac_user_custompermission_ref
(
    ID                      NUMBER(20, 0) NOT NULL ,
    VERSION                 NUMBER(10, 0) DEFAULT 0 ,
    CUSTOM_PERMISSION_CODE  VARCHAR(510) ,
    USERPERMISSION_ID       NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_user_custompermission_ref IS '自定义权限用户关联表';
COMMENT ON COLUMN rbac_user_custompermission_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_user_custompermission_ref.VERSION IS '版本';
COMMENT ON COLUMN rbac_user_custompermission_ref.CUSTOM_PERMISSION_CODE IS '其他限制编码';
COMMENT ON COLUMN rbac_user_custompermission_ref.USERPERMISSION_ID IS '用户权限ID';
/*==============================================================*/
/* Table:自定义权限角色关联表(rbac_role_custompermission_ref)                */
/*==============================================================*/
CREATE TABLE rbac_role_custompermission_ref
(
    ID                  NUMBER(20, 0) NOT NULL ,
    VERSION             NUMBER(10, 0) DEFAULT 0 ,
    CUSTOM_PERMISSION_CODE VARCHAR(510) ,
    ROLEPERMISSION_ID   NUMBER(20, 0) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_role_custompermission_ref IS '自定义权限角色关联表';
COMMENT ON COLUMN rbac_role_custompermission_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_role_custompermission_ref.VERSION IS '版本';
COMMENT ON COLUMN rbac_role_custompermission_ref.CUSTOM_PERMISSION_CODE IS '其他限制编码';
COMMENT ON COLUMN rbac_role_custompermission_ref.ROLEPERMISSION_ID IS '角色权限ID';
/*==============================================================*/
/* Table:工作流数据权限表(rbac_flow_permission)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission
(
    ID                   NUMBER(20, 0) NOT NULL,
    VERSION              NUMBER(10, 0) DEFAULT 0,
    DELETE_TIME          TIMESTAMP ,
    MODIFY_TIME          TIMESTAMP ,
    CREATE_TIME          TIMESTAMP ,
    TERMINATOR           VARCHAR(32) ,
    MODIFIER             VARCHAR(32) ,
    CREATOR              VARCHAR(32) ,
    CREATE_STAFF_ID NUMBER(20, 0)  ,
    MODIFY_STAFF_ID NUMBER(20, 0)  ,
    ENTITY_CODE          VARCHAR(510) ,
    PURVIEW_DISTRIBUTION NUMBER(10, 0),
    PURVIEW_STATE        NUMBER(10, 0),
    MEMO                 VARCHAR(510) ,
    UNLIMITED_POWER      NUMBER(1, 0) DEFAULT 0 ,
    GROUP_POWER_FLAG     NUMBER(10, 0) ,
    ASSIGN_STAFF_FLAG    NUMBER(1, 0) DEFAULT 0,
    ASSIGN_POS_FLAG      NUMBER(1, 0) DEFAULT 0,
    POSITION_POWER_FLAG  NUMBER(1, 0) DEFAULT 0 ,
    FLOW_PERMISSION_TYPE VARCHAR(510) ,
    TYPE_ID              NUMBER(20, 0) ,
    ACTIVITY_CODE        VARCHAR(510) ,
    FLOW_VERSION         VARCHAR(510) ,
    FLOW_KEY             VARCHAR(510) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_flow_permission IS '工作流数据权限表';
COMMENT ON COLUMN rbac_flow_permission.ID IS '主键ID';
COMMENT ON COLUMN rbac_flow_permission.MODIFIER IS '修改者';
COMMENT ON COLUMN rbac_flow_permission.CREATOR IS '创建者';
COMMENT ON COLUMN rbac_flow_permission.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN rbac_flow_permission.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN rbac_flow_permission.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN rbac_flow_permission.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN rbac_flow_permission.MODIFY_STAFF_ID IS '修改者人员ID';
COMMENT ON COLUMN rbac_flow_permission.VERSION IS '版本';
COMMENT ON COLUMN rbac_flow_permission.ENTITY_CODE IS '实体编码';
COMMENT ON COLUMN rbac_flow_permission.PURVIEW_DISTRIBUTION IS '权限分配来源，3工作流分配的权限';
COMMENT ON COLUMN rbac_flow_permission.PURVIEW_STATE IS '权限的来源：1流程,2开始活动';
COMMENT ON COLUMN rbac_flow_permission.MEMO IS '备注';
COMMENT ON COLUMN rbac_flow_permission.UNLIMITED_POWER IS '1:无限制';
COMMENT ON COLUMN rbac_flow_permission.GROUP_POWER_FLAG IS '组限制 0无组限制，1仅组员可见2仅组长可见';
COMMENT ON COLUMN rbac_flow_permission.ASSIGN_STAFF_FLAG IS '1:指定人员限制';
COMMENT ON COLUMN rbac_flow_permission.ASSIGN_POS_FLAG IS '1:指定岗位限制';
COMMENT ON COLUMN rbac_flow_permission.POSITION_POWER_FLAG IS '1:岗位限制 ';
COMMENT ON COLUMN rbac_flow_permission.FLOW_PERMISSION_TYPE IS '数据类型 枚举：USER,ROLE,WORKGROUP,DEPTMENT,POSITION';
COMMENT ON COLUMN rbac_flow_permission.TYPE_ID IS '根据数据权限类型对应的ID：// USERID，GROUPID，ROLEID，DEPTID，POSITIONID；';
COMMENT ON COLUMN rbac_flow_permission.ACTIVITY_CODE IS '活动编码';
COMMENT ON COLUMN rbac_flow_permission.FLOW_VERSION IS '流程版本';
COMMENT ON COLUMN rbac_flow_permission.FLOW_KEY IS '流程KEY';
create index IDX_DP_TYPEID on rbac_flow_permission(TYPE_ID);
create index IDX_DP_DPTYPE on rbac_flow_permission(FLOW_PERMISSION_TYPE);
create index IDX_DP_ENTITYCODE on rbac_flow_permission(ENTITY_CODE);
/*==============================================================*/
/* Table:工作流数据权限员工关联表(rbac_flow_permission_staff)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission_staff
(
    ID                   NUMBER(20, 0) NOT NULL,
    VERSION              NUMBER(10, 0) DEFAULT 0,
    staff_id             NUMBER(20, 0),
    flowpermission_id    NUMBER(20, 0),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_flow_permission_staff IS '工作流数据权限员工关联表';
COMMENT ON COLUMN rbac_flow_permission_staff.ID IS '主键ID';
COMMENT ON COLUMN rbac_flow_permission_staff.staff_id IS '人员ID';
COMMENT ON COLUMN rbac_flow_permission_staff.VERSION IS '版本';
COMMENT ON COLUMN rbac_flow_permission_staff.flowpermission_id IS '权限ID';
/*==============================================================*/
/* Table:工作流数据权限岗位关联表(rbac_flow_permission_position)                */
/*==============================================================*/
CREATE TABLE rbac_flow_permission_position
(
    ID                   NUMBER(20, 0) NOT NULL,
    VERSION              NUMBER(10, 0) DEFAULT 0,
    position_id          NUMBER(20, 0),
    include_lower        NUMBER(10, 0),
    flowpermission_id    NUMBER(20, 0),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_flow_permission_position IS '工作流数据权限岗位关联表';
COMMENT ON COLUMN rbac_flow_permission_position.ID IS '主键ID';
COMMENT ON COLUMN rbac_flow_permission_position.position_id IS '岗位ID';
COMMENT ON COLUMN rbac_flow_permission_position.include_lower IS '是否包含下级';
COMMENT ON COLUMN rbac_flow_permission_position.VERSION IS '版本';
COMMENT ON COLUMN rbac_flow_permission_position.flowpermission_id IS '权限ID';
/*==============================================================*/
/* Table:菜单操作编码URL关联表(rbac_menuOperateCode_url_ref)                */
/*==============================================================*/
CREATE TABLE rbac_menuOperateCode_url_ref
(
    ID                  NUMBER(20, 0) NOT NULL ,
    MENUOPERATE_CODE    VARCHAR(510) ,
    METHOD_TYPE         NUMBER(10, 0) ,
    URL                 VARCHAR(510) ,
    APP                 VARCHAR(32),
    IS_CUSTOM           NUMBER(1, 0) DEFAULT 0,
    REG_MATCH           NUMBER(1, 0) DEFAULT 0 ,
    IMPORT_TYPE         NUMBER(10, 0) DEFAULT 0,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menuOperateCode_url_ref IS '菜单操作编码URL关联表';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.MENUOPERATE_CODE IS '菜单操作编码';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.METHOD_TYPE IS '请求方法，0 GET,1 POST,2 PUT,3 DELETE';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.URL IS '对应URL';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.APP IS '应用名';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.IS_CUSTOM IS '是否自定义操作';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.REG_MATCH IS '是否需要正则匹配';
COMMENT ON COLUMN rbac_menuOperateCode_url_ref.IMPORT_TYPE IS '导入类型 0 注解导入、1 json文件导入';
create index MENU_OPERATE_CODE_IDX on rbac_menuOperateCode_url_ref(MENUOPERATE_CODE);
/*==============================================================*/
/* Table:用户与请求URL关联表(rbac_user_url_ref)                */
/*==============================================================*/
CREATE TABLE rbac_user_url_ref
(
    ID                  NUMBER(20, 0) NOT NULL,
    USER_ID             NUMBER(20, 0) ,
    URL                 VARCHAR(510) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_user_url_ref IS '用户与请求URL关联表';
COMMENT ON COLUMN rbac_user_url_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_user_url_ref.USER_ID IS '用户ID';
COMMENT ON COLUMN rbac_user_url_ref.URL IS '对应URL';
create index USER_ID_IDX on rbac_user_url_ref(USER_ID);
/*==============================================================*/
/* Table:菜单公司关联表(rbac_menuinfo_company_ref)                */
/*==============================================================*/
CREATE TABLE rbac_menuinfo_company_ref
(
    ID                  NUMBER(20, 0) NOT NULL ,
    MENUINFO_ID         NUMBER(20, 0) ,
    COMPANY_ID          NUMBER(20, 0) ,
    COMPANY_NAME        VARCHAR(510),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menuinfo_company_ref IS '菜单公司关联表';
COMMENT ON COLUMN rbac_menuinfo_company_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_menuinfo_company_ref.MENUINFO_ID IS '菜单ID';
COMMENT ON COLUMN rbac_menuinfo_company_ref.COMPANY_ID IS '公司ID';
COMMENT ON COLUMN rbac_menuinfo_company_ref.COMPANY_NAME IS '公司名';
/*==============================================================*/
/* Table:权限模块初始化版本信息表(rbac_init_verison_info)                */
/*==============================================================*/
CREATE TABLE rbac_init_verison_info
(
    ID                  NUMBER(20, 0) NOT NULL ,
    INIT_VERSION        NUMBER(10, 0) ,
    APP                 VARCHAR(510) ,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_init_verison_info IS '权限模块初始化版本信息表';
COMMENT ON COLUMN rbac_init_verison_info.ID IS '主键ID';
COMMENT ON COLUMN rbac_init_verison_info.INIT_VERSION IS '初始化脚本版本';
COMMENT ON COLUMN rbac_init_verison_info.APP IS '服务名';
INSERT INTO rbac_init_verison_info (ID,INIT_VERSION) VALUES (1,1);

/*==============================================================*/
/* Table:菜单助记码(rbac_menu_mnecode)                */
/*==============================================================*/
CREATE TABLE rbac_menu_mnecode
(
    ID                  NUMBER(20, 0) NOT NULL ,
    row_version         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    MENU_INFO           NUMBER(20, 0) ,
    MNE_CODE            VARCHAR(510),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menu_mnecode IS '菜单助记码';
COMMENT ON COLUMN rbac_menu_mnecode.ID IS '主键ID';
COMMENT ON COLUMN rbac_menu_mnecode.row_version IS '版本';
COMMENT ON COLUMN rbac_menu_mnecode.LANGUAGE IS '语言';
COMMENT ON COLUMN rbac_menu_mnecode.MNE_CODE IS '助记码';
COMMENT ON COLUMN rbac_menu_mnecode.MENU_INFO IS '菜单ID';

/*==============================================================*/
/* Table:角色助记码(rbac_role_mnecode)                */
/*==============================================================*/
CREATE TABLE rbac_role_mnecode
(
    ID                  NUMBER(20, 0) NOT NULL ,
    row_version         NUMBER(20) DEFAULT 0 NOT NULL  ,
    ROLE                NUMBER(20, 0) ,
    MNE_CODE            VARCHAR(510),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_role_mnecode IS '角色助记码';
COMMENT ON COLUMN rbac_role_mnecode.ID IS '主键ID';
COMMENT ON COLUMN rbac_role_mnecode.row_version IS '版本';
COMMENT ON COLUMN rbac_role_mnecode.MNE_CODE IS '助记码';
COMMENT ON COLUMN rbac_role_mnecode.ROLE IS '角色ID';

/*==============================================================*/
/* Table:菜单数据回滚表(rbac_menu_temp)                */
/*==============================================================*/
CREATE TABLE rbac_menu_temp
(
    ID                  NUMBER(20, 0) NOT NULL ,
    UUID                VARCHAR(200) ,
    OLD_DATA            CLOB,
    NEW_DATA            CLOB,
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_menu_temp IS '菜单数据回滚表';
COMMENT ON COLUMN rbac_menu_temp.ID IS '主键ID';
COMMENT ON COLUMN rbac_menu_temp.UUID IS '回滚标识';
COMMENT ON COLUMN rbac_menu_temp.OLD_DATA IS '老数据';
COMMENT ON COLUMN rbac_menu_temp.NEW_DATA IS '新数据';


--视图

CREATE OR REPLACE FORCE VIEW "BASE_DATAPERMISSION" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "ENTITY_CODE", "PURVIEW_DISTRIBUTION", "PURVIEW_STATE", "MEMO", "UNLIMITED_POWER", "GROUP_POWER_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "POSITION_POWER_FLAG", "DATA_PERMISSION_TYPE", "TYPE_ID", "ACTIVITY_CODE", "FLOW_VERSION", "FLOW_KEY") AS
  ( select ID as id, VERSION as version, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time, 1 as valid, ENTITY_CODE as entity_code, PURVIEW_DISTRIBUTION as purview_distribution, PURVIEW_STATE as purview_state, MEMO as memo, UNLIMITED_POWER as unlimited_power, GROUP_POWER_FLAG as group_power_flag, ASSIGN_STAFF_FLAG as assign_staff_flag, ASSIGN_POS_FLAG as assign_pos_flag, POSITION_POWER_FLAG as position_power_flag, FLOW_PERMISSION_TYPE as data_permission_type, TYPE_ID as type_id, ACTIVITY_CODE as activity_code, FLOW_VERSION as flow_version, FLOW_KEY as flow_key from rbac_flow_permission );


CREATE OR REPLACE FORCE VIEW "BASE_DATAPERMISSIONSTAFF" ("ID", "VERSION", "STAFF_ID", "DATAPERMISSION_ID") AS
  ( select id, version, staff_id, FLOWPERMISSION_ID from rbac_flow_permission_staff );


CREATE OR REPLACE FORCE VIEW "BASE_DATAPMSPOSITION" ("ID", "VERSION", "POSITION_ID", "INCLUDE_LOWER", "DATAPERMISSION_ID") AS
  ( select id, version, position_id, include_lower, FLOWPERMISSION_ID from rbac_flow_permission_position );


CREATE OR REPLACE FORCE VIEW "BASE_MENUINFO" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "CID", "SECURITY_CLASS", "ABSOLUTE_HIDDEN", "THREE_ROLE", "SHOW_TYPE", "REQUEST_TYPE", "HIDDEN_TYPE", "MENU_TYPE", "IS_HIDE", "GROUP_ONLY", "ENTITY_CODE", "EC_ENTITY_CODE", "MODULE_CODE", "SYSTEM_DEFAULT", "CSS_CLASS", "SORT", "NAMESPACE", "URL", "TARGET", "MEMO", "NAME", "NAME_ZH_CN", "CODE", "LAY_NO", "LAY_REC", "PARENT_ID", "FULL_PATH_NAME", "ACTION", "LEAF", "ST_FLAG", "ST_DIGITALSIGNATURE", "ST_INTRO", "ST_TABTYPEID", "ST_TYPE", "REMOTE_USER_NAME_NAMED", "REMOTE_PASSWORD_NAMED", "REMOTE_ID", "PIMS_MENU_TYPE", "MODULE", "ICON_URL","STATUS") AS
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

CREATE OR REPLACE FORCE VIEW "BASE_MENUOPERATE" ("ID", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "CID", "IS_ALLOW_PROXY", "IS_HIDDEN", "THREE_ROLE", "VIEW_CODE", "IS_QUERY", "IS_ORRELATION", "FOR_DATA_PERMISSION", "ENABLE_NORESTRICT", "ENABLE_OTHERRESTRICT", "ENABLE_SPECIALPERMISSION", "ENABLE_DEALERPERMISSION", "ENABLE_ASSIGNSTAFF", "ENABLE_ASSIGNPOS", "ENABLE_POSRESTRICT", "ENABLE_GROUPRESTRICT", "ENTITY_CODE", "IGNORE_PERMISSION", "POWER_FLAG", "FLOW_VERSION", "FLOW_KEY", "MSG_ASSEMBLED", "DEPLOYMENT_ID", "TYPE", "MENUINFO_ID", "ICON_CLS", "MODULE", "SORT", "MEMO", "TARGET", "NAMESPACE", "URL", "NAME_ZH_CN", "NAME", "CODE", "ACTION", "ST_FLAG", "ST_TYPE", "ST_TABLECODE", "ST_SHOWSTYLE", "ST_OPERATETYPE", "ST_ISVIEW", "ST_ISS2FLOWOPERATE", "ST_ISMAINQUERY", "ST_ISDEFAULT", "ST_FLOWKEY", "ST_DIGITALSIGNATURE", "ST_DEFAULTDISPLAY", "ST_ACTIVITYID", "MENUOPERATE_MAINOPERATECODE", "MENUOPERATE_ISCONTAINER", "MENUOPERATE_ENTRYOPERATECODE","MENUOPERATETYPE") AS
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

CREATE OR REPLACE FORCE VIEW "BASE_ROLE" ("ID", "VALID", "VERSION", "CID", "LEAF", "FULL_PATH_NAME", "PARENT_ID", "LAY_NO", "LAY_REC", "UUID", "THREE_ROLE_TYPE", "ROLE_TYPE", "SORT", "DESCRIPTION", "NAME", "CODE", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
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

CREATE OR REPLACE FORCE VIEW "BASE_ROLEPERMISSION" ("ID", "VERSION", "ROLE_ID", "MENUOPERATE_ID", "POSITION_FLAG", "GROUP_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "DEALER_PERMISSION_FLAG", "NO_RESTRICT_FLAG", "ASSIGN_SPECIALPERMISSION_FLAG", "ASSIGN_OTHERRESTRICT_FLAG", "URL_PATTERN", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
  ( select ID as id, VERSION as version, ROLE_ID as role_id, MENUOPERATE_ID as menuoperate_id, POSITION_FLAG as position_flag, GROUP_FLAG as group_flag, ASSIGN_STAFF_FLAG as assign_staff_flag, ASSIGN_POS_FLAG as assign_pos_flag, DEALER_PERMISSION_FLAG as dealer_permission_flag, NO_RESTRICT_FLAG as no_restrict_flag, ASSIGN_DATAPERMISSION_FLAG as assign_specialpermission_flag, ASSIGN_CUSTOMPERMISSION_FLAG as assign_otherrestrict_flag, URL_PATTERN as URL_PATTERN, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time from rbac_rolepermission );

CREATE OR REPLACE FORCE VIEW "BASE_ROLEPPOSITION" ("ID", "VERSION", "INCLUDE_LOWER", "POSITION_ID", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, INCLUDE_LOWER as include_lower, POSITION_ID as position_id, ROLEPERMISSION_ID as rolepermission_id from rbac_rolepposition );

CREATE OR REPLACE FORCE VIEW "BASE_ROLEPSTAFF" ("ID", "VERSION", "STAFF_ID", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, STAFF_ID as staff_id, ROLEPERMISSION_ID as rolepermission_id from rbac_rolepstaff );

CREATE OR REPLACE FORCE VIEW "BASE_ROLEUSER" ("ID", "VERSION", "POSITION_FLAG", "ROLE_ID", "USER_ID", "VALID", "END_TIME", "START_TIME") AS
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

CREATE OR REPLACE FORCE VIEW "BASE_ROLE_OTHERRESTRICT_REF" ("ID", "VERSION", "OTHER_RESTRICT_CODE", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CUSTOM_PERMISSION_CODE as other_restrict_code, ROLEPERMISSION_ID as rolepermission_id from rbac_role_custompermission_ref );

CREATE OR REPLACE FORCE VIEW "BASE_ROLE_SPECIALPERMISSION" ("ID", "VERSION", "CONFIG_STRING", "CONTENT", "SPECIAL_PERMISSION_CODE", "ROLEPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CONFIG_STRING as config_string, CONTENT as content, DATA_PERMISSION_CODE as special_permission_code, ROLEPERMISSION_ID as rolepermission_id from rbac_role_datapermission );

CREATE OR REPLACE FORCE VIEW "BASE_USERPERMISSION" ("ID", "VERSION", "USER_ID", "STAFF_ID", "CID", "MENUOPERATE_ID", "PURVIEW_TYPE", "POSITION_FLAG", "GROUP_FLAG", "ASSIGN_STAFF_FLAG", "ASSIGN_POS_FLAG", "DEALER_PERMISSION_FLAG", "NO_RESTRICT_FLAG", "ASSIGN_OTHERRESTRICT_FLAG", "ASSIGN_SPECIALPERMISSION_FLAG", "URL_PATTERN", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "ST_STATE", "ST_POWERFLAG", "ST_POWERCODE", "ST_OPERATESTATE", "ST_OPERATEID", "ST_MODULECODE", "ST_MENUID", "ST_MEMO", "ST_FLAG", "DEAL_TIME") AS
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

CREATE OR REPLACE FORCE VIEW "BASE_USERPPOSITION" ("ID", "VERSION", "INCLUDE_LOWER", "POSITION_ID", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, INCLUDE_LOWER as include_lower, POSITION_ID as position_id, USERPERMISSION_ID as userpermission_id from rbac_userpposition );

CREATE OR REPLACE FORCE VIEW "BASE_USERPSTAFF" ("ID", "VERSION", "STAFF_ID", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, STAFF_ID as staff_id, USERPERMISSION_ID as userpermission_id from rbac_userpstaff );

CREATE OR REPLACE FORCE VIEW "BASE_USER_OTHERRESTRICT_REF" ("ID", "VERSION", "OTHER_RESTRICT_CODE", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CUSTOM_PERMISSION_CODE as other_restrict_code, USERPERMISSION_ID as userpermission_id from rbac_user_custompermission_ref );

CREATE OR REPLACE FORCE VIEW "BASE_USER_SPECIALPERMISSION" ("ID", "VERSION", "CONFIG_STRING", "CONTENT", "SPECIAL_PERMISSION_CODE", "USERPERMISSION_ID") AS
  ( select ID as id, VERSION as version, CONFIG_STRING as config_string, CONTENT as content, DATA_PERMISSION_CODE as special_permission_code, USERPERMISSION_ID as userpermission_id from rbac_user_datapermission );

CREATE OR REPLACE FORCE VIEW "EC_OTHER_RESTRICT" ("CODE","HAND_WRITING_FLAG", "EC_ENV", "VERSION", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "VALID", "ENTITY_CODE", "MODULE_CODE", "MEMO", "TITLE", "CONDITION_SQL", "JSON_CONDITION", "VIEW_CODE") AS
  ( select CODE as code,HAND_WRITING_FLAG AS hand_writing_flag, EC_ENV as ec_env, VERSION as version, create_staff_id, modify_staff_id, NULL as delete_staff_id, CREATE_TIME as create_time, MODIFY_TIME as modify_time, DELETE_TIME as delete_time, VALID as valid, ENTITY_CODE as entity_code, MODULE_CODE as module_code, MEMO as memo, TITLE as title, CONDITION_SQL as condition_sql, JSON_CONDITION as json_condition, VIEW_CODE as view_code from rbac_custom_permission );


UPDATE RBAC_MENUINFO SET create_time=SYSTIMESTAMP WHERE create_time IS NULL;
alter table RBAC_MENUINFO modify create_time TimeStamp default systimestamp NOT NULL;
UPDATE RBAC_MENUINFO SET MODIFY_TIME=SYSTIMESTAMP WHERE MODIFY_TIME IS NULL;
alter table RBAC_MENUINFO modify MODIFY_TIME TimeStamp default systimestamp NOT NULL;
UPDATE RBAC_MENUINFO SET DELETE_TIME=SYSTIMESTAMP WHERE DELETE_TIME IS NULL;
alter table RBAC_MENUINFO modify DELETE_TIME TimeStamp default systimestamp NOT NULL;

## 1.0.1
/*==============================================================*/
/* Table:菜单APP关联表(rbac_app_ref)                */
/*==============================================================*/
CREATE TABLE rbac_app_ref
(
    ID                  NUMBER(20, 0) NOT NULL ,
    MENUID              NUMBER(20, 0) ,
    APPID               VARCHAR(200),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_app_ref IS '菜单APP关联表';
COMMENT ON COLUMN rbac_app_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_app_ref.MENUID IS '菜单ID';
COMMENT ON COLUMN rbac_app_ref.APPID IS 'APPID';
ALTER TABLE RBAC_APP_REF ADD CONSTRAINT RBAC_APP_REF_CONS UNIQUE (MENUID,APPID);

/* Table:APP公司关联表(rbac_app_company_ref)                */
/*==============================================================*/
CREATE TABLE rbac_app_company_ref
(
    ID                  NUMBER(20, 0) NOT NULL,
    CID                 NUMBER(20, 0),
    APPID               VARCHAR(200),
    PRIMARY KEY(ID)
);
COMMENT ON TABLE rbac_app_company_ref IS 'APP公司关联表';
COMMENT ON COLUMN rbac_app_company_ref.ID IS '主键ID';
COMMENT ON COLUMN rbac_app_company_ref.CID IS '公司ID';
COMMENT ON COLUMN rbac_app_company_ref.APPID IS 'APPID';
ALTER TABLE RBAC_APP_COMPANY_REF ADD CONSTRAINT RBAC_APP_COMPANY_REF_CONS UNIQUE (CID,APPID);
ALTER TABLE RBAC_MENUINFO_COMPANY_REF ADD APPID VARCHAR2(200);
COMMENT ON COLUMN RBAC_MENUINFO_COMPANY_REF.APPID IS 'APPID';

## 1.0.2
ALTER TABLE rbac_app_ref rename column ID to id;
ALTER TABLE rbac_app_company_ref rename column ID to id;
ALTER TABLE rbac_app_company_ref rename column CID to cid;
ALTER TABLE rbac_app_company_ref rename column APPID to appid;




## 1.0.6
/*==rbac_menuinfo 冗余字段 appid==*/
ALTER table rbac_menuinfo add appid varchar(200);
comment on column rbac_menuinfo.appid is '菜单所属app,冗余字段';
/*==rbac_menu_app_designer==*/
CREATE TABLE rbac_menu_app_designer
(
    id                  NUMBER(20, 0) NOT NULL,
    appid               VARCHAR(200),
    code                VARCHAR(200),
    parent_code         VARCHAR(200),
    PRIMARY KEY(ID)
);

COMMENT ON TABLE rbac_menu_app_designer IS '为方便获取设计器当前节点下的菜单树列表,维护App设计器与菜单的关联关系';
COMMENT ON COLUMN rbac_menu_app_designer.id IS '主键ID';
COMMENT ON COLUMN rbac_menu_app_designer.appid IS 'APP编码';
COMMENT ON COLUMN rbac_menu_app_designer.code IS '菜单编码';
COMMENT ON COLUMN rbac_menu_app_designer.parent_code IS '菜单父编码';
ALTER TABLE rbac_menu_app_designer ADD CONSTRAINT unique_menu_app_designer UNIQUE (appid,code);

--//补全菜单表 appid字段
update rbac_menuinfo rm set appid= (select appid from rbac_app_ref rar where rar.menuId = rm.ID);


## 1.0.7

CREATE TABLE rbac_data_resource_group
(
    id                        NUMBER(20, 0) NOT NULL, --COMMENT '主键ID',
    group_code                VARCHAR(4000) NOT NULL, --COMMENT '资源编码',
    group_name                VARCHAR(4000) NOT NULL, --COMMENT '资源名称',
    resource_url              VARCHAR(4000) NOT NULL, --COMMENT '资源获取地址',
    module_code               VARCHAR(1024) NOT NULL, --COMMENT '模块编码',
    cid                       NUMBER(20, 0) NULL, --COMMENT '公司ID',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL, --COMMENT '创建时间',
    create_staff_id           NUMBER(20, 0) NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               TIMESTAMP, --COMMENT '修改时间',
    modify_staff_id           NUMBER(20, 0), --COMMENT '修改者人员Id',
    constraint pk_rbac_data_resource_group PRIMARY KEY (id)
);
INSERT INTO rbac_data_resource_group (id, group_code, group_name, resource_url, module_code, creator, create_staff_id,create_time) VALUES (1, 'oodm-data-group-permission', '数据建模权限分组', '/project/dam/supngin/api/dam/v1/dataGroups', 'oodm', 'system', 0,sysdate);


CREATE TABLE rbac_role_data_permission
(
    id                        NUMBER(20, 0) NOT NULL, --COMMENT '主键ID',
    role_id                   NUMBER(20, 0) NOT NULL, --COMMENT '角色ID',
    cid                       NUMBER(20, 0) NOT NULL, --COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL, --COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL, --COMMENT '业务数据名称',
    resource_type             VARCHAR(512), --COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    valid                     NUMBER(1, 0) DEFAULT 1,  --COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL, --COMMENT '创建时间',
    create_staff_id           NUMBER(20, 0) NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               TIMESTAMP, --COMMENT '修改时间',
    modify_staff_id           NUMBER(20, 0), --COMMENT '修改者人员Id',
    constraint pk_rbac_role_data_permission PRIMARY KEY (id)
);

CREATE TABLE rbac_role_data_permission_ctrl
(
    id                        NUMBER(20, 0) NOT NULL, --COMMENT '主键ID',
    role_id                   NUMBER(20, 0) NOT NULL, --COMMENT '角色ID',
    cid                       NUMBER(20, 0) NOT NULL, --COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    controlled                NUMBER(1, 0) DEFAULT 1, --COMMENT 'group_code资源是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL, --COMMENT '创建时间',
    create_staff_id           NUMBER(20, 0) NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               TIMESTAMP, --COMMENT '修改时间',
    modify_staff_id           NUMBER(20, 0), --COMMENT '修改者人员Id',
    constraint pk_rbac_role_data_ctrl PRIMARY KEY (id),
    constraint udx_rbac_role_data_ctrl unique(role_id, cid, group_code)
);


CREATE TABLE rbac_user_data_permission
(
    id                        NUMBER(20, 0) NOT NULL, --COMMENT '主键ID',
    user_id                   NUMBER(20, 0) NOT NULL, --COMMENT '用户ID',
    cid                       NUMBER(20, 0) NOT NULL, --COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL, --COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL, --COMMENT '业务数据名称',
    resource_type             VARCHAR(512), --COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL ,--COMMENT '资源编码',
    role_id                   NUMBER(20, 0), --COMMENT '角色ID',
    purview_type              NUMBER(1, 0) NOT NULL, --COMMENT '授权方式：角色(0) 用户(1)',
    valid                     NUMBER(1, 0) DEFAULT 1, --COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL, --COMMENT '创建时间',
    create_staff_id           NUMBER(20, 0) NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               TIMESTAMP, --COMMENT '修改时间',
    modify_staff_id           NUMBER(20, 0), --COMMENT '修改者人员Id',
    constraint pk_rbac_user_data_permission PRIMARY KEY (id)
);


CREATE TABLE rbac_user_data_permission_ctrl
(
    id                        NUMBER(20, 0) NOT NULL, --COMMENT '主键ID',
    user_id                   NUMBER(20, 0) NOT NULL, --COMMENT '用户ID',
    cid                       NUMBER(20, 0) NOT NULL, --COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL, --COMMENT '资源编码',
    controlled                NUMBER(1, 0) DEFAULT 1, --COMMENT 'group_code数据分组是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL, --COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL, --COMMENT '创建时间',
    create_staff_id           NUMBER(20, 0) NOT NULL, --COMMENT '创建者人员Id',
    modifier                  VARCHAR(32), --COMMENT '修改人',
    modify_time               TIMESTAMP, --COMMENT '修改时间',
    modify_staff_id           NUMBER(20, 0), --COMMENT '修改者人员Id',
    constraint pk_rbac_user_data_ctrl PRIMARY KEY (id),
    constraint udx_rbac_user_data_ctrl unique(user_id, cid, group_code)
);

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
CREATE OR REPLACE VIEW base_role_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.role AS role,
	m.mne_code
FROM
	rbac_role_mnecode m
);

alter table rbac_menuinfo modify  URL VARCHAR2(1000);
alter table rbac_menuinfo modify  ROUTE VARCHAR2(1000);
alter table rbac_menuoperate modify  URL VARCHAR2(1000);

## 1.0.9
create index idx_rbac_userpermission_1 on rbac_userpermission(user_id,cid);
create index idx_rbac_menuinfo_1 on rbac_menuinfo(lay_no,status);

## 1.0.10
create index idx_rbac_flow_permission_1 on rbac_flow_permission(flow_permission_type, type_id);

## 1.0.11
update rbac_role set create_time = systimestamp where create_time is null;
update rbac_role set modify_time = create_time where modify_time is null;
alter table rbac_role modify create_time timestamp default systimestamp not null;
alter table rbac_role modify modify_time timestamp default systimestamp not null;

update rbac_roleuser set create_time = systimestamp where create_time is null;
update rbac_roleuser set modify_time = create_time where modify_time is null;
alter table rbac_roleuser modify create_time timestamp default systimestamp not null;
alter table rbac_roleuser modify modify_time timestamp default systimestamp not null;

update rbac_menuoperate set create_time = systimestamp where create_time is null;
update rbac_menuoperate set modify_time = create_time where modify_time is null;
alter table rbac_menuoperate modify create_time timestamp default systimestamp not null;
alter table rbac_menuoperate modify modify_time timestamp default systimestamp not null;

update rbac_menuinfo set create_time = systimestamp where create_time is null;
update rbac_menuinfo set modify_time = create_time where modify_time is null;
alter table rbac_menuinfo modify create_time timestamp default systimestamp not null;
alter table rbac_menuinfo modify modify_time timestamp default systimestamp not null;

update rbac_tag set create_time = systimestamp where create_time is null;
update rbac_tag set modify_time = create_time where modify_time is null;
alter table rbac_tag modify create_time timestamp default systimestamp not null;
alter table rbac_tag modify modify_time timestamp default systimestamp not null;

update rbac_userpermission set create_time = systimestamp where create_time is null;
update rbac_userpermission set modify_time = create_time where modify_time is null;
alter table rbac_userpermission modify create_time timestamp default systimestamp not null;
alter table rbac_userpermission modify modify_time timestamp default systimestamp not null;

update rbac_rolepermission set create_time = systimestamp where create_time is null;
update rbac_rolepermission set modify_time = create_time where modify_time is null;
alter table rbac_rolepermission modify create_time timestamp default systimestamp not null;
alter table rbac_rolepermission modify modify_time timestamp default systimestamp not null;

update rbac_data_permission set create_time = systimestamp where create_time is null;
update rbac_data_permission set modify_time = create_time where modify_time is null;
alter table rbac_data_permission modify create_time timestamp default systimestamp not null;
alter table rbac_data_permission modify modify_time timestamp default systimestamp not null;

update rbac_data_permission_ushow set create_time = systimestamp where create_time is null;
update rbac_data_permission_ushow set modify_time = create_time where modify_time is null;
alter table rbac_data_permission_ushow modify create_time timestamp default systimestamp not null;
alter table rbac_data_permission_ushow modify modify_time timestamp default systimestamp not null;

update rbac_data_permission_rshow set create_time = systimestamp where create_time is null;
update rbac_data_permission_rshow set modify_time = create_time where modify_time is null;
alter table rbac_data_permission_rshow modify create_time timestamp default systimestamp not null;
alter table rbac_data_permission_rshow modify modify_time timestamp default systimestamp not null;

update rbac_custom_permission set create_time = systimestamp where create_time is null;
update rbac_custom_permission set modify_time = create_time where modify_time is null;
alter table rbac_custom_permission modify create_time timestamp default systimestamp not null;
alter table rbac_custom_permission modify modify_time timestamp default systimestamp not null;

update rbac_flow_permission set create_time = systimestamp where create_time is null;
update rbac_flow_permission set modify_time = create_time where modify_time is null;
alter table rbac_flow_permission modify create_time timestamp default systimestamp not null;
alter table rbac_flow_permission modify modify_time timestamp default systimestamp not null;

update rbac_data_resource_group set create_time = systimestamp where create_time is null;
update rbac_data_resource_group set modify_time = create_time where modify_time is null;
alter table rbac_data_resource_group modify create_time timestamp default systimestamp not null;
alter table rbac_data_resource_group modify modify_time timestamp default systimestamp not null;

update rbac_role_data_permission set create_time = systimestamp where create_time is null;
update rbac_role_data_permission set modify_time = create_time where modify_time is null;
alter table rbac_role_data_permission modify create_time timestamp default systimestamp not null;
alter table rbac_role_data_permission modify modify_time timestamp default systimestamp not null;

update rbac_role_data_permission_ctrl set create_time = systimestamp where create_time is null;
update rbac_role_data_permission_ctrl set modify_time = create_time where modify_time is null;
alter table rbac_role_data_permission_ctrl modify create_time timestamp default systimestamp not null;
alter table rbac_role_data_permission_ctrl modify modify_time timestamp default systimestamp not null;

update rbac_user_data_permission set create_time = systimestamp where create_time is null;
update rbac_user_data_permission set modify_time = create_time where modify_time is null;
alter table rbac_user_data_permission modify create_time timestamp default systimestamp not null;
alter table rbac_user_data_permission modify modify_time timestamp default systimestamp not null;

update rbac_user_data_permission_ctrl set create_time = systimestamp where create_time is null;
update rbac_user_data_permission_ctrl set modify_time = create_time where modify_time is null;
alter table rbac_user_data_permission_ctrl modify create_time timestamp default systimestamp not null;
alter table rbac_user_data_permission_ctrl modify modify_time timestamp default systimestamp not null;

## 1.0.12
delete from rbac_menuoperatecode_url_ref where MENUOPERATE_CODE ='openMsgManager';