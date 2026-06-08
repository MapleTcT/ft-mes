## 1.0.0
/*==============================================================*/
/* Table: notice_protocol                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_protocol
(
   id                        BIGINT(20) NOT NULL COMMENT '协议ID',
   protocol                  VARCHAR(13) NOT NULL COMMENT '协议类型',
   name                      VARCHAR(50) NOT NULL COMMENT '协议展示名',
   app_name                  VARCHAR(128) NOT NULL COMMENT 'appName',
   vender_name               VARCHAR(256) NOT NULL COMMENT 'venderName',
   service_name              VARCHAR(256) NOT NULL COMMENT 'app访问地址',
   send_url                  VARCHAR(256) NOT NULL COMMENT '消息协议发送地址',
   config_url                VARCHAR(256) COMMENT '消息协议配置地址',
   system_config_app_code    VARCHAR(256) COMMENT '系统配置唯一标示：systemConfigAppCode + systemConfigCode',
   system_config_code        VARCHAR(256) COMMENT '系统配置唯一标示：systemConfigAppCode + systemConfigCode',
   default_template_code     VARCHAR(50) NOT NULL COMMENT '默认基础模板编号',
   content_type              TINYINT NOT NULL DEFAULT 0 COMMENT '消息内容支持格式, 0 纯文本、1 富文本',
   doc                       TEXT COMMENT '消息协议说明文档',
   i18n_module							 VARCHAR(100) COMMENT '国际化模块名',
   i18n_key							     VARCHAR(100) COMMENT '国际化key',
   system                    TINYINT DEFAULT 0 COMMENT '是否为系统协议',
   creator                   VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time               TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id           BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id           BIGINT(20) COMMENT '修改者人员Id',
   modifier                  VARCHAR(32) COMMENT '修改人',
   modify_time               TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_protocol_protocol(protocol)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议表';

/*==============================================================*/
/* Table: notice_protocol_config                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_protocol_config
(
   id                               BIGINT(20) NOT NULL COMMENT '配置ID',
   protocol                         VARCHAR(13) NOT NULL COMMENT '协议类型',
   config_value                     TEXT NOT NULL COMMENT '配置内容',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_protocol_config_protocol(protocol)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议配置表';

/*==============================================================*/
/* Table: notice_protocol_tmpl                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_protocol_tmpl
(
   id                               BIGINT(20) NOT NULL COMMENT '协议基础模板ID',
   code                             VARCHAR(50) NOT NULL COMMENT '基础模板编号',
   name                             VARCHAR(50) NOT NULL COMMENT '基础模板名称',
   i18n_key							            VARCHAR(100) COMMENT '国际化key',
   description                      VARCHAR(256) COMMENT '模板描述',
   template                         TEXT NOT NULL COMMENT '基础内容模板',
   system                           TINYINT DEFAULT 0 COMMENT '是否为系统基础模板',
   notice_protocol_id               BIGINT(20) NOT NULL COMMENT '协议ID',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_protocol_tmpl_code(code),
   UNIQUE INDEX udx_notice_protocol_tmpl_name(notice_protocol_id, name)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议基础模板表';

/*==============================================================*/
/* Table: notice_topic                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_topic
(
   id                               BIGINT(20) NOT NULL COMMENT '协议主题表ID',
   code                             VARCHAR(50) NOT NULL COMMENT '主题编号',
   name                             VARCHAR(50) NOT NULL COMMENT '主题名称',
   source                           VARCHAR(50) DEFAULT 'system'  NOT NULL COMMENT '主题来源',
   modify_sign                      TINYINT DEFAULT 1 COMMENT '修改标志,0 不允许修改 1 允许修改',
   cover_sign                       TINYINT DEFAULT 0 COMMENT '能否覆盖标志,0 不允许覆盖 1 允许覆盖',
   notice_topic_type_id             BIGINT(20) NOT NULL COMMENT '主题类型ID',
   description                      VARCHAR(256) COMMENT '主题类型描述',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   version                          INT DEFAULT 0 COMMENT '版本',
   valid                            TINYINT DEFAULT 0 COMMENT '是否删除',
   sort_value                       DOUBLE DEFAULT 0 NOT NULL COMMENT '主题排序字段',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_topic_code(code)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议主题表';

/*==============================================================*/
/* Table: notice_topic_type                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_topic_type
(
   id                               BIGINT(20) NOT NULL COMMENT '协议主题类型表ID',
   code                             VARCHAR(50) NOT NULL COMMENT '协议主题类型编码',
   name                             VARCHAR(50) NOT NULL COMMENT '协议主题类型名称',
   i18n_key							            VARCHAR(100) COMMENT '国际化key',
   source                           VARCHAR(50) DEFAULT 'system' NOT NULL COMMENT '协议主题类型来源',
   modify_sign                      TINYINT DEFAULT 1 COMMENT '修改标志,0 不允许修改 1 允许修改',
   parent_id                        BIGINT(20) COMMENT '协议主题类父级ID',
   sort_value                       DOUBLE DEFAULT 0 NOT NULL COMMENT '主题类型排序字段',
   description                      VARCHAR(256) COMMENT '主题类型描述',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   version                          INT  DEFAULT 0 COMMENT '版本',
   valid                            TINYINT DEFAULT 0 COMMENT '是否删除',
   lay_rec                          INT  DEFAULT 0 COMMENT '主题类型层级',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_topic_type_name(name)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议主题类型表';

/*==============================================================*/
/* Table: notice_tmpl                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_tmpl
(
   id                               BIGINT(20) NOT NULL COMMENT '协议模板表ID',
   code                             VARCHAR(50) NOT NULL COMMENT '协议模板编号',
   name                             VARCHAR(50) NOT NULL COMMENT '协议模板名称',
   params                           VARCHAR(50) COMMENT '模板默认参数',
   description                      VARCHAR(256) COMMENT '协议模板描述',
   template                         TEXT COMMENT '协议模板内容',
   source                           VARCHAR(50) DEFAULT 'system' NOT NULL COMMENT '协议模板来源',
   modify_sign                      TINYINT DEFAULT 1 COMMENT '修改标志,0 不允许修改 1 允许修改',
   cover_sign                       TINYINT DEFAULT 0 COMMENT '能否覆盖标志,0 不允许覆盖 1 允许覆盖',
   notice_protocol_id               BIGINT(20) COMMENT '协议表ID',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   version                          INT DEFAULT 0 COMMENT '版本',
   sort_value                       DOUBLE DEFAULT 0 NOT NULL COMMENT '排序',
   valid                            TINYINT DEFAULT 0 COMMENT '是否删除',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_tmpl_code(code)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='协议模板表';

/*==============================================================*/
/* Table: notice_topic_tmpl_rel                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_topic_tmpl_rel
(
   id                               BIGINT(20) NOT NULL COMMENT '协议模板关联表ID',
   notice_topic_id                  VARCHAR(32) NOT NULL COMMENT '主题表ID',
   notice_tmpl_id                   VARCHAR(32) NOT NULL COMMENT '模板表ID',
   notice_protocol_id               VARCHAR(32) NOT NULL COMMENT '协议表ID',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_topic_tmpl_rel(notice_topic_id, notice_tmpl_id ,notice_protocol_id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='主题模板关联表';

/*==============================================================*/
/* Table: notice_topic_range                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_topic_range
(
   id                               BIGINT(20) NOT NULL COMMENT '主题发送范围表ID',
   range_type                       TINYINT(2) NOT NULL COMMENT '推送方式, 分为0人员、1岗位、2部门、和3业务规则',
   bsmod_name                       VARCHAR(50) COMMENT '业务模块名称',
   bsmod_code                       VARCHAR(50) COMMENT '业务模块编码',
   bsmod_addr                       VARCHAR(32) COMMENT '业务模块提供接口地址，返回人员字符串',
   notice_topic_id                  BIGINT(20) NOT NULL COMMENT '协议模板表ID',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='主题发送范围表';

/*==============================================================*/
/* Table: notice_topic_range_ext                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_topic_range_ext
(
   id                               BIGINT(20) NOT NULL COMMENT '主题发送范围扩展表ID',
   receiver_id                      BIGINT(20) NOT NULL COMMENT '当推送方式为人员、部门、岗位时对应的ID',
   receiver_code                    VARCHAR(200) NOT NULL COMMENT '当推送方式为人员、部门、岗位时对应的CODE',
   contain_children                 TINYINT(2) NOT NULL DEFAULT 0 COMMENT '0 不包含子级, 1 包含子级',
   notice_topic_range_id            BIGINT(20) NOT NULL COMMENT '主题发送范围表ID',
   creator                          VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                      TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                  BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                  BIGINT(20) COMMENT '修改者人员Id',
   modifier                         VARCHAR(32) COMMENT '修改人',
   modify_time                      TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='主题发送范围扩展表';

/*==============================================================*/
/* Table: notice_task_protocol                                        */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_task_protocol
(
   id                                         BIGINT(20) NOT NULL COMMENT '发送任务协议表ID',
   notice_protocol_id                         BIGINT(20) NOT NULL COMMENT '协议表ID',
   notice_task_id                             BIGINT(20) NOT NULL COMMENT '发送任务表ID',
   content                                    TEXT NOT NULL COMMENT '消息内容',
   creator                                    VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                                TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                            BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                            BIGINT(20) COMMENT '修改者人员Id',
   modifier                                   VARCHAR(32) COMMENT '修改人',
   modify_time                                TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_task_protocol(notice_task_id, notice_protocol_id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='发送任务协议表';

/*==============================================================*/
/* Table: notice_message_unread                                 */
/*==============================================================*/
CREATE TABLE IF NOT EXISTS notice_message_unread_count
(
   id                                         BIGINT(20) NOT NULL COMMENT '未读消息数量统计表ID',
   staff_code                                 VARCHAR(200) NOT NULL COMMENT '人员code',
   notice_protocol_id                         BIGINT(20) NOT NULL COMMENT '协议表ID',
   unread_count                               BIGINT(20) NOT NULL default 0 COMMENT '未读消息数量统计',
   creator                                    VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                                TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                            BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                            BIGINT(20) COMMENT '修改者人员Id',
   modifier                                   VARCHAR(32) COMMENT '修改人',
   modify_time                                TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   UNIQUE INDEX udx_notice_muc_staff_protocol(staff_code, notice_protocol_id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='未读消息数量统计表';

/*==============================================================*/
/*                        DATA                                  */
/*==============================================================*/
INSERT INTO notice_protocol (id, protocol, name, i18n_key, app_name, vender_name, service_name, send_url, default_template_code, content_type, system, creator, create_staff_id) VALUES (1, 'email', 'notificationAdmin.src_common_mail', 'notificationAdmin.src_common_mail', 'email', 'supcon', 'supcon_email', 'sendUrl' , '001', 1, 1, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_protocol (id, protocol, name, i18n_key, app_name, vender_name, service_name, send_url, default_template_code, content_type, system, creator, create_staff_id) VALUES (2, 'stationLetter', 'notificationAdmin.src_common_information', 'notificationAdmin.src_common_information', 'stationLetter', 'supcon', 'supcon_stationLetter', 'sendUrl', '003', 0, 1, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator, create_staff_id) VALUES (1, '001', '行政通知', 'notificationAdmin.protocol_basic_module_admin', '**部门发布了《${name}$》的通知，请注意查收！', 1, 1, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator, create_staff_id) VALUES (2, '002', '待办消息', 'notificationAdmin.protocol_basic_module_todo', '${username}$，您有一条“${name}$”的待办，请及时处理！', 1, 1, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator, create_staff_id) VALUES (3, '003', '行政通知', 'notificationAdmin.protocol_basic_module_admin', '**部门发布了《${name}$》的通知，请注意查收！', 1, 2, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_protocol_tmpl (id, code, name, i18n_key, template, system, notice_protocol_id, creator, create_staff_id) VALUES (4, '004', '待办消息', 'notificationAdmin.protocol_basic_module_todo', '${username}$，您有一条“${name}$”的待办，请及时处理！', 1, 2, 'default', 1) ON DUPLICATE KEY UPDATE creator = VALUES (creator);


INSERT INTO notice_topic_type( id, parent_id, lay_rec, code, name,i18n_key, version, sort_value, description, valid, modify_sign, source, creator, create_staff_id, create_time ) VALUES ( 1000, 0, 0, 'defaultType' , '默认类型','notificationAdmin.type_default', 0, 0, '固有类型', 0, 0, 'system', 'admin', 1, '2020-05-18 20:31:47.875'  ) ON DUPLICATE KEY UPDATE creator = VALUES (creator);
INSERT INTO notice_topic_type( id, parent_id, lay_rec, code, name,i18n_key,version, sort_value, description, valid, modify_sign, source, creator, create_staff_id, create_time ) VALUES ( 1001, 0, 0, 'defaultType002' , '待办类型','notificationAdmin.type_todo', 0, 0, '固有类型', 0, 0, 'system', 'admin', 1, '2020-05-18 20:31:47.875'  ) ON DUPLICATE KEY UPDATE creator = VALUES (creator);


/* 初始化待办模板 */
INSERT INTO notice_tmpl ( id, code, name, template, notice_protocol_id, creator, create_time, create_staff_id) VALUES( 1, 'defult_bap_pending', '平台待办消息模板', '{"text":"<p>您有一条新待办消息! </p><p>流程名称：${title} </p><p>待办名称：${content} </p><p>${extendcontent} </p><p>${url} </p><p>创建人：${creator} </p><p>创建时间：${creationTime} </p>","subject":"平台待办消息模板"}', 1, 'default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_tmpl ( id, code, name, template, notice_protocol_id, creator, create_time, create_staff_id) VALUES( 2, 'defult_bap_reminding', '平台催办消息模板', '{"text":"<p>您有一条新催办消息! </p><p>流程名称：${title} </p><p>待办名称：${content} </p><p>${extendcontent} </p><p>${url} </p><p>创建人：${creator} </p><p>创建时间：${creationTime} </p>","subject":"平台催办消息模板"}', 1, 'default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_tmpl ( id, code, name, template, notice_protocol_id, creator, create_time, create_staff_id) VALUES( 3, 'defult_bap_over_pending', '平台超期待办消息模板', '{"text":"<p>${title}</p><p> 超期待办：${content} </p><p>${extendcontent} </p><p>${url}</p><p>创建人：${creator}</p><p>创建时间：${creationTime}</p>","subject":"平台超期待办消息模板"}', 1, 'default',  '2020-10-26 20:31:47.875', 1);

INSERT INTO notice_topic (id,code,name,modify_sign,notice_topic_type_id,creator,create_time,create_staff_id) VALUES (1,'bap_pending','待办消息',1,1001,'default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_topic (id,code,name,modify_sign,notice_topic_type_id,creator,create_time,create_staff_id) VALUES (2,'bap_reminding','催办消息',1,1001,'default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_topic (id,code,name,modify_sign,notice_topic_type_id,creator,create_time,create_staff_id) VALUES (3,'bap_over_pending','超期待办消息',1,1001,'default',  '2020-10-26 20:31:47.875', 1);

INSERT INTO notice_topic_tmpl_rel (id,notice_topic_id,notice_tmpl_id,notice_protocol_id,creator,create_time,create_staff_id) VALUES (1,'1','1','1','default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_topic_tmpl_rel (id,notice_topic_id,notice_tmpl_id,notice_protocol_id,creator,create_time,create_staff_id) VALUES (2,'2','2','1','default',  '2020-10-26 20:31:47.875', 1);
INSERT INTO notice_topic_tmpl_rel (id,notice_topic_id,notice_tmpl_id,notice_protocol_id,creator,create_time,create_staff_id) VALUES (3,'3','3','1','default',  '2020-10-26 20:31:47.875', 1);

## 1.0.1
ALTER TABLE notice_protocol ADD COLUMN valid TINYINT DEFAULT 1 COMMENT '逻辑删除 0 无效，1 有效';


## 1.0.2
ALTER TABLE notice_message_unread_count ADD topic_id BIGINT(20) ;--COMMENT '主题id'

ALTER TABLE notice_message_unread_count DROP KEY udx_notice_muc_staff_protocol;
ALTER TABLE notice_message_unread_count ADD CONSTRAINT udx_staff_protocol_topic UNIQUE KEY (staff_code, notice_protocol_id,topic_id);
