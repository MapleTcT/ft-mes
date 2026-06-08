package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.MneCodeGenterate;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.utils.StringUtils;
import com.supcon.supfusion.rbac.dao.MenuInfoMapper;
import com.supcon.supfusion.rbac.dao.MenuInfoMneCodeMapper;
import com.supcon.supfusion.rbac.dao.field.MenuInfoCompanyRefField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.field.MenuInfoMneCodeField;
import com.supcon.supfusion.rbac.dao.po.MenuInfoMneCodePO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.service.IMenuInfoMneCodeService;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.MneClientService;
import com.supcon.supfusion.rbac.service.bo.MneQueryBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.supcon.supfusion.rbac.common.Contants.Constants.SQL_LIKE_CHAR;

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
@Transactional
public class MenuInfoMneCodeServiceImpl extends ServiceImpl<MenuInfoMneCodeMapper, MenuInfoMneCodePO> implements IMenuInfoMneCodeService,MneClientService {

    @Autowired
    private II18nAdapter i18nAdapter;
    @Autowired
    private MenuInfoMapper menuInfoMapper;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private II18nAdapter ii18nAdapter;
    @Value("${supfusion.cloud.datasource.connect.system.db-type:}")
    private String dbType;
    @Value("${rbac.default-language:}")
    private String language;

    @Override
    public void createMenuInfoMneCodeI18NKey(String property,Long menuInfoId) {
        Locale locale = RpcContext.getContext().getLanguage();
        if (!ObjectUtils.isEmpty(locale)){
            language = locale.toString();
        }
        String name = i18nAdapter.getRemoteMessage(property, null,locale);
        if (ObjectUtils.isEmpty(name)){
            return;
        }
        //先清空原有的助记码,重新添加
        deleteMneCode(menuInfoId);
        List<String> strings = MneCodeGenterate.mneCodeTupleGenerate(name);
        List<MenuInfoMneCodePO> menuInfoMneCodePOS = strings.stream().map(s -> {
            MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
            menuInfoMneCodePO.setLanguage(language);
            menuInfoMneCodePO.setMenuInfoId(menuInfoId);
            menuInfoMneCodePO.setMneCode(s);
            return menuInfoMneCodePO;
        }).collect(Collectors.toList());
        MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
        menuInfoMneCodePO.setMenuInfoId(menuInfoId);
        menuInfoMneCodePO.setLanguage(language);
        menuInfoMneCodePO.setMneCode(name);
        menuInfoMneCodePOS.add(menuInfoMneCodePO);
        saveBatch(menuInfoMneCodePOS);
    }

    @Override
    public void createMenuInfoMneCode(String property, Long menuInfoId) {
        if (ObjectUtils.isEmpty(property)){
            return;
        }
        Locale locale = RpcContext.getContext().getLanguage();
        if (!ObjectUtils.isEmpty(locale)){
            language = locale.toString();
        }
        //先清空原有的助记码,重新添加
        deleteMneCode(menuInfoId);
        List<String> strings = MneCodeGenterate.mneCodeTupleGenerate(property);
        List<MenuInfoMneCodePO> menuInfoMneCodePOS = strings.stream().map(s -> {
            MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
            menuInfoMneCodePO.setLanguage(language);
            menuInfoMneCodePO.setMenuInfoId(menuInfoId);
            menuInfoMneCodePO.setMneCode(s);
            return menuInfoMneCodePO;
        }).collect(Collectors.toList());
        MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
        menuInfoMneCodePO.setMenuInfoId(menuInfoId);
        menuInfoMneCodePO.setLanguage(language);
        menuInfoMneCodePO.setMneCode(property);
        menuInfoMneCodePOS.add(menuInfoMneCodePO);
        saveBatch(menuInfoMneCodePOS);
    }

    @Override
    @Transactional
    public void createMenuInfoMneCodeI18NKey(List<MenuInfoPO> menuInfoPOS) {
        if (ObjectUtils.isEmpty(menuInfoPOS)){
            return;
        }
        Locale locale = RpcContext.getContext().getLanguage();
        if (!ObjectUtils.isEmpty(locale)){
            language = locale.toString();
        }
        List<MenuInfoMneCodePO> menuInfoMneCodePOS =new ArrayList<>();
        menuInfoPOS.forEach(menuInfoPO -> {
            Long menuInfoId = menuInfoPO.getId();
            String name = i18nAdapter.getRemoteMessageBlank(menuInfoPO.getName(), null,locale);
            if (ObjectUtils.isEmpty(name) || name.equals(menuInfoPO.getName())) {
                name= menuInfoPO.getNameDisplay();
            }
            //先清空原有的助记码,重新添加
            deleteMneCode(menuInfoId);
            if (ObjectUtils.isEmpty(name)){
                return;
            }
            List<String> strings = MneCodeGenterate.mneCodeTupleGenerate(name);
            List<MenuInfoMneCodePO> mneCodePOS = strings.stream().map(s -> {
                MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
                menuInfoMneCodePO.setLanguage(language);
                menuInfoMneCodePO.setMenuInfoId(menuInfoId);
                menuInfoMneCodePO.setMneCode(s);
                return menuInfoMneCodePO;
            }).collect(Collectors.toList());
            if (!ObjectUtils.isEmpty(mneCodePOS)){
                menuInfoMneCodePOS.addAll(mneCodePOS);
            }
            MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
            menuInfoMneCodePO.setMenuInfoId(menuInfoId);
            menuInfoMneCodePO.setLanguage(language);
            menuInfoMneCodePO.setMneCode(name);
            menuInfoMneCodePOS.add(menuInfoMneCodePO);
        });
        saveBatch(menuInfoMneCodePOS);
    }

    /**
     * 清空对应菜单的助记码
     */
    private void deleteMneCode(Long menuInfoId){
        remove(new QueryWrapper<MenuInfoMneCodePO>().eq(MenuInfoMneCodeField.menuInfoId,menuInfoId));
    }

    @Override
    public String getHandleType() {
        return null;
    }

    @Override
    public List<Map<String, Object>> search(MneQueryBO mneQueryBO, String condition) {
        QueryWrapper<MenuInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mne." + MenuInfoMneCodeField.language,LocaleContextHolder.getLocale().toString());
        if (Constants.DB_TYPE_ORACLE.equals(dbType)){
            queryWrapper.apply("mne."+ MenuInfoMneCodeField.mneCode + " like {0} escape '\\'", SQL_LIKE_CHAR + StringUtils.getStringDb(mneQueryBO.getSearchContent().toLowerCase(),dbType) + SQL_LIKE_CHAR);
        }else{
            queryWrapper.like("mne." + MenuInfoMneCodeField.mneCode,StringUtils.getStringDb(mneQueryBO.getSearchContent().toLowerCase(),dbType));
        }
        queryWrapper.eq("mi." + MenuInfoField.valid,1);
        queryWrapper.and(queryWrapper1 -> queryWrapper1.eq("mcr." + MenuInfoCompanyRefField.companyId, UserContext.getUserContext().getCompanyId()).or().eq("mcr." + MenuInfoCompanyRefField.companyId, -1L));

        if (Constants.DB_TYPE_ORACLE.equals(dbType)){
            queryWrapper.or().apply("mi."+ MenuInfoField.code + " like {0} escape '\\'", SQL_LIKE_CHAR + mneQueryBO.getSearchContent() + SQL_LIKE_CHAR);
        }else{
            queryWrapper.or().like("mi."+ MenuInfoField.code,mneQueryBO.getSearchContent());
        }
        List<Map<String, Object>> maps = menuInfoMapper.search(queryWrapper);
        maps.forEach(map ->{
            String name = ii18nAdapter.getRemoteMessage((String) map.get("name"), null, LocaleContextHolder.getLocale());
            map.put("nameDisplay",name);
        });
        return maps;
    }


}
