package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;

import java.util.List;

public interface IMenuAppDesignerRelService extends IService<MenuAppDesignerRelPO> {
    void updateAppDesinerRel(MenuAppDesignerRelPO menuAppDesignerRelPO);
    void deleteAppDesignerByCode(String code);
    void getMenuAppDesignerCodeList(String code,List<String> codeList);
}
