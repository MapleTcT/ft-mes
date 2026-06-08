package com.supcon.supfusion.rbac.webapi;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "菜单操作URL关联相关接口")
public class MenuOperateCodeUrlRefController extends BaseController {

    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;

    /**
     * @description: 刷新URL到redis中
     * @param: appName
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/mocUrl/updateUrl")
    @ApiOperation(value = "刷新URL到redis中")
    @ApiImplicitParams({
            @ApiImplicitParam(name="appName",value="服务名",required=false,paramType="query"),
    })
    public void updateUrl(@RequestParam(value = "appName",required = false) String appName){
        menuOperateCodeUrlRefService.updateUrl(appName);
    }
}

