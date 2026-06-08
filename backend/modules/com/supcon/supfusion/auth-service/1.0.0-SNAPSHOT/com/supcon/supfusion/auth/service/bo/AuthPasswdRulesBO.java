package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 密码规则表
 *
 * @author kk.c
 */
@Data
public class AuthPasswdRulesBO{
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 密码最小长度
     */
    private Integer minLength;
    /**
     * 密码最大长度
     */
    private Integer maxLength;
    /**
     * 规则类型
     */
    private Integer ruleType;
    /**
     * 大小写
     */
    private Boolean containLetterCase;
    /**
     * 数字
     */
    private Boolean containNumbers;
    /**
     * 特殊字符
     */
    private Boolean containSpecialChar;
    /**
     * 正则表达式内容
     */
    private String regularExpression;
    /**
     * 校验提示语
     */
    private String hint;
    /**
     * 是否开启找回密码
     */
    private Boolean findPwdSwitch;
}
