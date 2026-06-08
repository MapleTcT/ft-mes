package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密码复杂表
 *
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.AUTH_PASSWORD_RULES, autoResultMap = true)
public class PwdRulesPO extends BaseEntity {

    private Long id;

    private Long minLength;

    private Long maxLength;

    private Boolean containLetterCase;

    private Boolean containNumbers;

    private Boolean containSpecialChar;

}
