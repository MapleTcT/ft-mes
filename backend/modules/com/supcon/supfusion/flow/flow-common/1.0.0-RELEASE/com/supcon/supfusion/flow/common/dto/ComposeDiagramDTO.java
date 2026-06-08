/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.io.Serializable;
import java.util.List;

import com.supcon.supfusion.flow.common.po.DiagramPO;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月25日 下午1:26:09
 */
@Data
@AllArgsConstructor
public class ComposeDiagramDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程组态列表
     */
    private List<DiagramPO> diagrams;
}
