package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonSynchronizationInfoBO {

    private String code;

    private String name;

    private Integer valid;

    private String modifyTime;

    private UserBO user;

    private SystemCodeBO gender;

    private SystemCodeBO status;

    private MainPositionBaseBO mainPosition;

    private List<MainPositionBaseBO> positions;

    private List<PersonCompanyBaseBO> companies;

    private List<PersonDepartmentBaseBO> departments;

    /**
     * 头像
     */
    private String imageUrl;

    /**
     * 签名
     */
    private String signPicUrl;

    /**
     * 入职时间
     */
    private String entryDate;

    /**
     * 职称
     */
    private SystemCodeBO title;

    /**
     * 学历
     */
    private SystemCodeBO education;

    /**
     * 资质
     */
    private String qualification;

    /**
     * 专业
     */
    private String major;

    /**
     * 身份证号
     */
    private String idNumber;
}
