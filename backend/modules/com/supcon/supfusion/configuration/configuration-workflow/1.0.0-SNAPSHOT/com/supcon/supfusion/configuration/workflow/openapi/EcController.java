package com.supcon.supfusion.configuration.workflow.openapi;

import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.workflow.wrapper.ViewWrapper;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/19
 */
@Slf4j
@Controller
public class EcController extends BaseController {

    private static final ViewWrapper viewWrapper = new ViewWrapper();

    @Autowired
    private ViewService viewService;
    @Autowired
    private EntityService entityService;

    @RequestMapping(value = "/ec/entity/view-select")
    public String viewSelect(ModelMap map, boolean multiSelect, String callBackFuncName,
                             String env, @RequestParam("entity.code") String entityCode,
                             boolean workflowViewChoose, boolean unassignStaffSupport) {
        Entity entity = entityService.getEntity(entityCode);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("entity", entity);
        map.addAttribute("workflowViewChoose", workflowViewChoose);
        map.addAttribute("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("env", env);
        return "workflow/view-select";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/entity/view-list")
    public Page viewList(@Nullable Boolean entry, @RequestParam("entity.code") String entityCode) {
        ViewType[] vts;
        if (null != entry && entry) {
            vts = new ViewType[] { ViewType.EDIT };
        } else {
            vts = new ViewType[] { ViewType.EDIT, ViewType.VIEW };
        }
        Page<View> viewPage = new Page<>();
        Entity entity = new Entity();
        entity.setCode(entityCode);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        List<View> views = viewService.findViews(entity, 1, 4, 0, vts);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        Iterator<View> iterator = views.iterator();
        while (iterator.hasNext()) {
            View v = iterator.next();
            Model assModel = v.getAssModel();
            // 过滤非主模型视图
            if (null == assModel || !assModel.getIsMain()) {
                iterator.remove();
            }
        }
        viewPage.setResult(views);
        return viewWrapper.e2vPage(viewPage);
    }

}
