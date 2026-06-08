package com.supcon.supfusion.organization.webapi;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@InternalApi(path = "/baseService")
@Validated
@Api(tags = "BaseService服务接口", description = "BaseService服务接口描述", hidden = true)
public class OrganizationBaseServiceController {

    @Autowired
    private PersonService personService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    /**
     * 根据公司id查询公司信息
     * @param id
     * @param includes
     * @return
     */
    @GetMapping(value = "/company/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据公司id查询公司信息", notes = "根据公司id查询公司信息", tags = {"baseService"})
    public Result<JSONObject> getCompanyById(@ApiParam(value = "公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "id", required = true) Long id,
                                         @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject companyInfo = companyService.getCompanyById(id, includes);
        return new Result<JSONObject>(companyInfo);
    }

    /**
     * 根据部门id查询部门信息
     * @param id
     * @param includes
     * @return
     */
    @GetMapping(value = "/department/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据部门id查询部门信息", notes = "根据部门id查询部门信息", tags = {"baseService"})
    public Result<JSONObject> getDepartmentById(@ApiParam(value = "部门id", required = true) @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id,
                                                @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject departmentInfo = departmentService.getDepartmentById(id, includes);
        return new Result<JSONObject>(departmentInfo);
    }

    /**
     * 根据id查询部门的子节点列表(直接子部门)
     * @param treeId
     * @return
     */
    @PostMapping(value = {"/foundation/department/department/deptTreeSegTreeDataCustom", "/foundation/department/department/deptReftreeSegTreeDataCustom"})
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据id查询部门的子节点列表", notes = "根据id查询部门的子节点列表", tags = {"baseService"})
    public Result<List<JSONObject>> getDeptTree(@ApiParam(value = "部门id", required = true) @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY) @RequestParam(value = "treeId", required = true) Long treeId) {
        UserContext userContext = UserContext.getUserContext();
        if (userContext == null || userContext.getCompanyId() == null) {
            return new Result<>();
        }
        List<JSONObject> depTree = departmentService.getDeptTree(treeId, userContext.getCompanyId());
        return new Result<List<JSONObject>>(depTree);
    }

    /**
     * 根据岗位id查询岗位信息
     * @param id
     * @param includes
     * @return
     */
    @GetMapping(value = {"/position/common/get", "/position/common/getInfo"})
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据岗位id查询岗位信息", notes = "根据岗位id查询岗位信息", tags = {"baseService"})
    public Result<JSONObject> getPositionById(@ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id,
                                                @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject positionInfo = positionService.getPositionById(id, includes);
        return new Result<JSONObject>(positionInfo);
    }

    /**
     * 根据id查询岗位的子节点列表(直接子岗位)
     * @param treeId
     * @return
     */
    @PostMapping(value = {"/foundation/position/position/posTreeSegTreeDataCustom", "/foundation/position/position/posReftreeSegTreeDataCustom"})
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据id查询岗位的子节点列表", notes = "根据id查询岗位的子节点列表", tags = {"baseService"})
    public Result<List<JSONObject>> getPosTree(@ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam(value = "treeId", required = true) Long treeId) {
        UserContext userContext = UserContext.getUserContext();
        if (userContext == null || userContext.getCompanyId() == null) {
            return new Result<>();
        }
        List<JSONObject> posTree = positionService.getPosTree(treeId, userContext.getCompanyId());
        return new Result<List<JSONObject>>(posTree);
    }

    @GetMapping(value = "/staff/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据id查询人员", notes = "根据id查询人员", tags = {"baseService"})
    public Result<JSONObject> getStaffById(@ApiParam(value = "人员id", required = true) @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id,
                                           @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject personInfo = personService.getStaffById(id, includes);
        return new Result<JSONObject>(personInfo);
    }
}
