/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zhuangmh
 * @date: 2021年1月14日 下午3:23:26
 */
@Data
public class TaskTotalsDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean containTask;
    
    private List<TaskTotal> list;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskTotal implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        /**
         * 用户ID
         */
        private long userId;
        /**
         * 待办总数
         */
        private int total;
    }
    
}
