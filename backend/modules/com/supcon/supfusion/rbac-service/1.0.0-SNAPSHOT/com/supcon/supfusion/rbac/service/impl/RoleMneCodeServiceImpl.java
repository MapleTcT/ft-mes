package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.MneCodeGenterate;
import com.supcon.supfusion.rbac.dao.RoleMneCodeMapper;
import com.supcon.supfusion.rbac.dao.field.RoleMneCodeField;
import com.supcon.supfusion.rbac.dao.po.RoleMneCodePO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.service.IRoleMneCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单助记码表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Service
public class RoleMneCodeServiceImpl extends ServiceImpl<RoleMneCodeMapper, RoleMneCodePO> implements IRoleMneCodeService {

    @Override
    @Transactional
    public void createRoleMneCode(List<RolePO> rolePOS) {
        if (ObjectUtils.isEmpty(rolePOS)){
            return;
        }
        List<RoleMneCodePO> roleMneCodePOS = new ArrayList<>();
        rolePOS.forEach(rolePO -> {
            //先清空原有的助记码,重新添加
            deleteMneCode(rolePO.getId());
            List<String> strings = MneCodeGenterate.mneCodeTupleGenerate(rolePO.getName());
            List<RoleMneCodePO> mneCodePOS = strings.stream().map(s -> {
                RoleMneCodePO roleMneCodePO = new RoleMneCodePO();
                roleMneCodePO.setRoleId(rolePO.getId());
                roleMneCodePO.setMneCode(s);
                return roleMneCodePO;
            }).collect(Collectors.toList());
            if (!ObjectUtils.isEmpty(mneCodePOS)){
                roleMneCodePOS.addAll(mneCodePOS);
            }
            RoleMneCodePO roleMneCodePO = new RoleMneCodePO();
            roleMneCodePO.setRoleId(rolePO.getId());
            roleMneCodePO.setMneCode(rolePO.getName());
            roleMneCodePOS.add(roleMneCodePO);
        });
        saveBatch(roleMneCodePOS);
    }

    /**
     * 清空对应菜单的助记码
     */
    public void deleteMneCode(Long roleId){
        remove(new QueryWrapper<RoleMneCodePO>().eq(RoleMneCodeField.roleId,roleId));
    }

    @Override
    public void deleteMneCodeByIds(List<Long> roleIds) {
        remove(new QueryWrapper<RoleMneCodePO>().in(RoleMneCodeField.roleId,roleIds));
    }

}
