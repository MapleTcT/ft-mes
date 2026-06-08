package com.supcon.supfusion.organization.dao.po.person;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonBaseInfoPO {

    /**
     * 人员id
     */
    private Long id;

    /**
     * 人员编号
     */
    private String code;

    /**
     * 人员名称
     */
    private String name;

    /**
     * 删除标识
     */
    private Integer valid;

    /**
     * 性别编码
     */
    private String genderCode;

    /**
     * 状态编码
     */
    private String statusCode;

    /**
     * 主岗编码
     */
    private String mainPositionCode;

    /**
     * 主岗名称
     */
    private String mainPositionName;

    /**
     * 头像
     */
    private String imageUrl;

    /**
     * 签名
     */
    @TableField(value = "sign_pic_url")
    private String signPicUrl;

    /**
     * 入职时间
     */
    private String entryDate;

    /**
     * 职称
     */
    private String title;

    /**
     * 学历
     */
    private String education;

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
