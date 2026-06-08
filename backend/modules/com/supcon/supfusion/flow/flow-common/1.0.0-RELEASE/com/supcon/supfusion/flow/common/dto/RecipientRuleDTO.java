/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.List;

import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;

import lombok.Data;

/**
 * 待办接收规则,对应xml如下:
 * <flowable:assigneeRule id="r1" name="depart" value="" posRestrict="true" groupRestrict="true" position="" person="" unrestrict="false"></flowable:assigneeRule>
 * 
 * @author: zhuangmh
 * @date: 2020年9月22日 上午10:11:17
 */
@Data
public class RecipientRuleDTO {
    
    /**
     * 
     */
    private RecipientSelection recipientSelect;
    /**
     * 待办接收者, 可以是岗位,部门,角色,人员
     */
    private String value;
    /**
     * 岗位限制
     */
    private boolean posRestrict;
    /**
     * 组限制
     */
    private boolean groupRestrict;
    /**
     * 指定岗位
     */
    private List<String> positions;
    /**
     * 指定人员
     */
    private List<String> persons;
    /**
     * 无限制
     */
    private boolean unrestrict;
}
