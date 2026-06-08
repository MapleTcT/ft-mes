package com.supcon.supfusion.auth.service.kafka;

import com.supcon.supfusion.framework.cloud.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;

@Service
public class AuthUserMessage {

    @Autowired
    AuthUserStream organizationStream;

    public boolean publishMessage(Message message) {
        return organizationStream
                .authuserOutput()
                .send(MessageBuilder.withPayload(message)
                        .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                        .build());
    }

    /**
     * 构造消息类
     *
     * @param <T>
     */
    public static class Builder<T> {

        private Message message = new Message();

        public Builder setApiVersion(String apiVersion) {
            message.setApiVersion(apiVersion);
            return this;
        }

        public Builder setSender(String sender) {
            message.setSender(sender);
            return this;
        }

        public Builder setTenantId(String tenantId) {
            message.setTenantId(tenantId);
            return this;
        }

        public Builder setCreateTime(String createTime) {
            message.setCreateTime(createTime);
            return this;
        }

        public Builder setTopic(String topic) {
            message.setTopic(topic);
            return this;
        }

        public Builder setHeader(Map<String, Object> header) {
            message.setHeader(header);
            return this;
        }

        public Builder setBody(List<T> body) {
            message.setBody(body);
            return this;
        }

        public Message build() {
            return message;
        }
    }
}
