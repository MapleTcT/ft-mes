/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.io.Serializable;
import java.util.List;

import com.supcon.supfusion.flow.common.po.DiagramPO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zhuangmh
 * @date: 2021年1月8日 下午4:37:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransportDiagramDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private List<DiagramPO> diagrams;

}
