package com.supcon.supfusion.systemcode.service.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemcode.api.SystemEntityApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityAddDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityResultDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityUpdateDTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.service.SystemEntityService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author 
 * @date 20-5-11 下午14:30
 */
@ServiceApiService
public class SystemEntityApiServiceImpl extends BaseController implements SystemEntityApiService {

	@Autowired
	private SystemEntityService systemEntityService;

	@Override
	public PageResult<SystemEntityResultDTO> queryEntities(String keyword, String moduleId, Integer current, Integer pageSize) {
		PageResult<SystemEntityPO> systemEntityPageResult = systemEntityService.queryEntities(keyword, moduleId, current, pageSize);
		List<SystemEntityResultDTO> voList = new ArrayList<>(systemEntityPageResult.getList().size());
		systemEntityPageResult.getList().stream().forEach(po -> {
			SystemEntityResultDTO vo = new SystemEntityResultDTO();
			BeanUtils.copyProperties(po, vo);
			voList.add(vo);
		});
		return new PageResult<>(voList, systemEntityPageResult.getPagination().getTotal(), systemEntityPageResult.getPagination().getPageSize(), systemEntityPageResult.getPagination().getCurrent());
	}

	public Result<SystemEntityResultDTO> queryEntityByCode(String code) {
		SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(code);
		SystemEntityResultDTO systemEntityResultDTO = new SystemEntityResultDTO();
		BeanUtils.copyProperties(systemEntityPO, systemEntityResultDTO);
		return Result.data(systemEntityResultDTO);
	}

	@Override
	public void addEntity(SystemEntityAddDTO systemEntityAddDTO) {
		if (!Pattern.matches(Constants.PATTERN_CODE, systemEntityAddDTO.getCode())) {
			throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
		}
		SystemEntityPO systemEntityPO = new SystemEntityPO();
		BeanUtils.copyProperties(systemEntityAddDTO, systemEntityPO);
		systemEntityService.addEntityForRpc(systemEntityPO);

	}

	@Override
	public void updateEntity(SystemEntityUpdateDTO systemEntityUpdateDTO) {
		if (!Pattern.matches(Constants.PATTERN_CODE, systemEntityUpdateDTO.getCode())) {
			throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
		}
		SystemEntityPO systemEntityPO = new SystemEntityPO();
		BeanUtils.copyProperties(systemEntityUpdateDTO, systemEntityPO);
		systemEntityService.updateEntity(systemEntityPO);
	}

	@Override
	public void deleteEntityByCode(String code) {
		systemEntityService.deleteEntityByCode(code);
	}

	@Override
	public void batchDeleteEntities(String codes) {
		if (StringUtils.isBlank(codes)) {
			return;
		}
		String[] codeArray = codes.split(",");
		if (codeArray == null || codeArray.length <= 0) {
			return;
		}
		List<String> codeList = Arrays.asList(codeArray);
		systemEntityService.batchDeleteEntities(codeList);
	}

	@Override
	public void deleteEntityByModuleId(String moduleId) {
		systemEntityService.deleteEntityByModuleId(moduleId);
	}
	@Override
	public ListResult<SystemEntityResultDTO> getEntityByModuleIds(List<String> moduleIds) {
		List<SystemEntityPO>  systemEntityPOs=systemEntityService.getEntityByModuleIds(moduleIds);
		ArrayList<SystemEntityResultDTO> systemEntityResultDTOs = new ArrayList<>();
		systemEntityPOs.forEach(systemEntityPO -> {
			SystemEntityResultDTO systemEntityResultDTO = new SystemEntityResultDTO();
			BeanUtils.copyProperties(systemEntityPO, systemEntityResultDTO);
			systemEntityResultDTOs.add(systemEntityResultDTO);
		});
		return new ListResult<>(systemEntityResultDTOs);
	}

	@Override
	public ListResult<SystemEntityResultDTO> getSystemBaseEntity() {
		List<SystemEntityPO>  systemEntityPOs=systemEntityService.getEntityByModuleId("sys");
		ArrayList<SystemEntityResultDTO> systemEntityResultDTOs = new ArrayList<>();
		systemEntityPOs.forEach(systemEntityPO -> {
			SystemEntityResultDTO systemEntityResultDTO = new SystemEntityResultDTO();
			BeanUtils.copyProperties(systemEntityPO, systemEntityResultDTO);
			systemEntityResultDTOs.add(systemEntityResultDTO);
		});
		return new ListResult<>(systemEntityResultDTOs);
	}

	@Override
	public PageResult<SystemEntityResultDTO> queryEntitiesByModuleIds(String moduleIds, Integer current, Integer pageSize) {
		String[] moduleIdArray = moduleIds.split(",");
		List<String> moduleIdList = new ArrayList<>();
		if (moduleIdArray != null) {
			moduleIdList = Arrays.asList(moduleIdArray);
		}
		PageResult<SystemEntityPO> systemEntityPageResult = systemEntityService.queryEntitiesByModuleIds(moduleIdList, current, pageSize);

		// 系统编码数据转换为接口返回数据格式
		List<SystemEntityPO> systemEntityPOList = (List<SystemEntityPO>) systemEntityPageResult.getList();
		List<SystemEntityResultDTO> systemEntityResultDTOList = JSONArray.parseArray(JSON.toJSONString(systemEntityPOList), SystemEntityResultDTO.class);

		return new PageResult<>(systemEntityResultDTOList, systemEntityPageResult.getPagination().getTotal(), systemEntityPageResult.getPagination().getPageSize(), systemEntityPageResult.getPagination().getCurrent());
	}

}
