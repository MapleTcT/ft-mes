package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@FeignClient(name = "rbac",contextId = "roleUser")
public interface IRoleUserApiService {

    /**
     * @description: 查询角色根据用户ID和角色公司ID
     * @param: id
     * @param: cid
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.RoleDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/findRoleUserByUserId")
    @ResponseBody
    List<RoleDTO> findRoleUserByUserId(@RequestParam("id") Long id,@RequestParam("cid") Long cid);

    /**
     * @description: 根据角色ID查询角色用户关联
     * @param: ids
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.RoleUserDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/findUserByRoleId")
    @ResponseBody
    List<RoleUserDTO> findUserByRoleId(@RequestParam("ids") List<Long> ids);

    /**
     * @description: 根绝角色编码查询角色用户关联
     * @param: codes
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.RoleUserDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/findUserByRoleCode")
    @ResponseBody
    List<RoleUserDTO> findUserByRoleCode(@RequestParam("codes") List<String> codes);

    /**
     * @description: 保存角色用户关联
     * @param: roleUserDTO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PostMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/save")
    @ResponseBody
    void saveRoleUser(@RequestBody RoleUserDTO roleUserDTO);

    /**
     * @description: 批量保存角色用户关联
     * @param: roleUserAddBatchDTO
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/7
     */
    @PostMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/batchSaveOneUser")
    @ResponseBody
    void batchSaveOneUser(@RequestBody RoleUserAddBatchDTO roleUserAddBatchDTO);

    /**
     * @description: 批量保存角色用户关联
     * @param: roleUserDTOS
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/7
     */
    @PostMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/batchSave")
    @ResponseBody
    void batchSaveRoleUser(@RequestBody List<RoleUserDTO> roleUserDTOS);

    /**
     * 更新用户角色关联
     * @param roleUserAddBatchDTO
     */
    @PutMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/updateRoleUser")
    @ResponseBody
    void updateRoleUser(@RequestBody RoleUserAddBatchDTO roleUserAddBatchDTO);

    /**
     * 根据用户ID删除角色用户关联 删除用户时调
     * @param userId
     */
    @DeleteMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/deleteByUserId")
    @ResponseBody
    void deleteByUserId(@RequestParam("userId") List<Long> userId);


    /**
     * 新增用户角色关联
     * @param roleUserFRDTO
     */
    @PutMapping(API_PREFIX + Constants.ROLE_USER_PATH + "/batchSaveOneUserFR")
    @ResponseBody
    void batchSaveOneUserFR(@RequestBody List<RoleUserFRDTO> roleUserFRDTO);
}
