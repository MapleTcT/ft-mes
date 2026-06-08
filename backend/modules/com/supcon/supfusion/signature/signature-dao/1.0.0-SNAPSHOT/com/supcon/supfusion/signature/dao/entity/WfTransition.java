package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.signature.dao.annotation.BAPInternational;
import com.supcon.supfusion.signature.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
@TableName("wf_transition")
public class WfTransition extends LogicBasePO {

    private static final long serialVersionUID = 5886041919705534880L;

    @TableId
    private Long id;
    private String name;//名称
    private String code;//编码
    private Integer type;//1普通迁移线2驳回线
    private String fromNodeCode;//起始活动
    private String toNodeCode;//目标活动
    private Long deploymentId;//流程id
    private String selectStaff;//是否可选人0，否，1是，2跨公司选人、3本部门、4本部门及下级、5自定义
    private Boolean requiredStaff;//选人必填
    private String Expression;//表达式
    private Integer routeSequence;//序号
    @TableField(value="DEFAULT_STAFF")
    private Boolean defaultSelectStaff;//是否默认上次选人
}
