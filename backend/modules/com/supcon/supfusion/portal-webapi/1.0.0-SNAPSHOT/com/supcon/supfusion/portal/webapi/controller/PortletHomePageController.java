package com.supcon.supfusion.portal.webapi.controller;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.portal.service.PortletCodeService;
import com.supcon.supfusion.portal.service.PortletHomePageService;
import com.supcon.supfusion.portal.service.bo.EcMyPortletBO;
import com.supcon.supfusion.portal.service.bo.EcPortletBO;
import com.supcon.supfusion.portal.service.entity.MyPortlet;
import com.supcon.supfusion.portal.webapi.vo.portlet.EcMyPortletVO;
import com.supcon.supfusion.portal.webapi.vo.portlet.EcPortletVO;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author kk.C
 * @Date 2020/11/30 10:25
 */
@Api(tags = "门户首页相关接口")
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "portal" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
@RestController
public class PortletHomePageController {

    @Autowired
    private PortletHomePageService portletHomePageService;

    @ApiOperation(value = "获取用户首页可添加的门户组")
    @GetMapping("/homePage/portal")
    @ResponseStatus(HttpStatus.OK)
    @ApiImplicitParams({@ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Long", paramType = "query")})
    public Result<List<EcPortletVO>> getHomePagePortal() {
        List<EcPortletBO> homePagePortletResult = portletHomePageService.queryHomePagePortlet();
        List<EcPortletVO> ecPortletVOList = homePagePortletResult.stream().map(ecPortletBO -> {
            EcPortletVO ecPortletVO = new EcPortletVO();
            BeanUtils.copyProperties(ecPortletBO, ecPortletVO);
            return ecPortletVO;
        }).collect(Collectors.toList());
        return new Result<>(ecPortletVOList);
    }

    @ApiOperation(value = "获取用户首页自定义组态门户")
    @GetMapping("/homePage/myPortal")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<MyPortlet>> getMyPortal() {
        List<MyPortlet> myPortlets = portletHomePageService.queryMyPortal();
        return new Result<>(myPortlets);
    }

    @ApiOperation(value = "保存用户首页门户配置")
    @PostMapping("/homePage/myPortal")
    @ResponseStatus(HttpStatus.OK)
        public void saveMyPortal(@RequestBody @ApiParam(name = "myPortlet", value = "json格式", required = true) List<MyPortlet> myPortlet) {
        portletHomePageService.saveMyPortal(myPortlet);
    }
}
