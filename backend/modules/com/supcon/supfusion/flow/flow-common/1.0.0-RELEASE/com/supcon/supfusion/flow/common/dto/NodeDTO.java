/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月31日 上午10:10:50
 */
@Data
@AllArgsConstructor
public class NodeDTO {

    /**
     * 
     */
    /**
     * 节点ID
     */
    private String id;
    /**
     * 节点名
     */
    private String name;
    /**
     * 是否包含直属领导
     */
    private boolean containLeaderLevel1;
    /**
     * 是否包含隔级领导
     */
    private boolean containLeaderLevel2;

}
