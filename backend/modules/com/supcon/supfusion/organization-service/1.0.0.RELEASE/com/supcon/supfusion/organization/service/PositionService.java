package com.supcon.supfusion.organization.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.position.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;


/**
 * 岗位服务接口
 * @author
 * @date 20-5-26 上午10:46
 */
public interface PositionService {

    /**
     * 新增一个岗位
     * @param positionAddPO 新增岗位信息
     * @param managerIds
     * @param tenantId
     */
    void addPosition(PositionAddPO positionAddPO, List<Long> managerIds, String tenantId);

    /**
     * 修改一个岗位
     * @param positionAddPO 修改岗位信息
     * @param managerIds
     * @param tenantId
     */
    void updatePosition(PositionAddPO positionAddPO, List<Long> managerIds, String tenantId);

    /**
     * 根据岗位id查询岗位详细信息
     * @param posId 岗位id
     * @return
     */
    PositionDetailBO getPosDetail(Long posId);

    /**
     * 根据岗位id删除指定
     * @param posId 岗位id
     * @param tenantId
     */
    void deletePosById(Long posId, String tenantId);

    /**
     * 调整岗位的位置
     * @param positionLocationPO 调整岗位顺序
     * @param tenantId
     */
    void updatePosLocation(PositionLocationBO positionLocationPO, String tenantId);

    /**
     * 岗位树形结构
     * @param companyId 公司id
     * @param parentId 上级部门id
     * @param keyword 关键字
     * @return
     */
    PositionTreeBO getPosTree(Long companyId, Long parentId, String keyword);

    List<JSONObject> getPosTree(Long treeId, Long companyId);
    /**
     * 岗位树形结构,全量返回
     * @param companyId 公司id
     * @param keyword 关键字
     * @return
     */
    PositionTreeBO getPosTree(Long companyId, String keyword, Long positionId);

    /**
     * 新增岗位人员关系
     * @param positionPersonBO
     */
    void addPositionPerson(PositionPersonBO positionPersonBO);

    /**
     *  @param companyId
     * @param positionId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     */
    PageResult<PersonDetailBO> queryPositionPersons(Long companyId, Long positionId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser);

    List<PersonDetailBO> queryPositionPersonNoPage(Long companyId, Long positionId, String keyword, PersonDetailBO conditionQuery);

    /**
     * 根据岗位id查询岗位
     * @param positionIds
     * @return
     */
    List<PositionAddPO> queryPositionByIds(List<Long> positionIds);

    /**
     * 根据部门id查询所有岗位id
     * @param departmentId
     * @return
     */
    List<Long> queryPositionIdsbyDeptId(Long departmentId);

    /**
     * 删除岗位关联的人员关系
     * @param positionId 岗位id
     * @param personIds 人员id
     */
    void deleteRelations(Long positionId, Long[] personIds);

    /**
     * 模糊查询岗位列表
     * @param keyword
     * @param companyId
     * @return
     */
    List<PositionKeywordBO> queryPositionsByKeyword(String keyword, Long companyId);

    void addPositionRole(List<Long> ids, Long positionId);

    void deletePositionRole(Long roleId, Long positionId);

    List<PositionRoleBO> queryPositionRole(Long positionId);

    void importExcel(XSSFWorkbook workbook, Long id, Long companyId, String originalFilename, String tenantId, String s);

    void downlowdExcelTemplate(File file) throws IOException;

    void exportExcelData(List<Long> ids, Boolean all, Long id, Long companyId);

    void exportPersonExcelData(List<Long> ids, Boolean all, Long id, Long deptId);

    List<PositionDetailBO> queryPosInfoByIds(List<Long> ids);

    List<PositionDetailBO> queryPositionByCodes(List<String> codes);

    List<Long> querySubPositionIdsByPositionId(List<Long> id);

    boolean updateBatchById(Collection<PositionAddPO> entityList);

    /**
     * 根据code查询详情
     * @param code
     * @return
     */
    PositionDetailBO getPosDetailByCode(String code);

    /**
     * 根据部门id查询所有岗位id
     * @param companyId
     * @return
     */
    List<Long> queryPositionIdsbyCompanyId(Long companyId);

    JSONObject getPositionById(Long id, String includes);

    List<PositionDetailBO> querySubPositionByParentId(Long id, Boolean all, Long cid);

    //======old version======
    void addOldPosition(JSONObject body, String tenantId);

    List<PersonDetailBO> queryPositionUsers(Long companyId, Long positionId, String keyword, Boolean onlyUser);

    PageResult<PersonDetailBO> queryPositionUsers1(Long companyId, Long positionId, String keyword, Boolean onlyUser, Integer current, Integer pageSize);

    Long addVirtualPos(Long companyId, Long depId, String tenantId);

    PageResult<PositionAddPO> loadPositions(Integer current, Integer pageSize, Long fromTime);

    PageResult<PersonResultBO> queryPersonsByPositionId(Long positionIdId, Integer current, Integer pageSize);

    PageResult<PositionDetailBO> queryPositionsPage(Integer current, Integer pageSize);

    Boolean checkPositionSupAndSub(Long supPositionId, Long subPositionId);

    PageResult<PositionSynchronizationInfoBO> getPositions(String modifyTime, Integer current, Integer pageSize);

    PageResult<PositionBaseInfoBO> getPositionsByCompanyCode(String companyCode, Integer current, Integer pageSize);

    Boolean checkRolesExistPosition(List<Long> roleIds);

    List<PositionFlowSimpleBO> queryPositionIdByCodes(List<String> codes);

    PageResult<PositionSynchronizationInfoBO> getPositionsByPage(Integer current, Integer pageSize);

    PageResult<PositionDetailInfoBO> querySubPositionByParentCode(String positionCode, Boolean all, Integer current, Integer pageSize);

    String getPositionCodeById(Long positionId);

    List<PositionAddPO> listDepartments();

    Result<PositionDetailInfoBO> getPositionByCode(String positionCode);

    List<PositionDetailInfoBO> getPositionsByDepartment(DepartmentDetailBO departmentDetailBO);

    void publishDeletePositionMessage(List<PositionAddPO> positionPoList, String tenantId);

    PageResult<PersonDetailBO> queryPositionPersonsBetter(Long companyId, Long positionId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser);
}
