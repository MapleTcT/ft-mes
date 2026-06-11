CREATE TABLE IF NOT EXISTS ${notice_msg}
(
id                                         BIGINT NOT NULL,
staff_code                                 VARCHAR(200) NOT NULL,
staff_name                                 VARCHAR(200) NOT NULL,
bsmod_code                                 VARCHAR(200),
bsmod_name                                 VARCHAR(200),
topic_name                                 VARCHAR(32),
topic_id                                   BIGINT,
user_name                                  VARCHAR(200),
send_status                                SMALLINT NOT NULL,
error_result                               VARCHAR(200),
param                                      TEXT,
read_status                                SMALLINT NOT NULL,
retry                                      SMALLINT DEFAULT 0,
sharding_time                              BIGINT NOT NULL,
notice_task_id                             BIGINT NOT NULL,
notice_protocol_id                         BIGINT NOT NULL,
notice_task_protocol_id                    BIGINT NOT NULL,
creator                                    VARCHAR(32) NOT NULL,
create_time                                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
create_staff_id                            BIGINT NOT NULL,
modify_staff_id                            BIGINT,
modifier                                   VARCHAR(32),
modify_time                                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_${notice_msg}_staff_protocol
ON ${notice_msg}(staff_code, notice_protocol_id, read_status);
CREATE INDEX IF NOT EXISTS idx_${notice_msg}_staff_topic
ON ${notice_msg}(staff_code, topic_id);
CREATE INDEX IF NOT EXISTS idx_${notice_msg}_task
ON ${notice_msg}(notice_task_id);
