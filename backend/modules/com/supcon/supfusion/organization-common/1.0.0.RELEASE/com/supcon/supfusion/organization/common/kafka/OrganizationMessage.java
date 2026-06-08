package com.supcon.supfusion.organization.common.kafka;

import com.supcon.supfusion.framework.cloud.common.message.Message;
import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
public class OrganizationMessage {

    @Autowired
    private OrganizationStream organizationStream;

    public boolean publishMessage(Message message) {
        return organizationStream.organizationOutput().send(MessageBuilder.withPayload(message).setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build());
    }

    /**
     * 构造消息类
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

/*    public static void main(String[] args) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println(sdf.getTimeZone().getID());
        String str = "2021-01-26T16:02:15.666+0000";
        System.out.println(sdf.parse(str));

        System.out.println(sdf.format(new Date()));
    }*/
}
