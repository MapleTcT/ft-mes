package com.supcon.supfusion.auth.service;


import com.supcon.supfusion.auth.service.bo.IdentityCenterConfigBO;
import com.supcon.supfusion.auth.service.bo.RegisterOauthClientBo;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhb
 * @since 2021-05-08
 */

public interface IdentityCenterConfigService {

	/**
	 * 根据id查询IdentityCenterConfig
	 *
	 * @return
	 */
	IdentityCenterConfigBO getIdentityCenterConfigById(Long id) throws Exception;

	/**
	 * 查询IdentityCenterConfig列表
	 *
	 * @param keyword
	 * @return
	 */
	List<IdentityCenterConfigBO> listIdentityCenterConfig(String keyword) throws Exception;


	/**
	 * 新增IdentityCenterConfig
	 *
	 * @param identityCenterConfig
	 */
	Integer addIdentityCenterConfig(IdentityCenterConfigBO identityCenterConfig) throws Exception;

	/**
	 * 更新IdentityCenterConfig
	 */
	Integer updateIdentityCenterConfig(IdentityCenterConfigBO identityCenterConfig) throws Exception;

	/**
	 * 根据id物理删除IdentityCenterConfig
	 */
	Integer deleteIdentityCenterConfigById(Long id) throws Exception;

	Integer updateIdentityCenterConfigStatus(IdentityCenterConfigBO target);

	Integer deleteIdentityCenterConfigByIds(Long[] id);

	Map<String, List<IdentityCenterConfigBO>> getCurrentAuthConfig();


	String registerOauth2Client(RegisterOauthClientBo apply,String host);

}
