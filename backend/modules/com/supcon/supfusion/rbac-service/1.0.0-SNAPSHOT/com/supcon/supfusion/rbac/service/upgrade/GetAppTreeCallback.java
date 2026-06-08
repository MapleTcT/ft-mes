package com.supcon.supfusion.rbac.service.upgrade;

import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;

import java.util.List;

@FunctionalInterface
public interface GetAppTreeCallback {
    void callback(List<MenuAppDesignerRelPO> menuAppDesignerRelPOS);
}