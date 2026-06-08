package com.supcon.orchid.msgcenter.entity;

import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ToString
public class MSGCTRMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String MODULE_CODE = "MSGCTR_1.0.0";

    //传输消息UUID
    private String announceID;
    //推送方式（用户/岗位/主题）
    private String sendType;
    //消息类型
    private String msgType;
    //消息主题
    private String msgTheme;
    //是否启用模板
    private Boolean usedModel;
    //消息模板
    private String msgModel; // 消息模板
    //标题
    private String title;
    //正文
    private String content;
    //扩展内容
    private String extendContent;
    //用户接收方式
    private List<String> redirect;
    //模板参数
    private Map<String, Object> modelParam;
    //接收者
    private List<String> receiveStaff;
    //发送者
    private String sendStaff;
    //URL
    private String URL;
    //消息是否消费
    private Boolean hasConsume;
    //消息消费时间
    private Integer consumeTime;
    //消息发送时间
    private Date createTime;
    //岗位
    private List<String> position;

    private String host;

    public String getAnnounceID() {
        return announceID;
    }

    public void setAnnounceID(String announceID) {
        this.announceID = announceID;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }


    public Boolean getUsedModel() {
        return usedModel;
    }

    public void setUsedModel(Boolean usedModel) {
        this.usedModel = usedModel;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtendContent() {
        return extendContent;
    }

    public void setExtendContent(String extendContent) {
        this.extendContent = extendContent;
    }

    public List<String> getRedirect() {
        return redirect;
    }

    public void setRedirect(List<String> redirect) {
        this.redirect = redirect;
    }

    public Map<String, Object> getModelParam() {
        return modelParam;
    }

    public void setModelParam(Map<String, Object> modelParam) {
        this.modelParam = modelParam;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getMsgTheme() {
        return msgTheme;
    }

    public void setMsgTheme(String msgTheme) {
        this.msgTheme = msgTheme;
    }

    public String getMsgModel() {
        return msgModel;
    }

    public void setMsgModel(String msgModel) {
        this.msgModel = msgModel;
    }

    public List<String> getReceiveStaff() {
        return receiveStaff;
    }

    public void setReceiveStaff(List<String> receiveStaff) {
        this.receiveStaff = receiveStaff;
    }

    public String getSendStaff() {
        return sendStaff;
    }

    public void setSendStaff(String sendStaff) {
        this.sendStaff = sendStaff;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public List<String> getPosition() {
        return position;
    }

    public void setPosition(List<String> position) {
        this.position = position;
    }

    public Boolean getHasConsume() {
        return hasConsume;
    }

    public void setHasConsume(Boolean hasConsume) {
        this.hasConsume = hasConsume;
    }

    public Integer getConsumeTime() {
        return consumeTime;
    }

    public void setConsumeTime(Integer consumeTime) {
        this.consumeTime = consumeTime;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public MSGCTRMessage() {
        super();
    }

}
