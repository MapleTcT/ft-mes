CREATE TABLE IF NOT EXISTS notice_task_protocol_${month}
(
id                                         BIGINT NOT NULL,
notice_protocol_id                         BIGINT NOT NULL,
notice_task_id                             BIGINT NOT NULL,
content                                    TEXT NOT NULL,
creator                                    VARCHAR(32) NOT NULL,
create_time                                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
create_staff_id                            BIGINT NOT NULL,
modify_staff_id                            BIGINT,
modifier                                   VARCHAR(32),
modify_time                                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS udx_ntp_${month}
ON notice_task_protocol_${month}(notice_task_id, notice_protocol_id);
