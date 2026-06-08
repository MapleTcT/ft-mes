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
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import com.supcon.supfusion.organization.service.bo.position.PositionFlowSimpleBO;
import com.supcon.supfusion.organization.service.bo.position.PositionKeywordBO;
import com.supcon.supfusion.organization.service.bo.position.PositionLocationBO;
import com.supcon.supfusion.organization.service.bo.position.PositionPersonBO;
import com.supcon.supfusion.organization.service.bo.position.PositionRoleBO;
import com.supcon.supfusion.organization.service.bo.position.PositionTreeBO;
import com.supcon.supfusion.organization.webapi.vo.person.PersonDetailUserVO;
import com.supcon.supfusion.organization.webapi.vo.person.PersonDetailVO;
import com.supcon.supfusion.organization.webapi.vo.position.*;
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
 * 岗位管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "岗位管理", description = "岗位文档说明", hidden = true)
public class PositionInterController {

    @Autowired
    private PositionService positionService;

    @Autowired
    private OrganizationManagerService organizationManagerService;

    /**
     * 岗位新增openapi借口
     * @param positionAddVO
     */
    @PostMapping(value = "/position")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "新增岗位", notes = "根据岗位信息新增一个岗位")
    Result<PositionDetailVO> addPosition(@Validated @RequestBody PositionAddVO positionAddVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionAddPO positionAddPO = new PositionAddPO();
        BeanUtils.copyProperties(positionAddVO, positionAddPO);
        positionService.addPosition(positionAddPO, positionAddVO.getManagerIds(), tenantId);
        PositionDetailVO positionDetailVO = new PositionDetailVO();
        positionDetailVO.setId(positionAddPO.getId());
        return new Result<PositionDetailVO>(positionDetailVO);
    }

    /**
     * 根据id修改岗位信息
     * @param positionUpdateVO
     */
    @PutMapping(value = "/position")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "修改岗位", notes = "修改一个岗位的信息")
    void updatePosition(@Validated @RequestBody PositionUpdateVO positionUpdateVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionAddPO positionAddPO = new PositionAddPO();
        BeanUtils.copyProperties(positionUpdateVO, positionAddPO);
        positionService.updatePosition(positionAddPO, positionUpdateVO.getManagerIds(), tenantId);
    }

    /**
     * 查询岗位详细信息
     * @param id 岗位id
     * @return
     */
    @GetMapping(value = "/position")
    @ResponseBody
    @ApiOperation(value = "查询一个岗位的详情", notes = "查询一个岗位的详情")
    Result<PositionDetailVO> getPosDetail(@ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam("id") Long id) {
        PositionDetailBO positionDetailBO = positionService.getPosDetail(id);
        PositionDetailVO positionDetailVO = new PositionDetailVO();
        BeanUtils.copyProperties(positionDetailBO, positionDetailVO);
        return new Result<PositionDetailVO>(positionDetailVO);
    }

    /**
     *  删除指定岗位
     * @param id 岗位id
     */
    @DeleteMapping(value = "/position/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据岗位id删除岗位", notes = "根据岗位id删除岗位")
    void deletePos(@ApiParam(value = "岗位id", required = true) @PathVariable("id") Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        positionService.deletePosById(id, tenantId);
    }

    /**
     * 修改岗位的位置
     * @param positionLocationVO
     */
    @PutMapping(value = "/position/location")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "拖拽移动岗位的位置", notes = "拖拽移动岗位的位置")
    void updatePosLocation(@Validated @RequestBody PositionLocationVO positionLocationVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionLocationBO positionLocationPO = new PositionLocationBO();
        BeanUtils.copyProperties(positionLocationVO, positionLocationPO);
        positionService.updatePosLocation(positionLocationPO, tenantId);
    }

    /**
     * 查询岗位树形结构
     * @param companyId 公司id
     * @param keyword 关键字
     * @return
     */
    @GetMapping(value = {"/positions", "/positions/ref"})
    @ResponseBody
    @ApiOperation(value = "查询岗位树形结构", notes = "查询岗位树形结构")
    Result<PositionTreeVO> getPosTree(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.POSITION_PARAM_COMPANYID_NECESSARY) @RequestParam("companyId") Long companyId,
                                      @ApiParam(value = "岗位id,传入时返回当前部门级上层岗位") @RequestParam(value = "positionId", required = false) Long positionId,
                                      @ApiParam(value = "模糊查询关键字") @RequestParam(value = "keyword", required = false) String keyword) {
        PositionTreeBO positionTreePO = positionService.getPosTree(companyId, keyword, positionId);
        PositionTreeVO positionTreeVO = new PositionTreeVO();
        BeanUtils.copyProperties(positionTreePO, positionTreeVO);
        List<PositionTreeBO> poList = positionTreePO.getChildren();
        if (poList == null || poList.size() == 0) {
            return new Result<PositionTreeVO>(positionTreeVO);
        }
        positionTreeVO.setChildren(new ArrayList<PositionTreeVO>());

        poToVo(positionTreePO.getChildren(), positionTreeVO.getChildren());
        return new Result<PositionTreeVO>(positionTreeVO);
    }

    /**
     * 增加岗位关联人员
     * @param positionPersonVO
     */
    @PostMapping(value = {"/position/person"})
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "给岗位绑定人员", notes = "给岗位绑定人员")
    void addPositionPerson(@Validated @RequestBody PositionPersonVO positionPersonVO) {
        if (positionPersonVO == null || positionPersonVO.getPersons() == null || positionPersonVO.getPersons().size() == 0) {
            return;
        }
        PositionPersonBO positionPersonBo = new PositionPersonBO();
        BeanUtils.copyProperties(positionPersonVO, positionPersonBo);
        positionService.addPositionPerson(positionPersonBo);
    }

    /**
     * 分页查询岗位关联人员
     * @param companyId　公司id
     * @param positionId 岗位id
     * @param keyword 关键字
     * @param current 当前页
     * @param pageSize 每页条数
     * @return
     */
    @GetMapping(value = {"/position/person", "/position/person/ref"})
    @ResponseBody
    @ApiOperation(value = "查询岗位关联的人员", notes = "根据岗位id或模糊匹配分页查询岗位关联的人员")
    PageResult<PersonDetailVO> queryPositionPerson(@ApiParam(value = "所属公司id", required = false) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = false) Long companyId,
                                                   @ApiParam(value = "岗位id") @RequestParam(value = "positionId", required = false) Long positionId,
                                                   @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                   @ApiParam(value = "当前页码", required = true) @Min (value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                   @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                   @ApiParam(value = "人员编码") @RequestParam(value = "code", required = false) String code,
                                                   @ApiParam(value = "人员名称") @RequestParam(value = "name",required = false) String name,
                                                   @ApiParam(value = "描述") @RequestParam(value = "description", required = false) String description,
                                                   @ApiParam(value = "邮箱") @RequestParam(value = "email", required = false) String email,
                                                   @ApiParam(value = "性别") @RequestParam(value = "gender", required = false) String gender,
                                                   @ApiParam(value = "手机号") @RequestParam(value = "phone", required = false) String phone,
                                                   @ApiParam(value = "状态") @RequestParam(value = "status", required = false) String status,
                                                   @ApiParam(value = "是否包含用户信息") @RequestParam(value = "includeUser", required = false) Boolean includeUser) {
      /*  UserContext userContext = UserContext.getUserContext();
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
        PageResult<PersonDetailBO> pageResult = positionService.queryPositionPersonsBetter(companyId, positionId, keyword, current, pageSize, conditionQuery, includeUser);
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

    /**
     * 非分页的方式查询人员
     * @param companyId
     * @param positionId
     * @param keyword
     * @param code
     * @param name
     * @param description
     * @param email
     * @param gender
     * @param phone
     * @param status
     * @return
     */
    @GetMapping(value = {"/position/condition/person", "/position/condition/person/ref"})
    @ResponseBody
    @ApiOperation(value = "查询岗位关联的人员不使用分页", notes = "根据岗位id或模糊匹配分页查询岗位关联的人员")
    ListResult<PersonDetailVO> queryPositionPersonNoPage(@ApiParam(value = "所属公司id", required = false) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = false) Long companyId,
                                                         @ApiParam(value = "岗位id") @RequestParam(value = "positionId", required = false) Long positionId,
                                                         @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                         @ApiParam(value = "人员编码") @RequestParam(value = "code", required = false) String code,
                                                         @ApiParam(value = "人员名称") @RequestParam(value = "name",required = false) String name,
                                                         @ApiParam(value = "描述") @RequestParam(value = "description", required = false) String description,
                                                         @ApiParam(value = "邮箱") @RequestParam(value = "email", required = false) String email,
                                                         @ApiParam(value = "性别") @RequestParam(value = "gender", required = false) String gender,
                                                         @ApiParam(value = "手机号") @RequestParam(value = "phone", required = false) String phone,
                                                         @ApiParam(value = "状态") @RequestParam(value = "status", required = false) String status) {
        PersonDetailBO conditionQuery = new PersonDetailBO();
        conditionQuery.setCode(code);
        conditionQuery.setName(name);
        conditionQuery.setDescription(description);
        conditionQuery.setEmail(email);
        conditionQuery.setGender(gender);
        conditionQuery.setPhone(phone);
        conditionQuery.setStatus(status);
        List<PersonDetailBO> personList = positionService.queryPositionPersonNoPage(companyId, positionId, keyword, conditionQuery);
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

    @GetMapping(value = {"/position/keyword", "/position/keyword/ref"})
    @ResponseBody
    @ApiOperation(value = "岗位模糊查询列表", notes = "岗位模糊查询列表")
    ListResult<PositionKeywordVO> queryPositionByKeyword (@ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                          @ApiParam(value = "公司id") @RequestParam(value = "companyId", required = true) Long companyId) {
        List<PositionKeywordBO> list = positionService.queryPositionsByKeyword(keyword, companyId);
        List<PositionKeywordVO> results = new ArrayList<PositionKeywordVO>();
        if (list == null || list.size() == 0) {
            return new ListResult<PositionKeywordVO>(results);
        }

        list.stream().forEach(bo -> {
            PositionKeywordVO positionKeywordVO = new PositionKeywordVO();
            BeanUtils.copyProperties(bo, positionKeywordVO);
            results.add(positionKeywordVO);
        });
        return new ListResult<PositionKeywordVO>(results);
    }

    /**
     * 删除岗位关联的人员
     * @param positionId 岗位id
     * @param personIds 人员id,批量则分号分割
     */
    @DeleteMapping(value = "/position/{positionId}/person/{personIds}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "删除岗位人员关联", notes = "删除岗位人员关联")
    void deleteRelation(@ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @PathVariable("positionId") Long positionId,
                        @ApiParam(value = "人员id,批量人员则;分号分割", required = true) @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @PathVariable("personIds") Long[] personIds) {
        positionService.deleteRelations(positionId, personIds);
    }

    /**
     * 为岗位关联人员
     */
    @PostMapping(value = "/position/role")
    @ApiOperation(value = "为岗位关联角色", notes = "为岗位关联角色")
    void addPositionRole(@RequestBody PositionRoleAddVO positionRoleAddVO) {
        positionService.addPositionRole(positionRoleAddVO.getRoleIds(), positionRoleAddVO.getPositionId());
    }

    @DeleteMapping(value = "/position/role")
    @ApiOperation(value = "删除岗位关联角色", notes = "删除岗位关联角色")
    void deletePositionRole(@ApiParam(value = "角色id", required = true) @NotNull(message = Constants.POSITION_ROLE_ID_PARAM_NECESSARY) @RequestParam(value = "roleId", required = true) Long roleId,
                            @ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam(value = "positionId", required = true) Long positionId) {
        positionService.deletePositionRole(roleId, positionId);
    }

    @GetMapping(value = "/position/role")
    @ApiOperation(value = "查询岗位关联的角色")
    ListResult<PositionRoleVO> queryPositionRole(@ApiParam(value = "岗位id", required = true) @NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam(value = "positionId", required = true) Long positionId) {
        List<PositionRoleBO> list = positionService.queryPositionRole(positionId);
        List<PositionRoleVO> results = new ArrayList<PositionRoleVO>();
        list.stream().forEach(bo -> {
            PositionRoleVO positionRoleVO = new PositionRoleVO();
            BeanUtils.copyProperties(bo, positionRoleVO);
            results.add(positionRoleVO);
        });
        return new ListResult<PositionRoleVO>(results);
    }
    /**
     * 查询树形结构中PO对象转BO对象
     * @param poList
     * @param voList
     */
    private void poToVo(List<PositionTreeBO> poList, List<PositionTreeVO> voList) {
        poList.stream().forEach(poDep -> {
            PositionTreeVO voDep = new PositionTreeVO();
            BeanUtils.copyProperties(poDep, voDep);
            voList.add(voDep);
            if (poDep.getChildren() != null && poDep.getChildren().size() > 0) {
                voDep.setChildren(new ArrayList<PositionTreeVO>());
                poToVo(poDep.getChildren(), voDep.getChildren());
            }
        });
    }

    /**
     * 根据部门id批量查询岗位信息
     * @param ids
     * @return
     */
    @GetMapping("/positions/ids")
    @ResponseBody
    @ApiOperation(value = "根据部门id批量查询岗位信息", notes = "根据部门id批量查询岗位信息")
    public ListResult<PositionDetailVO> queryDeptInfoByIds(@ApiParam(value = "岗位ids") @RequestParam(value = "ids", required = true) List<Long> ids) {
        List<PositionDetailBO> list = positionService.queryPosInfoByIds(ids);
        if (list == null || list.size() == 0) {
            return new ListResult<PositionDetailVO>(new ArrayList<PositionDetailVO>());
        }
        List<PositionDetailVO> vos = new ArrayList<PositionDetailVO>();
        list.stream().forEach(bo -> {
            PositionDetailVO posDetailVO = new PositionDetailVO();
            BeanUtils.copyProperties(bo, posDetailVO);
            vos.add(posDetailVO);
        });
        return new ListResult<PositionDetailVO>(vos);
    }

    /**
     * 分页查询岗位关联人员用户
     * @param companyId　公司id
     * @param positionId 岗位id
     * @param keyword 关键字
     * @return
     */
    @GetMapping(value = {"/position/user/ref1"})
    @ResponseBody
    @ApiOperation(value = "查询岗位关联的人员用户", notes = "根据岗位id或模糊匹配分页查询岗位关联的人员用户")
    ListResult<PersonDetailVO> queryPositionUsers(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = true) Long companyId,
                                                   @ApiParam(value = "岗位id") @RequestParam(value = "positionId", required = false) Long positionId,
                                                   @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                  @ApiParam(value = "是否只包含关联用户的人员") @RequestParam(value = "onlyUser", required = false) Boolean onlyUser) {
        List<PersonDetailBO> persons = positionService.queryPositionUsers(companyId, positionId, keyword, onlyUser);
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
     * 分页查询岗位关联人员用户
     * @param companyId　公司id
     * @param positionId 岗位id
     * @param keyword 关键字
     * @return
     */
    @GetMapping(value = {"/position/user/ref"})
    @ResponseBody
    @ApiOperation(value = "查询岗位关联的人员用户", notes = "根据岗位id或模糊匹配分页查询岗位关联的人员用户")
    PageResult<PersonDetailUserVO> queryPositionUsers1(@ApiParam(value = "所属公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "companyId", required = true) Long companyId,
                                                   @ApiParam(value = "岗位id") @RequestParam(value = "positionId", required = false) Long positionId,
                                                   @ApiParam(value = "模糊匹配关键字(人员名或者用户名)") @RequestParam(value = "keyword", required = false) String keyword,
                                                  @ApiParam(value = "是否只包含关联用户的人员") @RequestParam(value = "onlyUser", required = false) Boolean onlyUser,
                                                   @ApiParam(value = "当前页码", required = false, defaultValue = "1") @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                                                   @ApiParam(value = "每页条数", required = false, defaultValue = "20") @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PersonDetailBO> pageResult = positionService.queryPositionUsers1(companyId, positionId, keyword, onlyUser, current, pageSize);
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


    /**
     * 加载岗位列表
     * @param current
     * @param pageSize
     * @param fromTime
     * @return
     */
    @GetMapping(value = "/positions/pages")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "加载岗位列表", notes = "加载岗位列表")
    public Result<PageResult<PositionDetailVO>> loadPositions(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                          @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                          @ApiParam(value = "时间", required = false) @RequestParam(value = "fromTime", required = false) Long fromTime) {
        PageResult<PositionAddPO> pageResult = positionService.loadPositions(current, pageSize, fromTime);
        if (pageResult == null || pageResult.getPagination().getTotal() == 0) {
            return new Result<>(new PageResult<>(null, 0, pageSize, current));
        }
        List<PositionDetailVO> vos = new ArrayList<>();
        pageResult.getList().stream().forEach(posPo -> {
            PositionDetailVO posResultVO = new PositionDetailVO();
            BeanUtils.copyProperties(posPo, posResultVO);
            vos.add(posResultVO);
        });
        return new Result<>(new PageResult<PositionDetailVO>(vos, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent()));
    }

    @GetMapping(value = "/positions/flow/codes")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据岗位编码查询岗位id信息", notes = "根据岗位编码查询岗位id信息")
    public ListResult<PositionFlowSimpleVO> queryPositionIdByCodes(@ApiParam(value = "岗位编码", required = true) @RequestParam(value = "codes", required = true) List<String> codes) {
        List<PositionFlowSimpleBO> positionFlowSimpleBOList = positionService.queryPositionIdByCodes(codes);

        List<PositionFlowSimpleVO> positionFlowSimpleVOList = new ArrayList<>();
        if (positionFlowSimpleBOList == null) {
            return new ListResult<>(positionFlowSimpleVOList);
        }
        positionFlowSimpleBOList.stream().forEach(positionFlowSimpleBO -> {
            PositionFlowSimpleVO positionFlowSimpleVO = new PositionFlowSimpleVO();
            BeanUtils.copyProperties(positionFlowSimpleBO, positionFlowSimpleVO);
            positionFlowSimpleVOList.add(positionFlowSimpleVO);
        });
        return new ListResult<>(positionFlowSimpleVOList);
    }
}
