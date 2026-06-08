package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;

import java.util.List;

/**
 * 岗位关联人员管理服务接口
 * @author shidongsheng
 * @date 20-6-4  下午14:31
 */
public interface PositionPersonService {

    /**
     * 岗位关联人员新增
     * @param relations
     */
    void addPositionPerson(List<PositionPersonPO> relations);

    /**
     * 根据岗位id查询人员id
     * @param positionId
     * @return
     */
    List<Long> queryPersonIdByPositionId(Long positionId);

    /**
     * 根据批量岗位id查询人员id
     * @param positionIds
     * @return
     */
    List<Long> queryPersonIdByPositionIds(List<Long> positionIds);

    /**
     * 条件查询公司下人员
     * @param companyId
     * @param keyword
     * @return
     */
    List<PersonDetailBO> queryPersonByCompanyId(Long companyId, String keyword);

    /**
     * 根据人员id查询所有岗位
     * @param personId
     * @return
     */
    List<PositionPersonPO> queryPositionByPersonId(Long personId);

    /**
     * 批量修改关联关系
     * @param list
     */
    void batchSaveOrUpdate(List<PositionPersonPO> list);

    /**
     * 删除人员岗位绑定关系
     * @param personId
     */
    void deleteByPersonId(Long personId);
    /**
     * 批量删除人员岗位关系
     * @param personIds
     */
    void batchDeleteByPersonId(List<Long> personIds);


    void batchDeleteRelations(List<PositionPersonPO> updateRels);

    /**
     * 根据人员id获取岗位人员信息
     *
     * @param personId
     * @return
     */
    List<PositionPersonPO> getByPersonId(Long personId);

    boolean saveBatchRel(List<PositionPersonPO> list);
}
