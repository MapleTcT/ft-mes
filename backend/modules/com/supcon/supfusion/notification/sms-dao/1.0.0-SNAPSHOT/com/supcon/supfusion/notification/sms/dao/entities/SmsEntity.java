package com.supcon.supfusion.notification.sms.dao.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 金仓短信猫，短信消息实体
 *
 * @author chenweinan
 * @version 1.0
 */
@Getter
@Setter
@ToString
@TableName(value = "sms_boxsending")
public class SmsEntity {

    @TableId(type = IdType.AUTO)
    private Long id;  //id,自增
    @TableField(value = "appid")
    private String appid;
    @TableField(value = "sender")
    private String sender; //发送人手机号
    @TableField(value = "receiver")
    private String receiver;  //接受人手机号，多个接收人之间用","隔开
    @TableField(value = "content")
    private String content;  //发送内容
    @TableField(value = "sendtime")
    private String sendTime;  //发送时间
    @TableField(value = "inserttime")
    private String insertTime;  //插入时间
    @TableField(value = "retrytimes")
    private Integer retryTimes;  //重复发送次数
    @TableField(value = "pri")
    private String pri;
    @TableField(value = "inpool")
    private String inpool;
    @TableField(value = "inpooltime")
    private String inpoolTime;
    @TableField(value = "task_batchName")
    private String taskBatchName;
    @TableField(value = "addtion1")
    private String addtion1;
    @TableField(value = "addtion2")
    private String addtion2;
    @TableField(value = "addtion3")
    private String addtion3;
    @TableField(value = "moduleid")
    private String moduleId;
    @TableField(value = "sendmode")
    private String sendmode;
    @TableField(value = "configid")
    private String configId;
    @TableField(value = "linkid1")
    private String linkId1;
    @TableField(value = "linkid2")
    private String linkId2;
    @TableField(value = "ifreceipt")
    private String ifreceipt;
    @TableField(value = "validityperiod")
    private String validityperiod;
    @TableField(value = "outteruser")
    private String outteruser;

    /**
     * 空构造函数
     */
    public SmsEntity() {
        this.retryTimes = 0;
    }

    /**
     * 带参数构造函数
     *
     * @param id
     * @param appid
     * @param sender
     * @param receiver
     * @param content
     * @param sendTime
     * @param insertTime
     * @param retryTimes
     * @param pri
     * @param inpool
     * @param inpoolTime
     * @param taskBatchName
     * @param addtion1
     * @param addtion2
     * @param addtion3
     * @param sendmode
     * @param configId
     * @param linkId1
     * @param linkId2
     * @param ifreceipt
     * @param validityperiod
     * @param outteruser
     */
    public SmsEntity(Long id, String appid, String sender, String receiver, String content, String sendTime, String insertTime, int retryTimes, String pri, String inpool, String inpoolTime, String taskBatchName, String addtion1, String addtion2, String addtion3, String moduleId, String sendmode, String configId, String linkId1, String linkId2, String ifreceipt, String validityperiod, String outteruser) {
        super();
        this.id = id;
        this.appid = appid;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.sendTime = sendTime;
        this.insertTime = insertTime;
        this.retryTimes = retryTimes;
        this.pri = pri;
        this.inpool = inpool;
        this.inpoolTime = inpoolTime;
        this.taskBatchName = taskBatchName;
        this.addtion1 = addtion1;
        this.addtion2 = addtion2;
        this.addtion3 = addtion3;
        this.moduleId = moduleId;
        this.sendmode = sendmode;
        this.configId = configId;
        this.linkId1 = linkId1;
        this.linkId2 = linkId2;
        this.ifreceipt = ifreceipt;
        this.validityperiod = validityperiod;
        this.outteruser = outteruser;
    }

}
