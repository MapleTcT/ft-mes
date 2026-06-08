/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service.rule;

import java.util.Map;

import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;

/**
 * @author: zhuangmh
 * @date: 2020年9月22日 上午11:10:23
 */
public interface RuleService<P, T> {
    
    T parse(P p, String processId);
    
    void setTemporaryPersonCache(Map<Long, PersonDetailDTO> personMap);
}
