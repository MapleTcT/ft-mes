/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;

import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午3:44:42
 */
@Data
public class AuditVO implements Comparable<Integer>, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 迁移线ID
     */
    @ApiModelProperty(value = "迁移线ID", name = "id", example = "NO_123")
    private String id;
    /**
     * 选择分支名称
     */
    @ApiModelProperty(value = "选择分支名称", name = "name", example = "驳回")
    private String name;
    /**
     * 选择分支(驳回OR同意OR其他)
     */
    @ApiModelProperty(value = "选择分支", name = "value", example = "1")
    private String value;
    /**
     * 输出分支排序, 从左到右依次递减
     */
    @ApiModelProperty(value = "输出分支排序, 从左到右依次递减", name = "order", example = "1")
    private int order;
    /**
     * 分支线类型 0: 普通迁移线 1: 驳回线
     */
    @ApiModelProperty(value = "分支线类型  0: 普通迁移线 1: 驳回线", name = "type", example = "0")
    @Pattern(regexp = "^[1-9]\\d+", message = "非法参数type")
    private int type;
    /**
     * 在有序队列中将按照从大到小排序
     */
    @Override
    public int compareTo(Integer arg0) {
        if (arg0 == null) {
            return -1;
        }
        return this.order >= arg0 ? 1 : -1;
    }
}
