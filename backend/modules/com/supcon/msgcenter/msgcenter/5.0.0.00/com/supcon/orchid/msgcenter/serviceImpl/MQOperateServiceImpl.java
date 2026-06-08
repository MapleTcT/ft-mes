package com.supcon.orchid.msgcenter.serviceImpl;

import com.google.gson.Gson;

import com.supcon.orchid.msgcenter.config.AppConfig;
import com.supcon.orchid.msgcenter.entity.MSGCTRMessage;
import com.supcon.orchid.msgcenter.kafka.Producer;
import com.supcon.orchid.msgcenter.service.MQOperateService;
import com.supcon.orchid.msgcenter.utils.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Service("mqOperateService")
public class MQOperateServiceImpl implements MQOperateService {

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Gson gson = new Gson();
    private static final String WS_GROUP_PATH = "/ms/msg/sendWebSocketGroup";
    private static final String WS_ALL_PATH = "/ms/msg/sendWebSocket";

    @Value("${msgctr.host}")
    private String host;
    @Autowired
    private Producer producer;

    @Override
    public String send(String topic, Map<String, Object> msgMap) {
        String sendType = msgMap.get("sendType") == null ? null : msgMap.get("sendType").toString();
        msgMap.put("announceID", UUID.randomUUID().toString());
        producer.send(topic, msgMap);
        return (String) msgMap.get("announceID");
    }

    @Override
    public String send(List<String> redirect, List<String> recievers, String title, String content, String url, String extendContent) {
        MSGCTRMessage message = new MSGCTRMessage();
        message.setSendType(AppConfig.sendType_user);
        message.setRedirect(redirect);
        message.setReceiveStaff(recievers);
        message.setTitle(title);
        message.setContent(content);
        message.setUsedModel(false);
        message.setURL(url);
        message.setExtendContent(extendContent);
        return this.send(message);
    }

    @Override
    public String send(String msgType, List<String> recievers, String modelParam) {
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson((String) modelParam, Map.class);
        MSGCTRMessage message = new MSGCTRMessage();
        message.setSendType(AppConfig.sendType_user);
        message.setUsedModel(true);
        message.setMsgType(msgType);
        message.setReceiveStaff(recievers);
        message.setModelParam(map);
        return this.send(message);
    }

    @Override
    public String send(String msgType, List<String> recievers, String title, String content, String url, String extendContent) {
        MSGCTRMessage message = new MSGCTRMessage();
        message.setSendType(AppConfig.sendType_user);
        message.setMsgType(msgType);
        message.setReceiveStaff(recievers);
        message.setTitle(title);
        message.setContent(content);
        message.setUsedModel(false);
        message.setURL(url);
        message.setExtendContent(extendContent);
        return this.send(message);
    }

    @Override
    public String send(String msgTheme, String modelParam) {
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson((String) modelParam, Map.class);
        MSGCTRMessage message = new MSGCTRMessage();
        message.setSendType(AppConfig.sendType_theme);
        message.setMsgTheme(msgTheme);
        message.setModelParam(map);
        message.setUsedModel(true);
        return this.send(message);
    }

    @Override
    public String send(String msgTheme, String title, String content, String url, String extendContent) {
        MSGCTRMessage message = new MSGCTRMessage();
        message.setSendType(AppConfig.sendType_theme);
        message.setMsgTheme(msgTheme);
        message.setTitle(title);
        message.setContent(content);
        message.setUsedModel(false);
        message.setURL(url);
        message.setExtendContent(extendContent);
        return this.send(message);
    }

    @Override
    public String send(MSGCTRMessage message) {
        message.setAnnounceID(UUID.randomUUID().toString());
        //处理MongoDB使用世界时间（东八区加8）
        Date sendTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(sendTime);
        cal.add(Calendar.HOUR, 8);
        //sendTime = cal.getTime();
        message.setCreateTime(sendTime);
        message.setSendStaff(AppConfig.MAIL_CODE);
        message.setConsumeTime(0);
        logger.info("发送消息开始：" + message.getAnnounceID());
        producer.send(message);
        return message.getAnnounceID();
    }

    /***
     * webSocket消息发送 指定人员
     * @param groupList  消息分组列表
     * @param message   消息字符串
     */
    @Override
    @Async
    public void sendWebsocket(String groupList, String message) {
        Map<String,String> map = new HashMap<>();
        map.put("groupList",groupList);
        map.put("str",message);
        String body = new Gson().toJson(map);
        URLUtils.actPostURL("http://" + host + WS_GROUP_PATH,body);
    }

    /***
     * webSocket消息发送 广播
     * @param message   消息字符串
     */
    @Override
    @Async
    public void sendWebsocket(String message) {
        Map<String,String> map = new HashMap<>();
        map.put("str",message);
        String body = new Gson().toJson(map);
        URLUtils.httpURLPOSTCase("http://" + host + WS_ALL_PATH,body);
    }

    /***
     * 处理发送BAP待办
     * @param receivers  接收者列表
     * @param title     标题
     * @param content   正文
     * @param url       url
     * @param tableNo   单据号
     * @param tableName 单据名称
     * @return
     */
    @Override
    @Async
    public String sendBAPPending(List<String> receivers,String title,String content,String url ,String tableNo,String tableName){
        Map<String,String> modelParam = new HashMap<>();
        Map<String,String> extendParam = new HashMap<>();
        modelParam.put("tableNo",tableNo);
        modelParam.put("tableName",tableName);
        modelParam.put("title",title);
        modelParam.put("content",content);
        modelParam.put("url",url);
//        modelParam.put("extendContent",gson.toJson(extendParam));
        modelParam.put("extendContent","{\"a\":\"b\"}");
        String code = this.send("BAP_Pending", receivers, gson.toJson(modelParam));
        return code;
    }
    /***
     * 处理发送BAP待办
     * @param receivers  接收者列表
     * @param title     标题
     * @param content   正文
     * @param url       url
     * @param tableNo   单据号
     * @param tableName 单据名称
     * @return
     */
    @Override
    @Async
    public String sendBAPReminding(List<String> receivers,String title,String content,String url ,String tableNo,String tableName){
        Map<String,String> modelParam = new HashMap<>();
        Map<String,String> extendParam = new HashMap<>();
        modelParam.put("tableNo",tableNo);
        modelParam.put("tableName",tableName);
        modelParam.put("title",title);
        modelParam.put("content",content);
        modelParam.put("url",url);
        //modelParam.put("extendContent",gson.toJson(extendParam));
        modelParam.put("extendContent","{\"a\":\"b\"}");
        String code = this.send("BAP_Reminding", receivers, gson.toJson(modelParam));
        return code;
    }
    /***
     * 处理发送BAP待办
     * @param receivers  接收者列表
     * @param title     标题
     * @param content   正文
     * @param url       url
     * @param tableNo   单据号
     * @param tableName 单据名称
     * @return
     */
    @Override
    @Async
    public String sendBAPOverPending(List<String> receivers,String title,String content,String url ,String tableNo,String tableName){
        Map<String,String> modelParam = new HashMap<>();
        Map<String,String> extendParam = new HashMap<>();
        modelParam.put("tableNo",tableNo);
        modelParam.put("tableName",tableName);
        modelParam.put("title",title);
        modelParam.put("content",content);
        modelParam.put("url",url);
//        modelParam.put("extendContent",gson.toJson(extendParam));
        modelParam.put("extendContent","{\"a\":\"b\"}");
        String code = this.send("BAP_Over_Pending", receivers, gson.toJson(modelParam));
        return code;
    }

}
