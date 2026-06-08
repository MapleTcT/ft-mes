package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.MenuOperate;

import java.util.List;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/17
 */
public interface MenuUserDealInfoService {

    void savePermissionChangesLoggerForWorkFlow(List<Map<String,Object>> permissionDatas);

    /**
     * 根据菜单操作物理删除处理记录
     * @param menuOperate
     */
    void deletePhysicalByMenuOperate(MenuOperate menuOperate);

}
