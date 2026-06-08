package com.supcon.supfusion.organization.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.service.bo.baseService.StaffDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.person.*;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 人员管理服务接口
 * @author shidongsheng
 * @date 20-6-3  下午14:31
 */
public interface PersonService {

    /**
     * 人员新增
     *
     * @param personAddPo
     */
    void addPerson(PersonAddPO personAddPo, String tenantId);

    void addPersonAndUser(PersonAddPO personAddPo, String userName, String password, String userDescription, List<Long> roles, String tenantId);

    /**
     * 人员修改
     *
     * @param personAddPo
     * @param tenantId
     */
    void updatePerson(PersonAddPO personAddPo, String tenantId);

    /**
     * 批量删除id
     *
     * @param id
     * @param tenantId
     */
    void deletePerson(Long[] id, String tenantId);

    /**
     * 根据人员id查询列表
     *
     * @param personIds
     * @param conditionQuery
     * @return
     */
    PageResult<PersonDetailBO> queryPersonsById(List<Long> personIds, Integer current, Integer pageSize, PersonDetailBO conditionQuery);


    /**
     * 根据公司id查询
     *
     * @param companyId
     * @param positionIds
     * @param keyword
     * @param current
     * @param pageSize
     * @param conditionQuery
     * @return
     */
    PageResult<PersonDetailBO> queryPersonsByCompanyId(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery);

    /**
     * 根据人员id查询当前的详情,来加载修改页面
     *
     * @param personId
     * @return
     */
    PersonUpdatePageBO queryDetailByPersonId(Long personId);

    /**
     * 岗位调入
     *
     * @param personPositionTransferBO
     * @param tenantId
     */
    void transferPosition(PersonPositionTransferBO personPositionTransferBO, String tenantId);

    /**
     * 人员id
     *
     * @param personIds
     * @return
     */
    List<PersonDetailBO> queryPersonsById(Long[] personIds);

    /**
     * 根据分组id查询人员
     *
     * @param groudId
     * @param keyword
     * @param current
     * @param pageSize
     * @return
     */
    PageResult<PersonDetailBO> queryPersonsByGroupId(Long groudId, String keyword, Integer current, Integer pageSize);

    /**
     * 根据人员编码查询人员详情
     *
     * @param codes
     * @return
     */
    List<PersonDetailBO> queryPersonsByCodes(List<String> codes);

    /**
     * 导入数据文件
     *
     * @param workbook
     * @param taskId
     * @param companyId
     * @param fileName
     * @param tenantId
     * @param timeZone
     */
    void importExcel(XSSFWorkbook workbook, Long taskId, Long companyId, String fileName, String tenantId, String timeZone);

    /**
     * 下载文件
     *
     * @param file
     * @throws IOException
     */
    void downlowdExcelTemplate(File file) throws IOException;

    /**
     * 根据人员id查询所属公司
     *
     * @param personId
     * @return
     */
    List<CompanyBO> queryCompanIdByPersonIds(Long personId);

    /**
     * 导出人员数据
     *
     * @param ids
     * @param all
     * @param excelPOId
     * @param companyId
     * @param conditionQuery
     * @param keyword
     * @param orgId
     */
    void exportExcelData(List<Long> ids, Boolean all, Long excelPOId, Long companyId, PersonDetailBO conditionQuery, String keyword, Long orgId);

    /**
     * 通知中心根据角色, 岗位, 部门, 人员查询人员信息
     *
     * @param roleCodes
     * @param positionCodes
     * @param departmentCodes
     * @param personCodes
     * @return
     */
    List<PersonDTO> queryPersonByNotification(List<String> roleCodes, List<String> positionCodes, List<String> departmentCodes, List<String> personCodes);

    List<PersonBO> queryAllPersons();

    List<CompanyBO> queryCompanIdByPersonCode(String personCode);

    List<PersonDetailBO> queryPersonInfoByIds(List<Long> ids);

    List<PersonPositionBO> queryPersonPosition(Long id, Long companyId);

    void offPosition(PersonOffPositionBO personOffPositionBO, String tenantId);

    List<DepartmentDetailBO> queryPersonsDepartmentsByPersonIds(List<Long> ids);

    List<PositionDetailBO> queryPersonsPositionsByPersonIds(List<Long> ids);

    /**
     * 删除人员
     *
     * @param code     人员编号
     * @param tenantId
     */
    void deletePersonByCode(String code, String tenantId);

    /**
     * 根据code批量删除
     *
     * @param codes
     * @param tenantId
     */
    void batchDeletePersonByCode(List<String> codes, String tenantId);

    JSONObject getCurrentLoginInfo(Long id, String includes);

    StaffDetailInfoBO getStaff(Long id, String includes);

    JSONObject getStaffById(Long id, String includes);

    List<PositionDetailBO> queryPersonPositionsByPersonId(Long id);

    //------old version------
    JSONObject queryPersonList(String keywords, Boolean hasAccount, Boolean isAll, Boolean includeOrgs, Integer page, Integer per_page, String noBear, String tenantId);

    JSONObject queryPersonDetail(String code, String noBear, String tenantId);

    /**
     * 根据人员编码修改
     *
     * @param personAddPo
     * @return
     */
    boolean updatePersonByCode(PersonAddPO personAddPo);

    List<Long> queryRoleIdByPersonId(Long personId);

    Set<Long> queryPersonsByCompanyId(Long id);

    Long addVirtualPerson(String userName, Long positionId, String tenantId);

    List<PersonDetailBO> queryPersonsByPositionId(Long positionId);

    List<PersonDetailBO> queryPersonsByDepartmentId(Long departmentId);

    Boolean checkPersonSupAndSub(Long supPersonId, Long subPersonId, Long companyId);

    /**
     * 人员id
     *
     * @param personIds
     * @return
     */
    List<PersonDetailBO> queryPersonsById(List<Long> personIds);

    PersonDepartmentBO queryMainDepartmentByPersonId(Long id);

    PageResult<PersonDetailBO> loadPersons(Integer current, Integer pageSize, Long fromTime);

    PersonResultBO queryPersonByPersonCode(String code);

    PersonLeaderBO getPersonLeader(Long personId);

    PageResult<PersonBaseInfoBO> getPersonsByDepartmentCode(String departmentCode, Integer current, Integer pageSize);

    PageResult<PersonBaseInfoBO> getPersonsByPositionCode(String positionCode, Integer current, Integer pageSize);

    PageResult<PersonBaseInfoBO> getPersonsByCompanyCode(String companyCode, Integer current, Integer pageSize);

    PageResult<PersonSynchronizationInfoBO> getPersons(String modifyTime, Integer current, Integer pageSize);

    Result<PersonSynchronizationInfoBO> getPersonDetailByPersonCode(String personCode);

    void deletePersonById(Long personId);

    List<PersonFlowSimpleBO> queryPersonIdByCodes(List<String> codes);

    /**
     * 根据人员id保存或修改用户信息
     */
    void saveOrUpdateUserByPersonId(PersonUserBO personUserBO);

    /**
     * 根据人员id删除用户信息
     */
    void deleteUserByPersonIds(List<Long> personId);

    PersonUpdatePageBO queryDetailByPersonCode(String code);

    Integer countPersonUser();

    PersonBO getPersonByUserName(String userName);

    List<Long> queryMultiCompanyPersonsByCompanyId(Long companyId);

    /**
     * 根据人员ID获取人员信息（性别、头像url）
     *
     * @param staffId
     * @return
     */
    PersonLoginInfoBO getHeadImgById(Long staffId);

    /**
     * 文件上传到服务器指定目录
     *
     * @param uploadFile
     * @param tenantId
     * @return
     */
    Result fileUpload(MultipartFile uploadFile, String tenantId) throws IOException;

    /**
     * 下载附件
     *
     * @param filePaths 附件相对路径
     * @return
     */
    Map<String, String> downloadFile(String[] filePaths);

    /**
     * 批量新增,修改,删除人员
     *
     * @param personBulkOperateOpenBO
     * @param tenantId
     */
    void bulkOperate(PersonBulkOperateOpenBO personBulkOperateOpenBO, String tenantId);

    PageResult<PersonDetailBO> queryPersonsByCompanyIdBetter(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery);

    void updateAvatarUrl(String personCode, String filePath, String tenantId);

    PersonAddPO queryPersonPOById(Long id);

    PageResult<PersonDetailBO> queryPersonsAndOrgDetailByCompanyIdBetter(Long companyId, List<Long> positionIds, String keyword, Integer current, Integer pageSize, PersonDetailBO conditionQuery, Long currentPositionId);
}