package com.supcon.supfusion.organization.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.organization.dao.po.group.GroupPO;
import com.supcon.supfusion.organization.dao.po.group.GroupPersonPO;
import com.supcon.supfusion.organization.service.bo.group.GroupBO;
import com.supcon.supfusion.organization.service.bo.group.GroupPersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;

/**
 * @author lifangyuan
 */
public interface GroupService {
    /**
     * 新增一个组
     * @param groupBO 新增部门信息
     */
    void addGroup(GroupBO groupBO);

    /**
     * 修改一个组
     * @param groupBO 修改部门信息
     */
    void updateGroup(GroupBO groupBO);

    /**
     * 根据id删除指定
     * @param groupId 部门id
     */
    void deleteGroupById(Long groupId);


    /**
     * 根据id获取组信息
     * @param groupId 部门id
     */
    GroupBO getGroupInfoById(Long groupId);


    /**
     * 分页查询
     * @param page
     * @param groupBO
     * @param page
     * @return
     */
    Page<GroupBO> queryPageList(GroupBO groupBO, Page<GroupPO> page);

     void addGroupPerson(GroupPersonBO groupPersonBO) ;

    /**
     * 删除组关联的人员关系
     * @param groupId 组id
     * @param personIds 人员id
     */
    void deleteRelations(Long groupId, Long[] personIds);

    PageResult<PersonDetailBO> queryGroupPersons(Long groupId, String keyword, Integer current, Integer pageSize);


}
