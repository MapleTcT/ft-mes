package com.supcon.supfusion.organization.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentFlowSimpleBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentKeywordBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentLocationBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentTreeBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;


/**
 * 部门服务接口
 * @author
 * @date 20-5-20 上午10:46
 */
public interface DepartmentService  {

    /**
     * 新增一个部门
     * @param departmentAddPO 新增部门信息
     * @param managerIds
     * @param tenantId
     */
    void addDepartment(DepartmentAddPO departmentAddPO, List<Long> managerIds, String tenantId);

    /**
     * 修改一个部门
     * @param departmentAddPO 修改部门信息
     * @param managerIds
     * @param tenantId
     */
    void updateDepartment(DepartmentAddPO departmentAddPO, List<Long> managerIds, String tenantId);

    /**
     * 根据部门id查询部门详细信息
     * @param depId 部门id
     * @return
     */
    DepartmentDetailBO getDepDetail(Long depId);

    /**
     * 根据部门id删除指定
     * @param depId 部门id
     * @param tenantId
     */
    void deleteDepById(Long depId, String tenantId);

    /**
     * 吸怪部门的位置
     * @param departmentLocationPO 调整部门顺序
     * @param tenantId
     */
    void updateDepLocation(DepartmentLocationBO departmentLocationPO, String tenantId);

    /**
     * 部门树形结构
     * @param companyId 公司id
     * @param parentId 上级部门id
     * @param keyword 关键字
     * @return
     */
    //DepartmentTreeBO getDepTree(Long companyId, Long parentId, String keyword);

    /**
     * 部门树形结构,全量返回
     * @param companyId 公司id
     * @param keyword 关键字
     * @return
     */
    DepartmentTreeBO getDepTree(Long companyId, String keyword, Long departmentId);

    /**
     * 查询部门关联的人员
     * @param companyId
     * @param positionId
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @param includeUser
     */
    PageResult<PersonDetailBO> queryDepartmentPersons(Long companyId, Long positionId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser);

    List<PersonDetailBO> queryDepartmentPersonNoPage(Long companyId, Long positionId, String keyword, PersonDetailBO conditionQuery);
    /**
     * 模糊查询部门列表
     * @param keyword
     * @param companyId
     * @return
     */
    List<DepartmentKeywordBO> queryDepartmentsByKeyword(String keyword, Long companyId);

    /**
     * 导入数据
     * @param workbook
     * @param id
     * @param companyId
     * @param fileName
     * @param tenantId
     */
    void importExcel(XSSFWorkbook workbook, Long id, Long companyId, String fileName, String tenantId);

    /**
     * 下载模板
     * @param file
     * @throws IOException
     */
    void downlowdExcelTemplate(File file) throws IOException;

    /**
     * 导出数据到excel
     * @param ids 选择的部门
     * @param all 是否全部
     * @param taskId 任务id
     * @param companyId 公司id
     */
    void exportExcelData(List<Long> ids, Boolean all, Long taskId, Long companyId);

    /**
     * 导出部门关联的人员
     * @param ids
     * @param all
     * @param taskId
     * @param deptId
     */
    void exportPersonExcelData(List<Long> ids, Boolean all, Long taskId, Long deptId);

    /**
     *  根据部门ids批量查询部门信息
     * @param ids
     * @return
     */
    List<DepartmentDetailBO> queryDeptInfoByIds(List<Long> ids);

    List<DepartmentDetailBO> queryDepartmentByCodes(List<String> codes);

    List<Long> querySubDepartmentIdsByDepartmentId(List<Long> ids);

    DepartmentDetailBO queryCurrentUserDept(String personCode, Long companyId);

    /**
     * 根据code查询详情
     * @param code
     * @return
     */
    DepartmentDetailBO getDepDetailByCode(String code);

    JSONObject getDepartmentById(Long id, String includes);

    List<JSONObject> getDeptTree(Long treeId, Long companyId);

    List<DepartmentDetailBO> querySubDepartmentByParentId(Long id, Boolean all, Long cid);

    boolean updateBatchByIds(Collection<DepartmentAddPO> entityList);

    //-------old version----
    void addOldDepartment(JSONObject body, String tenantId);


    List<PersonDetailBO> queryDepartmentUsers(Long companyId, Long departmentId, String keyword, Boolean onlyUser);

    PageResult<PersonDetailBO> queryDepartmentUsers1(Long companyId, Long departmentId, String keyword, Boolean onlyUser, Integer current, Integer pageSize);

    Long addVirtualDept(Long companyId, String tenantId);

    JSONObject queryDepartmentDetailRefInfo(Long companyId);

    PageResult<DepartmentAddPO> loadDepartments(Integer current, Integer pageSize, Long fromTime);

    PageResult<PersonResultBO> queryPersonsByDepartmentId(Long departmentId, Integer current, Integer pageSize);

    PageResult<DepartmentDetailBO> queryDepartmentsPage(Integer current, Integer pageSize);

    DepartmentDetailBO queryCurrentLoginPersonDepartment(Long personId, Long companyId);

    PageResult<DepartmentSynchronizationInfoBO> getDepartments(String modifyTime, Integer current, Integer pageSize);

    PageResult<DepartmentBaseInfoBO> getDepartmentsByCompanyCode(String companyCode, Integer current, Integer pageSize);

    Result<DepartmentDetailInfoBO> getDepartmentByCode(String departmentCode);

    List<DepartmentFlowSimpleBO> queryDepartmentIdByCodes(List<String> codes);


    /**
     * 根据departmentId查询code
     *
     * @param code
     * @return
     */
    DepartmentAddPO getDepartmentAddPoByCode(String code);

    /**
     * 查询子部门列表
     *
     * @param id
     * @param all
     * @param cid
     * @param current
     * @param pageSize
     * @return
     */
    PageResult<DepartmentDetailInfoBO> querySubDepartmentInfoByParentId(Long id, Boolean all, Long cid, Integer current, Integer pageSize);

    String getDepartmentCodeById(Long deptId);

    List<DepartmentAddPO> listDepartments();

    PageResult<PersonDetailBO> queryDepartmentPersonsBetter(Long companyId, Long departmentId, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Boolean includeUser);
}
