/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月18日 下午3:31:55
 */
@Data
@TableName("wfm_diagram_content")
public class DiagramContentPO extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 唯一ID
     */
    private Long id;
    /**
     * 已发布的流程JSON数据
     */
    private String publishedJson;
    /**
     * 待发布的流程JSON数据
     */
    private String draftJson;
    /**
     * 最近一次发布xml
     */
    private String xml;
}
