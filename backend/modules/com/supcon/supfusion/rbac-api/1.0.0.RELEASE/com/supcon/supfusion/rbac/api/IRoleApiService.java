package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.AdminRoleDTO;
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
@FeignClient(name = "rbac",contextId = "role")
public interface IRoleApiService {
    /**
     * @description: 根据ID查询角色名
     * @param: ids
     * @return: java.util.Map<java.lang.Long,java.lang.String>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_PATH + "/findBatchName")
    @ResponseBody
    Map<Long,String> findBatchName(@RequestParam("ids") String ids);

    /**
     * @description: 根据ID查询角色
     * @param: ids
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.RoleDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_PATH + "/findRoleByIds")
    @ResponseBody
    List<RoleDTO> findRoleByIds(@RequestParam("ids") List<Long> ids);

    /**
     * @description: 根据编码查询角色
     * @param: codes
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.RoleDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_PATH + "/findRoleByCodes")
    @ResponseBody
    List<RoleDTO> findRoleByCodes(@RequestParam("codes") List<String> codes);

    /**
     * @description: 创建管理员角色、关联用户 （创建新公司时 生成新管理员用户）
     * @param: adminRoleDTO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PostMapping(API_PREFIX + Constants.ROLE_PATH + "/bindAdminUser")
    @ResponseBody
    RoleDTO createAdminRole(@RequestBody AdminRoleDTO adminRoleDTO);

    @PostMapping(API_PREFIX + Constants.ROLE_PATH + "/bindCompanyAdminUser")
    @ResponseBody
    RoleDTO bindCompanyAdminUser(@RequestBody AdminRoleDTO adminRoleDTO);
}
