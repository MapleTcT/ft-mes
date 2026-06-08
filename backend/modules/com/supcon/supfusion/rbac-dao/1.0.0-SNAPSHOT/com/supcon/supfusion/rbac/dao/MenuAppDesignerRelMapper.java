package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MenuAppDesignerRelMapper extends BaseMapper<MenuAppDesignerRelPO> {
    @Select("select * from rbac_menu_app_designer")
    List<MenuAppDesignerRelPO> getAllAppDesignerRef();
}
