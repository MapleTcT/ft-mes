package com.supcon.supfusion.notification.apiserver.common.execption;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;

public class NotificationApiServerExecption extends BizException {

    public NotificationApiServerExecption(NotificationApiServerDefinition notificationAdminDefinition) {
        super(notificationAdminDefinition);
    }

    public NotificationApiServerExecption(NotificationApiServerDefinition notificationAdminDefinition, Throwable throwable) {
        super(notificationAdminDefinition, throwable);
    }
}
