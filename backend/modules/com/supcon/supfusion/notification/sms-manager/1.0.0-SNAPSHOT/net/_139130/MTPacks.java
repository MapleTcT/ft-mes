
package net._139130;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;MTPacks complex type的 Java 类。
 * <p>
 * &lt;p&gt;以下模式片段指定包含在此类中的预期内容。
 * <p>
 * &lt;pre&gt;
 * &amp;lt;complexType name="MTPacks"&amp;gt;
 * &amp;lt;complexContent&amp;gt;
 * &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 * &amp;lt;sequence&amp;gt;
 * &amp;lt;element name="uuid" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="batchID" type="{http://www.w3.org/2001/XMLSchema}string"/&amp;gt;
 * &amp;lt;element name="batchName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="sendType" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="msgType" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="medias" type="{http://www.139130.net}ArrayOfMediaItems" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="msgs" type="{http://www.139130.net}ArrayOfMessageData" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="bizType" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 * &amp;lt;element name="distinctFlag" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 * &amp;lt;element name="scheduleTime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="remark" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="customNum" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="deadline" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;element name="templateNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 * &amp;lt;/sequence&amp;gt;
 * &amp;lt;/restriction&amp;gt;
 * &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MTPacks", propOrder = {
        "uuid",
        "batchID",
        "batchName",
        "sendType",
        "msgType",
        "medias",
        "msgs",
        "bizType",
        "distinctFlag",
        "scheduleTime",
        "remark",
        "customNum",
        "deadline",
        "templateNo"
})
public class MTPacks {

    @XmlElement(required = true)
    protected String uuid;
    @XmlElement(required = true)
    protected String batchID;
    protected String batchName;
    protected int sendType;
    protected int msgType;
    protected ArrayOfMediaItems medias;
    protected ArrayOfMessageData msgs;
    protected int bizType;
    protected boolean distinctFlag;
    protected String scheduleTime;
    protected String remark;
    protected String customNum;
    protected String deadline;
    protected String templateNo;

    /**
     * 获取uuid属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * 设置uuid属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUuid(String value) {
        this.uuid = value;
    }

    /**
     * 获取batchID属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getBatchID() {
        return batchID;
    }

    /**
     * 设置batchID属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setBatchID(String value) {
        this.batchID = value;
    }

    /**
     * 获取batchName属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getBatchName() {
        return batchName;
    }

    /**
     * 设置batchName属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setBatchName(String value) {
        this.batchName = value;
    }

    /**
     * 获取sendType属性的值。
     */
    public int getSendType() {
        return sendType;
    }

    /**
     * 设置sendType属性的值。
     */
    public void setSendType(int value) {
        this.sendType = value;
    }

    /**
     * 获取msgType属性的值。
     */
    public int getMsgType() {
        return msgType;
    }

    /**
     * 设置msgType属性的值。
     */
    public void setMsgType(int value) {
        this.msgType = value;
    }

    /**
     * 获取medias属性的值。
     *
     * @return possible object is
     * {@link ArrayOfMediaItems }
     */
    public ArrayOfMediaItems getMedias() {
        return medias;
    }

    /**
     * 设置medias属性的值。
     *
     * @param value allowed object is
     *              {@link ArrayOfMediaItems }
     */
    public void setMedias(ArrayOfMediaItems value) {
        this.medias = value;
    }

    /**
     * 获取msgs属性的值。
     *
     * @return possible object is
     * {@link ArrayOfMessageData }
     */
    public ArrayOfMessageData getMsgs() {
        return msgs;
    }

    /**
     * 设置msgs属性的值。
     *
     * @param value allowed object is
     *              {@link ArrayOfMessageData }
     */
    public void setMsgs(ArrayOfMessageData value) {
        this.msgs = value;
    }

    /**
     * 获取bizType属性的值。
     */
    public int getBizType() {
        return bizType;
    }

    /**
     * 设置bizType属性的值。
     */
    public void setBizType(int value) {
        this.bizType = value;
    }

    /**
     * 获取distinctFlag属性的值。
     */
    public boolean isDistinctFlag() {
        return distinctFlag;
    }

    /**
     * 设置distinctFlag属性的值。
     */
    public void setDistinctFlag(boolean value) {
        this.distinctFlag = value;
    }

    /**
     * 获取scheduleTime属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getScheduleTime() {
        return scheduleTime;
    }

    /**
     * 设置scheduleTime属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setScheduleTime(String value) {
        this.scheduleTime = value;
    }

    /**
     * 获取remark属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置remark属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setRemark(String value) {
        this.remark = value;
    }

    /**
     * 获取customNum属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getCustomNum() {
        return customNum;
    }

    /**
     * 设置customNum属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCustomNum(String value) {
        this.customNum = value;
    }

    /**
     * 获取deadline属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getDeadline() {
        return deadline;
    }

    /**
     * 设置deadline属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDeadline(String value) {
        this.deadline = value;
    }

    /**
     * 获取templateNo属性的值。
     *
     * @return possible object is
     * {@link String }
     */
    public String getTemplateNo() {
        return templateNo;
    }

    /**
     * 设置templateNo属性的值。
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTemplateNo(String value) {
        this.templateNo = value;
    }

}
