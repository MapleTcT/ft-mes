/**
 * Licensed to the Deep Blue SUPCON
 * @author: zhuangmh
 * @date: 2020年5月18日 下午4:00:08
 */
package com.supcon.supfusion.flow.common.po;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;

import lombok.Data;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月18日 下午4:00:08
 */
@Data
@TableName(value = "wfm_diagram", autoResultMap = true)
public class DiagramPO extends LogicDeleteBaseEntity {

    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 
     */
    private String appId;
    /**
     * 公司ID
     */
    private Long cid;
    /**
     * 流程编号, 系统编码
     */
    private String processKey;
    /**
     * 流程名称
     */
    private String processName;
    /**
     * 流程JSON数据
     */
    private Long contentId;
    /**
     * 流程发布时间
     */
    @TableField(
            value = "publish_time",
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String publishTime;
    /**
     * 上一次修改组态的时间
     */
    @TableField(
            value = "latest_modify_time",
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String latestModifyTime;
    /**
     * 创建者人员名称
     */
    private String creatorStaff;
    /**
     * 流程发布者
     */
    private String publisher;
    /**
     * 流程版本
     */
    private Integer version;
    /**
     * 已经发布的组态数据
     */
    @TableField(exist=false)
    private String publishedJson;
    /**
     * 草稿组态数据
     */
    @TableField(exist=false)
    private String draftJson;
    /**
     * 是否启用
     */
    private Integer enabled;
    /**
     * 是否支持多公司 0-单公司 1-多公司
     */
    private Integer multiCompany;
    /**
     * 是否可以在移动端启动
     */
    private Integer startOnMobile;
    /**
     * 流程状态 1-新增, 2-发布, 3-发布并修改, 4-导入版本
     */
    private Integer processStatus;
    /**
     * 租户ID
     */
    private String tenantId;
    
}
