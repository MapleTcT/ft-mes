
package com.supcon.supfusion.license.webapi;


import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.license.service.LicenseService;
import com.supcon.supfusion.license.service.vo.LicenseInfoVO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@InternalApi(path = "/inter-api/supplant-license/v1")
@Slf4j
public class LicenseController extends BaseController {


    @Autowired
    private LicenseService licenseService;


    /**
     * 根据模块code获取授权信息
     *
     * @param moduleCode
     * @return
     */
    @GetMapping("/getLicenseByModule")
    @ResponseBody
    @ApiOperation(value = "根据模块code获取授权信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query"),
    })
    Result<LicenseInfoVO> getLicenseByModule(@RequestParam("moduleCode") String moduleCode) {
        return licenseService.getLicenseByModule(moduleCode);
    }

    /**
     * 分页查询授权信息
     */
    @GetMapping("/getLicensePage")
    @ResponseBody
    @ApiOperation(value = "分页查询授权信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "当前页数", required = true, paramType = "query"),
            @ApiImplicitParam(name = "size", value = "当前页查询条数", required = true, paramType = "query"),
    })
    PageResult<LicenseInfoVO> getLicensePage(@RequestParam("current") Long current, @RequestParam("size") Long size) {
        return licenseService.getLicensePage(current, size);
    }


}
