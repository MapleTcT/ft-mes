package com.supcon.supfusion.auth.manager.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.*;

/**
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LoginConfigBO extends BO {
    private Boolean bigSmall;

    private Boolean number;

    private Boolean specialChar;

    private Integer min;

    private Integer max;

    /**
     * 正则表达式内容
     */
    private String regularExpression;
    /**
     * 校验提示语
     */
    private String hint;

    private Integer ruleType;


}
