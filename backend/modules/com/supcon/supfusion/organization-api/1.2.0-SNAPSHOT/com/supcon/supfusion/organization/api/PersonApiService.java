package com.supcon.supfusion.organization.api;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.*;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
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
@ServiceApi(path = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2")
@Validated
public interface PersonApiService {
    /**
     * 人员id,多个人员id分号分割(如:1;2;3)
     * @param personIds
     * @return
     */
    @GetMapping(value = "/person")
    Map<Long, PersonDTO> queryPersonsById(@RequestParam(value = "personIds") Long[] personIds);

    /**
     * 部门新增rpc接口
     * @param departmentAddDTO
     */
    @PostMapping(value = "/department")
    @ResponseStatus(HttpStatus.OK)
    void addDepartment(@Validated @RequestBody DepartmentAddDTO departmentAddDTO);

    /**
     * 公司新增rpc接口
     * @param companyDTO
     */
    @PostMapping("/company")
    @ResponseStatus(HttpStatus.OK)
    void addCompany(@Validated @RequestBody CompanyDTO companyDTO);

    @GetMapping("/company/{id}")
    Result<CompanyResultDTO> findCompany(@PathVariable(value = "id") Long id);

    /**
     * 根据人员编码查询人员信息
     * @param codes
     * @return
     */
    @GetMapping("/person/codes")
    ListResult<PersonDetailDTO> queryPersonByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据部门编码查询
     * @param codes
     * @return
     */
    @GetMapping("/department/codes")
    ListResult<DepartmentDetailDTO> queryDepartmentByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据岗位编码查询
     * @param codes
     * @return
     */
    @GetMapping("/position/codes")
    ListResult<PositionDetailDTO> queryPositionByCodes(@RequestParam(value = "codes") List<String> codes);

    /**
     * 根据人员id查询所属公司
     * @param personId
     * @return
     */
    @GetMapping("/person/companyId")
    ListResult<CompanyDTO> queryCompanyIdByPersonId(@RequestParam("personId") Long personId);

    @GetMapping("/person/notification")
    ListResult<PersonDTO> queryPersonByNotification(@RequestParam(value = "roleCodes", required = false) List<String> roleCodes,
                                                    @RequestParam(value = "positionCodes", required = false) List<String> positionCodes,
                                                    @RequestParam(value = "departmentCodes", required = false) List<String> departmentCodes,
                                                    @RequestParam(value = "personCodes", required = false) List<String> personCodes);

    /**
     * 根据人员code查询所属公司
     * @param personCode
     * @return
     */
    @GetMapping("/person/codes/companies")
    ListResult<CompanyDTO> queryCompanyIdByPersonCode(@RequestParam("personCode") String personCode);

    /**
     * 查询所有人员信息
     * @return
     */
    @GetMapping("/person/all")
    ListResult<PersonDTO> queryAllPersons();

    /**
     * 根据岗位id查询所有下级岗位id
     * @param ids
     * @return
     */
    @GetMapping("/positions/{ids}")
    ListResult<Long> querySubPositionIdsByPositionId(@PathVariable("ids") List<Long> ids);

    /**
     * 根据部门id查询所有下级岗位id
     * @param ids
     * @return
     */
    @GetMapping("/departments/{ids}")
    ListResult<Long> querySubDepartmentIdsByDepartmentId(@PathVariable("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的部门
     * @param ids
     * @return
     */
    @GetMapping("/persons/departments")
    ListResult<DepartmentDetailDTO> queryPersonsDepartmentsByPersonIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的岗位
     * @param ids
     * @return
     */
    @GetMapping("/persons/positions")
    ListResult<PositionDetailDTO> queryPersonsPositionsByPersonIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据人员id查询对应的岗位
     * @param id
     * @return
     */
    @GetMapping("/persons/position")
    ListResult<PositionDetailDTO> queryPersonPositionsByPersonId(@RequestParam("id") Long id);

    /**
     * 根据人员id查询人员信息,包含:员工信息,主岗信息,主岗部门信息,主岗公司信息
     * @param id 人员id
     * @param includes 返回字段
     * @return
     */
    @GetMapping(value = "/person/getCurPerson")
    public Result<JSONObject> getCurPerson(@ApiParam(value = "人员id", required = true) @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id,
                                                  @ApiParam(value = "返回哪些字count段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes);

    @GetMapping(value = "/company/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据公司id查询公司信息", notes = "根据公司id查询公司信息", tags = {"baseService"})
    public Result<JSONObject> getCompanyById(@ApiParam(value = "公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "id", required = true) Long id,
                                             @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes);

    /**
     * 根据岗位id查询该岗位的下级岗位
     * @param id 岗位id
     * @param all 是否查询全部下级岗位true，还是只查询直接下级岗位false（默认为false）
     * @param cid 公司id
     * @return
     */
    @GetMapping("/positions/sub")
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
    @GetMapping("/departments/sub")
    ListResult<DepartmentDetailDTO> querySubDepartmentByParentId(@RequestParam(value = "id", required = false) Long id,
                                                                 @RequestParam(value = "all", required = false) Boolean all,
                                                                 @RequestParam(value = "cid", required = false) Long cid);

    /**
     * 查询所有公司
     * @return
     */
    @GetMapping("/companies")
    ListResult<CompanyResultDTO> queryAllCompanies();

    /**
     * 根据岗位id批量查询岗位信息
     * @param ids 岗位ids
     * @return
     */
    @GetMapping("/positions/{ids}/list")
    ListResult<PositionDetailDTO> queryPositionsByIds(@PathVariable(value = "ids", required = true) List<Long> ids);
}
