package com.supcon.orchid.msgcenter.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class AppConfig {
    /*   默认信道  */
    public static final String TOPIC_MESSAGE = "supPlant.message";

    //消息发送类型
    public static final String sendType_user = "USER";
    public static final String sendType_theme = "THEME";
    //推送类型
    public static final String pushType_position = "POSITION";
    public static final String pushType_business = "BSMOD";
    //邮箱
    public static final String MAIL_CODE = "staff002";
    //消息提醒类型
    public static final String redirectType_BAP = "BAP";
    public static final String redirectType_SMS = "SMS";
    public static final String redirectType_IM = "IM";
    public static final String redirectType_MOBILE = "MOBILE";
    public static final String redirectType_EMAIL = "EMAIL";
    public static final String redirectType_VX = "VX";
    public static final String redirectType_GIS = "GIS";
    //webSocket 心跳检测标记
    public static final String websocket_PING = "PING";
    public static final String websocket_PONG = "PONG";

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
