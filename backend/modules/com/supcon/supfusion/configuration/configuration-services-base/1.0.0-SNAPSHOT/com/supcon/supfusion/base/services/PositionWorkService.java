package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.PositionWork;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;


public interface PositionWorkService {

    Page<PositionWork> getByPage(Page<PositionWork> page, DetachedCriteria detachedCriteria);
}
