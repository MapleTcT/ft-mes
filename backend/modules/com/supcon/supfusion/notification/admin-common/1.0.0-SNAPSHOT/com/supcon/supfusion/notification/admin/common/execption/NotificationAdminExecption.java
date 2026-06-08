package com.supcon.supfusion.notification.admin.common.execption;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class NotificationAdminExecption extends BizException {

    public NotificationAdminExecption(NotificationAdminDefinition notificationAdminDefinition) {
        super(notificationAdminDefinition);
    }

    public NotificationAdminExecption(NotificationAdminDefinition notificationAdminDefinition, Throwable throwable) {
        super(notificationAdminDefinition, throwable);
    }

    public NotificationAdminExecption( ErrorDefinition notificationAdminDefinition, final Object... args){
        super(notificationAdminDefinition, args);
    }
}
