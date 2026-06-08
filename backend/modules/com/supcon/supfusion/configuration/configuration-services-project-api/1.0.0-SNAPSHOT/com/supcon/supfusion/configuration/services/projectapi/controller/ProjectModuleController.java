
package com.supcon.supfusion.configuration.services.projectapi.controller;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.ModelMap;
import javax.servlet.http.HttpSession;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.configuration.services.entity.*;

/**
 * 此Controller控制的CRUD及生成编译打包部署.
 *
 * @author songjiawei,wangting
 *
 */
@Slf4j
@Controller
public class ProjectModuleController extends ConfigurationBaseController {
    /**
     * 进入微服务模块管理主框架
     *
     * @return
     */
    @RequestMapping(value = "/ec/project/module/msManage")
    public String msManage(ModelMap map) throws Exception {
        HttpSession session = getRequest().getSession();
        boolean isDev = PropertyHolder.isDev();
        String isMsService ="false";
        map.addAttribute("isMsService", isMsService);
        map.addAttribute("isDev", isDev);
        return "project/module/manage";
    }

}
