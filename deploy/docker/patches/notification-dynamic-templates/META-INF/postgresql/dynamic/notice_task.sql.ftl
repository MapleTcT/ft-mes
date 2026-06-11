CREATE TABLE IF NOT EXISTS ${notice_task}
(
id                                 BIGINT NOT NULL,
code                               VARCHAR(128) NOT NULL,
bsmod_code                         VARCHAR(200) NOT NULL,
bsmod_name                         VARCHAR(200) NOT NULL,
task_type                          SMALLINT NOT NULL,
status                             SMALLINT DEFAULT 0,
sharding_time                      BIGINT NOT NULL,
notice_topic_id                    BIGINT,
creator                            VARCHAR(32) NOT NULL,
create_time                        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
create_staff_id                    BIGINT NOT NULL,
modify_staff_id                    BIGINT,
modifier                           VARCHAR(32),
modify_time                        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (id)
);
