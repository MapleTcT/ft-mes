package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.DataPermission;
import com.supcon.supfusion.base.entities.DataPermissionStaff;
import com.supcon.supfusion.base.entities.DataPmsPosition;

public interface CustomDataPermissionService {
    /**
     * 保存permission
     */
    void savePermission(DataPermission dataPermission);

    /**
     * 保存permissionStaff
     */
    void savePermissionStaff(DataPermissionStaff dataPermissionStaff);

    /**
     * 保存permissionPosition
     */
    void savePermissionPosition(DataPmsPosition dataPmsPosition);



}
