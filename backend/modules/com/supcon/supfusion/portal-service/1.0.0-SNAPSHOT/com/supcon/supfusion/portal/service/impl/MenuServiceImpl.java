package com.supcon.supfusion.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.portal.dao.mapper.BaseMenuInfoMapper;
import com.supcon.supfusion.portal.dao.po.BaseMenuInfoPO;
import com.supcon.supfusion.portal.service.MenuService;
import com.supcon.supfusion.portal.service.entity.MenuInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    BaseMenuInfoMapper baseMenuInfoMapper;

    /**
     * @return java.util.List<com.supcon.supfusion.portal.service.entity.MenuInfo>
     * @Author kk.C
     * @Description 根据菜单ID返回菜单实体
     * @Date 2020/10/22 15:57
     * @Param [menuInfoIds]
     *
     * @param menuCodes*/
    @Override
    public List<MenuInfo> getMenuInfoBySet(Set<String> menuCodes) {
        if(ObjectUtils.isEmpty(menuCodes)){
            return null;
        }
        LambdaQueryWrapper<BaseMenuInfoPO> baseMenuInfoPOLambdaQueryWrapper = Wrappers.lambdaQuery(BaseMenuInfoPO.class).in(BaseMenuInfoPO::getCode, menuCodes).eq(BaseMenuInfoPO::getValid, 1);
        List<BaseMenuInfoPO> baseMenuInfoPOList = baseMenuInfoMapper.selectList(baseMenuInfoPOLambdaQueryWrapper);
        if (ObjectUtils.isEmpty(baseMenuInfoPOList)) {
            return null;
        }
        return baseMenuInfoPOList.stream().map(baseMenuInfoPO -> {
            MenuInfo menuInfo = new MenuInfo();
            BeanUtils.copyProperties(baseMenuInfoPO, menuInfo);
            return menuInfo;
        }).collect(Collectors.toList());

    }
}
