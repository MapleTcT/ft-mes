package com.supcon.supfusion.i18n.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nLanguageDao;
import com.supcon.supfusion.i18n.dao.I18nVersionDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nVersionPO;
import com.supcon.supfusion.i18n.dao.vo.I18nLanguageVO;
import com.supcon.supfusion.i18n.service.I18nInterApiService;
import com.supcon.supfusion.i18n.service.I18nResourceService;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Service
public class I18nInterApiServiceImpl implements I18nInterApiService {

    @Autowired
    private I18nResourceService i18nResourceService;
    @Autowired
    private I18nVersionDao i18nVersionDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private I18nLanguageDao i18nLanguageDao;

    @Override
    public PageResult<I18nLanguageVO> getAllLanguage() {
    	String tenantId = TenantUtil.getTenantId();
    	List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        List<I18nLanguageVO> i18nLanguageVOS = new ArrayList<>();
        for (I18nLanguagePO i18nLanguage : allLanguage) {
            I18nLanguageVO i18nLanguageVO = new I18nLanguageVO();
            i18nLanguageVO.setId(i18nLanguage.getId());
            i18nLanguageVO.setLanguCode(i18nLanguage.getLanguCode());
            i18nLanguageVO.setLanguType(i18nLanguage.getLanguType());
            i18nLanguageVO.setLanguName(i18nLanguage.getLanguName());
            i18nLanguageVO.setValid(i18nLanguage.getValid());
            i18nLanguageVO.setUsed(Constants.ONE_STR.equals(i18nLanguage.getHasUsed()));
            i18nLanguageVOS.add(i18nLanguageVO);
        }
        return new PageResult<>(i18nLanguageVOS, Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
    }
    
    /**
     * 启用或者停用语言
     * <p>
     * 	系统初始化只有默认租户的语言, 如果租户用户设置了启用或停用, 那么会给租户设置一套语言副本,系统默认的语言属于全局共享不会更改
     * 	租户修改的语言都是租户自己的数据.
     * </p>
     */
    @Override
    @Transactional
    public void updateI18nLanguage(List<Map<String, Object>> params) {
        // 至少启用一种语言校验
    	long unusedCount = params.stream().filter(map -> !Boolean.parseBoolean(map.get(Constants.USED).toString())).count();
    	if (unusedCount == params.size()) {
    		throw new I18nException(I18nErrorEnum.LANGUAGE_HAS_USED_ERROR);
    	}
    	String tenantId = TenantUtil.getTenantId();
    	// 判断租户语言副本是否存在
    	boolean tenantLanguageExist = true;
    	LambdaQueryWrapper<I18nLanguagePO> langQueryWrapper = new QueryWrapper<I18nLanguagePO>().lambda().eq(I18nLanguagePO::getTenantId, tenantId);
    	if (!Constants.DEFAULT_TENANT.equals(tenantId)) {
    		int tenantCount = i18nLanguageDao.selectCount(langQueryWrapper);
    		tenantLanguageExist = tenantCount > 0;
    	}
    	// 如果租户语言副本不存在则创建副本
    	if (!tenantLanguageExist) {
    		langQueryWrapper = new QueryWrapper<I18nLanguagePO>().lambda().eq(I18nLanguagePO::getTenantId, Constants.DEFAULT_TENANT);
    		List<I18nLanguagePO> systemLanguages = i18nLanguageDao.selectList(langQueryWrapper);
    		for (I18nLanguagePO systemLanguage : systemLanguages) {
    			I18nLanguagePO tenantLanguagePO = buildNewI18nLanguagePO(systemLanguage);
    			i18nLanguageDao.insert(tenantLanguagePO);
    		}
    	}
    	// 更新租户或者系统语言
        for (Map<String, Object> param : params) {
        	langQueryWrapper.eq(I18nLanguagePO::getLanguCode, param.get(Constants.LANGU_CODE));
        	I18nLanguagePO language = new I18nLanguagePO();
        	String used = param.get(Constants.USED).toString();
        	if (Constants.TRUE.equals(used)) {
        		language.setHasUsed(Constants.ONE_STR);
        	} else {
        		language.setHasUsed(Constants.ZERO_STR);
        	}
        	i18nLanguageDao.update(language, new QueryWrapper<I18nLanguagePO>().lambda()
        			.eq(I18nLanguagePO::getTenantId, tenantId)
        			.eq(I18nLanguagePO::getLanguCode, param.get(Constants.LANGU_CODE)));
        }
    }

    private I18nLanguagePO buildNewI18nLanguagePO(I18nLanguagePO systemLanguage) {
    	String tenantId = TenantUtil.getTenantId();
    	I18nLanguagePO language = new I18nLanguagePO();
    	language.setId(IDGenerator.newInstance().generate().longValue());
    	language.setHasUsed(Constants.ONE_STR);
    	language.setLanguCode(systemLanguage.getLanguCode());
    	language.setLanguName(systemLanguage.getLanguName());
    	language.setLanguType(systemLanguage.getLanguType());
    	language.setTenantId(tenantId);
    	return language;
    }
    
    @Override
    public Result getI18nModuleVersionCode(List<String> list) {
        Result result = new Result<>();
        Map map = new HashMap();
        for (String moduleCode : list) {
            List<I18nVersionPO> i18nVersionPOs = i18nVersionDao.selectAllVersionsByModuleCode(moduleCode);
            List listVersionCode = new ArrayList();
            if (i18nVersionPOs != null && i18nVersionPOs.size() > 0) {
                for (I18nVersionPO i18nVersionPO : i18nVersionPOs) {
                    listVersionCode.add(i18nVersionPO.getModuleVersionCode());
                }
                map.put(moduleCode, listVersionCode);
            }
        }
        result.setData(map);
        return result;
    }

    @Override
    @Transactional
    public Result postI18nModuleVersionCode(Map map) {
        Result result = new Result();
        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String moduleVersion = (String) map.get(Constants.MODULE_VERSION_CODE);
        //校验版本号的新旧
        String dateNum = moduleVersion.substring(moduleVersion.length() - 12, moduleVersion.length());
        Long num = Long.valueOf(dateNum);
        //当前版本号低于该模块最新版本号不能新增
        LambdaQueryWrapper<I18nVersionPO> queryWrapper = new QueryWrapper<I18nVersionPO>().lambda()
        		.eq(I18nVersionPO::getModuleCode, moduleCode);
        I18nVersionPO i18nVersionPO = i18nVersionDao.selectOne(queryWrapper);
        if (i18nVersionPO != null) {
            String dateNumDB = i18nVersionPO.getModuleVersionCode().substring(i18nVersionPO.getModuleVersionCode().length() - 12, i18nVersionPO.getModuleVersionCode().length());
            Long numDB = Long.valueOf(dateNumDB);
            if (!(numDB < num)) {
                throw new I18nException(I18nErrorEnum.RESOURCE_ERROR);
            }
        }
        //新增当前的版本号
        I18nVersionPO i18nVersionPO2 = new I18nVersionPO();
        i18nVersionPO2.setId(IDGenerator.newInstance().generate().longValue());
        i18nVersionPO2.setModuleCode(moduleCode);
        i18nVersionPO2.setModuleVersionCode(moduleVersion);
        i18nVersionPO2.setValid(Constants.ONE_STR);
        i18nVersionDao.add(i18nVersionPO2);
        return new Result();
    }

    @Override
    public Result getI18nModuleIndexCode(String moduleCode) {
    	String tenantId = TenantUtil.getTenantId();
        Result result = new Result();
        Map map = new HashMap();
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
        		.eq(I18nIndexPO::getModuleCode, moduleCode)
        		.eq(I18nIndexPO::getTenantId, tenantId);
        I18nIndexPO i18nIndexPO = i18nIndexDao.selectOne(queryWrapper);
        if (i18nIndexPO != null) {
            map.put(moduleCode, i18nIndexPO.getModuleIndexCode());
        }
        result.setData(map);
        return result;
    }

    @Override
    public Result postI18nModuleIndexCode(String moduleCode) {
        //模块新增索引
        String moduleIndexCode = moduleCode + UUID.randomUUID();
        //删除之前所有的该模块的版本号
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setModuleCode(moduleCode);
        i18nIndexPO.setModuleIndexCode(moduleIndexCode);
        i18nIndexPO.setValid(Constants.ONE_STR);
        i18nIndexDao.updateByModuleIndexCode(i18nIndexPO);
        return new Result();
    }

}
