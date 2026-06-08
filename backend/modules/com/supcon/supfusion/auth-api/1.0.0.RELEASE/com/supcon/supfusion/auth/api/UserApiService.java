package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.*;
import com.supcon.supfusion.auth.api.result.UserSessinResult;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;

@FeignClient(name = "auth", contextId = "user")
public interface UserApiService {

    String API_PREFIX = "/service-api/auth";

    @PostMapping(API_PREFIX + "/v1/user")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Result<String> createUser(@Validated @RequestBody BatchInsertDTO batchInsertDTO);

    @GetMapping(API_PREFIX + "/v1/user/info")
    @ResponseBody
    Map<Long, UserDetailDTO> getUsersDetail(@RequestParam("userIds") String userIds);

    @GetMapping(API_PREFIX + "/v1/user/entity")
    @ResponseBody
    String getUserEntity(@RequestParam("userName") String userName);

    @PutMapping(API_PREFIX + "/v1/user/failUp")
    @ResponseBody
    String failUp(@RequestParam("userName") String userName);

    @PutMapping(API_PREFIX + "/v1/user/lock")
    @ResponseBody
    String setLock(@RequestParam("userName") String userName);

    @PutMapping(API_PREFIX + "/v1/user/failZero")
    @ResponseBody
    String failZero(@RequestParam("userName") String userName);

    @GetMapping(API_PREFIX + "/v1/user/person")
    @ResponseBody
    Map<Long, UserDetailDTO> getUsersDetailByPerson(@RequestParam("personIds") String personIds);

    @GetMapping(API_PREFIX + "/v1/user")
    @ResponseBody
    Result<UserOrgDetailDTO> getUsersDetailByName(@RequestParam("userName") String userName, @RequestParam(value = "companyId", required = false) Long companyId);

    @GetMapping(API_PREFIX + "/v1/user/{userId}")
    @ResponseBody
    Result<UserOrgDetailDTO> getUsersDetailById(@PathVariable("userId") Long userId);

    @GetMapping(API_PREFIX + "/v1/user/detail")
    @ResponseBody
    Result<UserOrgDetailDTO> getUserOrgDetailByName(@RequestParam("userName") String userName);

    @PostMapping(API_PREFIX + "/v1/user/loginCustom")
    @ResponseBody
    Result<LoginResponseDTO> loginCustom(@RequestBody LoginCustomDTO loginCustomDTO);

    @PutMapping(API_PREFIX + "/v1/user/company")
    @ResponseBody
    Result<LoginResponseDTO> companyChange(@RequestBody CompanyChangeDTO companyChangeDTO);

    @GetMapping(API_PREFIX + "/v1/user/userName")
    @ResponseBody
    Result<Map<String, UserDetailDTO>> getBatchUsersDetailByName(@RequestParam(value = "userName") String[] userNames);

    @GetMapping(API_PREFIX + "/v1/company/adminUser")
    @ResponseBody
    ListResult<UserDetailDTO> companyAdminUser(@RequestParam(value = "companyId") Long companyId);

    @DeleteMapping(API_PREFIX + "/v1/company/user")
    @ResponseBody
    Result<Boolean> deleteCompanyUser(@RequestParam(value = "companyId") Long companyId);

    @DeleteMapping(API_PREFIX + "/v1/user/person")
    @ResponseBody
    Result<Boolean> deleteCompanyUser(@RequestParam("personIds") Long[] personIds);

    /**
     * 角色模块关联角色解除角色时调用接口
     *
     * @param userRoleDTO
     * @return
     * @Author 袁阳
     */
    @PostMapping(API_PREFIX + "/v1/user/bindRole")
    @ResponseBody
    Result<Boolean> bindRole(@RequestBody UserRoleDTO userRoleDTO);

    @PostMapping(API_PREFIX + "/v1/users/query")
    @ResponseBody
    Map<Long, UserDetailDTO> queryUsersDetail(@RequestBody UserQueryDTO userQueryDTO);

    /**
     * 人员角色变得更新用户角色
     *
     * @param personRoleDTO
     * @return
     * @Author lifangyuan
     */
    @PutMapping(API_PREFIX + "/v1/user/role")
    @ResponseBody
    Result<Boolean> changeRole(@RequestBody List<PersonRoleDTO> personRoleDTO);

    @GetMapping(API_PREFIX+"/v1/user/userSessionInfo")
    public UserSessinResult<UserStaffDTO> getUserSessionInfo(@RequestParam("ticket") String ticket);

    /**
     * 人员角色变得更新用户角色
     *
     * @param passwordDTO
     * @return
     * @Author lifangyuan
     */
    @PostMapping("/service-api/auth/v1/user/password")
    @ResponseBody
    Result<Boolean> checkPassword(@RequestBody PasswordDTO passwordDTO);

    /**
     * 根据用户名获取用户id
     *
     * @param userName
     * @return
     */
    @GetMapping(API_PREFIX + "/v1/userId/{userName}")
    @ResponseBody
    Result<Long> getIdByUserName(@PathVariable("userName") String userName);

    /**
     * 获取所有
     */
    @GetMapping(API_PREFIX + "/v1/allPersonsUsers")
    @ResponseBody
    ListResult<UserDetailDTO> getAllPersonsUsers();
}
