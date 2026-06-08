package com.supcon.supfusion.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.portal.common.constant.PortalConstants;
import com.supcon.supfusion.portal.common.exception.PortalErrorEnum;
import com.supcon.supfusion.portal.common.exception.PortalException;
import com.supcon.supfusion.portal.dao.mapper.EcPortletMapper;
import com.supcon.supfusion.portal.dao.mapper.RuntimeModuleMapper;
import com.supcon.supfusion.portal.dao.po.EcPortletPO;
import com.supcon.supfusion.portal.dao.po.RuntimeModulePO;
import com.supcon.supfusion.portal.manager.PortalAdapter;
import com.supcon.supfusion.portal.service.MenuService;
import com.supcon.supfusion.portal.service.PortletCodeService;
import com.supcon.supfusion.portal.service.bo.EcPortletBO;
import com.supcon.supfusion.portal.service.entity.MenuInfo;
import com.supcon.supfusion.portal.service.entity.Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortletCodeServiceImpl implements PortletCodeService {

    public static final int I18N_LANGUAGE_LENGTH = 2;
    @Autowired
    private MenuService menuService;
    @Autowired
    private EcPortletMapper ecPortletMapper;
    @Autowired
    private RuntimeModuleMapper runtimeModuleMapper;
    @Autowired
    private PortalAdapter i18nAdapter;

    /**
     * @return com.supcon.supfusion.framework.cloud.common.result.PageResult<com.supcon.supfusion.portal.webapi.vo.portlet.EcPortletVO>
     * @Author kk.C
     * @Description 获取门户编码组
     * @Date 2020/10/22 16:19
     * @Param [moduleCode, category, current, pageSize]
     **/
    @Override
    public PageResult<EcPortletBO> queryCodes(String moduleCode, String code, String category, Integer current, Integer pageSize) {
        LambdaQueryWrapper<EcPortletPO> ecPortletPOLambdaQueryWrapper = Wrappers.lambdaQuery(EcPortletPO.class);
        LambdaQueryWrapper<RuntimeModulePO> runtimeModulePOLambdaQueryWrapper = Wrappers.lambdaQuery(RuntimeModulePO.class);
        List<Module> moduleList = new ArrayList<>();
        //当分类名不为空时，查出所有该分类下的module,并收集moduleCode，以便注入最后的VO类
        if (!StringUtils.isEmpty(category)) {
            runtimeModulePOLambdaQueryWrapper.eq(RuntimeModulePO::getCategory, category).eq(RuntimeModulePO::getValid, 1);
            List<RuntimeModulePO> runtimeModulePOList = runtimeModuleMapper.selectList(runtimeModulePOLambdaQueryWrapper);
            if (ObjectUtils.isEmpty(runtimeModulePOList)) {
                return new PageResult<>(new ArrayList<>(), runtimeModulePOList.size(), runtimeModulePOList.size(), runtimeModulePOList.size());
            }
            moduleList = runtimeModulePOList.stream().map(runtimeModulePO -> {
                Module module = new Module();
                BeanUtils.copyProperties(runtimeModulePO, module);
                module.setNameInternational(i18nAdapter.getRemoteMessage(runtimeModulePO.getName()));
                return module;
            }).collect(Collectors.toList());
        }
        //当moduleCode不为空时，只需要查出一个module
        else if (!StringUtils.isEmpty(moduleCode) && !"".equals(moduleCode)) {
            runtimeModulePOLambdaQueryWrapper.eq(RuntimeModulePO::getCode, moduleCode).eq(RuntimeModulePO::getValid, 1);
            RuntimeModulePO runtimeModulePO = runtimeModuleMapper.selectOne(runtimeModulePOLambdaQueryWrapper);
            Module module = new Module();
            BeanUtils.copyProperties(runtimeModulePO, module);
            module.setNameInternational(i18nAdapter.getRemoteMessage(runtimeModulePO.getName()));
            moduleList.add(module);
        }
        Map<String, Module> moduleMap = moduleList.stream().collect(Collectors.toMap(Module::getCode, Function.identity()));

        //根据moduleCode集合，查出所有符合条件的编码组，并收集所有的菜单ID
        if (moduleMap != null && moduleMap.size() > 0) {
            ecPortletPOLambdaQueryWrapper.in(EcPortletPO::getModuleCode, moduleMap.keySet());
        }
        //TODO 加入根据code搜索
        if (!StringUtils.isEmpty(code)) {
            ecPortletPOLambdaQueryWrapper.like(EcPortletPO::getCode, code);
        }
        Page<EcPortletPO> page = new Page<>();
        page.setCurrent(current).setPages(pageSize);
        Page<EcPortletPO> ecPortletPOPage = ecPortletMapper.selectPage(page, ecPortletPOLambdaQueryWrapper);
        if (ObjectUtils.isEmpty(ecPortletPOPage)) {
            return new PageResult<>(new ArrayList<>(), ecPortletPOPage.getTotal(), ecPortletPOPage.getSize(), ecPortletPOPage.getCurrent());
        }
        List<EcPortletBO> ecPortletBOList = ecPortletPOPage.getRecords().stream().map(ecPortletPO -> {
            EcPortletBO ecPortletBO = new EcPortletBO();
            BeanUtils.copyProperties(ecPortletPO, ecPortletBO);
            return ecPortletBO;
        }).collect(Collectors.toList());
        Set<String> menuCodes = new HashSet<>();
        ecPortletBOList.forEach(ecPortletBO -> {
            if (!StringUtils.isEmpty(ecPortletBO.getMenuCode())) {
                menuCodes.add(ecPortletBO.getMenuCode());
            }
        });
        //查出对应的菜单实体类
        List<MenuInfo> menuInfoList = menuService.getMenuInfoBySet(menuCodes);
        Map<String, MenuInfo> menuInfoMap = new HashMap<>();
        if (!ObjectUtils.isEmpty(menuInfoList)) {
            menuInfoList.forEach(menuInfo -> {
                String nameInternational = i18nAdapter.getRemoteMessage(menuInfo.getName());
                menuInfo.setName(nameInternational == null ? menuInfo.getName() : nameInternational);
            });
            menuInfoMap = menuInfoList.stream().collect(Collectors.toMap(MenuInfo::getCode, Function.identity()));
        }
        for (EcPortletBO ecPortletBO : ecPortletBOList) {
            ecPortletBO.setModule(moduleMap.get(ecPortletBO.getModuleCode()));
            ecPortletBO.setMenuInfo(menuInfoMap.get(ecPortletBO.getMenuCode()));
            ecPortletBO.setTitle(i18nAdapter.getRemoteMessage(ecPortletBO.getTitle()));
        }
        return new PageResult<>(ecPortletBOList, ecPortletPOPage.getTotal(), ecPortletPOPage.getSize(), ecPortletPOPage.getCurrent());
    }

    /**
     * @return void
     * @Author kk.C
     * @Description 新增编码
     * @Date 2020/10/23 14:11
     * @Param [ecPortletBO]
     **/
    @Override
    @Transactional
    public void addCode(EcPortletBO ecPortletBO) {
        if (Optional.ofNullable(ecPortletMapper.selectById(ecPortletBO.getCode())).isPresent()) {
            throw new PortalException(PortalErrorEnum.CODE_FOUND_REPEAT);
        }
        EcPortletPO ecPortletPO = new EcPortletPO();
        BeanUtils.copyProperties(addOrUpdateI18nMessage(ecPortletBO), ecPortletPO);
        ecPortletMapper.insert(ecPortletPO);
    }

    /**
     * @param ecPortletBO
     * @return void
     * @Author kk.C
     * @Description 删除编码
     * @Date 2020/10/23 15:10
     * @Param [code]
     */
    @Override
    public void deleteCode(EcPortletBO ecPortletBO) {
        ecPortletMapper.deleteById(ecPortletBO.getCode());
        i18nAdapter.messageResourceDeleteKeys(new String[]{ecPortletBO.getTitleKey()});
    }

    /**
     * @return void
     * @Author kk.C
     * @Description 修改编码
     * @Date 2020/10/23 15:14
     * @Param [ecPortletBO]
     **/
    @Override
    @Transactional
    public void updateCode(EcPortletBO ecPortletBO) {
        if (Optional.ofNullable(ecPortletMapper.selectById(ecPortletBO.getCode())).isPresent()) {
            throw new PortalException(PortalErrorEnum.CODE_NOT_FOUND);
        }
        EcPortletPO ecPortletPO = new EcPortletPO();
        BeanUtils.copyProperties(addOrUpdateI18nMessage(ecPortletBO), ecPortletPO);
        ecPortletMapper.updateById(ecPortletPO);
    }

    /**
     * @return void
     * @Author kk.C
     * @Description 写入或更新国际化信息
     * @Date 2020/10/26 9:25
     * @Param [ecPortletBO]
     **/
    private EcPortletBO addOrUpdateI18nMessage(EcPortletBO ecPortletBO) {
        Map<String, Object> i18nMap = new HashMap<>();
        Map<String, String> languageMap = new HashMap<>();
        //        I18nParam i18nParam = new I18nParam();
        //        i18nParam.setModuleCode(ServiceConstant.SERVICE_NAME);
        //        String language = StringUtils.isEmpty(ecPortletBO.getLanguage()) ? I18nConstant.ZH_CN : ecPortletBO.getLanguage();
        //        switch (language) {
        //            case I18nConstant.ZH_HK:
        //                i18nParam.setZh_HK(ecPortletBO.getTitleInternational());
        //                break;
        //            case I18nConstant.EN_US:
        //                i18nParam.setEn_US(ecPortletBO.getTitleInternational());
        //                break;
        //            default:
        //                i18nParam.setEn_US(ecPortletBO.getTitleInternational());
        //                i18nParam.setZh_HK(ecPortletBO.getTitleInternational());
        //                i18nParam.setZh_CN(ecPortletBO.getTitleInternational());
        //        }
        String key = i18nAdapter.initI18nKey(ecPortletBO.getModuleCode());
        i18nMap.put("i18n_key", key);
        i18nMap.put("moduleCode", PortalConstants.SERVICE_NAME);
        String titleInternational = ecPortletBO.getTitle();
        if (!StringUtils.isEmpty(titleInternational)) {
            if (titleInternational.contains("$&#")) {
                String[] languages = titleInternational.split("[$]&#");
                for (String title : languages) {
                    String[] language = title.split("=");
                    if (language.length == I18N_LANGUAGE_LENGTH) {
                        languageMap.put(language[0], language[1]);
                    }
                }
            } else {
                String[] language = titleInternational.split("=");
                if (language.length == I18N_LANGUAGE_LENGTH) {
                    languageMap.put(language[0], language[1]);
                }
            }
        }
        i18nMap.put("i18n_value", languageMap);
        Result result = i18nAdapter.messageResourceAddOrUpdateOne(i18nMap);
        if (!"SUCCESS!".equals(result.getMessage()) || languageMap.size() == 0) {
            throw new PortalException(PortalErrorEnum.ADD_I18N_ERROR);
        }
        ecPortletBO.setTitleKey(key);
        ecPortletBO.setTitle(key);
        return ecPortletBO;
    }

}
