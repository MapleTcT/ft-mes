CREATE TABLE ${notice_msg}
(
   id                                         BIGINT NOT NULL,
   staff_code                                 VARCHAR(200) NOT NULL,
   staff_name                                 VARCHAR(200) NOT NULL,
   bsmod_code                                 VARCHAR(200),
   bsmod_name                                 VARCHAR(200),
   topic_name                                 VARCHAR(32),
   topic_id                                   bigint,
   user_name                                  VARCHAR(200),
   send_status                                TINYINT NOT NULL,
   error_result                               VARCHAR(200),
   param                                      text ,
   read_status                                TINYINT NOT NULL,
   retry                                      TINYINT default 0,
   sharding_time                              BIGINT NOT NULL,
   notice_task_id                             BIGINT NOT NULL,
   notice_protocol_id                         BIGINT NOT NULL,
   notice_task_protocol_id                    BIGINT NOT NULL,
   creator                                    VARCHAR(32) NOT NULL,
   create_time                                datetime NOT NULL,
   create_staff_id                            BIGINT NOT NULL, -- COMMENT '创建者人员Id',
   modify_staff_id                            BIGINT, -- COMMENT '修改者人员Id',
   modifier                                   VARCHAR(32),
   modify_time                                datetime,
   PRIMARY KEY (id)
);
create INDEX ${notice_msg} on ${notice_msg}(sharding_time);
