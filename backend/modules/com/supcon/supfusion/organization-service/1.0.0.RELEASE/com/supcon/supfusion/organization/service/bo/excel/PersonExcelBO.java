package com.supcon.supfusion.organization.service.bo.excel;


import lombok.*;

/**
 * 人员Excel字段
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonExcelBO {

    /**
     * 人员编号
     */
    private String code;

    /**
     *  人员名称
     */
    private String name;

    /**
     * 性别
     */
    private String gender;

    /**
     * 人员状态
     */
    private String status;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
     private String email;

    /**
     * 描述
     */
    private String description;

    /**
     * 主岗编码
     */
    private String mainPosition;


}
