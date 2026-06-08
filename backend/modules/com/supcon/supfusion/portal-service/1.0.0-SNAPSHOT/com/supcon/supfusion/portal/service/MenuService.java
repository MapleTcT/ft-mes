package com.supcon.supfusion.portal.service;

import com.supcon.supfusion.portal.service.entity.MenuInfo;

import java.util.List;
import java.util.Set;

/**
 * @Author kk.C
 * @Description 菜单相关service
 * @Date 2020/10/22 14:41
 * @Param
 * @return
 **/
public interface MenuService {

    List<MenuInfo> getMenuInfoBySet(Set<String> menuCodes);
}
