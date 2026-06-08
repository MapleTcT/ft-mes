package com.supcon.supfusion.rbac.dao.bo;

import lombok.Data;

@Data
public class FlowPermissionBO {

    private Long typeId;

    private String activityCode;

    private Long userId;
}
