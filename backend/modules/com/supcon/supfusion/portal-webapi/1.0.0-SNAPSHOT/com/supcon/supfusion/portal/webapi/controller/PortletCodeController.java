package com.supcon.supfusion.portal.webapi.controller;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.portal.service.PortletCodeService;
import com.supcon.supfusion.portal.service.bo.EcPortletBO;
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
 * @Date 2020/10/21 10:25
 */
@Api(tags = "门户编码组相关接口")
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "portal" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
@RestController
public class PortletCodeController {

    @Autowired
    PortletCodeService portletCodeService;

    @ApiOperation(value = "获取门户模块编码组")
    @GetMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块代码", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "category", value = "分类名", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "code", value = "编码", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认20", required = false, dataType = "String", paramType = "query"),
    })
    public PageResult<EcPortletVO> getCodes(  @RequestParam(value = "moduleCode",required = false) String moduleCode,
                                              @RequestParam(value = "code",required = false) String code,
                                              @RequestParam(value = "category",required = false) String category,
                                              @RequestParam(defaultValue = "1", required = false) Integer current,
                                              @RequestParam(defaultValue = "20", required = false) Integer pageSize) {
        PageResult<EcPortletBO> portletCodeBOPageResult = portletCodeService.queryCodes(moduleCode, code, category, current, pageSize);
        List<EcPortletVO> ecPortletVOList = portletCodeBOPageResult.getList().stream().map(ecPortletBO -> {
            EcPortletVO ecPortletVO = new EcPortletVO();
            BeanUtils.copyProperties(ecPortletBO,ecPortletVO);
            return ecPortletVO;
        }).collect(Collectors.toList());
        return new PageResult<>(ecPortletVOList,ecPortletVOList.size(),portletCodeBOPageResult.getPagination().getPageSize(),portletCodeBOPageResult.getPagination().getCurrent());
    }

    @ApiOperation(value = "增加门户模块编码")
    @PostMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    public void addCode(@Validated @RequestBody @ApiParam(name = "ecPortlet", value = "json格式", required = true) EcPortletVO ecPortletVO) {
        EcPortletBO ecPortletBO = new EcPortletBO();
        BeanUtils.copyProperties(ecPortletVO,ecPortletBO);
        portletCodeService.addCode(ecPortletBO);
    }

    @ApiOperation(value = "删除门户模块编码")
    @DeleteMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCode(@Validated @RequestBody @ApiParam(name = "ecPortlet", value = "json格式", required = true) EcPortletVO ecPortletVO) {
        EcPortletBO ecPortletBO = new EcPortletBO();
        BeanUtils.copyProperties(ecPortletVO,ecPortletBO);
        portletCodeService.deleteCode(ecPortletBO);
    }

    @ApiOperation(value = "修改门户模块编码")
    @PutMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    public void updateCode(@Validated @RequestBody @ApiParam(name = "ecPortlet", value = "json格式", required = true) EcPortletVO ecPortletVO) {
        EcPortletBO ecPortletBO = new EcPortletBO();
        BeanUtils.copyProperties(ecPortletVO,ecPortletBO);
        portletCodeService.updateCode(ecPortletBO);
    }

}
