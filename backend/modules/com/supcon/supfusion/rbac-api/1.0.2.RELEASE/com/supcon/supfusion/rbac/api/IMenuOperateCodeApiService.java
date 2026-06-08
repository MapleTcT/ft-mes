package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.MenuOperateCodeUrlRefDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.supcon.supfusion.rbac.api.Constants.Constants.API_PREFIX;

/**
 *
 * 角色服务相关接口
 *
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一梠式为：/service-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @author
 * @date 20-5-11 下午2:14
 */
@Validated
@FeignClient(name = "rbac",contextId = "menuOperateCode")
public interface IMenuOperateCodeApiService {

    /**
     * @description: 批量保存操作URL
     * @param: list
     * @param: app
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PutMapping(API_PREFIX + Constants.MENUOPERATECODE_URL)
    @ResponseBody
    void saveBachUrl(@RequestBody List<MenuOperateCodeUrlRefDTO> list, @RequestParam("app") String app);
}
