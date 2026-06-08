package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.*;
import com.supcon.supfusion.custon.property.dao.mappers.ExtraViewMapper;
import com.supcon.supfusion.custon.property.dao.mappers.FastQueryJsonMapper;
import com.supcon.supfusion.custon.property.dao.mappers.ViewMapper;
import com.supcon.supfusion.custon.property.dao.utils.SerializeUitls;
import com.supcon.supfusion.custon.property.server.ButtonService;
import com.supcon.supfusion.custon.property.server.EcConfigService;
import com.supcon.supfusion.custon.property.server.EventService;
import com.supcon.supfusion.custon.property.server.FieldService;
import com.supcon.supfusion.custon.property.server.bo.ViewBO;
import com.supcon.supfusion.custon.property.server.utils.EcExtraViewIntegrationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class EcConfigServiceImpl implements EcConfigService {

    @Autowired
    private FieldService fieldService;
    @Autowired
    private EventService eventService;
    @Autowired
    private ButtonService buttonService;
    @Autowired
    private ViewMapper viewMapper;
    @Autowired
    private ExtraViewMapper extraViewMapper;
    @Autowired
    private FastQueryJsonMapper fastQueryJsonMapper;
    @Autowired
    private EcExtraViewIntegrationUtils ecExtraViewIntegrationUtils;

    /**
     * 获取完整的视图配置信息
     *
     * @param object
     * @return
     */
    @Transactional
    @Override
    public String getEcFullConfig(Object object) {

        Map<String, List<?>> infoMap = getFieldInfoMap(object);
//        if (object instanceof View) {
//            View view = (View) object;
//            ViewBO viewBO = new ViewBO();
//            BeanUtils.copyProperties(view,viewBO);
//            viewBO.setExtraView(extraViewMapper.selectById(view.getExtraViewCode()));
//            viewBO.setFastQueryJsons(fastQueryJsonMapper.selectList(new LambdaQueryWrapper<FastQueryJson>().eq(FastQueryJson::getViewCode,view.getCode())));
//            if (null != view.getIsShadow() && view.getIsShadow() && null != view.getShadowView()) {
//                if(null != view.getShadowView().getExtraViewCode()){
//                    ev.setConfig(view.getShadowView().getExtraView().getConfig());
//                }
//            }
//        }
        String evConfig = ecExtraViewIntegrationUtils.ecExtraViewIntegrationBuild(object, infoMap);

        return evConfig;
    }


    /**
     * 根据DataGrid获取关联视图的字段属性Map
     *
     * @param dataGrid
     * @return
     */
    @Transactional
    @Override
    public String getViewFieldConfigByDataGrid(DataGrid dataGrid) {
        String viewCode = dataGrid.getViewCode();
        View view = viewMapper.selectById(viewCode);
        if (view != null) {
            return getFieldsConfig(view);
        }

        return null;
    }

    /**
     * 获取视图字段属性信息
     *
     * @param object
     * @return xml String
     */
    @Transactional
    @Override
    public String getFieldsConfig(Object object) {
        Map<String, List<?>> infoMap = getFieldInfoMap(object);
        if (infoMap != null && !infoMap.isEmpty()) {
            Map<String, List<?>> attMap = new EcExtraViewIntegrationUtils().getFieldListMap(infoMap);
            if (attMap != null && !attMap.isEmpty()) {
                return SerializeUitls.serializeAsXml(attMap);
            }
        }
        return null;
    }

    /**
     * @param object
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Map<String, List<?>> getFieldInfoMap(Object object) {
        Map<String, List<?>> infoMap = new HashMap<String, List<?>>();
        String config = "";
        View view = null;
        DataGrid dataGrid = null;
        if (object instanceof View) {
            view = (View) object;
            String ev = view.getExtraViewCode();
            if(null != view.getIsShadow() ) {
                String shadowViewCode = view.getShadowViewCode();
                View shadowView = viewMapper.selectById(shadowViewCode);
                if (shadowView != null){
                    ev = shadowView.getExtraViewCode();
                }
            }
            ExtraView extraView = extraViewMapper.selectById(ev);
            if (null != extraView && null != extraView.getConfig() && extraView.getConfig().length() > 0) {
                config = extraView.getConfig();
            }
        } else if (object instanceof DataGrid) {
            dataGrid = (DataGrid) object;
            if (null != dataGrid.getConfig() && dataGrid.getConfig().length() > 0) {
                config = dataGrid.getConfig();
            }
        }

        Map configMap = (Map) SerializeUitls.deserialize(config);
        if (configMap != null && !configMap.isEmpty()) {
            Map layout = (Map) configMap.get("layout");
            if (layout != null && !layout.isEmpty()) {
                if (layout.get("layoutCode") != null && layout.get("layoutCode").toString().length() > 0) {
                    String layoutCode = layout.get("layoutCode").toString();
                    List<Event> events = eventService.getEventsByLayoutCode(layoutCode);
                    if (events != null && !events.isEmpty()) {
                        infoMap.put("events", events);
                    }
                }
            }
        }

        List<Field> fields = null;

        List<Button> buttons = null;

        if (object instanceof View) {
            String code = view.getCode();
            if(null != view.getIsShadow() && StringUtils.isNotBlank(view.getShadowViewCode())) {
                code = view.getShadowViewCode();
            }
            fields = fieldService.getFields(code);
            buttons = buttonService.getButtons(code);
        } else if (object instanceof DataGrid) {
            fields = fieldService.getFieldsByDataGridCode(dataGrid.getCode());
            buttons = buttonService.getButtonsByDataGridCode(dataGrid.getCode());
        }
        if (fields != null && !fields.isEmpty()) {
            infoMap.put("fields", fields);
        }
        if (buttons != null && !buttons.isEmpty()) {
            infoMap.put("buttons", buttons);
        }
        return infoMap;
    }
}
