package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.common.i18n.InternationalResource;
import com.supcon.supfusion.custon.property.dao.entity.Module;
import com.supcon.supfusion.custon.property.dao.entity.ModuleRelation;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleMapper;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleReferenceMapper;
import com.supcon.supfusion.custon.property.dao.mappers.ModuleRelationMapper;
import com.supcon.supfusion.custon.property.server.SystemCodeService;
import com.supcon.supfusion.custon.property.server.bo.GroupSystemEntityBO;
import com.supcon.supfusion.custon.property.server.bo.SystemEntityBO;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.systemcode.api.SystemEntityApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhang yafei
 */
@Service
public class SystemCodeServiceImpl implements SystemCodeService {

    @Autowired
    private ModuleRelationMapper moduleRelationMapper;

    @Autowired
    private ModuleReferenceMapper moduleReferenceMapper;

    @Autowired
    private ModuleMapper moduleMapper;

    @Autowired
    private SystemEntityApiService systemEntityApiService;

    @Autowired
    private InternationalResource internationalResource;

    @Override
    public  List<GroupSystemEntityBO> getSystemEntityMapByGroup(String moduleCode) {
        Locale locale = LocaleContextHolder.getLocale();
        List<String> moduleCodes = new ArrayList<>();
        moduleCodes.add(moduleCode);
        moduleCodes.add("BaseSet_1.0.0");
        List<ModuleRelation> moduleRelations = moduleRelationMapper.selectList(new LambdaQueryWrapper<ModuleRelation>().eq(ModuleRelation::getModuleCode, moduleCode));
        moduleRelations.forEach(moduleRelation -> {
            moduleCodes.add(moduleRelation.getTargetModuleCode());
        });
        List<Module> modules = moduleReferenceMapper.selectTargetModuleByModuleCode(moduleCode);
        modules.forEach(module ->{
            moduleCodes.add(module.getCode());
        });
        //去重
        List<String> codes = moduleCodes.stream().distinct().collect(Collectors.toList());
        ArrayList<String> strings = new ArrayList<>();
        for (String code : codes) {
            strings.add(dropVersionNumber(code));
        }
        List<GroupSystemEntityBO> groupSystemEntityBOS = new ArrayList<>();
        //获取关联模块的系统编码
        ListResult<SystemEntityResultDTO> listResult = systemEntityApiService.getEntityByModuleIds(strings);
        Collection<SystemEntityResultDTO> list = listResult.getList();
        codes.forEach(code -> {
            Module module = moduleMapper.selectById(code);
            if (module == null){
                return;
            }
            List<SystemEntityBO> systemEntityBOs = new ArrayList<>();
            list.forEach(systemEntityResultDTO -> {
                if (dropVersionNumber(module.getCode()).equals(systemEntityResultDTO.getModuleId())){
                    SystemEntityBO systemEntityBO = getSystemEntityBO(locale, systemEntityResultDTO);
                    systemEntityBOs.add(systemEntityBO);
                }

            });
            if (systemEntityBOs.size() > 0){
                GroupSystemEntityBO groupSystemEntityBO = new GroupSystemEntityBO();
                groupSystemEntityBO.setName(internationalResource.getI18nValue(module.getName(),locale));
                groupSystemEntityBO.setList(systemEntityBOs);
                groupSystemEntityBOS.add(groupSystemEntityBO);
            }
                });
        //获取系统基础模块的编码
        ListResult<SystemEntityResultDTO> baseSystemCode = systemEntityApiService.getSystemBaseEntity();
        List<SystemEntityBO> systemEntityBOs = new ArrayList<>();
        baseSystemCode.getList().forEach(systemEntityResultDTO -> {
            SystemEntityBO systemEntityBO = getSystemEntityBO(locale, systemEntityResultDTO);
            systemEntityBOs.add(systemEntityBO);
        });
        GroupSystemEntityBO groupSystemEntityBO = new GroupSystemEntityBO();
        groupSystemEntityBO.setName(internationalResource.getI18nValue("ec.module.systemcode.other",locale));
        groupSystemEntityBO.setList(systemEntityBOs);
        groupSystemEntityBOS.add(groupSystemEntityBO);

        return groupSystemEntityBOS;
    }

    //数据类型转换
    private SystemEntityBO getSystemEntityBO(Locale locale, SystemEntityResultDTO systemEntityResultDTO) {
        SystemEntityBO systemEntityBO = new SystemEntityBO();
        systemEntityBO.setType(systemEntityResultDTO.getType());
        systemEntityBO.setName(internationalResource.getI18nValue(systemEntityResultDTO.getName(), locale));
        systemEntityBO.setId(systemEntityResultDTO.getId().toString());
        systemEntityBO.setCode(systemEntityResultDTO.getCode());
        systemEntityBO.setCompanyType(systemEntityResultDTO.getCompanyName());
        return systemEntityBO;
    }

    private String dropVersionNumber(String code){
        String[] s = code.split("_");
        return s[0];
    }
}
