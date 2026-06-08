package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.util.List;

/**
 * 流程相关操作.
 * 
 * @author songjiawei
 * 
 */
public interface ProcessService {


	List<Deployment> findDeployments(String... entityCodes);
	Page<Deployment> findDeployments(Page<Deployment> page, String entityCode);
	Page<Deployment> findCurrentDeployments(Page<Deployment> page, String entityCode);

	public void deleteFlow(Deployment deployment, boolean flag, boolean flowFlag);

	public boolean repeat(String processKey, String entityCode);

	Deployment getDeployment(long id);

	String powerXml(Deployment dp, String powerXml);

	String getTranstionSelectStaffs(Long deploymentId);

	List<Staff> getSupervises(Long deploymentId);

	void saveDeployment(Deployment deployment, String operatePower, String actives, String updatePowerString, String superviseNamesMultiIDs,
						String selectStaffs, String linkRangeChage);

	String handleFlowXml(String flowXml);

	String analyticXml(String xmlStr, String language, String... env);

}
