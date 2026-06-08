package com.supcon.supfusion.organization.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyKeywordBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.position.PositionRoleBO;

import java.util.List;

/**
 * @Description: 公司服务
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
public interface CompanyService {

    /**
     * @Description: 查询公司列表
     * @Author:     HUNING
     */
    List<CompanyPO> listCompanies();

    /**
     * @Description: 查询单个公司信息
     * @Author:     HUNING
     */
    CompanyPO findCompany(Long id);

    /**
     * @Description: 新增/修改 公司信息
     * @Author:     HUNING
     */
    void addCompany(CompanyPO companyPO, List<String> tags, String userName, String password, String tenantId);

    /**
     * @Description: 新增/修改 公司信息
     * @Author:     HUNING
     */
    void saveOrUpdateCom(CompanyPO companyPO, List<String> tags, String tenantId);

    /**
     * @Description: 根据父节点Id查询子公司列表
     * @Author:     HUNING
     */
    void delCompany(Long id, String tenantId);

    List<CompanyPO> getSubCompanies(Long companyId, Long selectCompanyId, String keyword);

    List<CompanyKeywordBO> queryCompaniesByKeyword(String keyword, Long companyId);

    List<PositionRoleBO> queryCompanyRoles(Long companyId);

    JSONObject getCompanyById(Long id, String includes);

    CompanyBO getCompanyById(Long id);

    List<CompanyBO> queryAllCompanies();

    //---------------------------------old version----------------

    /**
     * 查询公司列表
     * @param keywords
     * @param page
     * @param per_page
     * @return
     */
    JSONObject listCompanies(String keywords, Integer page, Integer per_page);

    /**
     * 查询公司详情
     * @param orgName
     * @return
     */
    JSONObject queryCompanyDetail(String orgName);

    /**
     * 修改公司
     * @param orgName
     * @param body
     * @param tenantId
     */
    void updateCompany(String orgName, JSONObject body, String tenantId);

    /**
     * 批量查询组织树所有节点(公司,部门,岗位),结果为平铺结构
     * @param orgName
     * @param orgType
     * @param keywords
     * @param page
     * @param per_page
     * @return
     */
    JSONObject queryOrganizationTileStruct(String orgName, String orgType, String keywords, Integer page, Integer per_page);

    void batchDeleteOrg(JSONObject body, String tenantId);

    JSONObject queryOrgDetail(String orgName, String nodeName);

    void updateOrg(String orgName, String nodeName, JSONObject body, String tenantId);

    void deleteOrg(String orgName, String nodeName, String noBear, String tenantId);

    JSONObject queryOrgCorrelation(String nodeName, Integer page, Integer per_page);

    JSONObject queryCorrelationPerson(String nodeName, String keywords, Integer page, Integer per_page);

    void addCorrelationForOrg(String nodeName, JSONObject body);

    void deleteCorrelationPerson(String nodeName, JSONObject body);

    void addCorNode(String nodeName, JSONObject body);


    JSONObject getOrgTree(String orgName, String path, String deep, String nodeName, String noBear, String orgType, String tenantId);

    JSONObject getPersonChose(String nodeName, String curNodeName, String type, Boolean isAll, String keywords, Integer page, Integer per_page, String noBear, String tenantId);

    JSONObject queryOrgDetailByCode(String code, String type);

    CompanyPO findCompanyByCode(String code);

    List<Long> querySupCompaniesById(Long companyId);

    PageResult<CompanyPO> loadCompanies(Integer current, Integer pageSize, Long fromTime);

    PageResult<PersonResultBO> queryPersonsByCompanyId(Long companyId, Integer current, Integer pageSize);

    PageResult<CompanyBO> queryCompaniesPages(Integer current, Integer pageSize);

    PageResult<CompanyDetailInfoBO> getCompanies(String modifyTime, Integer current, Integer pageSize);

    Result<CompanyDetailInfoBO> getCompanyByCode(String companyCode);

    PageResult<CompanyDetailInfoBO> getSubCompaniesByCode(String companyCode, String keyword, Boolean isMultistage, Integer current, Integer pageSize);

    PageResult<PersonDetailBO> getCompanyUsers(String companyCode, String keyword, Boolean onlyUser, Integer current, Integer pageSize);
}
