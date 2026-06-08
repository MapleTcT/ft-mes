/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.webapi;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.enumeration.CategoryEnum;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.webapi.StatisticsVO;
import com.supcon.supfusion.flow.taskcenter.service.StatisticsService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 流程或者待办统计接口
 * @author: zhuangmh
 * @date: 2020年9月2日 上午10:59:14
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "统计查询相关文档", tags = "统计")
public class StatisticsWebapi {
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping(value = "/v1/statistics/tasks/pending")
    @ResponseBody
    @ApiOperation(value="统计待办V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "category", value = "分类: 活动(task)或者流程(process)", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<StatisticsVO> statistics(HttpServletRequest request) {
        String category = request.getParameter("category");
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        CategoryEnum c = CategoryEnum.getCategory(category);
        return statisticsService.statistics(c, current, pageSize);
    }
}
