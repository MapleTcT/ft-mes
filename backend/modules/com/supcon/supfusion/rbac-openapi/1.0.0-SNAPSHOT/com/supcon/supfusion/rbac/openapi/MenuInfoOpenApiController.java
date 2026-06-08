package com.supcon.supfusion.rbac.openapi;


import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 菜单表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Setter
@Getter
@Validated
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
public class MenuInfoOpenApiController extends BaseController {
    @Autowired
    private IMenuInfoService menuInfoService;

    /**
     * @description: 根据json更新创建菜单、操作、URL
     * @param: json
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @PostMapping("/menuInfo/saveByJson")
    public void saveBachUrlByJson(@RequestBody String json){
        menuInfoService.saveBachUrlByJson(json);
    }

    /**
     * @description: 根据xml更新创建菜单、操作、URL
     * @param: json
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @PostMapping("/menuInfo/saveByXml")
    public void saveBachUrlByXml(@RequestBody String xml) throws Exception {
        menuInfoService.saveBachUrlByXml(xml,null);
    }
}

