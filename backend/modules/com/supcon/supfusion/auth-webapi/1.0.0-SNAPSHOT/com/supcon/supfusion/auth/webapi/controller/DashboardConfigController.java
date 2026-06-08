package com.supcon.supfusion.auth.webapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.service.DashboardConfigService;
import com.supcon.supfusion.auth.service.bo.DashboardConfigBO;
import com.supcon.supfusion.auth.webapi.result.Response;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author lifangyuan
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth/v1/user")
@Validated
public class DashboardConfigController extends BaseController {
    @Resource
    private DashboardConfigService dashboardConfigService;

    @GetMapping(value = "/config/dashboard/{mkey}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String queryByMkey(@PathVariable String mkey) {
        Long userId = UserContext.getUserContext().getUserId();
        DashboardConfigBO userConfigDashboard = dashboardConfigService.findUserConfigDashboard(userId, mkey);
        JSONObject result = new JSONObject();
        if (userConfigDashboard != null) {
            result.put(Constants.MKEY, userConfigDashboard.getMkey());
            result.put(Constants.CONFIG_INFO, userConfigDashboard.getConfigInfo());
        }
        return result.toJSONString();
    }

    @GetMapping(value = "/config/dashboard", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String batchQuery(@RequestParam List<String> mkeys) {
        Long userId = UserContext.getUserContext().getUserId();
        List<DashboardConfigBO> list = dashboardConfigService.findUserConfigDashboards(userId, mkeys);
        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        if (list != null) {
            list.forEach(userConfig -> {
                JSONObject object = new JSONObject();
                object.put(Constants.MKEY, userConfig.getMkey());
                object.put(Constants.CONFIG_INFO, userConfig.getConfigInfo());
                array.add(object);
            });
        }
        result.put("list", array);
        return result.toJSONString();
    }

    @PostMapping(value = "/config/dashboard")
    public Response create(@RequestBody JSONObject body, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        Long userId = UserContext.getUserContext().getUserId();
        String mkey = body.getString(Constants.MKEY);
        String requestUrl = request.getRequestURL().toString() + "/" + mkey;
        if (dashboardConfigService.isExistUserConfigDashboard(userId, mkey) > 0) {
            Response response = getResponse(false, HttpStatus.SC_BAD_REQUEST, "create user config failed", null);
            httpServletResponse.setStatus(response.getCode());
            response.setRequestUrl(requestUrl);
            return response;
        }
        body.put(Constants.USER_ID, userId);
        DashboardConfigBO dashboardConfigBO = JSON.parseObject(body.toString(), DashboardConfigBO.class);
        int result = dashboardConfigService.insertUserConfigDashboard(dashboardConfigBO);
        Response response;
        if (result > 0) {
            response = getResponse(true, HttpStatus.SC_CREATED, "", null);
        } else {
            response = getResponse(false, HttpStatus.SC_BAD_REQUEST, "create user config failed", null);
        }
        httpServletResponse.setStatus(response.getCode());
        response.setRequestUrl(requestUrl);
        return response;
    }

    @PutMapping("/config/dashboard")
    public Response update(@RequestBody JSONObject body, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        Long userId = UserContext.getUserContext().getUserId();
        body.put(Constants.USER_ID, userId);
        String mkey = body.getString(Constants.MKEY);
        DashboardConfigBO dashboardConfigBO = JSON.parseObject(body.toString(), DashboardConfigBO.class);
        int result = 0;
        if (dashboardConfigService.isExistUserConfigDashboard(userId, mkey) > 0) {
            result = dashboardConfigService.updateUserConfigDashboard(dashboardConfigBO);
        } else {
            result = dashboardConfigService.insertUserConfigDashboard(dashboardConfigBO);
        }
        Response response;
        if (result > 0) {
            response = getResponse(true, HttpStatus.SC_CREATED, "", null);
        } else {
            response = getResponse(false, HttpStatus.SC_BAD_REQUEST, "update user config failed", null);
        }
        httpServletResponse.setStatus(response.getCode());
        String requestUrl = request.getRequestURL().toString() + "/" + mkey;
        response.setRequestUrl(requestUrl);
        return response;
    }

    @DeleteMapping("/config/dashboard")
    public Object delete(@RequestBody JSONObject body, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        JSONArray mkeys = body.getJSONArray("mkeys");
        if (null == mkeys || mkeys.isEmpty()) {
            httpServletResponse.setStatus(HttpStatus.SC_NO_CONTENT);
            return null;
        }
        List<String> mkeysList = mkeys.toJavaList(String.class);
        dashboardConfigService.deleteUserConfigDashboards(UserContext.getUserContext().getUserId(), mkeysList);
        Response response = getResponse(true, HttpStatus.SC_NO_CONTENT, "", null);
        return response;
    }

    private Response getResponse(boolean succeeded, int code, String message, Throwable other) {
        Response response = new Response();
        response.setSucceeded(succeeded);
        response.setCode(code);
        response.setMessage(message);
        response.setCause(other);
        return response;
    }
}
