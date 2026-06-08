/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 上午10:46:19
 */
@Data
public class StaffDTO {
    
    public StaffDTO(String staffCode, String staffName) {
        this.staffCode = staffCode;
        this.staffName = staffName;
    }
    /**
     * 人员名称
     */
    private String staffName;
    /**
     * 人员编码
     */
    private String staffCode;
}
