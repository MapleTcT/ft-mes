/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2021年3月9日 上午10:30:41
 */
@Data
public class DiagramPublishDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String id;

    private String xml;
}
