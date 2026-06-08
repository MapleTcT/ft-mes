package com.supcon.supfusion.auth.service.bo.bap;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap人员
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapStaffBO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 版本号
     */
    private Integer version = 0;
    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 性别
     */
    @JSONField(name = "sex2")
    private BapSystemCodeBO sex;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 人员状态
     */
    @JSONField(name = "workStatus2")
    private BapSystemCodeBO workStatus;
    /**
     * 密级
     */
    @JSONField(name = "securityClass2")
    private BapSystemCodeBO securityClass;
    /**
     * 是否有效
     */
    private Boolean valid;
    /**
     * 主岗id
     */
    private Long mainPositionId;
    /**
     * 主岗对象
     */
    private BapPositionBO mainPosition;
    /**
     * 备注
     */
    private String memo;
}
