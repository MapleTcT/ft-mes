package com.supcon.supfusion.framework.scaffold.kafka.util;

import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.stream.binder.kafka.utils.KafkaTopicUtils;

import java.util.Map;

/**
 * @author tomcat
 * @date 20-5-23 下午1:59
 */
public class TopicUtils implements InitializingBean {

    private static volatile Map<String, Object> adminProperties;

    public static void setAdminProperties(Map<String, Object> adminProperties) {
        TopicUtils.adminProperties = adminProperties;
    }

    /**
     * 新增Topic
     *
     * @param topic
     */
    public static void addTopic(NewTopic topic) {
        KafkaTopicUtils.validateTopicName(topic.name());
        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            adminClient.createTopics(Lists.newArrayList(topic));
        }
    }

    /**
     * 删除Topic
     *
     * @param topicName
     */
    public static void deleteTopic(String topicName) {
        KafkaTopicUtils.validateTopicName(topicName);
        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            adminClient.deleteTopics(Lists.newArrayList(topicName));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建租户消息主题
        NewTopic topic = new NewTopic(TenantInfo.topicName, 1, (short) 1);
        TopicUtils.addTopic(topic);
    }
}
