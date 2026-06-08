package com.supcon.supfusion.custon.property.server;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.dao.entity.DataGrid;
import com.supcon.supfusion.custon.property.dao.entity.View;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyViewBO;
import com.supcon.supfusion.custon.property.server.bo.ViewEnabledStatusCodeBO;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ViewServiceFoundation  extends IService<View> {
    List<View> findViews(String entityCode, ViewType... viewTypes);

    List<CustomPropertyViewBO> findCustomPropertyViewMappings(View view, Boolean isDataGrid);

    View getView(String viewCode);

    String findPropDisplayName(String propLayRec, String modelCode);

    List<CustomPropertyViewBO> findDgCustomePropertyByDgCode(DataGrid datagridCode, ViewType type);

    List<DataGrid> getDataGrids(String viewCode);

    DataGrid getDataGrid(String datagridCode);

    void saveCustomPropertyViewMapping(CustomPropertyViewBO customPropertyViewMapping);

    void showProperty(List<ViewEnabledStatusCodeBO> codes, List<Long> ids, Boolean enabled);

    List<View> findViewsByModelCode(String modelCode, ViewType... viewTypes);

}
