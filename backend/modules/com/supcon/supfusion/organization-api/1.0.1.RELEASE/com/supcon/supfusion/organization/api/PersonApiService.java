package com.supcon.supfusion.organization.api;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.*;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 *
 * 公司管理rpc调用的API接口
 *
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一梠式为：/internal-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @Description:
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
@FeignClient(name = "organization")
public interface PersonApiService {
    
    String BASE_PATH = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER;
    
    /**
     * 人员id,多个人员id分号分割(如:1;2;3)
     * @param personIds
     * @return
     */
    @GetMapping(value = BASE_PATH + "v2" + "/person")
    Map<Long, PersonDTO> queryPersonsById(@RequestParam(value = "personIds") Long[] personIds);

    /**
     * 部门新增rpc接口
     * @param departmentAddDTO
     */
    @PostMapping(value = BASE_PATH + "v2" + "/department")
    @ResponseStatus(HttpStatus.OK)
    void addDepartment(@RequestBody DepartmentAddDTO departmentAddDTO);

    /**
     * 公司新增rpc接口
     * @param companyDTO
     */
    @PostMapping(BASE_PATH + "v2" + "/company")
    @ResponseStatus(HttpStatus.OK)
    void addCompany(@RequestBody CompanyDTO companyDTO);

    @GetMapping(BASE_PATH + "v2" + "/company/{id}")
    Result<CompanyResultDTO> findCompany(@PathVariable(value = "id") Long id);

    /**
     * 根据人员编码查询人员信息
     * @param codes
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/person/codes")
    ListResult<PersonDetailDTO> queryPersonByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据部门编码查询
     * @param codes
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/department/codes")
    ListResult<DepartmentDetailDTO> queryDepartmentByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据岗位编码查询
     * @param codes
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/position/codes")
    ListResult<PositionDetailDTO> queryPositionByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据人员id查询所属公司
     * @param personId
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/person/companyId")
    ListResult<CompanyDTO> queryCompanyIdByPersonId(@RequestParam("personId") Long personId);

    @GetMapping(BASE_PATH + "v2" + "/person/notification")
    ListResult<PersonDTO> queryPersonByNotification(@RequestParam(value = "roleCodes", required = false) List<String> roleCodes,
                                                    @RequestParam(value = "positionCodes", required = false) List<String> positionCodes,
                                                    @RequestParam(value = "departmentCodes", required = false) List<String> departmentCodes,
                                                    @RequestParam(value = "personCodes", required = false) List<String> personCodes);

    /**
     * 根据人员code查询所属公司
     * @param personCode
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/person/codes/companies")
    ListResult<CompanyDTO> queryCompanyIdByPersonCode(@RequestParam("personCode") String personCode);

    /**
     * 查询所有人员信息
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/person/all")
    ListResult<PersonDTO> queryAllPersons();

    /**
     * 根据岗位id查询所有下级岗位id
     * @param ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/positions/{ids}")
    ListResult<Long> querySubPositionIdsByPositionId(@PathVariable("ids") List<Long> ids);

    /**
     * 根据部门id查询所有下级岗位id
     * @param ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/departments/{ids}")
    ListResult<Long> querySubDepartmentIdsByDepartmentId(@PathVariable("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的部门
     * @param ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/persons/departments")
    ListResult<DepartmentDetailDTO> queryPersonsDepartmentsByPersonIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的岗位
     * @param ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/persons/positions")
    ListResult<PositionDetailDTO> queryPersonsPositionsByPersonIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的岗位
     * @param id
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/persons/position")
    ListResult<PositionDetailDTO> queryPersonPositionsByPersonId(@RequestParam("id") Long id);

    /**
     * 根据人员id查询人员信息,包含:员工信息,主岗信息,主岗部门信息,主岗公司信息
     * @param id 人员id
     * @param includes 返回字段
     * @return
     */
    @GetMapping(value = BASE_PATH + "v2" + "/person/getCurPerson")
    public Result<JSONObject> getCurPerson(@RequestParam(value = "id", required = true) Long id, @RequestParam(value = "includes", required = false) String includes);

    @GetMapping(value = BASE_PATH + "v2" + "/company/common/get")
    @ResponseStatus(HttpStatus.OK)
    public Result<JSONObject> getCompanyById(@RequestParam(value = "id", required = true) Long id, @RequestParam(value = "includes", required = false) String includes);

    /**
     * 根据岗位id查询该岗位的下级岗位
     * @param id 岗位id
     * @param all 是否查询全部下级岗位true，还是只查询直接下级岗位false（默认为false）
     * @param cid 公司id
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/positions/sub")
    ListResult<PositionDetailDTO> querySubPositionByParentId(@RequestParam(value = "id", required = false) Long id,
                                                             @RequestParam(value = "all", required = false) Boolean all,
                                                             @RequestParam(value = "cid", required = false) Long cid);

    /**
     * 根据部门id查询该部门的下级部门
     * @param id 部门id，为null时则，则代表
     * @param all 是否查询全部下级部门true，还是只查询直接下级部门false（默认为false）
     * @param cid 公司id
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/departments/sub")
    ListResult<DepartmentDetailDTO> querySubDepartmentByParentId(@RequestParam(value = "id", required = false) Long id,
                                                                 @RequestParam(value = "all", required = false) Boolean all,
                                                                 @RequestParam(value = "cid", required = false) Long cid);

    /**
     * 查询所有公司
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/companies")
    ListResult<CompanyResultDTO> queryAllCompanies();

    /**
     * 根据岗位id批量查询岗位信息
     * @param ids 岗位ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/positions/{ids}/list")
    ListResult<PositionDetailDTO> queryPositionsByIds(@PathVariable(value = "ids", required = true) List<Long> ids);

    /**
     * 修改人员
     * @param personUpdateDTO
     */
    @PutMapping(value = BASE_PATH + "v2" + "/person")
    @ResponseStatus(HttpStatus.OK)
    Result<Boolean> updatePerson(@RequestBody PersonUpdateDTO personUpdateDTO);

    /**
     * 根据人员id查询该人员拥有的角色id
     * @param personId
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/role/{personId}")
    ListResult<Long> queryRoleIdByPersonId(@PathVariable(value = "personId", required = true) Long personId);

    /**
     * 根据公司id查询所有人员的id
     * @param id
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/company/{id}/persons")
    ListResult<Long> queryPersonsByCompanyId(@PathVariable(value = "id") Long id);

    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/company/code/{code}")
    Result<CompanyResultDTO> findCompanyByCode(@PathVariable(value = "code") String code);

    @PostMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/virtual/person")
    Result<Long> addVirtualPerson(@RequestParam(value = "userName", required = true) String userName, @RequestParam(value = "companyId", required = true) Long companyId);

    /**
     *  根据岗位id查询岗位下的人员
     * @param positionId
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/position/{positionId}/persons")
    ListResult<PersonDetailDTO> queryPersonsByPositionId(@PathVariable(value = "positionId") Long positionId);

    /**
     * 根据部门id查询部门下的人员
     * @param departmentId
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/Department/{departmentId}/persons")
    ListResult<PersonDetailDTO> queryPersonsByDepartmentId(@PathVariable(value = "departmentId") Long departmentId);

    /**
     * 根据两个人员id，判断2个人员是否存在上下级关系(所属岗位是否有上下级)
     * @param supPersonId
     * @param subPersonId
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/supAndSubPerson")
    Result<Boolean> checkPersonSupAndSub(@RequestParam(value = "supPersonId") Long supPersonId, @RequestParam(value = "subPersonId") Long subPersonId, @RequestParam(value = "companyId") Long companyId);

    /**
     * 人员id,多个人员id分号分割(如:1;2;3)
     * @param personIds
     * @return
     */
    @GetMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/persons")
    Map<Long, PersonDetailDTO> queryPersonsByIds(@RequestParam(value = "personIds") List<Long> personIds);

    @GetMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/sup/{companyId}/companies")
    List<Long> querySupCompaniesById(@PathVariable(value = "companyId") Long companyId);

    @GetMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/person/{personId}/leader")
    PersonLeaderDTO getPersonLeader(@PathVariable(value = "personId") Long personId);

    @GetMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/openapi/version")
    OpenapiVersionDTO getOpenapiVersion();

    /**
     * 根据两个岗位的ｉｄ判断两个岗位是否是上下级关系
     * @param supPositionId　上级岗位id
     * @param subPositionId　下级岗位id
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/supAndSubPosition")
    Result<Boolean> checkPositionSupAndSub(@RequestParam(value = "supPositionId") Long supPositionId, @RequestParam(value = "subPositionId") Long subPositionId);

    @GetMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/person/{personId}")
    Result<Boolean> deletePersonById(@PathVariable(value = "personId") Long personId);

    /**
     * 判断角色是否
     * @param roleIdDTO
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2" + "/role/relation")
    Result<Boolean> checkRolesExistPosition(@RequestBody RoleIdDTO roleIdDTO);


    /**
     * 根据人员id查询人员信息
     *
     * @param ids
     * @return
     */
    @GetMapping(BASE_PATH + "v2" + "/persons/ids")
    ListResult<PersonDetailDTO> queryPersonByIds(@RequestParam(value = "ids") List<Long> ids);

    @PostMapping(BASE_PATH + "v2" + "/persons/users")
    @ResponseStatus(HttpStatus.OK)
    void saveOrUpdateUsers(@RequestBody List<PersonUserDTO> personUserDTOS);

    @DeleteMapping(BASE_PATH + "v2" + "/persons/users/{personIds}")
    @ResponseStatus(HttpStatus.OK)
    void deleteUsersByPersonIds(@PathVariable(value = "personIds") List<Long> personIds);

    @GetMapping(BASE_PATH + "v2" + "/persons/company/{companyId}")
    ListResult<Long> queryMultiCompanyPersonsByCompanyId(@PathVariable(value = "companyId") Long companyId);
}
