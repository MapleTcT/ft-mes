package com.supcon.supfusion.rbac.webapi.vo.roleUser;

import lombok.Data;

import java.util.List;

@Data
public class ExportRoleUserVO {

    private List<Long> roleUserIds;

    private String id;

    private Long roleId;

    private int current;

    private int pageSize;

    private String keyword;
}
