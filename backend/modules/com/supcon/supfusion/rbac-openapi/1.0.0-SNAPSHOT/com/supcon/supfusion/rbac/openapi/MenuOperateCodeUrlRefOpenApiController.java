package com.supcon.supfusion.rbac.openapi;


import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 菜单操作编码URL关联表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class MenuOperateCodeUrlRefOpenApiController extends BaseController {

}

