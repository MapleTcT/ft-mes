package com.supcon.supfusion.iam.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午6:41
 */
public class AccountNotExistsException extends BizException {
    private static final long serialVersionUID = -1568801478250728015L;

    public AccountNotExistsException() {
        super(IAMErrorEnum.ACCOUNT_NOT_EXISTS);
    }
}
