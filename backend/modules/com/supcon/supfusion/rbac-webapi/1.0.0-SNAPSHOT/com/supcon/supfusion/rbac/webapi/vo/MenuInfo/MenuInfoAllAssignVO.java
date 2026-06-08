package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import lombok.Data;

import java.util.List;

@Data
public class MenuInfoAllAssignVO {

    private String menuInfoName;

    private List<MenuInfoAssignVO> ops;
}
