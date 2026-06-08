package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.SpecialPermission;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface SpecialPermissionService {

	void saveSpecialPermission(SpecialPermission specialPermission);

	/**
	 * 保存xml字符串 
	 * @param xmlSource
	 * @param modelCode
	 */
	public  void saveXmlSource(String xmlSource, String modelCode);
	
	
	/**
	 * 根据模型编码得到关联配置
	 * @param modelCode
	 * @return
	 */
	public List<SpecialPermission> findAllSpecialPermissionByModelCode(String modelCode);
	
	/**
	 * 根据模型编码生成条件预览
	 * @param modelCode
	 * @return
	 */
	public String generatePreview(String modelCode);
	
	/**
	 * 得到该属性对应的systemEntityCode
	 * @param p
	 * @return
	 */
	public String getSystemEntityCodeByProperty(Property p);
	
	/**
	 * 得到关联模型和字段信息
	 * @return
	 */
	public Map<String, Object> getObjectProperties(Model model);
	/**
	 * 
	 * @param code
	 * @param key
	 * @return
	 */
	public List<SpecialPermission> findSpecialPermissionsByCode(String code, String key);

	public  boolean  checkWhetherIsExist(String propertyCode);
	
	public  void   deleteSpcialPermissionByModelCode(String modelCode);
	
	/**
	 * 根据业务主键列表获取Map
	 * @param businessKeyName TODO
	 * @param businessKeys 业务主键列表
	 * @return Map<String,String> key：idmap  value：id;  key：count  value：idNum
	 */
	Map<String, Object> getMainDisplayMap(Serializable mainDisplayName, Serializable businessKeyName, List<Serializable> mainDisplayKeys);


	SpecialPermission load(String specialPermissionCode);
}
