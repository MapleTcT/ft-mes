package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.group.GroupPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.service.bo.group.GroupPersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;

import java.util.List;


/**
 * 组关联人员管理服务接口
 * @author lifangyuan
 */
public interface GroupPersonService {


    /**
     * 岗位关联人员新增
     * @param relations
     */
    void addGroupPerson(List<GroupPersonPO> relations);

    /**
     * 删除人员和组织的关系
     * @param personId 人员id
     */
    void deleteByPersonId(Long personId);
    /**
     * 批量删除人员和组织关系
     * @param personIds
     */
    void batchDeleteByPersonId(List<Long> personIds);

   List<Long> queryPersonIdByGroupId(Long groupId);

   void batchSaveOrUpdate(List<GroupPersonPO> list);

    
}
