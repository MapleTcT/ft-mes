package com.supcon.orchid.msgcenter.service;

import com.supcon.orchid.msgcenter.entity.MSGCTRMessage;

import java.util.List;
import java.util.Map;


public interface MQOperateService {
    /***
     * mq任意格式消息传递
     * @param topic  自定义mq信道
     * @param msgMap   自定义消息结构
     */
    public String send(String topic, Map<String, Object> msgMap);

    /***
     * mq规定格式传递
     * @param message   消息传输对象
     * {@link MSGCTRMessage}
     */
    public String send(MSGCTRMessage message);

    /****
     * 指定发送方式，发送人，发送内容
     * @param redirect  接收方式
     * @param recievers 接收人
     * @param title     标题
     * @param content   内容
     */
    public String send(List<String> redirect, List<String> recievers, String title, String content, String url, String extendContent);


    /****
     * 用户发送 1.1 启用消息模板
     * @param msgType   消息类型编码
     * @param recievers 接收者
     * @param modelParam    模板参数
     */
    public String send(String msgType, List<String> recievers, String modelParam);


    /***
     * 用户发送 1.2 不启用消息模板
     * @param msgType   消息类型
     * @param recievers 接收者
     * @param title     标题
     * @param content   内容
     */
    public String send(String msgType, List<String> recievers, String title, String content, String url, String extendContent);

    /***
     * 主题发送   2.1 启用模板
     * @param msgTheme  主题编码
     * @param modelParam    模板参数
     */
    public String send(String msgTheme, String modelParam);

    /***
     * 主题发送 不启用模板
     * @param msgTheme  主题编码
     * @param title     标题
     * @param content   内容
     */
    public String send(String msgTheme, String title, String content, String url, String extendContent);

    /***
     * webSocket消息发送
     * @param message   消息字符串
     */
    public void sendWebsocket(String message);

    /***
     * 指分组消息发送
     * @param groupList  消息分组列表
     * @param message   消息字符串
     */
    public void sendWebsocket(String groupList, String message);

    /****
     * 发送BAP平台待办
     * @param receivers 接收者编码
     * @param title     标题
     * @param content   正文（暂时无用）
     * @param url       url （暂时无用）
     * @param tableNo   流程编码
     * @param tableName 流程名
     * @return
     */
    public String sendBAPPending(List<String> receivers,String title,String content,String url ,String tableNo,String tableName);

    /***
     * 发送BAP平台催办
     * @param receivers 接收者编码
     * @param title     标题
     * @param content   正文（无用）
     * @param url       url （无用）
     * @param tableNo   流程编码
     * @param tableName 流程名
     * @return
     */
    public String sendBAPReminding(List<String> receivers,String title,String content,String url ,String tableNo,String tableName);

    /***
     * 发送BAP平台超期待办
     * @param receivers 接收者编码
     * @param title     标题
     * @param content   正文（无用）
     * @param url       url （无用）
     * @param tableNo   流程编码
     * @param tableName 流程名
     * @return
     */
        public String sendBAPOverPending(List<String> receivers,String title,String content,String url ,String tableNo,String tableName);



    }
