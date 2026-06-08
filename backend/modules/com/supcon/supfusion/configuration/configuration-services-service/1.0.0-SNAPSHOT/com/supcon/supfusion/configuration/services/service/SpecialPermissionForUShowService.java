package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.SpecialPermission;
import com.supcon.supfusion.configuration.services.entity.SpecialPermissionForUShow;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface SpecialPermissionForUShowService {
	public List<SpecialPermissionForUShow>  findAllShowInfo(Long userId, Long operateId, String specialPermissionCode);
	
	/**
	 * JDBC查询
	 * 
	 * @param page
	 * @param queryResultSQL
	 * @return
	 * @throws SQLException
	 */
	public Page<Map<String, Object>> findRecordPage(Page<Map<String, Object>> page, String queryResultSQL, String queryPageSQL, Object... objects)
			throws SQLException;
	/**
	 * 删除历史数据
	 * @param userId
	 * @param operateId
	 * @param specialPermissionCode
	 */
	public void  deleteUserShowHistoryData(Long userId,Long operateId,String specialPermissionCode); 
	
	public List<String> getConfigSpecialPermissonCode(Long userId, Long operateId,String configedCodes);
	
	public SpecialPermission loadSpecialPermission(String  code);

	void save(SpecialPermissionForUShow specialPermissionForUShow);
}
