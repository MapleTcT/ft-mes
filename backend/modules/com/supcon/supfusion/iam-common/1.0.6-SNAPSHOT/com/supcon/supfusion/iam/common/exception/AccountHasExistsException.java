package com.supcon.supfusion.iam.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:25
 */
public class AccountHasExistsException extends BizException {

    public AccountHasExistsException() {
        super(IAMErrorEnum.ACCOUNT_IS_EXISTS);
    }
}
