package com.supcon.supfusion.rbac.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.MenuAppDesignerRelMapper;
import com.supcon.supfusion.rbac.dao.field.MenuAppDesignerRelField;
import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;
import com.supcon.supfusion.rbac.service.IMenuAppDesignerRelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class MenuAppDesignerRelServiceImpl extends ServiceImpl<MenuAppDesignerRelMapper, MenuAppDesignerRelPO> implements IMenuAppDesignerRelService {
    @Autowired
    private IMenuAppDesignerRelService menuAppDesignerRelService;



    @Override
    public void updateAppDesinerRel(MenuAppDesignerRelPO menuAppDesignerRelPO) {
        UpdateWrapper<MenuAppDesignerRelPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("code", menuAppDesignerRelPO.getCode());
        update(menuAppDesignerRelPO, updateWrapper);
    }

    @Override
    public void deleteAppDesignerByCode(String code) {
        List<String> menuAppDesignerCodes = new ArrayList<>();
        // TODO 此方法需要优化
        this.getMenuAppDesignerCodeList(code, menuAppDesignerCodes);

        QueryWrapper<MenuAppDesignerRelPO> menuAppDesignerRelWrapper = new QueryWrapper<>();
        menuAppDesignerRelWrapper.lambda().in(MenuAppDesignerRelPO::getCode, menuAppDesignerCodes);
        menuAppDesignerRelService.remove(menuAppDesignerRelWrapper);
    }


    @Override
    public void getMenuAppDesignerCodeList(String code, List<String> codeList) {
        //查询节点下所有子节点
        QueryWrapper<MenuAppDesignerRelPO> appDesignerQueryWrapper = new QueryWrapper<>();
        appDesignerQueryWrapper.eq(MenuAppDesignerRelField.code, code);
        MenuAppDesignerRelPO menuAppDesignerRelPO = menuAppDesignerRelService.getOne(appDesignerQueryWrapper);
        if (null != menuAppDesignerRelPO) {
            // 保存menuid，menuPo
            codeList.add(menuAppDesignerRelPO.getCode());

            QueryWrapper<MenuAppDesignerRelPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("PARENT_CODE", menuAppDesignerRelPO.getCode());
            List<MenuAppDesignerRelPO> childMenuInfoPOList = menuAppDesignerRelService.list(queryWrapper);
            while (!CollectionUtils.isEmpty(childMenuInfoPOList)) {
                List<String> codes = new ArrayList<>();
                childMenuInfoPOList.stream().forEach(menuAppDesignerRel -> {
                    codes.add(menuAppDesignerRel.getCode());
                    codeList.add(menuAppDesignerRel.getCode());
                });

                QueryWrapper<MenuAppDesignerRelPO> subMenuWrapper = new QueryWrapper<>();
                subMenuWrapper.in("PARENT_CODE", codes);
                childMenuInfoPOList = menuAppDesignerRelService.list(subMenuWrapper);
            }
        }

    }
}
