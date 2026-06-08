/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.po;

import java.io.Serializable;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月2日 上午11:20:45
 */
@Data
public class StatisticsPO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程名称
     */
    private String diagramName;
    /**
     * 活动名称
     */
    private String taskName;
    /**
     * 总数
     */
    private Integer count;
}
