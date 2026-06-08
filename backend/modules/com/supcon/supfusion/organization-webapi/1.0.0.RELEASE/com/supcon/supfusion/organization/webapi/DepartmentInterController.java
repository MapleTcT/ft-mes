package com.supcon.supfusion.organization.webapi;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentFlowSimpleBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentKeywordBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentLocationBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentTreeBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentAddVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentDetailVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentFlowSimpleVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentKeywordVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentLocationVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentTreeVO;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentUpdateVO;
import com.supcon.supfusion.organization.webapi.vo.person.PersonDetailUserVO;
import com.supcon.supfusion.organization.webapi.vo.person.PersonDetailVO;
import com.supcon.supfusion.organization.webapi.vo.position.PositionDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 部门管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "部门管理", description = "部门文档说明", hidden = true)
public class DepartmentInterController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private OrganizationManagerService organizationManagerService;

    @Autowired
    private OrganizationAdapter organizationAdapter;


    /**
     * 部门新增openapi借口
     * @param departmentAddVO
     */
    @PostMapping(value = "/department")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "新增部门", notes = "根据部门信息新增一个部门")
    Result<DepartmentDetailVO> addDepartment(@Validated @RequestBody DepartmentAddVO departmentAddVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        BeanUtils.copyProperties(departmentAddVO, departmentAddPO);
        departmentService.addDepartment(departmentAddPO, departmentAddVO.getManagerIds(), tenantId);
        DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
        departmentDetailVO.setId(departmentAddPO.getId());
        return new Result<DepartmentDetailVO>(departmentDetailVO);
    }

    /**
     * 根据id修改部门信息
     * @param departmentUpdateVO
     */
    @PutMapping(value = "/department")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "修改部门", notes = "修改部门信息")
    void updateDepartment(@Validated @RequestBody DepartmentUpdateVO departmentUpdateVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        BeanUtils.copyProperties(departmentUpdateVO, departmentAddPO);
        departmentService.updateDepartment(departmentAddPO, departmentUpdateVO.getManagerIds(), tenantId);
    }

    /**
     * 查询部门详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/department")
    @ResponseBody
    @ApiOperation(value = "查询部门详情", notes = "根据部门id查询部门详情")
    Result<DepartmentDetailVO> getDepDetail(@ApiParam(value = "部门id", required = true) @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY) @RequestParam("id") Long id) {
        DepartmentDetailBO departmentDetailBO = departmentService.getDepDetail(id);
        DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
        BeanUtils.copyProperties(departmentDetailBO, departmentDetailVO);
        List<PositionDetailVO> voRelPos = new ArrayList<PositionDetailVO>();
        if (departmentDetailBO.getRelPos() == null) {
            return new Result<DepartmentDetailVO>(departmentDetailVO);
        }
        departmentDetailBO.getRelPos().stream().forEach(item -> {
            PositionDetailVO posForDepVO = new PositionDetailVO();
            BeanUtils.copyProperties(item, posForDepVO);
            voRelPos.add(posForDepVO);
        });
        departmentDetailVO.setRelPos(voRelPos);
        return new Result<DepartmentDetailVO>(departmentDetailVO);
    }

    /**
     *  删除指定部门
     * @param id
     */
    @DeleteMapping(value = "/department/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "删除部门", notes = "根据id删除部门")
    void deleteDep(@ApiParam(value = "部门id", required = true) @PathVariable("id") Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        departmentService.deleteDepById(id, tenantId);
    }

    /**
     * 修改部门的位置
     * @param departmentLocationVO
     */
    @PutMapping(value = "/department/location")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "移动部门位置", notes = "移动部门位置")
    void updateDepLocation(@Validated @RequestBody DepartmentLocationVO departmentLocationVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentLocationBO departmentLocationPO = new DepartmentLocationBO();
        BeanUtils.copyProperties(departmentLocationVO, departmentLocationPO);
        departmentService.updateDepLocation(departmentLocationPO, tenantId);
    }

    /**
     * 查询部门树形结构
     * @param companyId
     //* @param parentId
     * @param keyword
     * @return
     */
    @GetMapping(value = {"/departments", "/departments/ref"})
    @ResponseBody
    @ApiOperation(value = "查询部门树形结构", notes = "查询部门树形结构")
    Result<DepartmentTreeVO> getDepTree(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.DEPARTMENT_PARAM_COMPANYID_NECESSARY) @RequestParam("companyId") Long companyId,
                                        @ApiParam(value = "部门id,传入时返回当前部门级上层部门") @RequestParam(value = "departmentId", required = false) Long departmentId,
                                        @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword) {

        DepartmentTreeBO departmentTreePO = departmentService.getDepTree(companyId, keyword, departmentId);
        DepartmentTreeVO departmentTreeVO = new DepartmentTreeVO();
        BeanUtils.copyProperties(departmentTreePO, departmentTreeVO);
        List<DepartmentTreeBO> poList = departmentTreePO.getChildren();
        if (poList == null || poList.size() == 0) {
            return new Result<DepartmentTreeVO>(departmentTreeVO);
        }
        departmentTreeVO.setChildren(new ArrayList<DepartmentTreeVO>());

        poToVo(departmentTreePO.getChildren(), departmentTreeVO.getChildren());
        return new Result<DepartmentTreeVO>(departmentTreeVO);
    }

    /**
     * 部门列表模糊查询
     * @param keyword
     * @param companyId
     * @return
     */
    @GetMapping(value = {"/department/keyword", "/department/keyword/ref"})
    @ResponseBody
    @ApiOperation(value = "部门模糊查询列表", notes = "部门模糊查询列表")
    ListResult<DepartmentKeywordVO> queryPositionByKeyword (@ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                            @ApiParam(value = "公司id") @RequestParam(value = "companyId", required = true) Long companyId) {
        List<DepartmentKeywordBO> list = departmentService.queryDepartmentsByKeyword(keyword, companyId);
        List<DepartmentKeywordVO> results = new ArrayList<DepartmentKeywordVO>();
        if (list == null || list.size() == 0) {
            return new ListResult<DepartmentKeywordVO>(results);
        }
        list.stream().forEach(bo -> {
            DepartmentKeywordVO deptKeywordVO = new DepartmentKeywordVO();
            BeanUtils.copyProperties(bo, deptKeywordVO);
            results.add(deptKeywordVO);
        });
        return new ListResult<DepartmentKeywordVO>(results);
    }

    /**
     * 分页查询部门关联人员
     * @param companyId　公司id
     * @param departmentId 部门id
     * @param keyword 关键字
     * @param current 当前页
     * @param pageSize 每页条数
     * @return
     */
    @GetMapping(value = {"/department/person", "/department/person/ref"})
    @ResponseBody
    @ApiOperation(value = "查询部门关联的人员", notes = "根据部门id或模糊匹配分页查询部门关联的人员")
    PageResult<PersonDetailVO> queryPositionPerson(@ApiParam(value = "所属公司id", required = false) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = false) Long companyId,
                                                   @ApiParam(value = "部门id") @RequestParam(value = "departmentId", required = false) Long departmentId,
                                                   @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                   @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                   @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                   @ApiParam(value = "人员编码") @RequestParam(value = "code", required = false) String code,
                                                   @ApiParam(value = "人员名称") @RequestParam(value = "name",required = false) String name,
                                                   @ApiParam(value = "描述") @RequestParam(value = "description", required = false) String description,
                                                   @ApiParam(value = "邮箱") @RequestParam(value = "email", required = false) String email,
                                                   @ApiParam(value = "性别") @RequestParam(value = "gender", required = false) String gender,
                                                   @ApiParam(value = "手机号") @RequestParam(value = "phone", required = false) String phone,
                                                   @ApiParam(value = "状态") @RequestParam(value = "status", required = false) String status,
                                                   @ApiParam(value = "是否包含用户信息") @RequestParam(value = "includeUser", required = false) Boolean includeUser) {
/*        UserContext userContext = UserContext.getUserContext();
        if (userContext != null && userContext.getCompanyId() != null) {
            companyId = userContext.getCompanyId();
        }*/
        PersonDetailBO conditionQuery = new PersonDetailBO();
        conditionQuery.setCode(code);
        conditionQuery.setName(name);
        conditionQuery.setDescription(description);
        conditionQuery.setEmail(email);
        conditionQuery.setGender(gender);
        conditionQuery.setPhone(phone);
        conditionQuery.setStatus(status);
        PageResult<PersonDetailBO> pageResult = departmentService.queryDepartmentPersonsBetter(companyId, departmentId, keyword, current, pageSize, conditionQuery, includeUser);
        if (pageResult == null || pageResult.getList() == null) {
            return new PageResult<PersonDetailVO>(new ArrayList<PersonDetailVO>(), 0, pageSize, current);
        }
        List<PersonDetailVO> voList = new ArrayList<PersonDetailVO>();
        ((List<PersonDetailBO>)pageResult.getList()).stream().forEach(item -> {
            PersonDetailVO personDetailVO = new PersonDetailVO();
            BeanUtils.copyProperties(item, personDetailVO);
            voList.add(personDetailVO);
        });
        PageResult<PersonDetailVO> voPageResult = new PageResult<PersonDetailVO>(voList, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent());
        return voPageResult;
    }

    @GetMapping(value = {"/department/condition/person", "/department/condition/person/ref"})
    @ResponseBody
    @ApiOperation(value = "查询部门关联的人员", notes = "根据部门id或模糊匹配分页查询部门关联的人员")
    ListResult<PersonDetailVO> queryDepartmentPersonNoPage(@ApiParam(value = "所属公司id", required = false) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = false) Long companyId,
                                                   @ApiParam(value = "部门id") @RequestParam(value = "departmentId", required = false) Long departmentId,
                                                   @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                   @ApiParam(value = "人员编码") @RequestParam(value = "code", required = false) String code,
                                                   @ApiParam(value = "人员名称") @RequestParam(value = "name",required = false) String name,
                                                   @ApiParam(value = "描述") @RequestParam(value = "description", required = false) String description,
                                                   @ApiParam(value = "邮箱") @RequestParam(value = "email", required = false) String email,
                                                   @ApiParam(value = "性别") @RequestParam(value = "gender", required = false) String gender,
                                                   @ApiParam(value = "手机号") @RequestParam(value = "phone", required = false) String phone,
                                                   @ApiParam(value = "状态") @RequestParam(value = "status", required = false) String status) {
/*        UserContext userContext = UserContext.getUserContext();
        if (userContext != null && userContext.getCompanyId() != null) {
            companyId = userContext.getCompanyId();
        }*/
        PersonDetailBO conditionQuery = new PersonDetailBO();
        conditionQuery.setCode(code);
        conditionQuery.setName(name);
        conditionQuery.setDescription(description);
        conditionQuery.setEmail(email);
        conditionQuery.setGender(gender);
        conditionQuery.setPhone(phone);
        conditionQuery.setStatus(status);
        List<PersonDetailBO> personList = departmentService.queryDepartmentPersonNoPage(companyId, departmentId, keyword, conditionQuery);
        if (personList == null || personList.size() == 0) {
            return new ListResult<>();
        }
        List<PersonDetailVO> voList = new ArrayList<PersonDetailVO>();
        personList.stream().forEach(item -> {
            PersonDetailVO personDetailVO = new PersonDetailVO();
            BeanUtils.copyProperties(item, personDetailVO);
            voList.add(personDetailVO);
        });
        return new ListResult<>(voList);
    }

    /**
     * 查询树形结构中PO对象转BO对象
     * @param poList
     * @param voList
     */
    private void poToVo(List<DepartmentTreeBO> poList, List<DepartmentTreeVO> voList) {
        poList.stream().forEach(poDep -> {
            DepartmentTreeVO voDep = new DepartmentTreeVO();
            BeanUtils.copyProperties(poDep, voDep);
            voList.add(voDep);
            if (poDep.getChildren() != null && poDep.getChildren().size() > 0) {
                voDep.setChildren(new ArrayList<DepartmentTreeVO>());
                poToVo(poDep.getChildren(), voDep.getChildren());
            }
        });
    }

    /**
     * 根据部门id批量查询部门信息
     * @param ids
     * @return
     */
    @GetMapping("/departments/ids")
    @ResponseBody
    @ApiOperation(value = "根据部门id批量查询部门信息", notes = "根据部门id批量查询部门信息")
    public ListResult<DepartmentDetailVO> queryDeptInfoByIds(@ApiParam(value = "部门ids") @RequestParam(value = "ids", required = true) List<Long> ids) {
        List<DepartmentDetailBO> list = departmentService.queryDeptInfoByIds(ids);
        if (list == null || list.size() == 0) {
            return new ListResult<DepartmentDetailVO>(new ArrayList<DepartmentDetailVO>());
        }
        List<DepartmentDetailVO> vos = new ArrayList<DepartmentDetailVO>();
        list.stream().forEach(bo -> {
            DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
            BeanUtils.copyProperties(bo, departmentDetailVO);
            vos.add(departmentDetailVO);
        });
        return new ListResult<DepartmentDetailVO>(vos);
    }

    @GetMapping(value = {"/department/current/user", "/department/current/user/ref"})
    @ResponseBody
    @ApiOperation(value = "查询当前登录用户所在公司下的部门", notes = "查询当前登录用户所在公司下的部门")
    Result<DepartmentDetailVO> queryCurrentUserDept() {
        UserContext userContext = UserContext.getUserContext();
        String personCode = "";
        Long companyId =  null;
        if (userContext != null) {
            companyId = userContext.getCompanyId();
            personCode = organizationAdapter.getUsersDetailByName(userContext.getUserName(), companyId).getPersonCode();

        } else {
            return new Result<DepartmentDetailVO>();
        }

        DepartmentDetailBO departmentDetailBO = departmentService.queryCurrentUserDept(personCode, companyId);
        if (departmentDetailBO == null) {
            return new Result<DepartmentDetailVO>(new DepartmentDetailVO());
        }
        DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
        BeanUtils.copyProperties(departmentDetailBO, departmentDetailVO);
        return new Result<DepartmentDetailVO>(departmentDetailVO);
    }
    /**
     * 分页查询部门关联人员用户
     * @param companyId　公司id
     * @param departmentId 部门id
     * @param keyword 关键字
     * @return
     */
    @GetMapping(value = {"/department/user/ref1"})
    @ResponseBody
    @ApiOperation(value = "查询部门关联的人员用户", notes = "根据部门id或模糊匹配查询部门关联的人员用户")
    ListResult<PersonDetailVO> queryDepartmentUsers(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = true) Long companyId,
                                                  @ApiParam(value = "部门id") @RequestParam(value = "departmentId", required = false) Long departmentId,
                                                  @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                  @ApiParam(value = "是否只包含关联用户的人员") @RequestParam(value = "onlyUser", required = false) Boolean onlyUser) {
        List<PersonDetailBO> persons = departmentService.queryDepartmentUsers(companyId, departmentId, keyword, onlyUser);
        if (persons == null) {
            return new ListResult<>();
        }
        List<PersonDetailVO> voList = new ArrayList<PersonDetailVO>();
        persons.stream().forEach(item -> {
            PersonDetailVO personDetailVO = new PersonDetailVO();
            BeanUtils.copyProperties(item, personDetailVO);
            voList.add(personDetailVO);
        });
        return new ListResult<>(voList);
    }

    /**
     * 分页查询部门关联人员用户
     * @param companyId　公司id
     * @param departmentId 部门id
     * @param keyword 关键字
     * @return
     */
    @GetMapping(value = {"/department/user/ref"})
    @ResponseBody
    @ApiOperation(value = "查询部门关联的人员用户", notes = "根据部门id或模糊匹配查询部门关联的人员用户")
    PageResult<PersonDetailUserVO> queryDepartmentUsers1(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = true) Long companyId,
                                                     @ApiParam(value = "部门id") @RequestParam(value = "departmentId", required = false) Long departmentId,
                                                     @ApiParam(value = "模糊匹配关键字(人员名或者用户名)") @RequestParam(value = "keyword", required = false) String keyword,
                                                     @ApiParam(value = "是否只包含关联用户的人员") @RequestParam(value = "onlyUser", required = false) Boolean onlyUser,
                                                     @ApiParam(value = "当前页码", required = false, defaultValue = "1") @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                                                     @ApiParam(value = "每页条数", required = false, defaultValue = "20") @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PersonDetailBO> pageResult = departmentService.queryDepartmentUsers1(companyId, departmentId, keyword, onlyUser, current, pageSize);
        Collection<PersonDetailBO> persons = pageResult.getList();
        List<PersonDetailUserVO> voList = new ArrayList<>();
        persons.stream().forEach(item -> {
            PersonDetailUserVO PersonDetailUserVO = new PersonDetailUserVO();
            BeanUtils.copyProperties(item, PersonDetailUserVO);
            Long personId = item.getId();
            Long userId = item.getUserId();
            PersonDetailUserVO.setId(userId);
            PersonDetailUserVO.setPersonId(personId);
            voList.add(PersonDetailUserVO);
        });
        Pagination pagination = pageResult.getPagination();
        return new PageResult(voList, pagination.getTotal(), pagination.getPageSize(), pagination.getCurrent());
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

/*    @GetMapping(value = "/department/base/company")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据公司id查询部门列表", notes = "根据公司id查询部门列表")
    public Result<JSONObject> queryDepartmentDetailRefInfo(@ApiParam(value = "公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = true) Long companyId) {
        JSONObject result = departmentService.queryDepartmentDetailRefInfo(companyId);
        if (result == null) {
            return new Result<>();
        }
        return new Result<>(result);
    }*/

    @GetMapping(value = "/departments/pages")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "加载部门列表", notes = "加载部门列表")
    public Result<PageResult<DepartmentDetailVO>> loadDepartments(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                      @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                      @ApiParam(value = "时间", required = false) @RequestParam(value = "fromTime", required = false) Long fromTime) {
        PageResult<DepartmentAddPO> pageResult = departmentService.loadDepartments(current, pageSize, fromTime);
        if (pageResult == null || pageResult.getPagination().getTotal() == 0) {
            return new Result<>(new PageResult<>(null, 0, pageSize, current));
        }
        List<DepartmentDetailVO> vos = new ArrayList<>();
        pageResult.getList().stream().forEach(depPo -> {
            DepartmentDetailVO depResultVO = new DepartmentDetailVO();
            BeanUtils.copyProperties(depPo, depResultVO);
            vos.add(depResultVO);
        });
        return new Result<>(new PageResult<DepartmentDetailVO>(vos, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent()));
    }

    @GetMapping(value = "/department/current/login/person")
    @ApiOperation(value = "查询当前登录人的当前公司所在的部门")
    public Result<DepartmentDetailVO> queryCurrentLoginPersonDepartment() {
        UserContext userContext = UserContext.getUserContext();
        if (userContext == null) {
            return new Result<>();
        }
        Long personId = userContext.getStaffId();
        Long companyId = userContext.getCompanyId();
        DepartmentDetailBO departmentDetailBO = departmentService.queryCurrentLoginPersonDepartment(personId, companyId);
        if (departmentDetailBO == null) {
            return new Result<>();
        }
        DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
        BeanUtils.copyProperties(departmentDetailBO, departmentDetailVO);
        return new Result<>(departmentDetailVO);
    }

    @GetMapping(value = "/departments/flow/codes")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据部门编码查询岗位id信息", notes = "根据部门编码查询岗位id信息")
    public ListResult<DepartmentFlowSimpleVO> queryDepartmentIdByCodes(@ApiParam(value = "部门编码", required = true) @RequestParam(value = "codes", required = true) List<String> codes) {
        List<DepartmentFlowSimpleBO> departmentFlowSimpleBOList = departmentService.queryDepartmentIdByCodes(codes);

        List<DepartmentFlowSimpleVO> departmentFlowSimpleVOList = new ArrayList<>();
        if (departmentFlowSimpleBOList == null) {
            return new ListResult<>(departmentFlowSimpleVOList);
        }
        departmentFlowSimpleBOList.stream().forEach(departmentFlowSimpleBO -> {
            DepartmentFlowSimpleVO departmentFlowSimpleVO = new DepartmentFlowSimpleVO();
            BeanUtils.copyProperties(departmentFlowSimpleBO, departmentFlowSimpleVO);
            departmentFlowSimpleVOList.add(departmentFlowSimpleVO);
        });
        return new ListResult<>(departmentFlowSimpleVOList);
    }
}
