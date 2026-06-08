/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zhuangmh
 * @date: 2020年6月8日 下午4:46:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntrustUserVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 受托者-用户ID
     */
    private String mandatary;
    /**
     * 委托者-用户ID
     */
    private String principal;
}
