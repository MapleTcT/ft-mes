/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.vo.webapi.HelperResponseVO;
import com.supcon.supfusion.flow.taskcenter.service.HelperService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author: zhuangmh
 * @date: 2020年6月17日 上午10:15:12
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "帮助说明相关文档", tags = "帮助说明")
public class HelperWebapi {
    
    @Autowired
    private HelperService helperService;
    
    @GetMapping(value = "/v1/helper")
    @ResponseBody
    @ApiOperation(value="获取流程操作记录V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public HelperResponseVO getHelpDoc() {
        return helperService.getHelpDoc();
    }
    
}
