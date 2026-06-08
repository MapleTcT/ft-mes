package com.supcon.supfusion.rbac.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.rbac.api.IMenuOperateCodeApiService;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.dto.MenuOperateCodeUrlRefDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserDTO;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@ServiceApiService
@Transactional
public class MenuOperateCodeApiServiceImpl implements IMenuOperateCodeApiService {

    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;

    @Override
    public void saveBachUrl(List<MenuOperateCodeUrlRefDTO> list, String app) {
        menuOperateCodeUrlRefService.deleteByAppId(app);
        List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS = menuOperateCodeUrlRefService.list(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq("APP", app));
        List<MenuOperateCodeUrlRefPO> collect = list.stream().filter(menuOperateCodeUrlRefDTO -> {
            Optional<MenuOperateCodeUrlRefPO> first = menuOperateCodeUrlRefPOS.stream().filter(menuOperateCodeUrlRefPO -> menuOperateCodeUrlRefPO.getUrl().equals(menuOperateCodeUrlRefDTO.getUrl()) && menuOperateCodeUrlRefPO.getMethodType().equals(menuOperateCodeUrlRefDTO.getMethodType()) && menuOperateCodeUrlRefPO.getApp().equals(menuOperateCodeUrlRefDTO.getAppId()) && menuOperateCodeUrlRefPO.getMenuoperateCode().equals(menuOperateCodeUrlRefDTO.getMenuoperateCode())).findFirst();
            return !first.isPresent();
        }).map(menuOperateCodeUrlRefDTO -> {
            MenuOperateCodeUrlRefPO menuOperateCodeUrlRefPO = new MenuOperateCodeUrlRefPO();
            BeanUtils.copyProperties(menuOperateCodeUrlRefDTO, menuOperateCodeUrlRefPO);
            menuOperateCodeUrlRefPO.setImportType(0);
            menuOperateCodeUrlRefPO.setApp(app);
            return menuOperateCodeUrlRefPO;
        }).collect(Collectors.toList());
        menuOperateCodeUrlRefService.saveOrUpdateBatch(collect);
    }


}
