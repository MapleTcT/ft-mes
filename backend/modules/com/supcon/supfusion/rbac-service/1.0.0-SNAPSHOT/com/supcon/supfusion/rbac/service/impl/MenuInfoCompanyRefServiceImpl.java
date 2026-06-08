package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.dao.MenuInfoCompanyRefMapper;
import com.supcon.supfusion.rbac.dao.field.MenuInfoCompanyRefField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.IMenuInfoCompanyRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * <p>
 * 菜单公司关联表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-30
 */
@Slf4j
@Service
@Transactional
public class MenuInfoCompanyRefServiceImpl extends ServiceImpl<MenuInfoCompanyRefMapper, MenuInfoCompanyRefPO> implements IMenuInfoCompanyRefService {

    @Autowired
    private MenuInfoCompanyRefMapper menuInfoCompanyRefMapper;

    /**
     * @description: 公司菜单分页查询
     * @param: menuInfoId
     * @param: keyword
     * @param: current
     * @param: pageSize
     * @return: com.baomidou.mybatisplus.core.metadata.IPage<com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public IPage<MenuInfoCompanyRefPO> findByPage(Long menuInfoId, String keyword, Integer current, Integer pageSize) {
        Page<MenuInfoCompanyRefPO> page = new Page<>(current, pageSize);
        QueryWrapper<MenuInfoCompanyRefPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MenuInfoCompanyRefField.menuinfoId,menuInfoId);
        if (!ObjectUtils.isEmpty(keyword)){
            queryWrapper.and(menuInfoCompanyRefPOQueryWrapper -> menuInfoCompanyRefPOQueryWrapper.like("MI."+ MenuInfoField.name,keyword));
        }
        return menuInfoCompanyRefMapper.findPage(page, queryWrapper);

    }
}
