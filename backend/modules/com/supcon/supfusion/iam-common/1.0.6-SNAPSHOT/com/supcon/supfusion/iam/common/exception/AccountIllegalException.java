package com.supcon.supfusion.iam.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午6:49
 */
public class AccountIllegalException extends BizException {
    private static final long serialVersionUID = -8080796050276742025L;

    public AccountIllegalException() {
        super(IAMErrorEnum.ACCOUNT_IS_ILLEGAL);
    }
}
