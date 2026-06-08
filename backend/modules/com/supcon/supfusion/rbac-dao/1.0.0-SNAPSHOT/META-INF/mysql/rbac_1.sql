## 1.0.0
/*==============================================================*/
/* TABLE: 角色表(RBAC_ROLE)                                      */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_role (
    ID BIGINT NOT NULL COMMENT '主键ID',
    VALID  INT DEFAULT 1 COMMENT '是否有效',
    VERSION INT DEFAULT 0 COMMENT '版本信息(没用到)',
    CID  BIGINT COMMENT '公司ID',
    LEAF  INT DEFAULT 0 COMMENT '是否叶子(没用到)',
    FULL_PATH_NAME  VARCHAR(4000) COMMENT '层级全路径(没用到)',
    PARENT_ID  BIGINT COMMENT '上级节点ID(没用到)',
    LAY_NO  BIGINT COMMENT '层级(没用到)',
    LAY_REC  VARCHAR(4000) COMMENT '层级结构(没用到)',
    UUID  VARCHAR(4000) COMMENT '用于软件公司同步接口(没用到)',
    THREE_ROLE_TYPE  INT COMMENT '三员类型:1系统管理员,2安全保密员 ,3安全审计员(没用到)',
    ROLE_TYPE  VARCHAR(4000) COMMENT '角色类型(没用到)',
    SORT  FLOAT(53) COMMENT '排序',
    DESCRIPTION  VARCHAR(510) COMMENT '描述',
    NAME  VARCHAR(160) COMMENT '名称',
    CODE  VARCHAR(160) COMMENT '编码',
    CREATOR VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    MODIFIER VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    TERMINATOR VARCHAR(32) DEFAULT NULL COMMENT '删除者(没用到)',
    CREATE_TIME TIMESTAMP COMMENT '创建时间',
    MODIFY_TIME TIMESTAMP COMMENT '修改时间',
    DELETE_TIME TIMESTAMP COMMENT '删除时间(没用到)',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    TAG VARCHAR(510) COMMENT '标签(没用到)',
    PRIMARY KEY (ID),
    INDEX BASE_ROLE_VALID_IDX (VALID),
    INDEX INDEX_ROLE_CODE (CODE),
    INDEX INDEX_ROLE_NAME (NAME),
    INDEX INDEX_ROLE_TAG (TAG)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色表';
/*==============================================================*/
/* TABLE:角色用户表(rbac_roleuser)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_roleuser(
  ID BIGINT NOT NULL COMMENT '主键ID',
  VERSION INT DEFAULT 0 COMMENT '版本信息(没用到)',
  POSITION_FLAG INT DEFAULT 0 COMMENT '是否仅是岗位带入的角色(没用到)',
  ROLE_ID BIGINT COMMENT '角色ID',
  USER_ID BIGINT COMMENT '用户ID',
  VALID INT DEFAULT 1 COMMENT '是否有效',
  END_TIME TIMESTAMP(6) COMMENT '调出时间(没用到)',
  START_TIME TIMESTAMP(6) COMMENT '调入时间(没用到)',
  PERSON_NAME VARCHAR(160) COMMENT '人员姓名',
  PERSON_CODE VARCHAR(160) COMMENT '人员编号',
  USER_NAME VARCHAR(160) COMMENT '用户名',
  TERMINATOR VARCHAR(32) COMMENT '删除者(没用到)',
  MODIFIER VARCHAR(32) COMMENT '修改者',
  CREATOR VARCHAR(32) COMMENT '创建者',
  CREATE_TIME TIMESTAMP COMMENT '创建时间',
  MODIFY_TIME TIMESTAMP COMMENT '修改时间',
  DELETE_TIME TIMESTAMP COMMENT '删除时间(没用到)',
  CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
  MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
  FROM_POSITION INT DEFAULT 1 COMMENT '来源 1 来源于用户 2 来源于岗位 3两者都有',
  PRIMARY KEY (ID),
  INDEX INDEX_ROLEUSER_ROLE_ID (ROLE_ID),
  INDEX INDEX_ROLEUSER_USER_ID (USER_ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色用户表';
/*==============================================================*/
/* TABLE:操作表(RBAC_MENUOPERATE)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menuoperate
(
    ID BIGINT NOT NULL COMMENT '主键ID',
    ROW_VERSION BIGINT DEFAULT 0 COMMENT '版本(没用到)',
    DELETE_TIME TIMESTAMP COMMENT '删除时间(没用到)',
    MODIFY_TIME TIMESTAMP COMMENT '修改时间',
    CREATE_TIME TIMESTAMP COMMENT '创建时间',
    TERMINATOR VARCHAR(32) COMMENT '删除者(没用到)',
    MODIFIER VARCHAR(32) COMMENT '修改者',
    CREATOR VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    VALID INT DEFAULT 1 COMMENT '是否有效',
    CID BIGINT COMMENT '公司ID',
    IS_ALLOW_PROXY INT DEFAULT 0 COMMENT '是否允许委托',
    IS_HIDDEN INT DEFAULT 0 COMMENT '是否隐藏',
    THREE_ROLE INT DEFAULT 0 COMMENT '是否三员菜单(没用到)',
    VIEW_CODE VARCHAR(510) COMMENT '视图编码(BAP使用)',
    IS_QUERY INT DEFAULT 0 COMMENT '是否查询操作(BAP使用)',
    IS_ORRELATION INT COMMENT '该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND(BAP使用)',
    ENABLE_DATAPERMISSION INT DEFAULT 0 COMMENT '启用数据权限(BAP使用)',
    ENABLE_CUSTOMPERMISSION INT DEFAULT 0 COMMENT '启用自定义权限(BAP使用)',
    FOR_FLOW_PERMISSION INT DEFAULT 0 COMMENT '启用业务权限(BAP使用)',
    ENABLE_NORESTRICT INT DEFAULT 1 COMMENT '无限制',
    ENABLE_DEALERPERMISSION INT DEFAULT 0 COMMENT '启用处理人',
    ENABLE_ASSIGNSTAFF INT DEFAULT 0 COMMENT '启用指定人员',
    ENABLE_ASSIGNPOS INT DEFAULT 0 COMMENT '启用指定岗位',
    ENABLE_POSRESTRICT INT DEFAULT 0 COMMENT '岗位限制',
    ENABLE_DEPTRICT INT DEFAULT 0 COMMENT '部门限制',
    ENABLE_ASSIGNDEPT INT DEFAULT 0 COMMENT '启用指定部门',
    ENABLE_GROUPRESTRICT INT DEFAULT 0 COMMENT '启用组限制(没用到)',
    ENTITY_CODE VARCHAR(510) COMMENT '实体编码(BAP使用)',
    IGNORE_PERMISSION INT DEFAULT 0 COMMENT '忽视权限(BAP使用)',
    POWER_FLAG INT DEFAULT 0 COMMENT '是否是主列表视图的查询操作(BAP使用)',
    FLOW_VERSION VARCHAR(510) COMMENT '工作流版本(BAP使用)',
    FLOW_KEY VARCHAR(510) COMMENT '工作流KEY(BAP使用,用于判断是否为工作流操作)',
    MSG_ASSEMBLED INT COMMENT '(BAP使用)',
    DEPLOYMENT_ID BIGINT COMMENT '(BAP使用)',
    MENUOPERATETYPE VARCHAR(510) COMMENT '操作类型(BAP使用)',
    MENUINFO_ID BIGINT COMMENT '菜单ID',
    ICON_CLS VARCHAR(510) COMMENT '图表样式(BAP使用)',
    MODULE_CODE VARCHAR(510) COMMENT '模块信息：BUNDLE的SYMBOLICNAME组成(BAP使用)',
    SORT FLOAT(53) COMMENT '排序',
    MEMO VARCHAR(510) COMMENT '备注',
    TARGET VARCHAR(510) COMMENT '打开方式',
    ACTION_URL VARCHAR(510) COMMENT 'ACTION(BAP使用)',
    NAMESPACE VARCHAR(510) COMMENT '命名空间(BAP使用)',
    URL VARCHAR(510) COMMENT '地址(BAP使用)',
    NAME_ZH_CN VARCHAR(510) COMMENT '中文名(BAP使用)',
    NAME VARCHAR(510) COMMENT '名称',
    NAME_DISPLAY VARCHAR(510) COMMENT '国际化值 存默认名称',
    CODE VARCHAR(510) COMMENT '编码',
    APP VARCHAR(510) COMMENT '所属应用名',
    DEFAULT_OPERATE INT DEFAULT 0 COMMENT '默认操作标识，默认操作不可删除',
    EDITED INT DEFAULT 0 COMMENT '是否修改过 修改过的操作升级时不修改',
    PRIMARY KEY(ID),
    INDEX INDEX_MENUOPERATE_CODE (CODE),
    INDEX INDEX_MENUOPERATE_NAME (NAME),
    INDEX IND_MENUOPERATE_URL (URL),
    INDEX IND_MENUOPERATE_NAMESPACE (NAMESPACE),
    INDEX IND_MENUOPERATE_MENUINFO_ID (MENUINFO_ID),
    INDEX IDX_MENUOPERATE_ENTITYCODE (ENTITY_CODE),
    INDEX BASE_MENUOPERATE_VALID_IDX (VALID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='操作表';
/*==============================================================*/
/* TABLE:菜单表(rbac_menuinfo)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menuinfo
(
    ID BIGINT NOT NULL COMMENT '主键ID',
    VERSION INT DEFAULT 0 COMMENT '版本(没用到)',
    DELETE_TIME TIMESTAMP COMMENT '删除时间(没用到)' DEFAULT NOW(),
    MODIFY_TIME TIMESTAMP COMMENT '修改时间' DEFAULT NOW(),
    CREATE_TIME TIMESTAMP COMMENT '创建时间' DEFAULT NOW(),
    TERMINATOR VARCHAR(32) COMMENT '删除者(没用到)',
    LEAF INT DEFAULT 0 COMMENT '是否是叶子节点(BAP使用)',
    MODIFIER VARCHAR(32) COMMENT '修改者',
    CREATOR VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    VALID INT DEFAULT 1 COMMENT '是否有效',
    CID BIGINT COMMENT '公司ID',
    SECURITY_CLASS VARCHAR(510) COMMENT '密级(没用到)',
    ABSOLUTE_HIDDEN INT DEFAULT 0 COMMENT '绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏(没用到)',
    THREE_ROLE INT DEFAULT 0 COMMENT '是否三员菜单(没用到)',
    SHOW_TYPE INT COMMENT '请求方式0:链接页面，1：链接URL',
    REQUEST_TYPE INT COMMENT '请求类型(没用到)',
    HIDDEN_TYPE INT COMMENT '隐藏类型(没用到)',
    MENU_TYPE INT COMMENT '菜单类型',
    IS_HIDE INT DEFAULT 0 COMMENT '是否隐藏',
    GROUP_ONLY INT DEFAULT 0 COMMENT '是否仅集团使用(没用到)',
    ENTITY_CODE VARCHAR(510) COMMENT '实体编码(BAP使用)',
    MODULE_CODE VARCHAR(510) COMMENT '模块信息：BUNDLE的SYMBOLICNAME组成(BAP使用)',
    SYSTEM_DEFAULT INT DEFAULT 0 COMMENT '是否默认系统(BAP使用)',
    CSS_CLASS VARCHAR(510) COMMENT 'CSS_CLASS(菜单样式用)',
    SORT FLOAT(53) COMMENT '排序',
    ACTION_URL VARCHAR(510) COMMENT 'ACTION(没用到)',
    NAMESPACE VARCHAR(510) COMMENT '命名空间(没用到)',
    URL VARCHAR(510) COMMENT '地址',
    TARGET VARCHAR(510) default 'SELF' COMMENT '打开方式',
    MEMO VARCHAR(510) COMMENT '备注',
    NAME VARCHAR(510) COMMENT '名称',
    NAME_DISPLAY VARCHAR(510) COMMENT '国际化值 存默认名称',
    CODE VARCHAR(510) COMMENT '编码',
    APP VARCHAR(510) COMMENT '所属应用名',
    ENABLE INT DEFAULT 1 COMMENT '是否启用',
    LAY_NO INT COMMENT '层级',
    LAY_REC VARCHAR(4000) COMMENT '层级结构',
    PARENT_ID BIGINT COMMENT '上级节点ID',
    FULL_PATH VARCHAR(4000) COMMENT '层级全路径',
    FULL_PATH_NAME VARCHAR(4000) COMMENT '层级全路径 菜单名',
    SOURCE VARCHAR(510) COMMENT '来源',
    EDITED INT DEFAULT 0 COMMENT '是否修改过 修改过的菜单升级时不修改',
    TYPE INT DEFAULT 0 COMMENT '资源类型 0是菜单，后续更多的请看枚举类(没用到)',
    NO_RESTRICT INT DEFAULT 0 COMMENT '是否不受权限控制',
    STATUS INT DEFAULT 0 COMMENT '组态期 0，运行期 1, 两者都有 2',
    ROUTE VARCHAR(510) COMMENT '地址',
    EXTRA VARCHAR(1000) COMMENT '额外信息(没用到)',
    PRIMARY KEY(ID),
    INDEX IDX_MENUINFO_CODE (CODE),
    INDEX INDEX_MENUINFO_NAME (NAME),
    INDEX IDX_MENUINFO_MODULECODE (MODULE_CODE),
    INDEX BASE_MENUINFO_VALID_IDX (VALID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单表';
--ALTER TABLE rbac_menuinfo CHANGE APP MODULE_CODE_NO_VERSION  VARCHAR(510) COMMENT'模块编码';
/*===================================================================*/
/* Table: 标签表(rbac_tag)     没用到                                 */
/*===================================================================*/
create table if not exists rbac_tag (
    ID BIGINT NOT NULL COMMENT '主键ID',
    VERSION INT DEFAULT 0 COMMENT '版本号',
    TYPE VARCHAR(32) DEFAULT NULL COMMENT '标签类型',
    NAME VARCHAR(100) NOT NULL COMMENT '标签名',
    CID BIGINT NOT NULL COMMENT '公司ID',
    OBJECTID BIGINT NOT NULL COMMENT '关联ID',
    CREATOR VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    MODIFIER VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    VALID INT DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
    CREATE_TIME TIMESTAMP COMMENT '创建时间',
    MODIFY_TIME TIMESTAMP COMMENT '修改时间',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='标签表';
/*==============================================================*/
/* Table:用户权限表(rbac_userpermission)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_userpermission
(
    ID                            BIGINT NOT NULL COMMENT '主键ID',
    VERSION                       INT DEFAULT 0 COMMENT '版本信息(没用到)',
    USER_ID                       BIGINT COMMENT '用户ID',
    DEAL_STAFF                    BIGINT COMMENT '处理员工ID(没用到)',
    CID                           BIGINT COMMENT '公司',
    MENUOPERATE_ID                BIGINT COMMENT '菜单操作ID',
    PURVIEW_TYPE                  INT COMMENT '授权方式：角色(0)OR用户(1)',
    POSITION_FLAG                 INT DEFAULT 0 COMMENT '岗位限制',
    DEPARTMENT_FLAG               INT DEFAULT 0 COMMENT '部门限制',
    GROUP_FLAG                    INT DEFAULT 0 COMMENT '组限制：0 1 2',
    ASSIGN_STAFF_FLAG             INT DEFAULT 0 COMMENT '指定人员',
    ASSIGN_POS_FLAG               INT DEFAULT 0 COMMENT '指定岗位',
    ASSIGN_DEPT_FLAG              INT DEFAULT 0 COMMENT '指定部门',
    DEALER_PERMISSION_FLAG        INT DEFAULT 0 COMMENT '处理人权限：0 1',
    NO_RESTRICT_FLAG              INT DEFAULT 0 COMMENT '无限制',
    ASSIGN_DATAPERMISSION_FLAG    INT DEFAULT 0 COMMENT '指定业务数据权限限制：0 1',
    ASSIGN_CUSTOMPERMISSION_FLAG  INT DEFAULT 0 COMMENT '指定自定义权限(没用到)',
    URL_PATTERN                   VARCHAR(510) COMMENT 'URL正则(没用到)',
    MENUOPERATE_CODE              VARCHAR(510) COMMENT '冗余操作编码(没用到)',
    DELETE_TIME TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME TIMESTAMP COMMENT '修改时间',
    CREATE_TIME TIMESTAMP COMMENT '创建时间',
    TERMINATOR VARCHAR(32) COMMENT '删除者',
    MODIFIER VARCHAR(32) COMMENT '修改者',
    CREATOR VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    INDEX IND_USERPERMISSION_USER_ID (USER_ID),
    INDEX INDEX_USERPERMISSION_MO (MENUOPERATE_ID),
    PRIMARY KEY(ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='用户权限表';
/*==============================================================*/
/* Table:角色权限表(rbac_rolepermission)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_rolepermission
(
    ID                            BIGINT NOT NULL COMMENT '主键ID',
    CID                           BIGINT COMMENT '公司ID',
    VERSION                       INT DEFAULT 0 COMMENT '版本(没用到)',
    ROLE_ID                       BIGINT COMMENT '角色ID',
    MENUOPERATE_ID                BIGINT COMMENT '菜单操作ID',
    POSITION_FLAG                 INT DEFAULT 0 COMMENT '岗位限制',
    DEPARTMENT_FLAG               INT DEFAULT 0 COMMENT '部门限制',
    GROUP_FLAG                    INT DEFAULT 0 COMMENT '组限制：0 1 2',
    ASSIGN_STAFF_FLAG             INT DEFAULT 0 COMMENT '指定人员',
    ASSIGN_POS_FLAG               INT DEFAULT 0 COMMENT '指定岗位',
    ASSIGN_DEPT_FLAG              INT DEFAULT 0 COMMENT '指定部门',
    DEALER_PERMISSION_FLAG        INT DEFAULT 0 COMMENT '处理人权限：0 1',
    NO_RESTRICT_FLAG              INT DEFAULT 0 COMMENT '无限制',
    ASSIGN_DATAPERMISSION_FLAG    INT DEFAULT 0 COMMENT '指定业务数据权限限制：0 1',
    ASSIGN_CUSTOMPERMISSION_FLAG  INT DEFAULT 0 COMMENT '指定自定义权限(没用到)',
    URL_PATTERN                   VARCHAR(510) COMMENT 'URL正则(没用到)',
    DELETE_TIME TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME TIMESTAMP COMMENT '修改时间',
    CREATE_TIME TIMESTAMP COMMENT '创建时间',
    TERMINATOR VARCHAR(32) COMMENT '删除者',
    MODIFIER VARCHAR(32) COMMENT '修改者',
    CREATOR VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    INDEX INDEX_RM_ROLE_ID (ROLE_ID),
    INDEX INDEX_RM_MENUOPERATE_ID (MENUOPERATE_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色权限表';
/*==============================================================*/
/* Table:用户指定岗位表(rbac_userpposition)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_userpposition
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    INCLUDE_LOWER     INT DEFAULT 0 COMMENT '包含下级(没用到)',
    POSITION_ID       BIGINT COMMENT '岗位ID',
    USERPERMISSION_ID BIGINT COMMENT '用户权限ID',
    INDEX INDEX_USERPPOSITION_UP_ID (USERPERMISSION_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='用户指定岗位表';
/*==============================================================*/
/* Table:用户指定部门表(rbac_userpdepartment)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_userpdepartment
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    INCLUDE_LOWER     INT DEFAULT 0 COMMENT '包含下级(没用到)',
    DEPARTMENT_ID       BIGINT COMMENT '部门ID',
    USERPERMISSION_ID BIGINT COMMENT '用户权限ID',
    INDEX INDEX_USERPDEPARTMENT_UP_ID (USERPERMISSION_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='用户指定部门表';
/*==============================================================*/
/* Table:用户指定人员(rbac_userpstaff)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_userpstaff
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    STAFF_ID          BIGINT COMMENT '人员ID',
    USERPERMISSION_ID BIGINT COMMENT '用户权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='用户指定人员';
/*==============================================================*/
/* Table:角色指定岗位表(rbac_rolepposition)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_rolepposition
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    INCLUDE_LOWER     INT DEFAULT 0 COMMENT '包含下级(没用到)',
    POSITION_ID       BIGINT COMMENT '岗位ID',
    ROLEPERMISSION_ID BIGINT COMMENT '角色权限ID',
    INDEX INDEX_USERPPOSITION_UP_ID (ROLEPERMISSION_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色指定岗位表';
/*==============================================================*/
/* Table:角色指定部门表(rbac_rolepdepartment)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_rolepdepartment
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    INCLUDE_LOWER     INT DEFAULT 0 COMMENT '包含下级(没用到)',
    DEPARTMENT_ID       BIGINT COMMENT '部门ID',
    ROLEPERMISSION_ID BIGINT COMMENT '角色权限ID',
    INDEX INDEX_ROLEPDEPARTMENT_UP_ID (ROLEPERMISSION_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色指定部门表';
/*==============================================================*/
/* Table:角色指定人员(rbac_rolepstaff)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_rolepstaff
(
    ID                BIGINT NOT NULL COMMENT '主键ID',
    VERSION           INT DEFAULT 0 COMMENT '版本(没用到)',
    STAFF_ID          BIGINT COMMENT '人员ID',
    ROLEPERMISSION_ID BIGINT COMMENT '角色权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色指定人员';
/*==============================================================*/
/* Table:业务数据权限定义表(rbac_data_permission)      没用到          */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_data_permission
(
    CODE              VARCHAR(1000) NOT NULL COMMENT '编码',
    EC_ENV            VARCHAR(510) COMMENT '模式DEV或PRODUCT 默认PRODUCT',
    VERSION           INT DEFAULT 0 COMMENT '版本',
    VALID             INT DEFAULT 1 COMMENT '是否有效数据',
    DELETE_TIME       TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME       TIMESTAMP COMMENT '修改时间',
    CREATE_TIME       TIMESTAMP COMMENT '创建时间',
    TERMINATOR        VARCHAR(32) COMMENT '删除者',
    MODIFIER          VARCHAR(32) COMMENT '修改者',
    CREATOR           VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    ENTITY_CODE       VARCHAR(510) COMMENT '实体编码',
    MODULE_CODE       VARCHAR(510) COMMENT '模块编码',
    ORDER_NO          FLOAT(53) COMMENT '顺序',
    PROPERTY_CODE     VARCHAR(510) COMMENT '字段编码',
    REF_VIEW_CODE     VARCHAR(510) COMMENT '关联的参照视图编码',
    IS_TREE           INT DEFAULT 0 COMMENT '是否树结构',
    TARGET_MODEL_CODE VARCHAR(510) COMMENT '关联模型编码',
    TYPE              VARCHAR(510) COMMENT '类型',
    RELATION          VARCHAR(510) COMMENT '关系',
    RANK              INT COMMENT '等级',
    MODEL_CODE        VARCHAR(510) COMMENT '模型编码',
    INDEX DATA_P_VALID_IDX (VALID),
    PRIMARY KEY(CODE)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='业务数据权限定义表';
/*==============================================================*/
/* Table:业务数据权限用户权限配置临时表(rbac_data_permission_ushow)      没用到          */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_data_permission_ushow
(
    ID                      BIGINT NOT NULL COMMENT '主键ID',
    VERSION                 INT DEFAULT 0 COMMENT '版本',
    DELETE_TIME             TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME             TIMESTAMP COMMENT '修改时间',
    CREATE_TIME             TIMESTAMP COMMENT '创建时间',
    TERMINATOR              VARCHAR(32) COMMENT '删除者',
    MODIFIER                VARCHAR(32) COMMENT '修改者',
    CREATOR                 VARCHAR(32) COMMENT '创建者',
    IS_ASSIGNED             INT DEFAULT 0 COMMENT '是否启用',
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          INT DEFAULT 0 COMMENT '是否包含上下级',
    OPERATE_ID              BIGINT COMMENT '操作ID',
    VALUE_CODE              VARCHAR(510) COMMENT '编码值',
    VALUE_TITLE             VARCHAR(510) COMMENT '标题值',
    VALUE_ID                VARCHAR(510) COMMENT 'ID值',
    DATA_PERMISSION_CODE    VARCHAR(510) COMMENT '关联特殊权限',
    USER_ID                 BIGINT COMMENT '关联用户ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='业务数据权限用户权限配置临时表';
/*==============================================================*/
/* Table:业务数据权限角色权限配置临时表(rbac_data_permission_rshow)      没用到          */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_data_permission_rshow
(
    ID                      BIGINT NOT NULL COMMENT '主键ID',
    VERSION                 INT DEFAULT 0 COMMENT '版本',
    DELETE_TIME             TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME             TIMESTAMP COMMENT '修改时间',
    CREATE_TIME             TIMESTAMP COMMENT '创建时间',
    DELETE_STAFF_ID         VARCHAR(32) COMMENT '删除者',
    MODIFY_STAFF_ID         VARCHAR(32) COMMENT '修改者',
    CREATE_STAFF_ID         VARCHAR(32) COMMENT '创建者',
    IS_ASSIGNED             INT DEFAULT 0 COMMENT '是否启用',
    LAY_REC                 VARCHAR(510),
    IS_INCLUDE_SUB          INT DEFAULT 0 COMMENT '是否包含上下级',
    OPERATE_ID              BIGINT COMMENT '操作ID',
    VALUE_CODE              VARCHAR(510) COMMENT '编码值',
    VALUE_TITLE             VARCHAR(510) COMMENT '标题值',
    VALUE_ID                VARCHAR(510) COMMENT 'ID值',
    DATA_PERMISSION_CODE    VARCHAR(510) COMMENT '关联特殊权限',
    ROLE_ID                 BIGINT comment '关联角色id',
    PRIMARY KEY(ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='业务数据权限角色权限配置临时表';
/*==============================================================*/
/* Table:业务数据权限用户关联表(rbac_user_datapermission)         没用到       */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_user_datapermission
(
    ID                      BIGINT NOT NULL COMMENT '主键ID',
    VERSION                 INT DEFAULT 0 COMMENT '版本',
    CONFIG_STRING           LONGTEXT COMMENT '配置内容',
    CONTENT                 VARCHAR(4000) COMMENT 'SQL内容',
    DATA_PERMISSION_CODE    VARCHAR(510) COMMENT '其他限制编码',
    USERPERMISSION_ID       BIGINT COMMENT '用户权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='业务数据权限用户关联表';
/*==============================================================*/
/* Table:业务数据权限角色关联表(rbac_role_datapermission)       没用到         */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_role_datapermission
(
    ID                      BIGINT NOT NULL COMMENT '主键ID',
    VERSION                 INT DEFAULT 0 COMMENT '版本',
    CONFIG_STRING           LONGTEXT COMMENT '配置内容',
    CONTENT                 VARCHAR(4000) COMMENT 'SQL内容',
    DATA_PERMISSION_CODE    VARCHAR(510) COMMENT '其他限制编码',
    ROLEPERMISSION_ID       BIGINT COMMENT '角色权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='业务数据权限角色关联表';
/*==============================================================*/
/* Table:自定义权限表(rbac_custom_permission)       没用到         */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_custom_permission
(
    CODE              VARCHAR(1000) NOT NULL COMMENT '编码',
    EC_ENV            VARCHAR(510) COMMENT '模式DEV或PRODUCT 默认PRODUCT',
    VERSION           INT DEFAULT 0 COMMENT '版本',
    DELETE_TIME       TIMESTAMP COMMENT '删除时间',
    MODIFY_TIME       TIMESTAMP COMMENT '修改时间',
    CREATE_TIME       TIMESTAMP COMMENT '创建时间',
    TERMINATOR        VARCHAR(32) COMMENT '删除者',
    MODIFIER          VARCHAR(32) COMMENT '修改者',
    CREATOR           VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID BIGINT COMMENT '修改者人员ID',
    VALID             INT DEFAULT 1 COMMENT '是否有效数据',
    ENTITY_CODE       VARCHAR(510) COMMENT '实体编码',
    MODULE_CODE       VARCHAR(510) COMMENT '模块编码',
    MEMO              VARCHAR(510) COMMENT '备注',
    TITLE             VARCHAR(510) COMMENT '标题',
    CONDITION_SQL     VARCHAR(4000) COMMENT '条件SQL',
    JSON_CONDITION    VARCHAR(4000) COMMENT 'JSON条件',
    VIEW_CODE         VARCHAR(510) COMMENT '视图编码',
    HAND_WRITING_FLAG INT DEFAULT 0 COMMENT '手写自定义条件',
    INDEX CUSTOM_P_VALID_IDX (VALID),
    PRIMARY KEY(CODE)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='自定义权限表';
/*==============================================================*/
/* Table:自定义权限用户关联表(rbac_user_custompermission_ref)     没用到           */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_user_custompermission_ref
(
    ID                      BIGINT NOT NULL COMMENT '主键ID',
    VERSION                 INT DEFAULT 0 COMMENT '版本',
    CUSTOM_PERMISSION_CODE  VARCHAR(510) COMMENT '其他限制编码',
    USERPERMISSION_ID       BIGINT COMMENT '用户权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='自定义权限用户关联表';
/*==============================================================*/
/* Table:自定义权限角色关联表(rbac_role_custompermission_ref)      没用到          */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_role_custompermission_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    VERSION             INT DEFAULT 0 COMMENT '版本',
    CUSTOM_PERMISSION_CODE VARCHAR(510) COMMENT '其他限制编码',
    ROLEPERMISSION_ID   BIGINT COMMENT '角色权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='自定义权限角色关联表';
/*==============================================================*/
/* Table:工作流数据权限表(rbac_flow_permission)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_flow_permission
(
    ID                   BIGINT NOT NULL COMMENT '主键ID',
    VERSION              INT DEFAULT 0 COMMENT '版本(没用到)',
    DELETE_TIME          TIMESTAMP COMMENT '删除时间(没用到)',
    MODIFY_TIME          TIMESTAMP COMMENT '修改时间',
    CREATE_TIME          TIMESTAMP COMMENT '创建时间',
    TERMINATOR           VARCHAR(32) COMMENT '删除者(没用到)',
    MODIFIER             VARCHAR(32) COMMENT '修改者',
    CREATOR              VARCHAR(32) COMMENT '创建者',
    CREATE_STAFF_ID      BIGINT COMMENT '创建者人员ID',
    MODIFY_STAFF_ID      BIGINT COMMENT '修改者人员ID',
    ENTITY_CODE          VARCHAR(510) COMMENT '实体编码(BAP使用)',
    PURVIEW_DISTRIBUTION INT COMMENT '权限分配来源，3工作流分配的权限',
    PURVIEW_STATE        INT COMMENT '权限的来源：1流程,2开始活动',
    MEMO                 VARCHAR(510) COMMENT '备注',
    UNLIMITED_POWER      INT DEFAULT 0 COMMENT '1:无限制',
    GROUP_POWER_FLAG     INT COMMENT '组限制 0无组限制，1仅组员可见2仅组长可见',
    ASSIGN_STAFF_FLAG    INT DEFAULT 0 COMMENT '1:指定人员限制',
    ASSIGN_POS_FLAG      INT DEFAULT 0 COMMENT '1:指定岗位限制',
    POSITION_POWER_FLAG  INT DEFAULT 0 COMMENT '1:岗位限制 ',
    FLOW_PERMISSION_TYPE VARCHAR(510) COMMENT '数据类型 枚举：USER,ROLE,WORKGROUP(没用到),DEPTMENT(没用到),POSITION(没用到)',
    TYPE_ID              BIGINT COMMENT '根据数据权限类型对应的ID：// USERID，GROUPID(没用到)，ROLEID，DEPTID(没用到)，POSITIONID(没用到)；',
    ACTIVITY_CODE        VARCHAR(510) COMMENT '活动编码(对应工作流操作的code)',
    FLOW_VERSION         VARCHAR(510) COMMENT '流程版本',
    FLOW_KEY             VARCHAR(510) COMMENT '流程KEY',
    INDEX IDX_DP_TYPEID (TYPE_ID),
    INDEX IDX_DP_DPTYPE (FLOW_PERMISSION_TYPE),
    INDEX IDX_DP_ENTITYCODE (ENTITY_CODE),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='工作流数据权限表';
/*==============================================================*/
/* Table:工作流数据权限员工关联表(rbac_flow_permission_staff)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_flow_permission_staff
(
    ID                   BIGINT NOT NULL COMMENT '主键ID',
    VERSION              INT DEFAULT 0 COMMENT '版本(没用到)',
    staff_id             BIGINT COMMENT '人员ID',
    flowpermission_id    BIGINT COMMENT '权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='工作流数据权限员工关联表';
/*==============================================================*/
/* Table:工作流数据权限岗位关联表(rbac_flow_permission_position)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_flow_permission_position
(
    ID                   BIGINT NOT NULL COMMENT '主键ID',
    VERSION              INT DEFAULT 0 COMMENT '版本(没用到)',
    position_id          BIGINT COMMENT '岗位ID',
    include_lower        INT DEFAULT 0 COMMENT '是否包含下级(没用到)',
    flowpermission_id    BIGINT COMMENT '权限ID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='工作流数据权限岗位关联表';
/*==============================================================*/
/* Table:菜单操作编码URL关联表(rbac_menuoperatecode_url_ref)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menuoperatecode_url_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    MENUOPERATE_CODE    VARCHAR(510) COMMENT '菜单操作编码',
    METHOD_TYPE         INT COMMENT '请求方法，0 GET,1 POST,2 PUT,3 DELETE',
    URL                 VARCHAR(510) COMMENT '对应URL',
    APP                 VARCHAR(32) COMMENT '应用名',
    IS_CUSTOM           INT DEFAULT 0 COMMENT '是否自定义操作',
    REG_MATCH           INT DEFAULT 0 COMMENT '是否需要正则匹配',
    IMPORT_TYPE         INT DEFAULT 0 COMMENT '导入类型 0 注解导入、1 json文件导入',
    INDEX MENU_OPERATE_CODE_IDX (MENUOPERATE_CODE),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单操作编码URL关联表';
/*==============================================================*/
/* Table:用户与请求URL关联表(rbac_user_url_ref)         没用到       */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_user_url_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    USER_ID             BIGINT COMMENT '用户ID',
    URL                 VARCHAR(510) COMMENT '对应URL',
    INDEX USER_ID_IDX (USER_ID),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='用户与请求URL关联表';

/*==============================================================*/
/* Table:菜单公司关联表(rbac_menuinfo_company_ref)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menuinfo_company_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    MENUINFO_ID         BIGINT COMMENT '菜单ID',
    COMPANY_ID          BIGINT COMMENT '公司ID',
    COMPANY_NAME        VARCHAR(510) COMMENT '公司名',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单公司关联表';

/*==============================================================*/
/* Table:权限模块初始化版本信息表(rbac_init_verison_info)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_init_verison_info
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    INIT_VERSION        INT COMMENT '初始化脚本版本',
    APP                 VARCHAR(510) COMMENT '服务名',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='权限模块初始化版本信息表';
INSERT INTO rbac_init_verison_info (ID,INIT_VERSION) SELECT 1,1 FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_init_verison_info WHERE ID = 1);

/*==============================================================*/
/* Table:菜单助记码(rbac_menu_mnecode)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menu_mnecode
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    row_version         BIGINT NOT NULL DEFAULT 0 COMMENT 'update时+1',
    LANGUAGE            VARCHAR(510) COMMENT '语言',
    MENU_INFO           BIGINT COMMENT '菜单ID',
    MNE_CODE            VARCHAR(510),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单助记码';

/*==============================================================*/
/* Table:角色助记码(rbac_role_mnecode)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_role_mnecode
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    row_version         BIGINT NOT NULL DEFAULT 0 COMMENT 'update时+1',
    ROLE                BIGINT COMMENT '角色ID',
    MNE_CODE            VARCHAR(510),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='角色助记码';

/*==============================================================*/
/* Table:菜单数据回滚表(rbac_menu_temp)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menu_temp
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    UUID                VARCHAR(200) COMMENT '回滚标识',
    OLD_DATA            BLOB COMMENT '老数据',
    NEW_DATA            BLOB COMMENT '新数据',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单数据回滚表';



create or replace  view base_datapermission (id, version, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, valid, entity_code, purview_distribution, purview_state, memo, unlimited_power, group_power_flag, assign_staff_flag, assign_pos_flag, position_power_flag, data_permission_type, type_id, activity_code, flow_version, flow_key) as
  ( select id as id, version as version, create_staff_id, modify_staff_id, null as delete_staff_id, create_time as create_time, modify_time as modify_time, delete_time as delete_time, 1 as valid, entity_code as entity_code, purview_distribution as purview_distribution, purview_state as purview_state, memo as memo, unlimited_power as unlimited_power, group_power_flag as group_power_flag, assign_staff_flag as assign_staff_flag, assign_pos_flag as assign_pos_flag, position_power_flag as position_power_flag, flow_permission_type as data_permission_type, type_id as type_id, activity_code as activity_code, flow_version as flow_version, flow_key as flow_key from rbac_flow_permission );


create or replace  view base_datapermissionstaff (id, version, staff_id, datapermission_id) as
  ( select id, version, staff_id, flowpermission_id from rbac_flow_permission_staff );


create or replace  view base_datapmsposition (id, version, position_id, include_lower, datapermission_id) as
  ( select id, version, position_id, include_lower, flowpermission_id from rbac_flow_permission_position );



create or replace  view base_menuoperate (id, version, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, valid, cid, is_allow_proxy, is_hidden, three_role, view_code, is_query, is_orrelation, for_data_permission, enable_norestrict, enable_otherrestrict, enable_specialpermission, enable_dealerpermission, enable_assignstaff, enable_assignpos, enable_posrestrict, enable_grouprestrict, entity_code, ignore_permission, power_flag, flow_version, flow_key, msg_assembled, deployment_id, type, menuinfo_id, icon_cls, module, sort, memo, target, namespace, url, name_zh_cn, name, code, action, st_flag, st_type, st_tablecode, st_showstyle, st_operatetype, st_isview, st_iss2flowoperate, st_ismainquery, st_isdefault, st_flowkey, st_digitalsignature, st_defaultdisplay, st_activityid, menuoperate_mainoperatecode, menuoperate_iscontainer, menuoperate_entryoperatecode,menuoperatetype) as
  (
select
	id as id,
	row_version as version,
	create_staff_id,
	modify_staff_id,
	null as delete_staff_id,
	create_time as create_time,
	modify_time as modify_time,
	delete_time as delete_time,
	valid as valid,
	cid as cid,
	is_allow_proxy as is_allow_proxy,
	is_hidden as is_hidden,
	three_role as three_role,
	view_code as view_code,
	is_query as is_query,
	is_orrelation as is_orrelation,
	for_flow_permission as for_data_permission,
	enable_norestrict as enable_norestrict,
	enable_custompermission as enable_otherrestrict,
	enable_datapermission as enable_specialpermission,
	enable_dealerpermission as enable_dealerpermission,
	enable_assignstaff as enable_assignstaff,
	enable_assignpos as enable_assignpos,
	enable_posrestrict as enable_posrestrict,
	enable_grouprestrict as enable_grouprestrict,
	entity_code as entity_code,
	ignore_permission as ignore_permission,
	power_flag as power_flag,
	flow_version as flow_version,
	flow_key as flow_key,
	msg_assembled as msg_assembled,
	deployment_id as deployment_id,
	menuoperatetype as type,
	menuinfo_id as menuinfo_id,
	icon_cls as icon_cls,
	module_code as module,
	sort as sort,
	memo as memo,
	target as target,
	namespace as namespace,
	url as url,
	name_zh_cn as name_zh_cn,
	name as name,
	code as code,
	action_url as action,
	0 as st_flag,
	null as st_type,
	null as st_tablecode,
	null as st_showstyle,
	null as st_operatetype,
	null as st_isview,
	null as st_iss2flowoperate,
	null as st_ismainquery,
	null as st_isdefault,
	null as st_flowkey,
	null as st_digitalsignature,
	null as st_defaultdisplay,
	null as st_activityid,
	null as menuoperate_mainoperatecode,
	null as menuoperate_iscontainer,
	null as menuoperate_entryoperatecode,
	menuoperatetype as menuoperatetype

from
	rbac_menuoperate );

create or replace  view base_role (id, valid, version, cid, leaf, full_path_name, parent_id, lay_no, lay_rec, uuid, three_role_type, role_type, sort, description, name, code, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time) as
  (
select
	id as id,
	valid as valid,
	version as version,
	cid as cid,
	leaf as leaf,
	full_path_name as full_path_name,
	parent_id as parent_id,
	lay_no as lay_no,
	lay_rec as lay_rec,
	uuid as uuid,
	three_role_type as three_role_type,
	role_type as role_type,
	sort as sort,
	description as description,
	name as name,
	code as code,
	create_staff_id,
	modify_staff_id,
	null as delete_staff_id,
	create_time as create_time,
	modify_time as modify_time,
	delete_time as delete_time
from
	rbac_role );
	
create or replace
algorithm = UNDEFINED view base_role_mnecode as (
select
    m.ID as id,
    m.row_version as version,
    m.ROLE as role,
    m.MNE_CODE as mne_code
from
    rbac_role_mnecode m);
    
create or replace  view base_rolepermission (id, version, role_id, menuoperate_id, position_flag, group_flag, assign_staff_flag, assign_pos_flag, dealer_permission_flag, no_restrict_flag, assign_specialpermission_flag, assign_otherrestrict_flag, url_pattern, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time) as
  ( select id as id, version as version, role_id as role_id, menuoperate_id as menuoperate_id, position_flag as position_flag, group_flag as group_flag, assign_staff_flag as assign_staff_flag, assign_pos_flag as assign_pos_flag, dealer_permission_flag as dealer_permission_flag, no_restrict_flag as no_restrict_flag, assign_datapermission_flag as assign_specialpermission_flag, assign_custompermission_flag as assign_otherrestrict_flag, url_pattern as url_pattern, create_staff_id, modify_staff_id, null as delete_staff_id, create_time as create_time, modify_time as modify_time, delete_time as delete_time from rbac_rolepermission );

create or replace  view base_rolepposition (id, version, include_lower, position_id, rolepermission_id) as
  ( select id as id, version as version, include_lower as include_lower, position_id as position_id, rolepermission_id as rolepermission_id from rbac_rolepposition );

create or replace  view base_rolepstaff (id, version, staff_id, rolepermission_id) as
  ( select id as id, version as version, staff_id as staff_id, rolepermission_id as rolepermission_id from rbac_rolepstaff );

create or replace  view base_roleuser (id, version, position_flag, role_id, user_id, valid, end_time, start_time) as
  (
select
	id as id,
	version as version,
	position_flag as position_flag,
	role_id as role_id,
	user_id as user_id,
	valid as valid,
	end_time as end_time,
	start_time as start_time
from
	rbac_roleuser );

create or replace  view base_role_otherrestrict_ref (id, version, other_restrict_code, rolepermission_id) as
  ( select id as id, version as version, custom_permission_code as other_restrict_code, rolepermission_id as rolepermission_id from rbac_role_custompermission_ref );

create or replace  view base_role_specialpermission (id, version, config_string, content, special_permission_code, rolepermission_id) as
  ( select id as id, version as version, config_string as config_string, content as content, data_permission_code as special_permission_code, rolepermission_id as rolepermission_id from rbac_role_datapermission );

create or replace  view base_userpermission (id, version, user_id, staff_id, cid, menuoperate_id, purview_type, position_flag, group_flag, assign_staff_flag, assign_pos_flag, dealer_permission_flag, no_restrict_flag, assign_otherrestrict_flag, assign_specialpermission_flag, url_pattern, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, st_state, st_powerflag, st_powercode, st_operatestate, st_operateid, st_modulecode, st_menuid, st_memo, st_flag, deal_time) as
  (
select
	id as id,
	version as version,
	user_id as user_id,
	deal_staff as staff_id,
	cid as cid,
	menuoperate_id as menuoperate_id,
	purview_type as purview_type,
	position_flag as position_flag,
	group_flag as group_flag,
	assign_staff_flag as assign_staff_flag,
	assign_pos_flag as assign_pos_flag,
	dealer_permission_flag as dealer_permission_flag,
	no_restrict_flag as no_restrict_flag,
	assign_custompermission_flag as assign_otherrestrict_flag,
	assign_datapermission_flag as assign_specialpermission_flag,
	url_pattern as url_pattern,
	create_staff_id,
	modify_staff_id,
	null as delete_staff_id,
	create_time as create_time,
	modify_time as modify_time,
	delete_time as delete_time,
	null as st_state,
	null as st_powerflag,
	null as st_powercode,
	null as st_operatestate,
	null as st_operateid,
	null as st_modulecode,
	null as st_menuid,
	null as st_memo,
	null as st_flag,
	null as deal_time
from
	rbac_userpermission );

create or replace  view base_userpposition (id, version, include_lower, position_id, userpermission_id) as
  ( select id as id, version as version, include_lower as include_lower, position_id as position_id, userpermission_id as userpermission_id from rbac_userpposition );

create or replace  view base_userpstaff (id, version, staff_id, userpermission_id) as
  ( select id as id, version as version, staff_id as staff_id, userpermission_id as userpermission_id from rbac_userpstaff );

create or replace  view base_user_otherrestrict_ref (id, version, other_restrict_code, userpermission_id) as
  ( select id as id, version as version, custom_permission_code as other_restrict_code, userpermission_id as userpermission_id from rbac_user_custompermission_ref );

create or replace  view base_user_specialpermission (id, version, config_string, content, special_permission_code, userpermission_id) as
  ( select id as id, version as version, config_string as config_string, content as content, data_permission_code as special_permission_code, userpermission_id as userpermission_id from rbac_user_datapermission );

create or replace  view ec_other_restrict (code,hand_writing_flag, ec_env, version, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, valid, entity_code, module_code, memo, title, condition_sql, json_condition, view_code) as
  ( select code as code,hand_writing_flag as hand_writing_flag, ec_env as ec_env, version as version, create_staff_id, modify_staff_id, null as delete_staff_id, create_time as create_time, modify_time as modify_time, delete_time as delete_time, valid as valid, entity_code as entity_code, module_code as module_code, memo as memo, title as title, condition_sql as condition_sql, json_condition as json_condition, view_code as view_code from rbac_custom_permission );

ALTER TABLE rbac_menuinfo MODIFY COLUMN TARGET varchar(510) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'SELF' NULL COMMENT '打开方式';
ALTER TABLE rbac_menuinfo ADD STATUS INT DEFAULT 0 NULL COMMENT '组态期 0，运行期 1, 两者都有 2';
ALTER TABLE rbac_menuinfo ADD ROUTE VARCHAR(510) NULL COMMENT '地址';
ALTER TABLE rbac_menuinfo ADD EXTRA VARCHAR(1000) NULL COMMENT '额外信息';
ALTER TABLE rbac_menuinfo_company_ref ADD APPID VARCHAR(200) COMMENT 'APPID';

## 1.0.1
/*==============================================================*/
/* Table:菜单APP关联表(rbac_app_ref)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_app_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    menuId              BIGINT COMMENT '菜单ID',
    appid             VARCHAR(200) COMMENT 'APP编码',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单APP关联表';
ALTER TABLE rbac_app_ref add unique index unique_menu_app (`menuId`,`appid`);
/*==============================================================*/
/* Table:APP公司关联表(rbac_app_company_ref)                */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_app_company_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    CID                 BIGINT COMMENT '公司ID',
    APPID               VARCHAR(200) COMMENT 'APPID',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='APP公司关联表';
ALTER TABLE rbac_app_company_ref add unique index unique_ra_Id (`CID`,`APPID`);

ALTER TABLE rbac_menuinfo_company_ref ADD APPID VARCHAR(200) COMMENT 'APPID';

## 1.0.2
ALTER TABLE rbac_app_ref CHANGE ID id BIGINT;
ALTER TABLE rbac_app_company_ref CHANGE ID id BIGINT;
ALTER TABLE rbac_app_company_ref CHANGE CID cid BIGINT;
ALTER TABLE rbac_app_company_ref CHANGE APPID appid VARCHAR(200);
## 1.0.6
--//补全菜单表 appid字段
update rbac_menuinfo rm set appid= (select appid from rbac_app_ref rar where rar.menuId = rm.ID);

## 1.0.7
--ALTER table rbac_app_ref add relation tinyint(1) not null default 0 comment '菜单和app的关系. 0:菜单属于app, 1:菜单与app是关联关系';
/*==rbac_menu_app_designer==*/
CREATE TABLE IF NOT EXISTS rbac_menu_app_designer
(
    id                  BIGINT NOT NULL COMMENT '主键ID',
    appid             VARCHAR(200) COMMENT 'APP编码',
    code               VARCHAR(200) COMMENT '菜单code',
    parent_code               VARCHAR(200) COMMENT '菜单父code',
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='为方便获取设计器当前节点下的菜单树列表,维护App设计器与菜单的关联关系';
ALTER TABLE rbac_menu_app_designer add unique index unique_menu_app_designer (`appid`,`code`);
/*==rbac_menuinfo 冗余字段 appid==*/
ALTER table rbac_menuinfo add appid varchar(200) comment '菜单所属app,冗余字段';


CREATE TABLE IF NOT EXISTS rbac_data_resource_group
(
    id                        BIGINT NOT NULL COMMENT '主键ID',
    group_code                VARCHAR(512) NOT NULL COMMENT '资源编码',
    group_name                VARCHAR(512) NOT NULL COMMENT '资源名称',
    resource_url              VARCHAR(4000) NOT NULL COMMENT '资源获取地址',
    module_code               VARCHAR(100) NOT NULL COMMENT '模块编码',
    cid                       BIGINT NULL COMMENT '公司ID',
    creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
    create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
    modifier                  VARCHAR(32) COMMENT '修改人',
    modify_time               TIMESTAMP COMMENT '修改时间',
    modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
    PRIMARY KEY (id)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='资源集';

CREATE TABLE IF NOT EXISTS rbac_role_data_permission
(
    id                        BIGINT NOT NULL COMMENT '主键ID',
    role_id                   BIGINT NOT NULL COMMENT '角色ID',
    cid                       BIGINT NOT NULL COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL COMMENT '业务数据名称',
    resource_type             VARCHAR(512) COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL COMMENT '资源编码',
    valid                     INT DEFAULT 1 COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
    create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
    modifier                  VARCHAR(32) COMMENT '修改人',
    modify_time               TIMESTAMP COMMENT '修改时间',
    modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
    PRIMARY KEY (id)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='资源集角色权限';

CREATE TABLE IF NOT EXISTS rbac_role_data_permission_ctrl
(
    id                        BIGINT NOT NULL COMMENT '主键ID',
    role_id                   BIGINT NOT NULL COMMENT '角色ID',
    cid                       BIGINT NOT NULL COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL COMMENT '资源编码',
    controlled                INT NOT NULL DEFAULT 1 COMMENT 'group_code资源是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
    create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
    modifier                  VARCHAR(32) COMMENT '修改人',
    modify_time               TIMESTAMP COMMENT '修改时间',
    modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
    PRIMARY KEY (id),
    UNIQUE INDEX udx_role_data_ctrl(role_id, cid, group_code)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='资源集角色是否受控';


CREATE TABLE IF NOT EXISTS rbac_user_data_permission
(
    id                        BIGINT NOT NULL COMMENT '主键ID',
    user_id                   BIGINT NOT NULL COMMENT '用户ID',
    cid                       BIGINT NOT NULL COMMENT '公司ID',
    resource_code             VARCHAR(512) NOT NULL COMMENT '业务数据编码',
    resource_name             VARCHAR(512) NOT NULL COMMENT '业务数据名称',
    resource_type             VARCHAR(512) COMMENT '业务数据类型',
    group_code                VARCHAR(512) NOT NULL COMMENT '资源编码',
    role_id                   BIGINT COMMENT '角色ID',
    purview_type              INT NOT NULL COMMENT '授权方式：角色(0) 用户(1)',
    valid                     INT DEFAULT 1 COMMENT '是否有效',
    creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
    create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
    modifier                  VARCHAR(32) COMMENT '修改人',
    modify_time               TIMESTAMP COMMENT '修改时间',
    modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
    PRIMARY KEY (id)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='资源集用户权限';


CREATE TABLE IF NOT EXISTS rbac_user_data_permission_ctrl
(
    id                        BIGINT NOT NULL COMMENT '主键ID',
    user_id                   BIGINT NOT NULL COMMENT '用户ID',
    cid                       BIGINT NOT NULL COMMENT '公司ID',
    group_code                VARCHAR(512) NOT NULL COMMENT '资源编码',
    controlled                INT NOT NULL DEFAULT 1 COMMENT 'group_code数据分组是否受控 0 不受控　1 受控',
    creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
    create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
    create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
    modifier                  VARCHAR(32) COMMENT '修改人',
    modify_time               TIMESTAMP COMMENT '修改时间',
    modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
    PRIMARY KEY (id),
    UNIQUE INDEX udx_user_data_ctrl(user_id, cid, group_code)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='资源集用户是否受控';


CREATE INDEX COMPANY_REF_MENUINFO_ID_IDX USING BTREE ON rbac_menuinfo_company_ref (MENUINFO_ID);
CREATE INDEX INDEX_MENUOPERATE_CODE_IDX USING BTREE ON RBAC_USERPERMISSION (MENUOPERATE_CODE);
CREATE INDEX IDX_DP_ACTIVITYCODE USING BTREE ON RBAC_FLOW_PERMISSION (ACTIVITY_CODE);
CREATE INDEX INDEX_USER_ID_IDX USING BTREE ON RBAC_USERPERMISSION (USER_ID);
CREATE INDEX INDEX_CID_IDX USING BTREE ON RBAC_USERPERMISSION (CID);
CREATE INDEX INDEX_PURVIEW_TYPE_IDX USING BTREE ON RBAC_USERPERMISSION (PURVIEW_TYPE);
CREATE INDEX INDEX_FLOW_POSITION_ID_IDX USING BTREE ON RBAC_FLOW_PERMISSION_POSITION (FLOWPERMISSION_ID);
CREATE INDEX INDEX_FLOW_STAFF_ID_IDX USING BTREE ON RBAC_FLOW_PERMISSION_STAFF (FLOWPERMISSION_ID);
CREATE INDEX INDEX_CUSTOM_UP_ID_IDX USING BTREE ON RBAC_USER_CUSTOMPERMISSION_REF (USERPERMISSION_ID);
CREATE INDEX INDEX_DATA_UP_ID_IDX USING BTREE ON RBAC_USER_DATAPERMISSION (USERPERMISSION_ID);

## 1.0.7
--解决 zero date value prohibited 导致的问题
alter table rbac_role modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_role modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_role modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;
update rbac_role rr set rr.CREATE_TIME = current_timestamp where rr.CREATE_TIME is null or rr.CREATE_TIME = '0000-00-00 00:00:00.000000';
update rbac_role rr set rr.MODIFY_TIME = current_timestamp where rr.MODIFY_TIME is null or rr.MODIFY_TIME = '0000-00-00 00:00:00.000000';


alter table rbac_roleuser modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_roleuser modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_roleuser modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;
alter table rbac_roleuser modify END_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_roleuser modify START_TIME TIMESTAMP NULL DEFAULT NULL;
update rbac_roleuser rr set rr.CREATE_TIME = current_timestamp where rr.CREATE_TIME is null or rr.CREATE_TIME = '0000-00-00 00:00:00.000000';
update rbac_roleuser rr set rr.MODIFY_TIME = current_timestamp where rr.MODIFY_TIME is null or rr.MODIFY_TIME = '0000-00-00 00:00:00.000000';

alter table rbac_menuoperate modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_menuoperate modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_menuoperate modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_menuinfo modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_menuinfo modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_menuinfo modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;
update rbac_menuinfo rr set rr.CREATE_TIME = current_timestamp where rr.CREATE_TIME is null or rr.CREATE_TIME = '0000-00-00 00:00:00.000000';
update rbac_menuinfo rr set rr.MODIFY_TIME = current_timestamp where rr.MODIFY_TIME is null or rr.MODIFY_TIME = '0000-00-00 00:00:00.000000';

alter table rbac_tag modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_tag modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
update rbac_tag rr set rr.CREATE_TIME = current_timestamp where rr.CREATE_TIME is null or rr.CREATE_TIME = '0000-00-00 00:00:00.000000';
update rbac_tag rr set rr.MODIFY_TIME = current_timestamp where rr.MODIFY_TIME is null or rr.MODIFY_TIME = '0000-00-00 00:00:00.000000';

alter table rbac_userpermission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_userpermission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_userpermission modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_rolepermission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_rolepermission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_rolepermission modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_data_permission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_data_permission_ushow modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission_ushow modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission_ushow modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_data_permission_rshow modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission_rshow modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_permission_rshow modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_custom_permission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_custom_permission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_custom_permission modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_flow_permission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_flow_permission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_flow_permission modify DELETE_TIME TIMESTAMP NULL DEFAULT NULL;

alter table rbac_data_resource_group modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_data_resource_group modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
update rbac_data_resource_group rr set rr.CREATE_TIME = current_timestamp where rr.CREATE_TIME is null or rr.CREATE_TIME = '0000-00-00 00:00:00.000000';
update rbac_data_resource_group rr set rr.MODIFY_TIME = current_timestamp where rr.MODIFY_TIME is null or rr.MODIFY_TIME = '0000-00-00 00:00:00.000000';


alter table rbac_role_data_permission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_role_data_permission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table rbac_role_data_permission_ctrl modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_role_data_permission_ctrl modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table rbac_user_data_permission modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_user_data_permission modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table rbac_user_data_permission_ctrl modify CREATE_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table rbac_user_data_permission_ctrl modify MODIFY_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID) SELECT 1,'管理员角色','systemRole','管理员角色','管理员角色',1000 FROM DUAL WHERE NOT EXISTS (SELECT CODE FROM rbac_role WHERE CODE = 'systemRole');
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID) SELECT 2,'公司管理员角色','companySystemRole','公司管理员角色','公司管理员角色',1000 FROM DUAL WHERE NOT EXISTS (SELECT CODE FROM rbac_role WHERE CODE = 'companySystemRole');
INSERT INTO rbac_role (ID,NAME,CODE,TAG,DESCRIPTION,CID) SELECT 3,'普通用户角色','normalRole','普通用户角色','普通用户角色',1000 FROM DUAL WHERE NOT EXISTS (SELECT CODE FROM rbac_role WHERE CODE = 'normalRole');

INSERT INTO rbac_roleuser(ID,ROLE_ID,USER_ID,PERSON_NAME,PERSON_CODE,USER_NAME) SELECT 1,1,1,'默认人员','default','admin' FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_roleuser WHERE ID = 1);

INSERT INTO rbac_menuinfo (ID, VERSION, DELETE_TIME, MODIFY_TIME, CREATE_TIME, TERMINATOR, MODIFIER, CREATOR, VALID, CID, SECURITY_CLASS, ABSOLUTE_HIDDEN, THREE_ROLE, SHOW_TYPE, REQUEST_TYPE, HIDDEN_TYPE, MENU_TYPE, IS_HIDE, GROUP_ONLY, ENTITY_CODE, MODULE_CODE, SYSTEM_DEFAULT, CSS_CLASS, SORT, ACTION_URL, NAMESPACE, URL, TARGET, MEMO, NAME, CODE, LAY_NO, LAY_REC, PARENT_ID, FULL_PATH, SOURCE, LEAF) SELECT -1, 0, null, null, null, null, null, null, 1, 1000, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '系统默认', 'rbac.MENU_NAME_MENU_LIST', 'menu_list', null, 1, null, null, null, 0 FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_menuinfo WHERE ID = -1);

INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID,VALID) SELECT 1,'管理员角色',1000,1,1 FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_tag WHERE ID = 1);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID,VALID) SELECT 2,'公司管理员角色',1000,1,2 FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_tag WHERE ID = 2);
INSERT INTO rbac_tag(ID,NAME,CID,OBJECTID,VALID) SELECT 3,'普通用户角色',1000,1,3 FROM DUAL WHERE NOT EXISTS (SELECT ID FROM rbac_tag WHERE ID = 3);

INSERT INTO rbac_data_resource_group (id, group_code, group_name, resource_url, module_code, creator, create_staff_id) VALUES (1, 'oodm-data-group-permission', '数据建模权限分组', '/project/dam/supngin/api/dam/v1/dataGroups', 'oodm', 'system', 0);
## 1.0.8
alter table rbac_menuinfo modify COLUMN URL varchar(1000);
alter table rbac_menuinfo modify COLUMN ROUTE varchar(1000);
alter table rbac_menuoperate modify COLUMN URL varchar(1000);

/*==============================================================*/
/* Table:菜单操作编码URL关联表(rbac_menuoperatecode_url_ref)        */
/* 防止 非空环境无法创建表 rbac_menuoperatecode_url_ref，因此再执行下建表语句      */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS rbac_menuoperatecode_url_ref
(
    ID                  BIGINT NOT NULL COMMENT '主键ID',
    MENUOPERATE_CODE    VARCHAR(510) COMMENT '菜单操作编码',
    METHOD_TYPE         INT COMMENT '请求方法，0 GET,1 POST,2 PUT,3 DELETE',
    URL                 VARCHAR(510) COMMENT '对应URL',
    APP                 VARCHAR(32) COMMENT '应用名',
    IS_CUSTOM           INT DEFAULT 0 COMMENT '是否自定义操作',
    REG_MATCH           INT DEFAULT 0 COMMENT '是否需要正则匹配',
    IMPORT_TYPE         INT DEFAULT 0 COMMENT '导入类型 0 注解导入、1 json文件导入',
    INDEX MENU_OPERATE_CODE_IDX (MENUOPERATE_CODE),
    PRIMARY KEY(ID)
)
ENGINE=INNODB
DEFAULT CHARSET=UTF8
COMMENT='菜单操作编码URL关联表';

## 1.0.9
create index idx_rbac_userpermission_1 on rbac_userpermission(user_id,cid);
create index idx_rbac_menuinfo_1 on rbac_menuinfo(lay_no,status);

## 1.0.10
create index idx_rbac_flow_permission_1 on rbac_flow_permission(flow_permission_type, type_id);
## 1.0.11
create or replace  view base_menuinfo (id, version, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, valid, cid, security_class, absolute_hidden, three_role, show_type, request_type, hidden_type, menu_type, is_hide, group_only, entity_code, ec_entity_code, module_code, system_default, css_class, sort, namespace, url, target, memo, name, name_zh_cn, code, lay_no, lay_rec, parent_id, full_path_name, action, leaf, st_flag, st_digitalsignature, st_intro, st_tabtypeid, st_type, remote_user_name_named, remote_password_named, remote_id, pims_menu_type, module, icon_url,status) as
  (
select
	id as id,
	version as version,
	create_staff_id,
	modify_staff_id,
	null as delete_staff_id,
	create_time as create_time,
	modify_time as modify_time,
	delete_time as delete_time,
	valid as valid,
	cid as cid,
	security_class as security_class,
	absolute_hidden as absolute_hidden,
	three_role as three_role,
	show_type as show_type,
	request_type as request_type,
	hidden_type as hidden_type,
	menu_type as menu_type,
	is_hide as is_hide,
	group_only as group_only,
	entity_code as entity_code,
	entity_code as ec_entity_code,
	module_code as module_code,
	system_default as system_default,
	css_class as css_class,
	sort as sort,
	namespace as namespace,
	url as url,
	target as target,
	memo as memo,
	name as name,
	name_display as name_zh_cn,
	code as code,
	lay_no as lay_no,
	lay_rec as lay_rec,
	parent_id as parent_id,
	full_path_name as full_path_name,
	action_url as action,
	leaf as leaf,
	null as st_flag,
	null as st_digitalsignature,
	null as st_intro,
	null as st_tabtypeid,
	null as st_type,
	null as remote_user_name_named,
	null as remote_password_named,
	null as remote_id,
	null as pims_menu_type,
	null as module,
	null as icon_url,
	status as status
from
	rbac_menuinfo );

## 1.0.12
delete from rbac_menuoperatecode_url_ref where MENUOPERATE_CODE ='openMsgManager';