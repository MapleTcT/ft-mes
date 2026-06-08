package com.supcon.supfusion.rbac.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.rbac.api.IMenuOperateApiService;
import com.supcon.supfusion.rbac.api.dto.MenuOperateDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateGroupRestrictDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateSimpleDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateUpdateDTO;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.service.IMenuOperateService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class MenuOperateApiServiceImpl implements IMenuOperateApiService {

    @Autowired
    private IMenuOperateService menuOperateService;

    @Override
    public List<MenuOperateDTO> findMenuOperateByEntityCode(String entityCode, Integer powerFlag) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<MenuOperatePO>().eq("ENTITY_CODE", entityCode);
        if (!ObjectUtils.isEmpty(powerFlag)){
            queryWrapper.eq("POWER_FLAG", powerFlag);
        }
        List<MenuOperatePO> menuOperatePOS = menuOperateService.list(queryWrapper);
        if (!ObjectUtils.isEmpty(menuOperatePOS)){
            return menuOperatePOS.stream().map(menuOperatePO -> {
                MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
                BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
                return menuOperateDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<MenuOperateDTO> findMenuOperateByCodeAndCid(String code, List<Long> cids) {
        List<MenuOperatePO> menuOperatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq("CODE", code).in("CID",cids));
        if (!ObjectUtils.isEmpty(menuOperatePOS)){
            return menuOperatePOS.stream().map(menuOperatePO -> {
                MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
                BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
                return menuOperateDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        menuOperateService.removeById(id);
    }

    @Override
    public void deleteByCode(String code) {
        menuOperateService.remove(new QueryWrapper<MenuOperatePO>().eq("CODE",code));
    }

    @Override
    public void deleteByCodePhysics(List<String> codes) {
        if (!ObjectUtils.isEmpty(codes)){
            menuOperateService.deleteByCodePhysics(codes);
        }
    }

    @Override
    public void changeOperateGroupRestrict(MenuOperateUpdateDTO menuOperateUpdateDTO) {
        UpdateWrapper<MenuOperatePO> updateWrapper = new UpdateWrapper<>();
        if (!ObjectUtils.isEmpty(menuOperateUpdateDTO.getCodes())){
            updateWrapper.in("CODE",menuOperateUpdateDTO.getCodes()).set("ENABLE_GROUPRESTRICT",menuOperateUpdateDTO.getEnableGrouprestrict());
        }
        menuOperateService.update(updateWrapper);
    }

    @Override
    public List<MenuOperateDTO> findMenuOperateByMenuInfo(String code, Long id) {

        List<MenuOperatePO> menuOperatePOS = menuOperateService.findMenuOperateByMenuInfo(code, id);
        if (!ObjectUtils.isEmpty(menuOperatePOS)){
            return menuOperatePOS.stream().map(menuOperatePO -> {
                MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
                BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
                return menuOperateDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<MenuOperateDTO> findMenuOperateByEntityCodeAndNotId(String entityCode, Long id) {

        List<MenuOperatePO> menuOperatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq("ENTITY_CODE",entityCode).notIn("ID",id).eq("POWER_FLAG",1));
        if (!ObjectUtils.isEmpty(menuOperatePOS)){
            return menuOperatePOS.stream().map(menuOperatePO -> {
                MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
                BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
                return menuOperateDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void save(List<MenuOperateDTO> menuOperateDTOS) {
        if (ObjectUtils.isEmpty(menuOperateDTOS)){
            return;
        }
        List<MenuOperatePO> list = menuOperateService.list(new QueryWrapper<MenuOperatePO>().in(MenuOperateField.code, menuOperateDTOS.stream().map(MenuOperateDTO::getCode).collect(Collectors.toList())));
        Map<String,Long> map = new HashMap<>();
        list.forEach(menuOperatePO -> {
            map.put(menuOperatePO.getCode(),menuOperatePO.getId());
        });
        menuOperateService.saveOrUpdateBatch(menuOperateDTOS.stream().map(menuOperateDTO -> {
            MenuOperatePO menuOperatePO = new MenuOperatePO();
            BeanUtils.copyProperties(menuOperateDTO,menuOperatePO);
            if (!ObjectUtils.isEmpty(map.get(menuOperatePO.getCode()))){
                menuOperatePO.setId(map.get(menuOperatePO.getCode()));
            }
            return menuOperatePO;
        }).collect(Collectors.toList()));
    }

    @Override
    public MenuOperateDTO findMenuOperateById(Long id) {

        MenuOperatePO menuOperatePO = menuOperateService.getById(id);
        MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
        if (!ObjectUtils.isEmpty(menuOperatePO)){
            BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
            return menuOperateDTO;
        }
        return null;
    }

    @Override
    public List<MenuOperateDTO> findMenuOperateByMenuCodeAndCids(String code, List<Long> cids, Long menuInfoId) {

        List<MenuOperatePO> menuOperatePOS = menuOperateService.findMenuOperateByMenuCodeAndCids(code, cids,menuInfoId);
        if (!ObjectUtils.isEmpty(menuOperatePOS)){
            return menuOperatePOS.stream().map(menuOperatePO -> {
                MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
                BeanUtils.copyProperties(menuOperatePO,menuOperateDTO);
                return menuOperateDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void deleteByMenuInfoIds(List<Long> ids) {
        if (!ObjectUtils.isEmpty(ids)){
            menuOperateService.remove(new QueryWrapper<MenuOperatePO>().in("MENUINFO_ID",ids));
        }
    }

    @Override
    public void updateOperateGroupRestrictByEntityCodeAndOther(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO) {
        UpdateWrapper<MenuOperatePO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ENTITY_CODE",menuOperateGroupRestrictDTO.getEntityCode());
        updateWrapper.and(menuOperatePOUpdateWrapper -> {
           menuOperatePOUpdateWrapper.eq("MENUOPERATETYPE","FLOWOPERATE");
           menuOperatePOUpdateWrapper.or().eq("MENUOPERATETYPE","ACTIVEOPERATE");
           menuOperatePOUpdateWrapper.or().like("CODE",menuOperateGroupRestrictDTO.getEntityCode() + "%_self");
           menuOperatePOUpdateWrapper.or(wrapper -> {
               wrapper.eq("TARGET","SELF");
               wrapper.isNull("ACTION_URL");
           });
            menuOperatePOUpdateWrapper.notLike("CODE","StartEvent%");
            menuOperatePOUpdateWrapper.notLike("CODE","start%");
        });
        updateWrapper.set("ENABLE_GROUPRESTRICT",menuOperateGroupRestrictDTO.getEnableGrouprestrict());
        menuOperateService.update(null,updateWrapper);
    }

    @Override
    public void updateOperateGroupRestrictByEntityCode(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO) {
        UpdateWrapper<MenuOperatePO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ENTITY_CODE",menuOperateGroupRestrictDTO.getEntityCode());
        updateWrapper.set("ENABLE_GROUPRESTRICT",menuOperateGroupRestrictDTO.getEnableGrouprestrict());
        menuOperateService.update(null,updateWrapper);
    }

    @Override
    public List<MenuOperateSimpleDTO> getOperateListByUserIdMenuId(Long userId, String menuCode) {
        List<MenuOperatePO> menuOperatePOList = menuOperateService.getOperateListByUserIdMenuId(userId, menuCode);
        if (!ObjectUtils.isEmpty(menuOperatePOList)) {
            return menuOperatePOList.stream().map(menuOperatePO -> {
                MenuOperateSimpleDTO menuOperateSimpleDTO = new MenuOperateSimpleDTO();
                BeanUtils.copyProperties(menuOperatePO, menuOperateSimpleDTO);
                return menuOperateSimpleDTO;
            }).collect(Collectors.toList());
        }
        return null;
    }
}
