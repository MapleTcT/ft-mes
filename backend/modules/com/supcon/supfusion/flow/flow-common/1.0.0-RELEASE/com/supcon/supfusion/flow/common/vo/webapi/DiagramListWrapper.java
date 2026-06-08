/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zhuangmh
 * @date: 2020年11月9日 下午3:01:07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiagramListWrapper implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String appId;

    private List<DiagramExportResponseVO> list;
    
}
