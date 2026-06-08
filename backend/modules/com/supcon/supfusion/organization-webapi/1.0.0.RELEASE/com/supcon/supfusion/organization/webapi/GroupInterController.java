package com.supcon.supfusion.organization.webapi;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.common.constants.Constants;

import com.supcon.supfusion.organization.service.GroupPersonService;
import com.supcon.supfusion.organization.service.GroupService;
import com.supcon.supfusion.organization.service.OrganizationManagerService;
import com.supcon.supfusion.organization.service.bo.group.GroupBO;
import com.supcon.supfusion.organization.service.bo.group.GroupPersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.webapi.vo.group.*;
import com.supcon.supfusion.organization.webapi.vo.person.PersonDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 组管理接口
 *
 * @author lifangyuan
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "组管理", description = "组管理文档说明", hidden = true)
public class GroupInterController {

    @Resource
    private GroupService groupService;


    @Autowired
    private OrganizationManagerService organizationManagerService;
    
    @Resource
    private GroupPersonService groupPersonService;
    

    /**
     * 组新增openapi借口
     *
     * @param groupAddVO
     */
    @PostMapping(value = "/group")
    @ResponseBody
    void addDepartment(@Validated @RequestBody GroupAddVO groupAddVO) {
        GroupBO groupBO = new GroupBO();
        BeanUtils.copyProperties(groupAddVO, groupBO);
        groupBO.setId(IDGenerator.newInstance().generate().longValue());
        groupService.addGroup(groupBO);
        if (groupAddVO.getManagerIds() != null) {
            organizationManagerService.addManager(groupAddVO.getManagerIds(), groupBO.getId(), Constants.GROUP);
        }
        
    }

    /**
     * 修改部门信息
     *
     * @param groupUpdateVO
     */
    @PutMapping(value = "/group")
    @ResponseBody
    void updateDepartment(@Validated @RequestBody GroupUpdateVO groupUpdateVO) {
        GroupBO po = new GroupBO();
        BeanUtils.copyProperties(groupUpdateVO, po);
        groupService.updateGroup(po);
        if (groupUpdateVO.getManagerIds() != null) {
            organizationManagerService.addManager(groupUpdateVO.getManagerIds(), groupUpdateVO.getId(), Constants.GROUP);
        }
    }

    /**
     * 删除指定部门
     *
     * @param id
     */
    @DeleteMapping(value = "/group/{id}")
    @ResponseBody
    void deleteDep(@PathVariable("id") Long id) {
        groupService.deleteGroupById(id);
    }

    /**
     * 查询组详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/group")
    @ResponseBody
    Result<GroupDetailVO> getGroupDetail(@NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY) @RequestParam("id") Long id) {
        GroupBO groupBO = groupService.getGroupInfoById(id);
        GroupDetailVO detailVO = new GroupDetailVO();
        detailVO.setCode(groupBO.getCode());
        detailVO.setDescription(groupBO.getDescription());
        detailVO.setName(groupBO.getName());
        detailVO.setManagers(groupBO.getManagers());
        return new Result<>(detailVO);
    }

    /**
     * 查询组详细信息
     *
     * @param companyId
     * @return
     */
    @GetMapping(value = "/groups")
    @ResponseBody
    PageResult<GroupVO> getGroups(@RequestParam("keyword") String keyword,@RequestParam("companyId") Long companyId,@RequestParam("current") Integer current, @RequestParam("pageSize") Integer pageSize) {
        Page page = new Page<>(current, pageSize);
        GroupBO GroupBO = new GroupBO();
        GroupBO.setCompanyId(companyId);
        if(!StringUtils.isEmpty(keyword)){
            GroupBO.setName(keyword);
        }
        Page<GroupBO> entityPage = groupService.queryPageList(GroupBO, page);
        List<GroupBO> records = entityPage.getRecords();
        List<GroupVO> groupPageVOS = new ArrayList<>();
        for (GroupBO po:records) {
            GroupVO groupVO = new GroupVO();
            groupVO.setId(po.getId());
            groupVO.setName(po.getName());
            groupPageVOS.add(groupVO);
        }
        return new PageResult(groupPageVOS, entityPage.getTotal(),entityPage.getSize(),entityPage.getCurrent());
    }


//    /**
//     * 查询组详细信息
//     *
//     * @param groupPageVo
//     * @return
//     */
//    @GetMapping(value = "/groups/search")
//    @ResponseBody
//    PageResult<GroupVO> getGroups(@RequestBody GroupPageVo groupPageVo) {
//        Page page = new Page<>(groupPageVo.getCurrent(), groupPageVo.getPageSize());
//        GroupBO GroupBO = new GroupBO();
//        GroupBO.setCompanyId(groupPageVo.getCompanyId());
//        GroupBO.setName(groupPageVo.getName());
//        Page<GroupBO> entityPage = groupService.queryPageList(GroupBO, page);
//        List<GroupBO> records = entityPage.getRecords();
//        List<GroupVO> groupPageVOS = new ArrayList<>();
//        for (GroupBO po:records) {
//            GroupVO groupVO = new GroupVO();
//            groupVO.setId(po.getId());
//            groupVO.setName(po.getName());
//            groupPageVOS.add(groupVO);
//        }
//        return new PageResult(groupPageVOS, entityPage.getTotal(),entityPage.getSize(),entityPage.getCurrent());
//    }

    /**
     * 增加组关联人员
     * @param groupPersonVO
     */
    @PostMapping(value = "/group/person")
    @ResponseStatus(HttpStatus.OK)
    void addPositionPerson (@Validated @RequestBody GroupPersonVO groupPersonVO) {
        if (groupPersonVO.getPersons() == null || groupPersonVO.getPersons().size() == 0) {
            return;
        }
        GroupPersonBO groupPersonBO = new GroupPersonBO();
        BeanUtils.copyProperties(groupPersonVO, groupPersonBO);
        groupService.addGroupPerson(groupPersonBO);
    }

    /**
     * 删除组关联的人员
     * @param positionId 岗位id
     * @param personIds 人员id,批量则分号分割
     */
    @DeleteMapping(value = "/group/{groupId}/person/{personIds}")
    @ResponseStatus(HttpStatus.OK)
    void deleteRelation(@NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @PathVariable("groupId") Long positionId,
                        @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @PathVariable("personIds") Long[] personIds) {
        groupService.deleteRelations(positionId, personIds);
    }

    /**
     * 分页查询岗位关联人员
     * @param groupId　公司id
     * @param groupId 组id
     * @param keyword 关键字
     * @param current 当前页
     * @param pageSize 每页条数
     * @return
     */
    @GetMapping(value = "/group/person")
    @ResponseBody
    PageResult<PersonDetailVO> queryGroupPerson(@RequestParam(value = "groupId", required = false) Long groupId,
                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                   @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                  @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<PersonDetailBO> pageResult = groupService.queryGroupPersons(groupId, keyword, current, pageSize);
        if (pageResult == null || pageResult.getList() == null) {
            return new PageResult<PersonDetailVO>(new ArrayList<PersonDetailVO>(), 0,pageSize, current);
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
}
