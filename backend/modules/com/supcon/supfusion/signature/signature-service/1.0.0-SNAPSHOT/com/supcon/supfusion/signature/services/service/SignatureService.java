package com.supcon.supfusion.signature.services.service;

import com.supcon.supfusion.signature.dao.entity.WfTask;
import com.supcon.supfusion.signature.dao.entity.WfTransition;

import java.util.Map;

/**
 * @author zhang yafei
 */
public interface SignatureService {
    Boolean getDeployment(Long deploymentId);

    Object[] getSignatureEnable(Long fvTableInfoId, String modelCode);

    WfTask getTask(String activityName, Long deploymentId);

    WfTransition getTransition(String transitionCode, Long deploymentId);
}
