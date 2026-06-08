package com.supcon.supfusion.configuration.workflow.service;


import com.supcon.supfusion.base.entities.Transition;

import java.util.List;

/**
 * 迁移线
 * 
 * @author shichenwei
 * @version $Id$
 */
public interface TransitionService {
	
	public Transition getTransition(String code, Long deploymentId);
	/**
	 * 保存迁移线
	 * @param t
	 */
	public void save(Transition t);
	/**
	 * 删除迁移线
	 * @param t
	 */
	public void delete(Transition t);
	/**
	 * 找活动出去的迁移线
	 * @param deploymentId
	 * @param taskCode
	 * @return
	 */
	public List<Transition> getOutTransition(long deploymentId,String taskCode);

}
