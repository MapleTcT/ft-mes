package com.supcon.orchid.msgcenter.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.supcon.orchid.msgcenter.config.IpConfig;
import com.supcon.orchid.msgcenter.entity.MSGCTRMessage;
import com.supcon.orchid.msgcenter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Map;

/**
 * @Author: shx
 * @Description:
 * @Date: 2019年11月20日
 */
@Component
public class Producer {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static Gson gson = new GsonBuilder().create();

    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    IpConfig ip;

    //Kafka服务地址
    @Value("${spring.kafka.bootstrap-servers}")
    private String host;

    @Value("${spring.kafka.producer.key:kafkaKey}")
    private String kafkaKey;

    //发送消息方法Map
    public void send(String topic, Map message) {
        logger.debug("执行消息发送-自定义:" + message.toString());
        kafkaTemplate.send(topic, kafkaKey, gson.toJson(message));
        logger.debug("发送消息成功，信道为：" + topic);
    }

    //发送消息方法MSGCTRMessage
    public void send(MSGCTRMessage message) {
        message.setHost(host);
        kafkaTemplate.send(AppConfig.TOPIC_MESSAGE, kafkaKey, gson.toJson(message));
        logger.debug("发送消息成功，信道为：" + AppConfig.TOPIC_MESSAGE);
    }


}
