package com.supcon.supfusion.systemcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.SystemEntityMapper;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.manager.I18nAdapter;
import com.supcon.supfusion.systemcode.service.ModuleService;
import com.supcon.supfusion.systemcode.service.SystemCodeService;
import com.supcon.supfusion.systemcode.service.SystemEntityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.supcon.supfusion.systemcode.common.constants.Constants.SQL_LIKE_CHAR;

/**
 * @author 
 * @date 20-5-11 下午14:30
 */
@Slf4j
@Service
public class SystemEntityServiceImpl extends ServiceImpl<SystemEntityMapper, SystemEntityPO> implements SystemEntityService {

    @Autowired
    SystemCodeService systemCodeService;

    @Autowired
    PersonApiService personApiService;

    @Autowired
    ModuleService moduleService;

    @Autowired
    I18nAdapter i18nAdapterService;

    @Autowired
    DbStringUtil dbStringUtil;

    @Value("${supfusion.cloud.datasource.connect.system.db-type:}")
    String dbType;

    @Override
    public PageResult<SystemEntityPO> queryEntities(String keyword, String moduleId, int current, int pageSize) {
        QueryWrapper<SystemEntityPO> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNoneBlank(keyword) && StringUtils.isEmpty(moduleId)) {
            Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(keyword);
            queryWrapper.and(query -> {
                if (Constants.DB_TYPE_ORACLE.equals(dbType)){
                    query.apply(SystemEntityPO.getCodeFieldName() + " like {0} escape '\\'", SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + SQL_LIKE_CHAR);
                }else{
                    query.like(SystemEntityPO.getCodeFieldName(), keyword);
                }
                query.like("code", keyword);
                if (!ObjectUtils.isEmpty(map)) {
                    Set<String> strings = map.keySet();
                    List<String> name = new ArrayList<>(strings);
                    int batch = name.size() / 1000;
                    if (batch == 0){
                        query.or().in("name", name);
                    }else{
                        for (int i = 0; i < batch; i++) {
                            query.or().in("name",name.subList(i * 1000, i * 1000 + 1000));
                        }
                        if (name.size() % 1000 != 0) {
                            query.or().in(SystemCodePO.getNameFieldName(), name.subList(batch * 1000, name.size()));
                        }
                    }
                } else {
                    query.or().eq(SystemCodePO.getNameFieldName(), "");
                }
            });
        }
        if (StringUtils.isEmpty(keyword) && StringUtils.isNoneBlank(moduleId)) {
            queryWrapper = new QueryWrapper<SystemEntityPO>().eq(SystemEntityPO.getModuleIdFieldName(), moduleId);
        }
        // 调用组织架构接口获取公司信息
//        Long companyId = UserContext.getUserContext().getCompanyId();
//        if (null == companyId || "".equals(companyId)) {
//            throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//        }
//        queryWrapper.eq(SystemEntityPO.getCidFieldName(), companyId);
        queryWrapper.eq(SystemEntityPO.getValidFieldName(), 1);
        if (StringUtils.isEmpty(moduleId)){
            queryWrapper.isNull(SystemEntityPO.getSourceName());
        }

        Integer count = count(queryWrapper);
        queryWrapper.orderByDesc(SystemEntityPO.getCreateTimeFieldName());
        Page<SystemEntityPO> pageInfo = new Page<>(current, pageSize, count);
        page(pageInfo, queryWrapper);
        List<SystemEntityPO> systemEntityList = pageInfo.getRecords();
        for (SystemEntityPO systemEntityPO : systemEntityList) {
            systemEntityPO.setCompanyName(queryCompanyName(systemEntityPO.getCid()));
            systemEntityPO.setModuleName(queryModuleById(systemEntityPO.getModuleId()));
            setEntityI18n(systemEntityPO);
        }

        PageResult<SystemEntityPO> systemEntityPageResult = new PageResult<>(systemEntityList, pageInfo.getTotal(), pageInfo.getSize(), pageInfo.getCurrent());
        return systemEntityPageResult;
    }

    @Override
    public SystemEntityPO queryEntityByCode(String code) {
        QueryWrapper queryWrapper = new QueryWrapper<SystemEntityPO>();
        queryWrapper.eq(SystemEntityPO.getCodeFieldName(), code);
        queryWrapper.eq(SystemEntityPO.getValidFieldName(), 1);
        SystemEntityPO systemEntityPO = getOne(queryWrapper);
        if (Objects.nonNull(systemEntityPO)) {
            systemEntityPO.setCompanyName(queryCompanyName(systemEntityPO.getCid()));
            systemEntityPO.setModuleName(queryModuleById(systemEntityPO.getModuleId()));
            setEntityI18n(systemEntityPO);
        }
        return systemEntityPO;
    }

    @Override
    public boolean validateEntityExist(String code) {
        QueryWrapper queryWrapper = new QueryWrapper<SystemEntityPO>();
        queryWrapper.eq(SystemEntityPO.getCodeFieldName(), code);
        queryWrapper.eq(SystemEntityPO.getValidFieldName(), 1);
        int count = count(queryWrapper);
        if (count < 1) return false;
        return true;
    }

    @Override
    public SystemEntityPO queryEntityById(Long id) {
        SystemEntityPO systemEntityPO = getById(id);
        if (Objects.nonNull(systemEntityPO)) {
            systemEntityPO.setCompanyName(queryCompanyName(systemEntityPO.getCid()));
            systemEntityPO.setModuleName(queryModuleById(systemEntityPO.getModuleId()));
            setEntityI18n(systemEntityPO);
        }
        return systemEntityPO;
    }

    @Override
    public void addEntity(SystemEntityPO systemEntityPO) {
        if (validateEntityExist(systemEntityPO.getCode())) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_ENTITY_CODE_IS_EXISTS);
        }
        save(systemEntityPO);
    }


    @Override
    public void addEntityForRpc(SystemEntityPO systemEntityPO) {
        if (validateEntityExist(systemEntityPO.getCode())) {
            log.info("编码已存在，code:{}", systemEntityPO.getCode());
        } else {
            save(systemEntityPO);
        }

    }

    @Override
    public void updateEntity(SystemEntityPO systemEntityPO) {
        UpdateWrapper<SystemEntityPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq(SystemEntityPO.getCodeFieldName(), systemEntityPO.getCode());

        QueryWrapper queryWrapper = new QueryWrapper<SystemEntityPO>();
        queryWrapper.eq(SystemEntityPO.getCodeFieldName(), systemEntityPO.getCode());
        queryWrapper.eq(SystemEntityPO.getValidFieldName(), 1);
        SystemEntityPO entityPO = getOne(queryWrapper);
        if (null != entityPO.getRowVersion()) {
            systemEntityPO.setRowVersion(entityPO.getRowVersion() + 1);
        }
        update(systemEntityPO, updateWrapper);
    }

    @Override
    @Transactional
    public void deleteEntityByCode(String code) {
        // 逻辑删除该系统字典项
        if (StringUtils.isBlank(code)) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
        }
        UpdateWrapper<SystemEntityPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq(SystemEntityPO.getCodeFieldName(), code);
        updateWrapper.set(SystemEntityPO.getValidFieldName(), 0);
        update(updateWrapper);
        // 逻辑删除该系统字典项下的所有编码值
        systemCodeService.deleteValueByEntityCode(code);
    }

    @Override
    @Transactional
    public void batchDeleteEntities(List<String> codeList) {
        // 批量逻辑删除该系统字典项以及该系统字典项下的所有编码值
        if (CollectionUtils.isEmpty(codeList)) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
        }
        UpdateWrapper<SystemEntityPO> updateWrapper = new UpdateWrapper<>();
        for (int i = 0; i < codeList.size(); i++) {
            String code = codeList.get(i);
            if (i < (codeList.size() - 1)) {
                updateWrapper.eq(SystemEntityPO.getCodeFieldName(), code).or();
            } else {
                updateWrapper.eq(SystemEntityPO.getCodeFieldName(), code);
            }
            systemCodeService.deleteValueByEntityCode(code);
        }
        updateWrapper.set(SystemEntityPO.getValidFieldName(), 0);
        update(updateWrapper);
    }

    @Transactional
    @Override
    public void deleteEntityByModuleId(String moduleId) {
        if (StringUtils.isBlank(moduleId)) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_BY_MODULE_ID_IS_NOT_EMPTY);
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(SystemEntityPO.getModuleIdFieldName(), moduleId);
        List<SystemEntityPO> systemEntityPOList = list(queryWrapper);
        List<String> entityCodeList = systemEntityPOList.stream().map(SystemEntityPO::getCode).collect(Collectors.toList());
        // 逻辑删除系统编码
        UpdateWrapper<SystemEntityPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq(SystemEntityPO.getModuleIdFieldName(), moduleId);
        updateWrapper.set(SystemEntityPO.getValidFieldName(), 0);
        update(updateWrapper);
        // 删除编码值
        if (!CollectionUtils.isEmpty(entityCodeList)) {
            UpdateWrapper<SystemCodePO> update = new UpdateWrapper<>();
            for (int i = 0; i < entityCodeList.size(); i++) {
                String entityCode = entityCodeList.get(i);
                if (i < (entityCodeList.size() - 1)) {
                    update.eq(SystemCodePO.getEntityCodeFieldName(), entityCode).or();
                } else {
                    update.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
                }
            }
            update.set(SystemCodePO.getValidFieldName(), 0);
            systemCodeService.update(update);
        }
    }
    @Override
    public List<SystemEntityPO> getEntityByModuleIds(List<String> moduleIds) {
        QueryWrapper queryWrapper = new QueryWrapper<SystemEntityPO>();
        queryWrapper.eq(SystemEntityPO.getValidFieldName(),1);
        queryWrapper.in(SystemEntityPO.getModuleIdFieldName(),moduleIds);
        queryWrapper.orderByDesc(SystemEntityPO.getIdFieldName());
        return list(queryWrapper);
    }

    @Override
    public List<SystemEntityPO> getEntityByModuleId(String moduleId) {
        QueryWrapper queryWrapper = new QueryWrapper<SystemEntityPO>();
        queryWrapper.eq(SystemEntityPO.getValidFieldName(),1);
        queryWrapper.eq(SystemEntityPO.getModuleIdFieldName(),"sys");
        return list(queryWrapper);
    }

    @Override
    public PageResult<SystemEntityPO> queryEntitiesByModuleIds(List<String> moduleIdList, int current, int pageSize) {
        // 构造查询数据库条件
        QueryWrapper<SystemEntityPO> queryWrapper = new QueryWrapper();
        queryWrapper.eq(SystemEntityPO.getValidFieldName(), 1);
        if (!CollectionUtils.isEmpty(moduleIdList)) {
            queryWrapper.in(SystemEntityPO.getModuleIdFieldName(), moduleIdList);
        }
        queryWrapper.orderByDesc(SystemEntityPO.getCreateTimeFieldName());

        // 查询分页信息
        Integer count = count(queryWrapper);
        Page<SystemEntityPO> pageInfo = new Page<>(current, pageSize, count);
        page(pageInfo, queryWrapper);

        // 处理查询系统编码结果数据
        List<SystemEntityPO> systemEntityList = new ArrayList<>();
        List<SystemEntityPO> systemEntityPOList = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(systemEntityPOList)) {
            return new PageResult<>(systemEntityList, pageInfo.getTotal(), pageInfo.getSize(), pageInfo.getCurrent());
        }
        systemEntityPOList.stream().forEach(systemEntityPO -> {
            SystemEntityPO systemEntity = new SystemEntityPO();
            BeanUtils.copyProperties(systemEntityPO, systemEntity);
            systemEntity.setCompanyName(queryCompanyName(systemEntityPO.getCid()));
            systemEntity.setModuleName(queryModuleById(systemEntityPO.getModuleId()));
            setEntityI18n(systemEntity);
            systemEntityList.add(systemEntity);
        });

        return new PageResult<>(systemEntityList, pageInfo.getTotal(), pageInfo.getSize(), pageInfo.getCurrent());
    }

    /**
     * 调用组织架构接口获取公司信息
     * @param companyId
     * @return
     */
    private String queryCompanyName(Long companyId){
        if (null == companyId) {
            companyId = 1000L;
        }
        Result<CompanyResultDTO> companyInfo = personApiService.findCompany(companyId);
        String companyName = "";
        if (null != companyInfo && null != companyInfo.getData()) {
            companyName = companyInfo.getData().getFullName();
        }
        return companyName;
    }

    /**
     * 调用注册模块接口获取模块名称
     * @param moduleId
     * @return
     */
    private String queryModuleById(String moduleId) {
        ModuleDTO moduleDTO = moduleService.queryModuleByModuleId(moduleId);
        if (Objects.isNull(moduleDTO)) {
            return null;
        }
        return moduleDTO.getModuleName();
    }

    private void setEntityI18n(SystemEntityPO systemEntityPO) {
        if (Objects.isNull(systemEntityPO)) {
            return;
        }
        if (!ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemEntityPO.getName()))) {
            systemEntityPO.setDisplayName(i18nAdapterService.getRemoteMessage(systemEntityPO.getName()));
        }
    }
}
