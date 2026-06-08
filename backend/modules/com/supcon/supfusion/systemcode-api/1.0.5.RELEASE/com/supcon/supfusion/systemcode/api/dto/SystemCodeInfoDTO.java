package com.supcon.supfusion.systemcode.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeInfoDTO extends DTO {

    private static final long serialVersionUID = 4716280542388821430L;

    /**
     * 值的编码
     */
    @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
    @Length(max = 100, message = "systemCode.CODE_LENGTH_MAX_ERROR")
    private String code;

    /**
     * 值的名称,国际化键
     */
    @NotBlank(message = "systemCode.NAME_PARAM_NECESSARY")
    @Length(max = 500, message = "systemCode.NAME_LENGTH_MAX_ERROR")
    private String name;

    /**
     * 显示名称
     */
    @NotBlank(message = "systemCode.NAME_PARAM_NECESSARY")
    @Length(max = 500, message = "systemCode.NAME_LENGTH_MAX_ERROR")
    private String displayName;

    /**
     * 所属公司ID
     */
    @NotNull(message = "systemCode.COMPANY_ID_PARAM_NECESSARY")
    private Long cid;

    /**
     * 父节点编码
     */
    private String parentCode;

    /**
     * 顺序
     */
    private Double sort;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 是否默认
     */
    private Integer defaultFlag;

    /**
     * 是否叶子节点
     * 如果没有子节点为true,反之则为false
     */
    private boolean leaf;

    /**
     * 备注
     */
    @Length(max = 255, message = "systemCode.MEMO_LENGTH_MAX_ERROR")
    private String memo;

    /**
     * 描述C
     */
    @Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
    private String desC;

    /**
     * 描述B
     */
    @Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
    private String desB;

    /**
     * 描述A
     */
    @Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
    private String desA;

}
