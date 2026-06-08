CREATE TABLE IF NOT EXISTS ${notice_msg}
(
   id                                         BIGINT(20) NOT NULL COMMENT '消息表ID',
   staff_code                                 VARCHAR(200) NOT NULL COMMENT '人员CODE',
   staff_name                                 VARCHAR(200) NOT NULL COMMENT '人员名称',
   bsmod_code                                 VARCHAR(200) COMMENT '业务方事务编号,用于站内信查询',
   bsmod_name                                 VARCHAR(200) COMMENT '业务模块名称,用于站内信查询',
   topic_name                                 VARCHAR(32) COMMENT '主题名称,用于站内信查询',
   topic_id                                   BIGINT(20)  COMMENT '主题id',
   user_name                                  VARCHAR(200) COMMENT '用户名称，兼容通知中心1.0的发送接口',
   send_status                                TINYINT(2) NOT NULL COMMENT '发送状态 0失败，1成功，2未知',
   error_result                               VARCHAR(200) COMMENT '发送失败返回结果',
   param                                      LONGTEXT COMMENT '业务端原始json参数',
   read_status                                TINYINT(2) NOT NULL COMMENT '阅读状态 0未读，1已读，2未知',
   retry                                      TINYINT(2) default 0 COMMENT '失败重试次数',
   sharding_time                              BIGINT(20) NOT NULL COMMENT '分表时间戳',
   notice_task_id                             BIGINT(20) NOT NULL COMMENT '发送任务表ID',
   notice_protocol_id                         BIGINT(20) NOT NULL COMMENT '协议表ID',
   notice_task_protocol_id                    BIGINT(20) NOT NULL COMMENT '发送任务协议表ID',
   creator                                    VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                                TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                            BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                            BIGINT(20) COMMENT '修改者人员Id',
   modifier                                   VARCHAR(32) COMMENT '修改人',
   modify_time                                TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   INDEX ${notice_msg}(sharding_time)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='消息表';
