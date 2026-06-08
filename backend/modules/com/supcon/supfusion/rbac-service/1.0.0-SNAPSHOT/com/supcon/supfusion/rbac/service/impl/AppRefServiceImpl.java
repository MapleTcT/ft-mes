package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.AppRefMapper;
import com.supcon.supfusion.rbac.dao.field.AppRefField;
import com.supcon.supfusion.rbac.dao.po.AppRefPO;
import com.supcon.supfusion.rbac.service.IAppRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 自定义权限表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Slf4j
@Service
@Transactional
public class AppRefServiceImpl extends ServiceImpl<AppRefMapper, AppRefPO> implements IAppRefService {
    @Override
    public List<Long> findAppRefMenuId(List<Long> menuInfoIds) {
        if (ObjectUtils.isEmpty(menuInfoIds)){
            return new ArrayList<>();
        }
        QueryWrapper<AppRefPO> childAppRefWrapper =new QueryWrapper<AppRefPO>();        
        int batch = menuInfoIds.size() / 1000;
        if (batch == 0) {
            childAppRefWrapper.in(AppRefField.menuId, menuInfoIds);
	    } else {
	        for (int i = 0; i < batch; i++) {
	        	childAppRefWrapper.or().in(AppRefField.menuId, menuInfoIds.subList(i * 1000, i * 1000 + 1000));
	        }
	        childAppRefWrapper.or().in(AppRefField.menuId, menuInfoIds.subList(batch * 1000, menuInfoIds.size()));
	    }
//        List<AppRefPO> list = this.list(new QueryWrapper<AppRefPO>().in(AppRefField.menuId, menuInfoIds));
        List<AppRefPO> list = this.list(childAppRefWrapper);
        if (!ObjectUtils.isEmpty(list)){
            return list.stream().map(AppRefPO::getMenuId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<Long> queryMenuIdListByAppId(String appId) {
        if(StringUtils.isEmpty(appId)){
            return new ArrayList<>();
        }
        List<AppRefPO> list = this.list(new QueryWrapper<AppRefPO>().in(AppRefField.appId, appId));
        if (!ObjectUtils.isEmpty(list)){
            return list.stream().map(AppRefPO::getMenuId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
