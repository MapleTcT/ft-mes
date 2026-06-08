package com.supcon.supfusion.rbac.webapi;


import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 自定义权限表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class CustomPermissionController extends BaseController {

}

