package com.supcon.supfusion.systemcode.service.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeAddDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeInfoDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeSortDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeUpdateDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityDetailDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityInfoDTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeSortPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.service.ModuleService;
import com.supcon.supfusion.systemcode.service.SystemCodeService;
import com.supcon.supfusion.systemcode.service.SystemEntityService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author 
 * @date 20-5-11 下午14:30
 */
@ServiceApiService
public class SystemCodeApiServiceImpl extends BaseController implements SystemCodeApiService {

	@Autowired
	private SystemCodeService systemCodeService;
	@Autowired
	private SystemEntityService systemEntityService;
	@Autowired
	private ModuleService moduleService;

	@Override
	public void addValue(SystemCodeAddDTO systemCodeAddDTO) {
		if (!Pattern.matches(Constants.PATTERN_CODE, systemCodeAddDTO.getCode())) {
			throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
		}
		SystemCodePO systemCodePO = new SystemCodePO();
		BeanUtils.copyProperties(systemCodeAddDTO, systemCodePO);

		systemCodeService.addValueForRpc(systemCodePO);
	}

	@Override
	public PageResult<SystemCodeResultDTO> queryValueList(String entityCode, String keyword, Integer page, Integer perPage) {

		PageResult<SystemCodePO> systemCodePOPageResult = systemCodeService.queryValueList(entityCode, keyword, page, perPage);
		List<SystemCodeResultDTO> systemCodeResultDTOList = new ArrayList<>(systemCodePOPageResult.getList().size());
		systemCodePOPageResult.getList().stream().forEach(systemCodePO -> {
			SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
			BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
			systemCodeResultDTOList.add(systemCodeResultDTO);
		});
		return new PageResult<>(systemCodeResultDTOList, systemCodePOPageResult.getPagination().getTotal(), systemCodePOPageResult.getPagination().getPageSize(), systemCodePOPageResult.getPagination().getCurrent());
	}

	@Override
	public ListResult<SystemCodeResultDTO> queryValueListNoPage(String entityCode, String keyword, String displayName) {
		List<SystemCodePO> systemCodePOList = systemCodeService.queryValueListNoPage(entityCode, keyword, displayName, "");
		List<SystemCodeResultDTO> systemCodeResultDTOList = new ArrayList<>();
		for (SystemCodePO systemCodePO : systemCodePOList) {
			SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
			BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
			systemCodeResultDTOList.add(systemCodeResultDTO);
		}
		return new ListResult<>(systemCodeResultDTOList);
	}

	@Override
	public Result<SystemEntityDetailDTO> queryValueTree(String entityCode) {
		SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(entityCode);
		SystemEntityDetailPO systemEntityDetailPO = new SystemEntityDetailPO();
		BeanUtils.copyProperties(systemEntityPO, systemEntityDetailPO);
		systemCodeService.queryValueTree(systemEntityDetailPO);

		SystemEntityDetailDTO systemEntityDetailDTO = new SystemEntityDetailDTO();
		BeanUtils.copyProperties(systemEntityDetailPO, systemEntityDetailDTO);
		return new Result<>(systemEntityDetailDTO);
	}

	@Override
	public void updateValue(SystemCodeUpdateDTO systemCodeUpdateDTO) {
		if (!Pattern.matches(Constants.PATTERN_CODE, systemCodeUpdateDTO.getCode())) {
			throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
		}
		SystemCodePO systemCodePo = new SystemCodePO();
		BeanUtils.copyProperties(systemCodeUpdateDTO, systemCodePo);
		systemCodeService.updateValue(systemCodePo);
	}

	@Override
	public void deleteValue(String entityCode, String code) {
		systemCodeService.deleteValue(entityCode, code);
	}

	@Override
	public void batchDeleteValues(String entityCode, String codes) {
		String[] codeArray = codes.split(",");
		if (codeArray == null || codeArray.length <= 0) {
			return;
		}
		List<String> codeList = Arrays.asList(codeArray);
		systemCodeService.batchDeleteValues(entityCode, codeList);
	}

	@Override
	public PageResult<SystemCodeResultDTO> queryValueNodes(String entityCode, Long parentId, String keyword, Integer current, Integer pageSize) {
		PageResult<SystemCodePO> systemCodePOPageResult = systemCodeService.queryValueNodes(entityCode, parentId, keyword, current, pageSize);
		List<SystemCodeResultDTO> systemCodeResultDTOList = new ArrayList<>(systemCodePOPageResult.getList().size());
		systemCodePOPageResult.getList().stream().forEach(systemCodePO -> {
			SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
			BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
			systemCodeResultDTOList.add(systemCodeResultDTO);
		});
		return new PageResult<>(systemCodeResultDTOList, systemCodePOPageResult.getPagination().getTotal(), systemCodePOPageResult.getPagination().getPageSize(), systemCodePOPageResult.getPagination().getCurrent());
	}

	@Override
	public Result<SystemCodeResultDTO> queryValueById(Long id) {
		SystemCodePO systemCodePO = systemCodeService.queryValueById(id);
		SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
		BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
		return new Result<>(systemCodeResultDTO);
	}

	@Override
	public Result<SystemCodeResultDTO> queryValueByCode(String code) {
		SystemCodePO systemCodePO = systemCodeService.queryCodeValueByCode(code);
		SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
		BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
		return new Result<>(systemCodeResultDTO);
	}

	@Override
	public Result<SystemCodeResultDTO> queryValueByCode(String entityCode, String code) {
		SystemCodePO systemCodePO = systemCodeService.queryAllCodeValueByCode(entityCode, code);
		SystemCodeResultDTO systemCodeResultDTO = new SystemCodeResultDTO();
		if (Objects.nonNull(systemCodePO)) {
			BeanUtils.copyProperties(systemCodePO, systemCodeResultDTO);
		}
		return new Result<>(systemCodeResultDTO);
	}

	@Override
	public void modifyValueSort(SystemCodeSortDTO systemCodeSortDTO) {
		SystemCodeSortPO systemCodeSortPO = new SystemCodeSortPO();
		BeanUtils.copyProperties(systemCodeSortDTO, systemCodeSortPO);
		systemCodeService.modifyValueSort(systemCodeSortPO);
	}

	@Override
	@Transactional
	public void batchAddSystemCode(List<SystemEntityInfoDTO> list) {
		Long companyId = UserContext.getUserContext().getCompanyId();
		for (SystemEntityInfoDTO systemEntityInfoDTO : list) {
			if (!systemEntityService.validateEntityExist(systemEntityInfoDTO.getCode())) {
				SystemEntityPO systemEntityPO = new SystemEntityPO();
				BeanUtils.copyProperties(systemEntityInfoDTO, systemEntityPO);
				Long entityId = IDGenerator.newInstance().generate().longValue();
				systemEntityPO.setId(entityId);
				if (null == systemEntityPO.getCid()) {
					systemEntityPO.setCid(companyId);
				}
				systemEntityService.addEntity(systemEntityPO);

				List<SystemCodeInfoDTO> systemCodeInfoDTOList = systemEntityInfoDTO.getSystemCodeInfoDTOList();
				List<SystemCodePO> systemCodePOList = JSONArray.parseArray(JSON.toJSONString(systemCodeInfoDTOList), SystemCodePO.class);
				systemCodePOList.stream().forEach(systemCodePO -> systemCodePO.setId(IDGenerator.newInstance().generate().longValue()));
				for (SystemCodePO systemCodePO : systemCodePOList) {
					Optional<SystemCodePO> systemCodePOOptional = systemCodePOList.stream().filter(item -> item.getCode().equals(systemCodePO.getParentCode())).findFirst();
					if (!systemCodePOOptional.isPresent()) {
						systemCodePO.setFullPath(systemCodePO.getCode());
						systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
						systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
						systemCodePO.setParentId(null);
						systemCodePO.setParentName(systemEntityInfoDTO.getName());
						systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
					} else {
						SystemCodePO systemCodeParent = systemCodePOOptional.get();
						systemCodePO.setParentId(systemCodeParent.getParentId());
						systemCodePO.setParentName(systemCodeParent.getName());
						systemCodePO.setLayNo(systemCodeParent.getLayNo() + 1);
						systemCodePO.setFullPath(systemCodeParent.getFullPath() + "/" + systemCodePO.getCode());
						systemCodePO.setFullPathName(systemCodeParent.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
						systemCodePO.setLayRec(systemCodeParent.getLayRec() + "-" + systemCodePO.getId());
						systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
					}
					if (null == systemCodePO.getCid()) {
						systemCodePO.setCid(companyId);
					}
				}
				systemCodeService.saveBatch(systemCodePOList);
			} else {
				List<SystemCodeInfoDTO> systemCodeInfoDTOList = systemEntityInfoDTO.getSystemCodeInfoDTOList();
				List<SystemCodePO> systemCodePOList = JSONArray.parseArray(JSON.toJSONString(systemCodeInfoDTOList), SystemCodePO.class);
				List<SystemCodePO> systemCodeAddList = new ArrayList<>();
				for (SystemCodePO systemCodePO : systemCodePOList) {
					SystemCodePO currentSystemCode = systemCodeService.queryCodeValueByCode(systemEntityInfoDTO.getCode(), systemCodePO.getCode());
					if (Objects.isNull(currentSystemCode)) {
						if (StringUtils.isEmpty(systemCodePO.getParentCode())) {
							systemCodePO.setFullPath(systemCodePO.getCode());
							systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
							systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
							systemCodePO.setParentId(null);
							systemCodePO.setParentName(systemEntityInfoDTO.getName());
							systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
							systemCodeAddList.add(systemCodePO);
						} else {
							SystemCodePO systemCodeParent = systemCodeService.queryCodeValueByCode(systemEntityInfoDTO.getCode(), systemCodePO.getParentCode());
							if (Objects.isNull(systemCodeParent)) {
								Optional<SystemCodePO> systemCodePOOptional = systemCodePOList.stream().filter(item -> item.getCode().equals(systemCodePO.getParentCode())).findFirst();
								if (!systemCodePOOptional.isPresent()) {
									systemCodePO.setFullPath(systemCodePO.getCode());
									systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
									systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
									systemCodePO.setParentId(null);
									systemCodePO.setParentName(systemEntityInfoDTO.getName());
									systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
									systemCodeAddList.add(systemCodePO);
								} else {
									SystemCodePO systemCodeParentOp = systemCodePOOptional.get();
									systemCodePO.setParentId(systemCodeParentOp.getParentId());
									systemCodePO.setParentName(systemCodeParentOp.getName());
									systemCodePO.setLayNo(systemCodeParentOp.getLayNo() + 1);
									systemCodePO.setFullPath(systemCodeParentOp.getFullPath() + "/" + systemCodePO.getCode());
									systemCodePO.setFullPathName(systemCodeParentOp.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
									systemCodePO.setLayRec(systemCodeParentOp.getLayRec() + "-" + systemCodePO.getId());
									systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
									systemCodeAddList.add(systemCodePO);
								}
							}
						}
					}
				}
				systemCodeService.saveBatch(systemCodeAddList);
			}
		}
	}

	@Override
	@Transactional
	public void upgradeSystemCode(List<SystemEntityInfoDTO> list) {
		List<SystemEntityPO> systemEntityPOList = new ArrayList<>();
		Long companyId = UserContext.getUserContext().getCompanyId();
		for (SystemEntityInfoDTO systemEntityInfoDTO : list) {
			if (!systemEntityService.validateEntityExist(systemEntityInfoDTO.getCode())) {
				SystemEntityPO systemEntityPO = new SystemEntityPO();
				BeanUtils.copyProperties(systemEntityInfoDTO, systemEntityPO);
				Long entityId = IDGenerator.newInstance().generate().longValue();
				systemEntityPO.setId(entityId);
				if (null == systemEntityPO.getCid()) {
					systemEntityPO.setCid(companyId);
				}
				systemEntityService.addEntity(systemEntityPO);

				List<SystemCodeInfoDTO> systemCodeInfoDTOList = systemEntityInfoDTO.getSystemCodeInfoDTOList();
				List<SystemCodePO> systemCodePOList = JSONArray.parseArray(JSON.toJSONString(systemCodeInfoDTOList), SystemCodePO.class);
				if(CollectionUtils.isEmpty(systemCodePOList)){
					continue;
				}
				systemCodePOList.stream().forEach(systemCodePO -> systemCodePO.setId(IDGenerator.newInstance().generate().longValue()));
				List<SystemCodePO> systemCodePOS = Lists.newArrayList();
				for (SystemCodePO systemCodePO : systemCodePOList) {
					Optional<SystemCodePO> systemCodePOOptional = systemCodePOList.stream().filter(item -> item.getCode().equals(systemCodePO.getParentCode())).findFirst();
					if (!systemCodePOOptional.isPresent()) {
						systemCodePO.setFullPath(systemCodePO.getCode());
						systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
						systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
						systemCodePO.setParentId(null);
						systemCodePO.setParentName(systemEntityInfoDTO.getName());
						systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
					} else {
						SystemCodePO systemCodeParent = systemCodePOOptional.get();
						systemCodePO.setParentId(systemCodeParent.getParentId());
						systemCodePO.setParentName(systemCodeParent.getName());
						systemCodePO.setLayNo(systemCodeParent.getLayNo() + 1);
						systemCodePO.setFullPath(systemCodeParent.getFullPath() + "/" + systemCodePO.getCode());
						systemCodePO.setFullPathName(systemCodeParent.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
						systemCodePO.setLayRec(systemCodeParent.getLayRec() + "-" + systemCodePO.getId());
						systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
					}
					systemCodePOS.add(systemCodePO);
				}
				systemCodeService.saveBatch(systemCodePOS);
			} else {
				SystemEntityPO systemEntity = systemEntityService.queryEntityByCode(systemEntityInfoDTO.getCode());
				if (0 == systemEntity.getRowVersion()) {
					UpdateWrapper<SystemEntityPO> updateWrapper = new UpdateWrapper<>();
					updateWrapper.eq(SystemEntityPO.getCodeFieldName(), systemEntity.getCode());
					systemEntityService.update(systemEntity, updateWrapper);

					List<SystemCodeInfoDTO> systemCodeInfoDTOList = systemEntityInfoDTO.getSystemCodeInfoDTOList();
					List<SystemCodePO> systemCodePOList = JSONArray.parseArray(JSON.toJSONString(systemCodeInfoDTOList), SystemCodePO.class);
					if(CollectionUtils.isEmpty(systemCodePOList)){
						continue;
					}
					List<SystemCodePO> addSystemCodePOList = new ArrayList<>();
					List<SystemCodePO> updateSystemCodePOList = new ArrayList<>();
					for (SystemCodePO systemCodePO : systemCodePOList) {
						SystemCodePO currentSystemCode = systemCodeService.queryCodeValueByCode(systemEntity.getCode(), systemCodePO.getCode());
						if (Objects.nonNull(currentSystemCode)) {
							if (0 == currentSystemCode.getRowVersion()) {
								if (StringUtils.isEmpty(currentSystemCode.getParentId())) {
									systemCodePO.setFullPath(systemCodePO.getCode());
									systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
									systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
									systemCodePO.setParentId(null);
									systemCodePO.setParentName(systemEntityInfoDTO.getName());
									systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
									systemCodePO.setId(currentSystemCode.getId());
								} else {
									SystemCodePO systemCodeParent = systemCodeService.getById(currentSystemCode.getParentId());
									if (Objects.isNull(systemCodeParent)) {
										Optional<SystemCodePO> systemCodePOOptional = systemCodePOList.stream().filter(item -> item.getCode().equals(systemCodePO.getParentCode())).findFirst();
										if (!systemCodePOOptional.isPresent()) {
											systemCodePO.setFullPath(systemCodePO.getCode());
											systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
											systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
											systemCodePO.setParentId(null);
											systemCodePO.setParentName(systemEntityInfoDTO.getName());
											systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
											systemCodePO.setId(currentSystemCode.getId());
										} else {
											SystemCodePO systemCodeParent1 = systemCodePOOptional.get();
											systemCodePO.setParentId(systemCodeParent1.getParentId());
											systemCodePO.setParentName(systemCodeParent1.getName());
											systemCodePO.setLayNo(systemCodeParent1.getLayNo() + 1);
											systemCodePO.setFullPath(systemCodeParent1.getFullPath() + "/" + systemCodePO.getCode());
											systemCodePO.setFullPathName(systemCodeParent1.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
											systemCodePO.setLayRec(systemCodeParent1.getLayRec() + "-" + systemCodePO.getId());
											systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
											systemCodePO.setId(currentSystemCode.getId());
										}
									} else {
										systemCodePO.setParentId(systemCodeParent.getId());
										systemCodePO.setParentName(systemCodeParent.getName());
										systemCodePO.setLayNo(systemCodeParent.getLayNo() + 1);
										systemCodePO.setFullPath(systemCodeParent.getFullPath() + "/" + systemCodePO.getCode());
										systemCodePO.setFullPathName(systemCodeParent.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
										systemCodePO.setLayRec(systemCodeParent.getLayRec() + "-" + systemCodePO.getId());
										systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
										systemCodePO.setId(currentSystemCode.getId());
									}
								}
							}
							updateSystemCodePOList.add(systemCodePO);
						} else {
							if (StringUtils.isEmpty(systemCodePO.getParentCode())) {
								systemCodePO.setFullPath(systemCodePO.getCode());
								systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
								systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
								systemCodePO.setParentId(null);
								systemCodePO.setParentName(systemEntityInfoDTO.getName());
								systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
							} else {
								SystemCodePO systemCodeParent = systemCodeService.queryCodeValueByCode(systemEntity.getCode(), systemCodePO.getParentCode());
								if (Objects.isNull(systemCodeParent)) {
									Optional<SystemCodePO> systemCodePOOptional = systemCodePOList.stream().filter(item -> item.getCode().equals(systemCodePO.getParentCode())).findFirst();
									if (!systemCodePOOptional.isPresent()) {
										systemCodePO.setFullPath(systemCodePO.getCode());
										systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
										systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
										systemCodePO.setParentId(null);
										systemCodePO.setParentName(systemEntityInfoDTO.getName());
										systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
									} else {
										SystemCodePO systemCodeParent1 = systemCodePOOptional.get();
										systemCodePO.setParentId(systemCodeParent1.getParentId());
										systemCodePO.setParentName(systemCodeParent1.getName());
										systemCodePO.setLayNo(systemCodeParent1.getLayNo() + 1);
										systemCodePO.setFullPath(systemCodeParent1.getFullPath() + "/" + systemCodePO.getCode());
										systemCodePO.setFullPathName(systemCodeParent1.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
										systemCodePO.setLayRec(systemCodeParent1.getLayRec() + "-" + systemCodePO.getId());
										systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
									}
								} else {
									systemCodePO.setParentId(systemCodeParent.getId());
									systemCodePO.setParentName(systemCodeParent.getName());
									systemCodePO.setLayNo(systemCodeParent.getLayNo() + 1);
									systemCodePO.setFullPath(systemCodeParent.getFullPath() + "/" + systemCodePO.getCode());
									systemCodePO.setFullPathName(systemCodeParent.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
									systemCodePO.setLayRec(systemCodeParent.getLayRec() + "-" + systemCodePO.getId());
									systemCodePO.setEntityCode(systemEntityInfoDTO.getCode());
								}
							}
							addSystemCodePOList.add(systemCodePO);
						}
					}
					systemCodeService.updateBatchById(updateSystemCodePOList);
					systemCodeService.saveBatch(addSystemCodePOList);
				}
			}
		}
	}

	private void convertToSystemCode(SystemCodePO systemCodePO) {
		QueryWrapper<SystemCodePO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(SystemCodePO.getEntityCodeFieldName(), systemCodePO.getEntityCode());
		// 如果创建的值的默认属性设置为是,则其他的值都为否
		if (1 == systemCodePO.getDefaultFlag()) {
			List<SystemCodePO> systemCodePOList = systemCodeService.list(queryWrapper);
			for (SystemCodePO systemCode : systemCodePOList) {
				systemCode.setDefaultFlag(0);
			}
			systemCodeService.updateBatchById(systemCodePOList);
		}
		Long id = IDGenerator.newInstance().generate().longValue();
		systemCodePO.setId(id);
		if (systemCodePO.getParentId() == null) {
			systemCodePO.setLayNo(1);
			systemCodePO.setFullPath(systemCodePO.getCode());
			systemCodePO.setFullPathName(systemCodeService.queryDisplayName(systemCodePO));
			systemCodePO.setLayRec(String.valueOf(id));
			queryWrapper.isNull(SystemCodePO.getParentIdFieldName());
		} else {
			SystemCodePO parentSystemCodePo = systemCodeService.getById(systemCodePO.getParentId());
			systemCodePO.setLayNo(parentSystemCodePo.getLayNo() + 1);
			systemCodePO.setFullPath(parentSystemCodePo.getFullPath() + "/" + systemCodePO.getCode());
			systemCodePO.setFullPathName(parentSystemCodePo.getFullPathName() + "/" + systemCodeService.queryDisplayName(systemCodePO));
			systemCodePO.setLayRec(parentSystemCodePo.getId() + "-" + id);
			queryWrapper.eq(SystemCodePO.getParentIdFieldName(), systemCodePO.getParentId());
		}

		List<SystemCodePO> brotherSystemCodeList = systemCodeService.list(queryWrapper);
		if (CollectionUtils.isEmpty(brotherSystemCodeList)) {
			systemCodePO.setSort(1.0);
		} else {
			systemCodePO.setSort(brotherSystemCodeList.size() + 1.0);
		}
	}

	@Override
	public void batchDeleteSystemCode(String appId) {
		List<String> moduleIdList = moduleService.queryModuleByAppId(appId);
		for (String moduleId : moduleIdList) {
			systemEntityService.deleteEntityByModuleId(moduleId);
		}
	}

	@Override
	public ListResult<SystemCodeResultDTO> queryValueListByEntityCodes(String entityCodes) {
		String[] entityCodeArray = entityCodes.split(",");
		List<String> entityCodeList = new ArrayList<>();
		if (entityCodeArray != null) {
			entityCodeList = Arrays.asList(entityCodeArray);
		}
		List<SystemCodePO> systemCodePOList = systemCodeService.queryValueListByEntityCodes(entityCodeList);
		List<SystemCodeResultDTO> systemCodeResultDTOList = JSONArray.parseArray(JSON.toJSONString(systemCodePOList), SystemCodeResultDTO.class);
		return new ListResult<>(systemCodeResultDTOList);
	}
}