## 1.0.0
/*==============================================================*/
/* Table: notice_protocol                                        */
/*==============================================================*/
CREATE TABLE notice_protocol --COMMENT='协议表'
(
   id                        number(20,0) NOT NULL,--COMMENT '协议ID',
   protocol                  VARCHAR2(13) NOT NULL,-- COMMENT '协议类型',
   name                      VARCHAR2(256) NOT NULL,-- COMMENT '协议展示名',
   app_name                  VARCHAR2(512) NOT NULL,-- COMMENT 'appName',
   vender_name               VARCHAR2(1024) NOT NULL,-- COMMENT 'venderName',
   service_name              VARCHAR2(1024) NOT NULL,-- COMMENT 'app访问地址',
   send_url                  VARCHAR2(1024) NOT NULL,-- COMMENT '消息协议发送地址',
   config_url                VARCHAR2(1024),-- COMMENT '消息协议配置地址',
   system_config_app_code    VARCHAR2(1024),-- COMMENT '系统配置唯一标示：systemConfigAppCode + systemConfigCode',
   system_config_code        VARCHAR2(1024),-- COMMENT '系统配置唯一标示：systemConfigAppCode + systemConfigCode',
   default_template_code     VARCHAR2(256) NOT NULL,-- COMMENT '默认基础模板编号',
   content_type              number(3,0) DEFAULT 0,-- COMMENT '消息内容支持格式, 0 纯文本、1 富文本'
   doc                       clob,-- COMMENT '消息协议说明文档'
   i18n_module							 VARCHAR2(512),-- COMMENT '国际化模块名',
   i18n_key							     VARCHAR2(512),-- COMMENT '国际化key',
   system                    number(3,0) DEFAULT 0,-- COMMENT '是否为系统协议',
   creator                   VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time               TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id           number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id           number(20,0), -- COMMENT '修改者人员Id',
   modifier                  VARCHAR2(128),-- COMMENT '修改人',
   modify_time               TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_protocol_id PRIMARY KEY (id),
   constraint udx_notice_protocol_protocol unique(protocol)
);



/*==============================================================*/
/* Table: notice_protocol_config                                        */
/*==============================================================*/
CREATE TABLE notice_protocol_config --COMMENT='协议配置表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '配置ID',
   protocol                         VARCHAR2(13) NOT NULL,-- COMMENT '协议类型',
   config_value                     clob NOT NULL,-- COMMENT '配置内容',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_protocol_config_id PRIMARY KEY (id),
   constraint udx_notice_protocol_config_pro unique(protocol)
);

/*==============================================================*/
/* Table: notice_protocol_tmpl                                        */
/*==============================================================*/
CREATE TABLE notice_protocol_tmpl --COMMENT='协议基础模板表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '协议基础模板ID',
   code                             VARCHAR2(256) NOT NULL,-- COMMENT '基础模板编号',
   name                             VARCHAR2(256) NOT NULL,-- COMMENT '基础模板名称',
   i18n_key                         VARCHAR2(512) ,-- COMMENT '国际化key',
   description                      VARCHAR2(1024),-- COMMENT '模板描述',
   template                         clob NOT NULL,-- COMMENT '基础内容模板',
   system                           number(3,0) DEFAULT 0,-- COMMENT '是否为系统基础模板',
   notice_protocol_id               number(20,0) NOT NULL,-- COMMENT '协议ID',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_protocol_tmpl_id PRIMARY KEY (id),
   constraint udx_notice_protocol_tmpl_code unique(code),
   constraint udx_notice_protocol_tmpl_name unique(name, notice_protocol_id)
);

/*==============================================================*/
/* Table: notice_topic                                        */
/*==============================================================*/
CREATE TABLE notice_topic --COMMENT='协议主题表'
(
   id                               number(20,0) NOT NULL,-- COMMENT '协议主题表ID',
   code                             VARCHAR2(256) NOT NULL,-- COMMENT '主题编号',
   name                             VARCHAR2(256) NOT NULL,-- COMMENT '主题名称',
   source                           VARCHAR2(1024) DEFAULT 'system'  NOT NULL,-- COMMENT '主题来源',
   modify_sign                      number(1,0) DEFAULT 1,-- COMMENT '修改标志,0 不允许修改 1 允许修改',
   cover_sign                       number(1,0) DEFAULT 0,-- COMMENT '能否覆盖标志,0 不允许覆盖 1 允许覆盖',
   notice_topic_type_id             number(20,0) NOT NULL,-- COMMENT '主题类型ID',
   description                      VARCHAR2(1024),-- COMMENT,-- '主题类型描述',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   version                          number(10,0) DEFAULT 0,-- COMMENT '版本',
   valid                            number(1,0) DEFAULT 0,-- COMMENT '是否删除',
   sort_value                       number(20,4) DEFAULT 0 NOT NULL,-- COMMENT '主题排序字段',
   constraint pk_notice_topic_id PRIMARY KEY (id),
   constraint udx_notice_topic_code unique(code)
);

/*==============================================================*/
/* Table: notice_topic_type                                        */
/*==============================================================*/
CREATE TABLE notice_topic_type --COMMENT='协议主题类型表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '协议主题类型表ID',
   code                             VARCHAR2(256) NOT NULL,-- COMMENT '协议主题类型编码',
   name                             VARCHAR2(256) NOT NULL,-- COMMENT '协议主题类型名称',
   i18n_key                         VARCHAR2(512) ,-- COMMENT '国际化key',
   source                           VARCHAR2(1024) DEFAULT 'system' NOT NULL,-- COMMENT '协议主题类型来源',
   modify_sign                      number(1,0) DEFAULT 1,-- COMMENT '修改标志,0 不允许修改 1 允许修改',
   parent_id                        number(20,0),-- COMMENT '协议主题类父级ID',
   sort_value                       number(20,4) DEFAULT 0 NOT NULL,-- COMMENT '主题类型排序字段',
   description                      VARCHAR2(1024),-- COMMENT '主题类型描述',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   version                          number(10,0)  DEFAULT 0,-- COMMENT '版本',
   valid                            number(1,0) DEFAULT 0,-- COMMENT '是否删除',
   lay_rec                          number(10,0)  DEFAULT 0,-- COMMENT '主题类型层级',
   constraint pk_notice_topic_type_id PRIMARY KEY (id),
   constraint udx_notice_topic_type_name unique(name)
);

/*==============================================================*/
/* Table: notice_tmpl                                        */
/*==============================================================*/
CREATE TABLE notice_tmpl --COMMENT='协议模板表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '协议模板表ID',
   code                             VARCHAR2(256) NOT NULL,-- COMMENT '协议模板编号',
   name                             VARCHAR2(256) NOT NULL,-- COMMENT '协议模板名称',
   params                           VARCHAR2(256),-- COMMENT '模板默认参数',
   description                      VARCHAR2(1024),-- COMMENT '协议模板描述',
   template                         clob,-- COMMENT '协议模板内容',
   source                           VARCHAR2(1024) DEFAULT 'system' NOT NULL,-- COMMENT '协议模板来源',
   modify_sign                      number(1,0) DEFAULT 1,-- COMMENT '修改标志,0 不允许修改 1 允许修改',
   cover_sign                       number(1,0) DEFAULT 0,-- COMMENT '能否覆盖标志,0 不允许覆盖 1 允许覆盖',
   notice_protocol_id               number(20,0),-- COMMENT '协议表ID',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   version                          number(10,0) DEFAULT 0,-- COMMENT '版本',
   sort_value                       number(20,4) DEFAULT 0,-- NOT NULL COMMENT '排序',
   valid                            number(1,0) DEFAULT 0,-- COMMENT '是否删除',
   constraint pk_notice_tmpl_id PRIMARY KEY (id),
   constraint udx_notice_tmpl_code unique(code)
);

/*==============================================================*/
/* Table: notice_topic_tmpl_rel                                        */
/*==============================================================*/
CREATE TABLE notice_topic_tmpl_rel --COMMENT='主题模板关联表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '协议模板关联表ID',
   notice_topic_id                  VARCHAR2(128) NOT NULL,-- COMMENT '主题表ID',
   notice_tmpl_id                   VARCHAR2(128) NOT NULL,-- COMMENT '模板表ID',
   notice_protocol_id               VARCHAR2(128) NOT NULL,-- COMMENT '协议表ID',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_topic_tmpl_rel_id PRIMARY KEY (id),
   constraint udx_notice_topic_tmpl_rel unique(notice_topic_id, notice_tmpl_id ,notice_protocol_id)
);

/*==============================================================*/
/* Table: notice_topic_range                                        */
/*==============================================================*/
CREATE TABLE notice_topic_range --COMMENT='主题发送范围表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '主题发送范围表ID',
   range_type                       number(1,0) NOT NULL,-- COMMENT '推送方式, 分为0人员、1岗位、2部门、和3业务规则',
   bsmod_name                       VARCHAR2(256),-- COMMENT '业务模块名称',
   bsmod_code                       VARCHAR2(256),-- COMMENT '业务模块编码',
   bsmod_addr                       VARCHAR2(128),-- COMMENT '业务模块提供接口地址，返回人员字符串',
   notice_topic_id                  number(20,0) NOT NULL,-- COMMENT '协议模板表ID',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_topic_range_id PRIMARY KEY (id)
);

/*==============================================================*/
/* Table: notice_topic_range_ext                                        */
/*==============================================================*/
CREATE TABLE notice_topic_range_ext --COMMENT='主题发送范围扩展表';
(
   id                               number(20,0) NOT NULL,-- COMMENT '主题发送范围扩展表ID',
   receiver_id                      number(20,0) NOT NULL,-- COMMENT '当推送方式为人员、部门、岗位时对应的ID',
   receiver_code                    VARCHAR2(256) NOT NULL,-- COMMENT '当推送方式为人员、部门、岗位时对应的CODE',
   contain_children                 number(1,0) DEFAULT 0 NOT NULL ,-- COMMENT '0 不包含子级, 1 包含子级',
   notice_topic_range_id            number(20,0) NOT NULL,-- COMMENT '主题发送范围表ID',
   creator                          VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                  number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                  number(20,0), -- COMMENT '修改者人员Id',
   modifier                         VARCHAR2(128),-- COMMENT '修改人',
   modify_time                      TIMESTAMP,-- COMMENT '修改时间',
  constraint pk_notice_topic_range_ext_id PRIMARY KEY (id)
);

/*==============================================================*/
/* Table: notice_task_protocol                                        */
/*==============================================================*/
CREATE TABLE notice_task_protocol --COMMENT='发送任务协议表';
(
   id                                         number(20,0) NOT NULL,-- COMMENT '发送任务协议表ID',
   notice_protocol_id                         number(20,0) NOT NULL,-- COMMENT '协议表ID',
   notice_task_id                             number(20,0) NOT NULL,-- COMMENT '发送任务表ID',
   content                                    clob NOT NULL,-- COMMENT '消息内容',
   creator                                    VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                                TIMESTAMP NOT NULL,-- COMMENT '创建时间',
   create_staff_id                            number(20,0) NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                            number(20,0), -- COMMENT '修改者人员Id',
   modifier                                   VARCHAR2(128),-- COMMENT '修改人',
   modify_time                                TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_task_protocol_id PRIMARY KEY (id),
   constraint udx_notice_task_protocol unique(notice_task_id, notice_protocol_id)
);

/*==============================================================*/
/* Table: notice_message_unread                                 */
/*==============================================================*/
CREATE TABLE notice_message_unread_count
(
   id                                         number(20,0) NOT NULL,-- COMMENT '未读消息数量统计表ID',
   staff_code                                 VARCHAR2(256) NOT NULL,-- COMMENT '人员code',
   notice_protocol_id                         number(20,0) NOT NULL,-- COMMENT '协议表ID',
   unread_count                               number(20,0) DEFAULT 0 NOT NULL,-- COMMENT '未读消息数量统计',
   creator                                    VARCHAR2(128) NOT NULL,-- COMMENT '创建人',
   create_time                                TIMESTAMP NOT NULL ,--COMMENT '创建时间',
   create_staff_id                            number(20,0) NOT NULL,-- COMMENT '创建者人员Id',
   modify_staff_id                            number(20,0),-- COMMENT '修改者人员Id',
   modifier                                   VARCHAR2(128),-- COMMENT '修改人',
   modify_time                                TIMESTAMP,-- COMMENT '修改时间',
   constraint pk_notice_muc_id PRIMARY KEY (id),
   constraint udx_notice_muc_staff_protocol unique(staff_code, notice_protocol_id)
);



/*==============================================================*/
/*                        DATA                                  */
/*==============================================================*/
INSERT INTO notice_protocol (id, protocol, name, i18n_key, app_name, vender_name, service_name, send_url, default_template_code, content_type, system, creator,create_time,create_staff_id) VALUES (1, 'email', 'notificationAdmin.src_common_mail', 'notificationAdmin.src_common_mail', 'email', 'supcon', 'supcon_email', 'sendUrl' , '001', 1, 1, 'default',(select sysdate from dual),1);
INSERT INTO notice_protocol (id, protocol, name, i18n_key, app_name, vender_name, service_name, send_url, default_template_code, content_type, system, creator,create_time,create_staff_id) VALUES (2, 'stationLetter', 'notificationAdmin.src_common_information', 'notificationAdmin.src_common_information', 'stationLetter', 'supcon', 'supcon_stationLetter', 'sendUrl', '003', 0, 1, 'default',(select sysdate from dual),1);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator,create_time,create_staff_id) VALUES (1, '001', '行政通知', 'notificationAdmin.protocol_basic_module_admin', '**部门发布了《${name}$》的通知，请注意查收！', 1, 1, 'default',(select sysdate from dual),1);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator,create_time,create_staff_id) VALUES (2, '002', '待办消息', 'notificationAdmin.protocol_basic_module_todo', '${username}$，您有一条“${name}$”的待办，请及时处理！', 1, 1, 'default',(select sysdate from dual),1);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator,create_time,create_staff_id) VALUES (3, '003', '行政通知', 'notificationAdmin.protocol_basic_module_admin', '**部门发布了《${name}$》的通知，请注意查收！', 1, 2, 'default',(select sysdate from dual),1);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator,create_time,create_staff_id) VALUES (4, '004', '待办消息', 'notificationAdmin.protocol_basic_module_todo', '${username}$，您有一条“${name}$”的待办，请及时处理！', 1, 2, 'default',(select sysdate from dual),1);

INSERT INTO notice_topic_type( id, parent_id, lay_rec, code, name,i18n_key, version, sort_value, description, valid, modify_sign, source, creator, create_time,create_staff_id ) VALUES ( 5120, 0, 0, 'defaultType' , '默认类型','notificationAdmin.type_default', 0, 0, '固有类型', 0, 0, 'system', 'admin',(select sysdate from dual),1);
INSERT INTO notice_topic_type( id, parent_id, lay_rec, code, name,i18n_key, version, sort_value, description, valid, modify_sign, source, creator, create_time,create_staff_id ) VALUES ( 5121, 0, 0, 'defaultType002' , '待办类型','notificationAdmin.type_todo', 0, 0, '固有类型', 0, 0, 'system', 'admin',(select sysdate from dual),1);


/* 初始化待办模板 */
INSERT INTO NOTICE_TMPL ( ID, CODE, NAME, TEMPLATE, NOTICE_PROTOCOL_ID, CREATOR, CREATE_TIME, CREATE_STAFF_ID) VALUES( 1, 'defult_BAP_Pending', '平台待办消息模板', '{"text":"<p>您有一条新待办消息! </p><p>流程名称：${title} </p><p>待办名称：${content} </p><p>${extendcontent} </p><p>${url} </p><p>创建人：${creator} </p><p>创建时间：${creationTime} </p>","subject":"平台待办消息模板"}', 1, 'default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TMPL ( ID, CODE, NAME, TEMPLATE, NOTICE_PROTOCOL_ID, CREATOR, CREATE_TIME, CREATE_STAFF_ID) VALUES( 2, 'defult_BAP_Reminding', '平台催办消息模板', '{"text":"<p>您有一条新催办消息!${title} </p><p>新催办：${content} </p><p>${extendContent}</p><p> ${url}</p><p>创建人：${creator}</p><p>创建时间：${creationTime}</p>","subject":"平台催办消息模板"}', 1, 'default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TMPL ( ID, CODE, NAME, TEMPLATE, NOTICE_PROTOCOL_ID, CREATOR, CREATE_TIME, CREATE_STAFF_ID) VALUES( 3, 'defult_BAP_Over_Pending', '平台超期待办消息模板', '{"text":"<p>${title}</p><p> 超期待办：${content} </p><p>${extendContent} </p><p>${url}</p><p>创建人：${creator}</p><p>创建时间：${creationTime}</p>","subject":"平台超期待办消息模板"}', 1, 'default',(SELECT sysdate FROM dual), 1);

INSERT INTO NOTICE_TOPIC (ID,CODE,NAME,MODIFY_SIGN,NOTICE_TOPIC_TYPE_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (1,'BAP_Pending','待办消息',1,5121,'default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TOPIC (ID,CODE,NAME,MODIFY_SIGN,NOTICE_TOPIC_TYPE_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (2,'BAP_Reminding','催办消息',1,5121,'default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TOPIC (ID,CODE,NAME,MODIFY_SIGN,NOTICE_TOPIC_TYPE_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (3,'BAP_Over_Pending','超期待办消息',1,5121,'default', (SELECT sysdate FROM dual), 1);

INSERT INTO NOTICE_TOPIC_TMPL_REL (ID,NOTICE_TOPIC_ID,NOTICE_TMPL_ID,NOTICE_PROTOCOL_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (1,'1','1','1','default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TOPIC_TMPL_REL (ID,NOTICE_TOPIC_ID,NOTICE_TMPL_ID,NOTICE_PROTOCOL_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (2,'2','2','1','default', (SELECT sysdate FROM dual), 1);
INSERT INTO NOTICE_TOPIC_TMPL_REL (ID,NOTICE_TOPIC_ID,NOTICE_TMPL_ID,NOTICE_PROTOCOL_ID,CREATOR,CREATE_TIME,CREATE_STAFF_ID) VALUES (3,'3','3','1','default', (SELECT sysdate FROM dual), 1);

## 1.0.1
ALTER TABLE NOTICE_PROTOCOL ADD VALID NUMBER(1,0) DEFAULT 1;--COMMENT '逻辑删除 0 无效，1 有效'
## 1.0.2
ALTER TABLE notice_message_unread_count ADD topic_id NUMBER(20,0) ;--COMMENT '主题id'

ALTER TABLE notice_message_unread_count DROP CONSTRAINT udx_notice_muc_staff_protocol;
ALTER TABLE notice_message_unread_count ADD CONSTRAINT udx_staff_protocol_topic unique(staff_code, notice_protocol_id,topic_id);
