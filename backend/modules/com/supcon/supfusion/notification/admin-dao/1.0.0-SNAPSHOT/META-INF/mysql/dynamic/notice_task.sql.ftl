CREATE TABLE IF NOT EXISTS ${notice_task}
(
   id                                 BIGINT(20) NOT NULL COMMENT '发送任务表ID',
   code                               VARCHAR(128) NOT NULL COMMENT '发送任务表CODE = id + date',
   bsmod_code                         VARCHAR(200) NOT NULL COMMENT '业务方事务编号',
   bsmod_name                         VARCHAR(200) NOT NULL COMMENT '业务模块名称',
   task_type                          TINYINT(2) NOT NULL COMMENT '任务类型,0topic 1message',
   status                             TINYINT(2) default 0 COMMENT '任务状态',
   sharding_time                      BIGINT(20) NOT NULL COMMENT '分表时间戳',
   notice_topic_id                    BIGINT(20) COMMENT '协议主题表ID',
   creator                            VARCHAR(32) NOT NULL COMMENT '创建人',
   create_time                        TIMESTAMP NOT NULL COMMENT '创建时间',
   create_staff_id                    BIGINT(20) NOT NULL COMMENT '创建者人员Id',
   modify_staff_id                    BIGINT(20) COMMENT '修改者人员Id',
   modifier                           VARCHAR(32) COMMENT '修改人',
   modify_time                        TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (id),
   INDEX ${notice_task}(sharding_time)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='发送任务表';
