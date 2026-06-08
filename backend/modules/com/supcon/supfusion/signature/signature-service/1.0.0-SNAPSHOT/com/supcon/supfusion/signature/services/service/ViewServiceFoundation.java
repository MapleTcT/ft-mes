package com.supcon.supfusion.signature.services.service;

import com.supcon.supfusion.signature.dao.entity.Entity;
import com.supcon.supfusion.signature.dao.entity.View;
import com.supcon.supfusion.signature.dao.enums.ViewType;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ViewServiceFoundation {

    /**
     *
     * @param entityCode 视图code
     * @param viewTypes 视图类型
     * @return
     */
    List<View> findViews(String entityCode, ViewType... viewTypes);

    View getView(String viewCode);
}
