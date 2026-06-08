package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 密码规则表
 *
 * @author kk.c
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuthPasswdRulesVO extends VO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 密码最小长度
     */
    @Min(value = 8,message = "密码最小长度8")
    private Integer minLength;
    /**
     * 密码最大长度
     */
    @Max(value = 32,message = "密码最大长度32")
    private Integer maxLength;
    /**
     * 规则类型
     */
    @NotNull(message = "规则类型必填")
    private Integer ruleType;
    /**
     * 大小写
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean containLetterCase;
    /**
     * 数字
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean containNumbers;
    /**
     * 特殊字符
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean containSpecialChar;
    /**
     * 正则表达式内容
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String regularExpression;
    /**
     * 校验提示语
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hint;
    /**
     * 是否开启找回密码
     */
    private Boolean findPwdSwitch;
}
