/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2021年3月12日 上午11:21:55
 */
@Data
public class AuditRequestVO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String seqKey;
    
    private String value;

}
