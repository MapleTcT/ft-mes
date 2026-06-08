/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

import java.util.List;

/**
 * 
 * @author 曹伟彪
 * 
 */

public interface PositionService {

	Position load(Long id);

	List<Position> getTreeChildren(Long positionId, Long companyId);

	List<Position> getAssignPositions(String assignPositions);

    List<Position> getAllParents(String positionLayRec);

    Page getByPage(Page page, DetachedCriteria detachedCriteria);
}
