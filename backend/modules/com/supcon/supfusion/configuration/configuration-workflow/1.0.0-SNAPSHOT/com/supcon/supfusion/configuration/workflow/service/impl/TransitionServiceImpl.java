/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.workflow.service.impl;

import com.supcon.supfusion.base.entities.Transition;
import com.supcon.supfusion.configuration.workflow.dao.TransitionDaoImpl;
import com.supcon.supfusion.configuration.workflow.service.TransitionService;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransitionServiceImpl implements TransitionService {

	@Autowired
	private TransitionDaoImpl transitionDao;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Transition getTransition(String code, Long deploymentId) {
		List<Transition> list = transitionDao.findByCriteria(Restrictions.eq("code", code), Restrictions.eq("deploymentId", deploymentId));
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	public Transition getTransition(Long id) {
		return transitionDao.get(id);
	}

	@Override
	@Transactional
	public void save(Transition t) {
		Transition old = getTransition(t.getCode(), t.getDeploymentId());
		if (old != null) {
			old.setName(t.getName());
			old.setType( (t.getType()!=null)?t.getType():0);
			old.setFromNodeCode(t.getFromNodeCode());
			old.setToNodeCode(t.getToNodeCode());
			old.setSelectStaff((t.getSelectStaff()!=null)?t.getSelectStaff():"");
			old.setRequiredStaff((t.getRequiredStaff()!=null&&t.getRequiredStaff())?true:false);
			old.setExpression((t.getExpression()!=null)?t.getExpression():"");
			old.setRouteSequence((t.getRouteSequence()!=null)?t.getRouteSequence():0);
			transitionDao.save(old);
		} else {
			transitionDao.save(t);
		}
	}
	
	@Override
	public void delete(Transition t){
		t = getTransition(t.getCode(), t.getDeploymentId());
		transitionDao.delete(t);
	}

	@Override
	@Transactional(readOnly=true,propagation= Propagation.SUPPORTS)
	public List<Transition> getOutTransition(long deploymentId, String taskCode) {
		String hql = "from Transition where deploymentId=? and fromNodeCode = ?";
		Object[] params = new Object[2];
		params[0] = deploymentId;
		params[1] = taskCode;
		List<Transition> transitionList = transitionDao.findByHql(hql, params);
		return transitionList;
	}

}
