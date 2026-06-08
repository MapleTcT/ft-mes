package com.supcon.supfusion.systemcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.SystemCodeMapper;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeSortPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.manager.I18nAdapter;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.supcon.supfusion.systemcode.common.constants.Constants.SQL_LIKE_CHAR;

/**
 * @author 
 * @date 20-5-11 下午14:30
 */
@Slf4j
@Service
public class SystemCodeServiceImpl extends ServiceImpl<SystemCodeMapper, SystemCodePO> implements SystemCodeService {

	@Autowired
	PersonApiService personApiService;

	@Autowired
	I18nAdapter i18nAdapterService;

	@Autowired
	SystemEntityService systemEntityService;

	@Autowired
	DbStringUtil dbStringUtil;

	@Value("${supfusion.cloud.datasource.connect.system.db-type:}")
	String dbType;

	@Override
	@Transactional
	public void addValue(SystemCodePO systemCodePO) {
		if (validateCodeValueExist(systemCodePO.getEntityCode(), systemCodePO.getCode())) {
			throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_CODE_VALUE_IS_EXISTS);
		}
		insertValue(systemCodePO);
	}

	@Override
	@Transactional
	public void addValueForRpc(SystemCodePO systemCodePO) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePO.getEntityCode());
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), systemCodePO.getCode());
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		SystemCodePO codePO = getOne(queryWrapper);
		if (null != codePO) {
			log.info("编码已存在，code:{}", systemCodePO.getCode());
			// 若版本为0 则更新
			if (codePO.getRowVersion() == 0) {
				updateValue(systemCodePO);
			}
		} else {
			insertValue(systemCodePO);
		}

	}

	private void insertValue(SystemCodePO systemCodePO) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePO.getEntityCode());
		// 如果创建的值的默认属性设置为是,则其他的值都为否
		if (1 == systemCodePO.getDefaultFlag()) {
			List<SystemCodePO> systemCodePOList = list(queryWrapper);
			for (SystemCodePO systemCode : systemCodePOList) {
				systemCode.setDefaultFlag(0);
			}
			updateBatchById(systemCodePOList);
		}

		Long id = IDGenerator.newInstance().generate().longValue();
		systemCodePO.setId(id);
		if (systemCodePO.getParentId() == null) {
			systemCodePO.setLayNo(1);
			systemCodePO.setFullPath(systemCodePO.getCode());
			systemCodePO.setFullPathName(queryDisplayName(systemCodePO));
			systemCodePO.setLayRec(String.valueOf(id));
			queryWrapper.isNull(SystemCodePO.getParentIdFieldName());
		} else {
			SystemCodePO parentSystemCodePo = getById(systemCodePO.getParentId());
			systemCodePO.setLayNo(parentSystemCodePo.getLayNo() + 1);
			systemCodePO.setFullPath(parentSystemCodePo.getFullPath() + "/" + systemCodePO.getCode());
			systemCodePO.setFullPathName(parentSystemCodePo.getFullPathName() + "/" + queryDisplayName(systemCodePO));
			systemCodePO.setLayRec(parentSystemCodePo.getId() + "-" + id);
			queryWrapper.eq(SystemCodePO.getParentIdFieldName(), systemCodePO.getParentId());
		}

		List<SystemCodePO> brotherSystemCodeList = list(queryWrapper);
		if (CollectionUtils.isEmpty(brotherSystemCodeList)) {
			systemCodePO.setSort(1.0);
		} else {
			systemCodePO.setSort(brotherSystemCodeList.size() + 1.0);
		}
		save(systemCodePO);
	}



	@Override
	public PageResult<SystemCodePO> queryValueList(String entityCode, String keyword, Integer page, Integer perPage) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);

		if (StringUtils.isNotBlank(keyword)) {
			Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(keyword);
			queryWrapper.and(query -> {
				if (Constants.DB_TYPE_ORACLE.equals(dbType)){
					query.apply(SystemCodePO.getCodeFieldName() + " like {0} escape '\\'", SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + SQL_LIKE_CHAR);
				}else{
					query.like(SystemCodePO.getCodeFieldName(), keyword);
				}
//				query.like(SystemCodePO.getCodeFieldName(), keyword);
				if (!ObjectUtils.isEmpty(map)) {
					Set<String> strings = map.keySet();
					List<String> name = new ArrayList<>(strings);
					int batch = name.size() / 1000;
					if (batch == 0){
						query.or().in(SystemCodePO.getNameFieldName(), name);
					}else{
						for (int i = 0; i < batch; i++) {
							query.or().in(SystemCodePO.getNameFieldName(), name.subList(i * 1000, i * 1000 + 1000));
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
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		Integer count = count(queryWrapper);
		queryWrapper.orderByAsc(SystemCodePO.getLayNoFieldName(), SystemCodePO.getSortFieldName());
		Page<SystemCodePO> pageInfo = new Page<>(page, perPage, count);
		page(pageInfo, queryWrapper);
		List<SystemCodePO> systemCodeList = pageInfo.getRecords();
		for (SystemCodePO systemCodePO : systemCodeList) {
			systemCodePO.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCodePO);
		}
		return new PageResult<>(systemCodeList, pageInfo.getTotal(), pageInfo.getSize(), pageInfo.getCurrent());
	}

	@Override
	public List<SystemCodePO> queryValueListNoPage(String entityCode, String keyword, String displayName, String code) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);

		if (StringUtils.isNotBlank(keyword)) {
			Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(keyword);
			queryWrapper.and(query -> {
				if (Constants.DB_TYPE_ORACLE.equals(dbType)){
					query.apply(SystemCodePO.getCodeFieldName() + " like {0} escape '\\'", SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + SQL_LIKE_CHAR);
				}else{
					query.like(SystemCodePO.getCodeFieldName(), keyword);
				}
//				query.like(SystemCodePO.getCodeFieldName(), keyword);
				if (!ObjectUtils.isEmpty(map)) {
					Set<String> strings = map.keySet();
					List<String> names = new ArrayList<>(strings);
					int batch = names.size() / 1000;
					if (batch == 0){
						query.or().in(SystemCodePO.getNameFieldName(), names);
					}else{
						for (int i = 0; i < batch; i++) {
							query.or().in(SystemCodePO.getNameFieldName(),names.subList(i * 1000, i * 1000 + 1000));
						}
						if (names.size() % 1000 != 0) {
							query.or().in(SystemCodePO.getNameFieldName(), names.subList(batch * 1000, names.size()));
						}
					}
				} else {
					query.or().eq(SystemCodePO.getNameFieldName(), "");
				}
			});
		}

		if (StringUtils.isNoneBlank(displayName)) {
			Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(displayName);
			if (!ObjectUtils.isEmpty(map)) {
				Set<String> strings = map.keySet();
				List<String> names = new ArrayList<>(strings);
				int batch = names.size() / 1000;
				queryWrapper.and(query -> {
					if (batch == 0) {
						query.or().in(SystemCodePO.getNameFieldName(), names);
					} else {
						for (int i = 0; i < batch; i++) {
							query.or().in(SystemCodePO.getNameFieldName(), names.subList(i * 1000, i * 1000 + 1000));
						}
						if (names.size() % 1000 != 0) {
							query.or().in(SystemCodePO.getNameFieldName(), names.subList(batch * 1000, names.size()));
						}
					}
				});
			} else {
				queryWrapper.eq(SystemCodePO.getNameFieldName(), "");
			}
		}
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.orderByAsc(SystemCodePO.getLayNoFieldName(), SystemCodePO.getSortFieldName());
		List<SystemCodePO> systemCodePOList = list(queryWrapper);
		for (SystemCodePO systemCodePO : systemCodePOList) {
			systemCodePO.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCodePO);
		}
		// 根据indexof排序
		if (StringUtils.isNoneBlank(displayName)) {
			// 根据indexof排序
			Collections.sort(systemCodePOList, (SystemCodePO arg0, SystemCodePO arg1) -> {
				int range1 = arg0.getDisplayName().indexOf(displayName);
				int range2 = arg1.getDisplayName().indexOf(displayName);
				return range1 - range2;
			});
		}

//		List<SystemCodePO> systemCodePOS = new ArrayList<>();
//		systemCodePOS.addAll(systemCodePOList);
		if (StringUtils.isNoneEmpty(code)) {
			String[] codeArr = code.split("/");
			String entityCodeStr = null;
			String codeStr = null;
			for (int i = 0; i < codeArr.length; i++) {
				entityCodeStr = codeArr[0];
				codeStr = codeArr[1];
			}
			QueryWrapper<SystemCodePO> query = new QueryWrapper<>();
			query.eq(SystemCodePO.getEntityCodeFieldName(), entityCodeStr);
			query.eq(SystemCodePO.getCodeFieldName(), codeStr);
			SystemCodePO systemCodePO = getOne(query);
			if (Objects.nonNull(systemCodePO)) {
				boolean result = true;
				for (SystemCodePO systemCode : systemCodePOList) {
					if (systemCode.getCode().equals(systemCodePO.getCode())) {
						result = false;
						break;
					}
				}
				if (result) {
					systemCodePOList.add(systemCodePO);
				}
			}
		}
		return systemCodePOList;
	}

	@Override
	public List<SystemCodePO> queryValueListByApp(String entityCode) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);

		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.orderByAsc(SystemCodePO.getLayNoFieldName(), SystemCodePO.getSortFieldName());
		List<SystemCodePO> systemCodePOList = list(queryWrapper);
		for (SystemCodePO systemCodePO : systemCodePOList) {
			systemCodePO.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCodePO);
		}
		return systemCodePOList;
	}

	@Override
	public void queryValueTree(SystemEntityDetailPO systemEntityDetailPO) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemEntityDetailPO.getCode());
		queryWrapper.eq(SystemCodePO.getLayNoFieldName(), 1);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.orderByAsc(SystemCodePO.getSortFieldName());
		List<SystemCodePO> list = list(queryWrapper);
		List<SystemCodeDetailPO> detailList = new ArrayList<>();
		systemEntityDetailPO.setCompanyName(queryCompanyName(systemEntityDetailPO.getCid()));
		systemEntityDetailPO.setChildren(detailList);
		if (list == null || list.size() < 1) {
			return;
		}
		for (SystemCodePO scPo : list) {
			SystemCodeDetailPO scdPo = new SystemCodeDetailPO();
			BeanUtils.copyProperties(scPo, scdPo);
			scdPo.setCompanyName(queryCompanyName(scPo.getCid()));
			setCodeValueTreeI18n(scdPo);
			detailList.add(scdPo);
		}
		detailList.stream().forEach(item -> generateSonCodes(item));
	}

	private void generateSonCodes(SystemCodeDetailPO systemCodeDetailPo) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getParentIdFieldName(), systemCodeDetailPo.getId());
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		queryWrapper.orderByAsc(SystemCodePO.getSortFieldName());
		List<SystemCodePO> children = list(queryWrapper);
		List<SystemCodeDetailPO> detailList = new ArrayList<>();
		systemCodeDetailPo.setCompanyName(queryCompanyName(systemCodeDetailPo.getCid()));
		systemCodeDetailPo.setChildren(detailList);
		if (children == null || children.size() < 1) {
			return;
		}
		for (SystemCodePO scPo : children) {
			SystemCodeDetailPO scdPo = new SystemCodeDetailPO();
			BeanUtils.copyProperties(scPo, scdPo);
			scdPo.setCompanyName(queryCompanyName(scPo.getCid()));
			setCodeValueTreeI18n(scdPo);
			detailList.add(scdPo);
		}
		detailList.stream().forEach(item -> generateSonCodes(item));
	}
	
	@Override
	@Transactional
	public void updateValue(SystemCodePO systemCodePo) {
		// 如果修改编码值的默认属性设置为是,则其他的值都为否
		if (1 == systemCodePo.getDefaultFlag()) {
			QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePo.getEntityCode());
			queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
			List<SystemCodePO> systemCodePOList = list(queryWrapper);
			for (SystemCodePO systemCode : systemCodePOList) {
				systemCode.setDefaultFlag(0);
				updateRowVersion(systemCode);
			}
			updateBatchById(systemCodePOList);
		}
		UpdateWrapper<SystemCodePO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePo.getEntityCode());
		updateWrapper.eq(SystemCodePO.getCodeFieldName(), systemCodePo.getCode());
		updateRowVersion(systemCodePo);
		update(systemCodePo, updateWrapper);
	}

	private void updateRowVersion(SystemCodePO systemCodePo) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePo.getEntityCode());
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), systemCodePo.getCode());
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		SystemCodePO codePO = getOne(queryWrapper);

		if (null != codePO.getRowVersion()) {
			systemCodePo.setRowVersion(codePO.getRowVersion() + 1);
		}
	}


	@Override
	public void deleteValue(String entityCode, String code) {
		SystemCodePO currentSystemCodePO = queryCodeValueByCode(entityCode, code);
		if (!Objects.isNull(currentSystemCodePO)) {
			List<SystemCodePO> systemCodePOList = new ArrayList<>();
			// 查询编码值的系统编码,如果是树形结构,则删除当前节点以及所有的子节点;如果为列表结构,则只删除当前节点
			SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(entityCode);
			if ("tree".equals(systemEntityPO.getType())) {
				QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
				queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
				queryWrapper.likeRight(SystemCodePO.getFullPathFieldName(), currentSystemCodePO.getFullPath());
				List<SystemCodePO> systemCodeList = list(queryWrapper);
				systemCodePOList.addAll(systemCodeList);
			} else {
				systemCodePOList.add(currentSystemCodePO);
			}

			for (SystemCodePO systemCodePO : systemCodePOList) {
				systemCodePO.setValid(0);
			}
			updateBatchById(systemCodePOList);
		}
	}

	@Override
	public void batchDeleteValues(String entityCode, List<String> list) {
		if (CollectionUtils.isEmpty(list)) {
			throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
		}

		List<SystemCodePO> systemCodePOList = new ArrayList<>();
		for (String code : list) {
			SystemCodePO systemCodePO = queryCodeValueByCode(entityCode, code);
			if (Objects.isNull(systemCodePO)) continue;

			// 查询编码值的系统编码,如果是树形结构,则删除当前节点以及所有的子节点;如果为列表结构,则只删除当前节点
			SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(entityCode);
			if ("tree".equals(systemEntityPO.getType())) {
				QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
				queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
				queryWrapper.likeRight(SystemCodePO.getFullPathFieldName(), systemCodePO.getFullPath());
				List<SystemCodePO> systemCodeList = list(queryWrapper);
				systemCodePOList.addAll(systemCodeList);
			} else {
				systemCodePOList.add(systemCodePO);
			}
		}
		for (SystemCodePO systemCodePO : systemCodePOList) {
			systemCodePO.setValid(0);
		}
		updateBatchById(systemCodePOList);
	}

	@Override
	public PageResult<SystemCodePO> queryValueNodes(String entityCode, Long parentId, String keyword, Integer current, Integer pageSize) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		if (StringUtils.isNotBlank(keyword)) {
			Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(keyword);
			queryWrapper.and(query -> {
				if (Constants.DB_TYPE_ORACLE.equals(dbType)){
					query.apply(SystemCodePO.getCodeFieldName() + " like {0} escape '\\'", SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + SQL_LIKE_CHAR);
				}else{
					query.like(SystemCodePO.getCodeFieldName(), keyword);
				}
//				query.like(SystemCodePO.getCodeFieldName(), keyword);
				if (!ObjectUtils.isEmpty(map)) {
					Set<String> strings = map.keySet();
					List<String> name = new ArrayList<>(strings);
					int batch = name.size() / 1000;
					if (batch == 0){
						query.or().in(SystemCodePO.getNameFieldName(), name);
					}else{
						for (int i = 0; i < batch; i++) {
							query.or().in(SystemCodePO.getNameFieldName(),name.subList(i * 1000, i * 1000 + 1000));
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
		if (StringUtils.isBlank(keyword) && null != parentId) {
			if (-1 == parentId) {
				queryWrapper.isNull(SystemCodePO.getParentIdFieldName());
			} else {
				queryWrapper.and(item -> item.eq(SystemCodePO.getParentIdFieldName(), parentId).or().eq(SystemCodePO.getIdFieldName(), parentId));
			}
		}
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
//		Integer count = count(queryWrapper);
		queryWrapper.orderByAsc(SystemCodePO.getLayNoFieldName(), SystemCodePO.getSortFieldName());
		Page<SystemCodePO> pageInfo = new Page<>(current, pageSize);
		page(pageInfo, queryWrapper);
		List<SystemCodePO> systemCodeList = pageInfo.getRecords();
		for (SystemCodePO systemCodePO : systemCodeList) {
			systemCodePO.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCodePO);
		}
		return new PageResult<>(systemCodeList, pageInfo.getTotal(), pageInfo.getSize(), pageInfo.getCurrent());
	}

	@Override
	public SystemCodePO queryValueById(Long id) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getIdFieldName(), id);
		SystemCodePO systemCodePO = getOne(queryWrapper);
		setCodeValueI18n(systemCodePO);
		return systemCodePO;
	}

	@Override
	public SystemCodePO queryCodeValueByCode(String code) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), code);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		SystemCodePO systemCodePO = getOne(queryWrapper);
		setCodeValueI18n(systemCodePO);
		return systemCodePO;
	}

	@Override
	public SystemCodePO queryCodeValueByCode(String entityCode, String code) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), code);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		SystemCodePO systemCodePO = getOne(queryWrapper);
		setCodeValueI18n(systemCodePO);
		return systemCodePO;
	}

	@Override
	public SystemCodePO queryAllCodeValueByCode(String entityCode, String code) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), code);
		SystemCodePO systemCodePO = getOne(queryWrapper);
		setCodeValueI18n(systemCodePO);
		return systemCodePO;
	}

	@Override
	public boolean validateCodeValueExist(String entityCode, String code) {
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		queryWrapper.eq(SystemCodePO.getCodeFieldName(), code);
		int count = count(queryWrapper);
		if (count < 1) return false;
		return true;
	}

	@Override
	public void modifyValueSort(SystemCodeSortPO systemCodeSortPO) {
		String parentIdStr = systemCodeSortPO.getParentId();
		String parentNameStr = systemCodeSortPO.getParentName();
		String prevIdStr = systemCodeSortPO.getPrevId();
		String nextIdStr = systemCodeSortPO.getNextId();
		String currentIdStr = systemCodeSortPO.getCurrentId();

		// 获取上一个节点和下一个节点的排序号
		Double prevSort = 0.0;
		Double nextSort = 0.0;
		if (StringUtils.isNoneBlank(prevIdStr)) {
			SystemCodePO prevSystemCodePO = getById(Long.parseLong(prevIdStr));
			prevSort = prevSystemCodePO.getSort();
		}
		if (StringUtils.isNoneBlank(nextIdStr)) {
			SystemCodePO nextSystemCodePO = getById(Long.parseLong(nextIdStr));
			nextSort = nextSystemCodePO.getSort();
		}
		// 计算获取当前节点的排序号
		Double newSort = getNewOrder(prevSort, nextSort);

		// 修改当前节点的排序号和父节点的Id和名称
		Long currentId = Long.parseLong(currentIdStr);
		Long parentId = null;
		if (StringUtils.isNoneBlank(parentIdStr)) {
			parentId = Long.parseLong(parentIdStr);
		}
		SystemCodePO currentSystemCodePO = getById(currentId);
		if (Objects.isNull(currentSystemCodePO)) return;
		currentSystemCodePO.setSort(newSort);
		currentSystemCodePO.setParentId(parentId);
		currentSystemCodePO.setParentName(parentNameStr);
		updateRowVersion(currentSystemCodePO);
		updateById(currentSystemCodePO);

		// 根据当前节点的fullPath和layNo获取排序后当前节点以及字节点新的fullPath和layNo
		String oldCurrentSystemCodeFullPath = currentSystemCodePO.getFullPath();
		String newCurrentSystemCodeFullPath;
		Integer oldCurrentSystemCodeLayNo = currentSystemCodePO.getLayNo();
		Integer newCurrentSystemCodeLayNo;
		if (null == parentId) {
			newCurrentSystemCodeLayNo = 1;
			newCurrentSystemCodeFullPath = currentSystemCodePO.getCode();
		} else {
			SystemCodePO parentSystemCodePo = getById(parentId);
			Integer parentSystemCodePoLayNo = parentSystemCodePo.getLayNo();
			newCurrentSystemCodeLayNo = parentSystemCodePoLayNo + 1;
			newCurrentSystemCodeFullPath = parentSystemCodePo.getFullPath() + "|" + currentSystemCodePO.getCode();
		}

		Integer diffLayNo = newCurrentSystemCodeLayNo - oldCurrentSystemCodeLayNo;
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), currentSystemCodePO.getEntityCode());
		queryWrapper.likeRight(SystemCodePO.getFullPathFieldName(), currentSystemCodePO.getFullPath());
		List<SystemCodePO> systemCodeList = list(queryWrapper);

		systemCodeList.stream().forEach(item -> {
			Integer sonLayNo = item.getLayNo() + diffLayNo;
			String sonFullPath = item.getFullPath();
			sonFullPath = newCurrentSystemCodeFullPath + sonFullPath.substring(oldCurrentSystemCodeFullPath.length());
			item.setLayNo(sonLayNo);
			item.setFullPath(sonFullPath);
			updateRowVersion(item);
		});
		updateBatchById(systemCodeList);
	}

	@Override
	public void deleteValueByEntityCode(String entityCode) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		List<SystemCodePO> systemCodeList = list(queryWrapper);
		for (SystemCodePO systemCodePO : systemCodeList) {
			systemCodePO.setValid(0);
		}
		updateBatchById(systemCodeList);
	}

	private Double getNewOrder(Double prevOrder, Double nextOrder) {
		if (prevOrder == 0.0 && nextOrder == 0.0) {
			//父节点下唯一节点
			return 1.0;
		}
		if (prevOrder == 0.0 && nextOrder != 0.0) {
			// 移动到第一位
			return nextOrder / 2;
		}
		if (prevOrder != 0.0 && nextOrder == 0.0) {
			// 移动到最后位
			return prevOrder + 10000.0;
		}
		// 移动到中间某个位置
		return (prevOrder + nextOrder) / 2;
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

	@Override
	public List<SystemCodeDetailPO> queryCodeValueBaseTree(String systemEntityCode, String idStr) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemEntityCode);
		if (StringUtils.isBlank(idStr)) {
			queryWrapper.eq(SystemCodePO.getLayNoFieldName(), 1);
		} else {
			String[] codeArray = idStr.split("/");
			if (codeArray == null || codeArray.length <= 0) {
				throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
			}
			List<String> codeList = Arrays.asList(codeArray);
			queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), codeList.get(0));
			queryWrapper.eq(SystemCodePO.getCodeFieldName(), codeList.get(1));
			SystemCodePO systemCodePO = getOne(queryWrapper);
			queryWrapper.eq(SystemCodePO.getLayNoFieldName(), systemCodePO.getLayNo());
		}

		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.orderByAsc(SystemCodePO.getSortFieldName());
		List<SystemCodePO> list = list(queryWrapper);

		List<SystemCodeDetailPO> systemCodeDetailPOList = new ArrayList<>();
		SystemCodeDetailPO systemCodeDetailPO = new SystemCodeDetailPO();
		systemCodeDetailPO.setChildren2(systemCodeDetailPOList);
		if (list == null || list.size() < 1) {
			return systemCodeDetailPOList;
		}
		for (SystemCodePO scPO : list) {
			SystemCodeDetailPO scdPO = new SystemCodeDetailPO();
			BeanUtils.copyProperties(scPO, scdPO);
			scdPO.setValue(queryDisplayName(scdPO));
			if (null != scdPO.getParentId()) {
				SystemCodePO systemCodeParent = getById(scdPO.getParentId());
				scdPO.setParentCodeStr(systemCodeParent.getEntityCode() + "/" + systemCodeParent.getCode());
			}
			systemCodeDetailPOList.add(scdPO);
		}
		systemCodeDetailPOList.stream().forEach(item -> generateChildren(item));
		return systemCodeDetailPOList;
	}

	private void generateChildren(SystemCodeDetailPO systemCodeDetailPO) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getParentIdFieldName(), systemCodeDetailPO.getId());
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		queryWrapper.eq(SystemCodePO.getCidFieldName(), companyId);
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		queryWrapper.orderByAsc(SystemCodePO.getSortFieldName());
		List<SystemCodePO> children = list(queryWrapper);

		List<SystemCodeDetailPO> systemCodeDetailPOList = new ArrayList<>();
		systemCodeDetailPO.setChildren2(systemCodeDetailPOList);
		if (children == null || children.size() < 1) {
			return;
		}
		for (SystemCodePO scPO : children) {
			SystemCodeDetailPO scdPO = new SystemCodeDetailPO();
			BeanUtils.copyProperties(scPO, scdPO);
			scdPO.setValue(queryDisplayName(scdPO));
			if (null != scdPO.getParentId()) {
				SystemCodePO systemCodeParent = getById(scdPO.getParentId());
				scdPO.setParentCodeStr(systemCodeParent.getEntityCode() + "/" + systemCodeParent.getCode());
			}
			systemCodeDetailPOList.add(scdPO);
		}
		systemCodeDetailPOList.stream().forEach(item -> generateChildren(item));
	}

	@Override
	public String queryDisplayName(Object object) {
		if (object instanceof SystemCodeDetailPO) {
			SystemCodeDetailPO systemCodeDetailPO = (SystemCodeDetailPO) object;
			String displayName = i18nAdapterService.getRemoteMessage(systemCodeDetailPO.getName());
			if (StringUtils.isBlank(displayName)) {
				displayName = systemCodeDetailPO.getDisplayName();
			}
			return displayName;
		} else if (object instanceof SystemCodePO) {
			SystemCodePO systemCodePO = (SystemCodePO) object;
			String displayName = i18nAdapterService.getRemoteMessage(systemCodePO.getName());
			if (StringUtils.isBlank(displayName)) {
				displayName = systemCodePO.getDisplayName();
			}
			return displayName;
		}
		return null;
	}

	/**
	 * 编码值国际化
	 * @param systemCodePO
	 */
	private void setCodeValueI18n(SystemCodePO systemCodePO) {
		if (Objects.isNull(systemCodePO)) {
			return;
		}
		if (!ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemCodePO.getName()))) {
			systemCodePO.setDisplayName(i18nAdapterService.getRemoteMessage(systemCodePO.getName()));
		}
		if (!ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemCodePO.getParentName()))) {
			systemCodePO.setParentDisplayName(i18nAdapterService.getRemoteMessage(systemCodePO.getParentName()));
		}
//		if (null == systemCodePO.getParentId()) {
//			SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(systemCodePO.getEntityCode());
//			if (Objects.nonNull(systemEntityPO) && !ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemEntityPO.getName()))) {
//				systemCodePO.setParentName(i18nAdapterService.getRemoteMessage(systemEntityPO.getName()));
//			}
//		} else {
//			SystemCodePO systemCode = getById(systemCodePO.getParentId());
//			if (Objects.nonNull(systemCode) && !ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemCode.getName()))) {
//				systemCodePO.setParentName(i18nAdapterService.getRemoteMessage(systemCode.getName()));
//			}
//		}
	}

	/**
	 * 编码值国际化
	 * @param systemCodeDetailPO
	 */
	private void setCodeValueTreeI18n(SystemCodeDetailPO systemCodeDetailPO) {
		if (Objects.isNull(systemCodeDetailPO)) {
			return;
		}
		if (!ObjectUtils.isEmpty(i18nAdapterService.getRemoteMessage(systemCodeDetailPO.getName()))) {
			systemCodeDetailPO.setDisplayName(i18nAdapterService.getRemoteMessage(systemCodeDetailPO.getName()));
		}
		List<SystemCodeDetailPO> systemCodeDetailPOList = systemCodeDetailPO.getChildren();
		if (!CollectionUtils.isEmpty(systemCodeDetailPOList)) {
			for (SystemCodeDetailPO po : systemCodeDetailPOList) {
				setCodeValueI18n(po);
			}
		}
	}

	@Override
	public Map<String, String> getSystemCodeList(String companyCode, String entityCode, Boolean senior) {
		Result<CompanyResultDTO> companyResult = personApiService.findCompanyByCode(companyCode);
		CompanyResultDTO companyResultDTO = companyResult.getData();
		if (companyResultDTO == null) {
			return null;
		}
		int systemEntityCount = systemEntityService.count(Wrappers.lambdaQuery(SystemEntityPO.class)
				.eq(SystemEntityPO::getValid, 1)
				.eq(SystemEntityPO::getCode, entityCode)
				.eq(SystemEntityPO::getCid, companyResultDTO.getId()));
		if (systemEntityCount == 0) {
			return null;
		}
		List<SystemCodePO> systemCodePOS = list(Wrappers.lambdaQuery(SystemCodePO.class)
				.eq(SystemCodePO::getValid, 1)
				.eq(SystemCodePO::getEntityCode, entityCode));
		if (systemCodePOS.isEmpty()) {
			return null;
		}
		Map<String, String> result = new LinkedHashMap<>();
		for (SystemCodePO systemCodePO : systemCodePOS) {
			String interValue = i18nAdapterService.getRemoteMessage(systemCodePO.getName());
			result.put((senior ? systemCodePO.getCode() : entityCode + "/" + systemCodePO.getCode()), interValue);
		}
		return result;
	}

	@Override
	public Map<String, Object> queryEntityValueList(String entityCode, String code, Integer page, Integer perPage) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
		// 调用组织架构接口获取公司信息
//		Long companyId = UserContext.getUserContext().getCompanyId();
//		if (null == companyId || "".equals(companyId)) {
//			throw new SystemCodeException(SystemCodeErrorEnum.COMPANY_ID_IS_NOT_FOUND);
//		}
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		Integer count = count(queryWrapper);
		queryWrapper.orderByAsc(SystemCodePO.getLayNoFieldName(), SystemCodePO.getSortFieldName());
		Page<SystemCodePO> pageInfo = new Page<>(page, perPage, count);
		IPage<SystemCodePO> systemCodeIPage = page(pageInfo, queryWrapper);
		List<SystemCodePO> systemCodeList = systemCodeIPage.getRecords();
		for (SystemCodePO systemCodePO : systemCodeList) {
			systemCodePO.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCodePO);
		}

		Map<String, Object> map = new HashMap();
		map.put("data", new PageResult<>(systemCodeList, count, perPage, page));
		if (StringUtils.isNoneEmpty(code)) {
			QueryWrapper<SystemCodePO> query = new QueryWrapper<>();
			query.eq(SystemCodePO.getEntityCodeFieldName(), entityCode);
			query.eq(SystemCodePO.getCodeFieldName(), code);
			query.eq(SystemCodePO.getValidFieldName(), 1);
			SystemCodePO systemCodePO = getOne(query);
			map.put(code, systemCodePO);
		}
		return map;
	}

	@Override
	public List<SystemCodePO> queryValueListByEntityCodes(List<String> entityCodeList) {
		// 构造查询数据库条件
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getValidFieldName(), 1);
		if (!CollectionUtils.isEmpty(entityCodeList)) {
			queryWrapper.in(SystemCodePO.getEntityCodeFieldName(), entityCodeList);
		}

		// 处理通过系统编码查询编码值结果数据
		List<SystemCodePO> systemCodeList = new ArrayList<>();
		List<SystemCodePO> systemCodePOList = list(queryWrapper);
		systemCodePOList.stream().forEach(systemCodePO -> {
			SystemCodePO systemCode = new SystemCodePO();
			BeanUtils.copyProperties(systemCodePO, systemCode);
			systemCode.setCompanyName(queryCompanyName(systemCodePO.getCid()));
			setCodeValueI18n(systemCode);
			systemCodeList.add(systemCode);
		});

		return systemCodeList;
	}
}
