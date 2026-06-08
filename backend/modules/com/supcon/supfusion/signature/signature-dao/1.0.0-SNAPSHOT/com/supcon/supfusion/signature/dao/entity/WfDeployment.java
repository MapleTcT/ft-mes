package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.signature.dao.entity.base.LogicBasePO;
import lombok.Data;

import java.util.Date;

/**
 * @author zhang yafei
 */
@Data
@TableName("wf_deployment")
public class WfDeployment extends LogicBasePO {

    private static final long serialVersionUID = -5425438079328401144L;

    @TableId
    private Long id;

    private String name;// 流程名称英文
    private String processKey; // 流程key
    private int processVersion;// 版本
    private String description;// 描述
    private String processName;// 引擎中对应名称
    private String deploymentId;// 对应JBPM中DEPLOYMENT的Id
    private Boolean isSuspended;// 是否暂停
    private Boolean isCurrentVersion = false;// 是否当前版本
    private String processDefinitionId;// 对应JBPM中流程定义的ID
    private Long menuInfoId;
    private String menuCode;
    // private Long entityId;//所述实体ID
    private String entityCode;
    private Boolean publishFlag;// 是否发布
    private String operatePowers;
    private String processXml;// 流程XML
    private String entryUrl;// 首次打开地址
    private String tempProcessXml;// 临时保存的流程xml
    private Boolean flowEditFlag;// 是否可以修改发布
    private java.math.BigDecimal requiredTime;// 规定完成时间
    private Boolean mobilequery = false;
    private Boolean mobileinitiate = false;
    private Boolean mobileapprove = true;
    private Boolean allowInvalid = false; // 允许管理员作废
    private Boolean graduallyReject = false; // 逐级驳回
    private Boolean recallAble = false;
    private Long recallRemainTime;
    private Date publishTime; // 发布时间
    private Long cid;
    /**
     * @author chaibohai
     * BAP-XA-DBZY
     * 加上电子签名字段
     */
    private Boolean signatureEnable;


    private String mainViewViewCode; // 工作流查看视图
}
