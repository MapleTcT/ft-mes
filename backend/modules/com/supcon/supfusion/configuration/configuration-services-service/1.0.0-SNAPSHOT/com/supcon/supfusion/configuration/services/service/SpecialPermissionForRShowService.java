package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.SpecialPermissionForRShow;

import java.util.List;

public interface SpecialPermissionForRShowService {
	/**
	 * 查询得到关联的展示信息
	 * @param roleId
	 * @param operateId
	 * @param specialPermissionCode
	 * @return
	 */
	public List<SpecialPermissionForRShow>  findAllShowInfo(Long roleId, Long operateId, String specialPermissionCode);

	/**
	 * 删除历史数据
	 * @param roleId
	 * @param operateId
	 * @param specialPermissionCode
	 */
	public void  deleteRoleShowHistoryData(Long roleId,Long operateId,String specialPermissionCode); 

	public List<String>  getConfigSpecialPermissonCode(Long roleId,Long operateId);

	void save(SpecialPermissionForRShow specialPermissionForRShow);
}
