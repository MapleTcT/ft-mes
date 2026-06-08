package com.supcon.supfusion.organization.dao.mapper.person;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.person.*;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人员mapper
 */
public interface PersonMapper extends BaseMapper<PersonAddPO> {

    /**
     * 根据部门编码查询人员列表
     * @param departmentCode
     * @return
     */
    List<PersonBaseInfoPO> getPersonsByDepartmentCode(@Param("departmentCode") String departmentCode, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 根据部门编码查询人员数量
     * @param departmentCode
     * @return
     */
    Integer getPersonsCountByDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * 根据岗位编码查询人员列表
     * @param positionCode
     * @return
     */
    List<PersonBaseInfoPO> getPersonsByPositionCode(@Param("positionCode") String positionCode, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 根据岗位编码查询人员数量
     * @param positionCode
     * @return
     */
    Integer getPersonsCountByPositionCode(@Param("positionCode") String positionCode);

    /**
     * 根据公司编码查询人员列表
     * @param companyCode
     * @return
     */
    List<PersonBaseInfoPO> getPersonsByCompanyCode(@Param("companyCode") String companyCode, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 根据公司编码查询人员数量
     * @param companyCode
     * @return
     */
    Integer getPersonsCountByCompanyCode(@Param("companyCode") String companyCode);

    /**
     * 根据时间查询人员列表
     * @param modifyTime
     * @param current
     * @param pageSize
     * @param dbType
     * @return
     */
    List<PersonSynchronizationInfoPO> getPersons(@Param("modifyTime") String modifyTime, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer getPersonCount(@Param("modifyTime") String modifyTime, @Param("dbType") String dbType);

    /**
     * 根据人员id批量查询岗位信息
     * @param personIds
     * @return
     */
    List<PersonSynchronizationPositionPO> getPositionsByPersonIds(@Param("personIds") List<Long> personIds);

    /**
     * 查询用户基本信息,主岗信息
     */
    PersonSynchronizationInfoPO getPersonByPersonCode(@Param("personCode") String personCode);

    /**
     *  查询岗位列表
     */
//    @Select("select opp.person_id as personId, position.code as positionCode, position.name as positionName from org_person_position opp left join org_position position on opp.position_id = position.id ${ew.customSqlSegment}")
    List<PersonSynchronizationPositionPO> getPositionByPersonId(@Param("personId") Long personId);

    /**
     * 根据人员id保存或修改用户信息
     */
    void saveOrUpdateUserByPersonId(@Param("personId") Long personId, @Param("userId") Long userId, @Param("userName") String userName);

    /**
     * 根据人员id删除用户信息
     */
    void deleteUserByPersonId(@Param("personIds") List<Long> personIds);

    /**
     * 获取人员表中用户id不为空的数量
     */
    Integer getCountOfUser();

    /**
     * 查询部门下人员用户信息
     * @param departmentId
     * @param keyword
     * @param onlyUser
     * @param current
     * @param pageSize
     * @param dbType
     */
    List<DepartmentPersonPO> queryUserOfDept(@Param("departmentId") Long departmentId, @Param("keyword") String keyword, @Param("onlyUser") Boolean onlyUser,
                                             @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 查询公司下人员用户信息
     * @param companyId
     * @param keyword
     * @param onlyUser
     * @param current
     * @param pageSize
     * @param dbType
     * @return
     */
    List<CompanyPersonPO> queryUserOfCompany(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                                             @Param("onlyUser") Boolean onlyUser, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 部门下用户总数
     *  @param departmentId
     * @param keyword
     * @param onlyUser
     */
    Integer totalUserOfDept(@Param("departmentId") Long departmentId, @Param("keyword") String keyword, @Param("onlyUser") Boolean onlyUser);

    /**
     * 公司下用户总数
     *
     * @param companyId
     * @param keyword
     * @param onlyUser
     */
    Integer totalUserOfCompany(@Param("companyId") Long companyId, @Param("keyword") String keyword, @Param("onlyUser") Boolean onlyUser);

    List<PositionPersonPO> queryUserOfPosition(@Param("positionId") Long positionId, @Param("keyword") String keyword,
                                               @Param("onlyUser") Boolean onlyUser, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    Integer totalUserOfPosition(@Param("positionId") Long positionId, @Param("keyword") String keyword, @Param("onlyUser") Boolean onlyUser);

    /**
     * 公司下人员信息
     */
    List<CompanyPersonPO> queryPersonOfCompany(@Param("companyId") String companyId, @Param("keyword") String keyword, @Param("onlyUser") Boolean onlyUser, @Param("current") Integer current, @Param("pageSize") Integer pageSize, @Param("dbType") String dbType);

    /**
     * 公司下人员总数
     */
    Integer totalPersonOfCompany(@Param("companyId") String companyId, @Param("onlyUser") Boolean onlyUser, @Param("keyword") String keyword);

    Integer countPersonUser();

    /**
     * 查询这个公司下属于多公司的人
     * @param companyId
     * @return
     */
    List<PersonCompanyPO> queryMultiCompanyPersonsByCompanyId(@Param("companyId") Long companyId);

    /**
     * 查询登陆人员信息(人员头像)
     * @param peronId
     * @return
     */
    PersonLoginPO queryLoginPersonById(@Param("peronId") Long peronId);

    /**
     * 根据公司和条件查询人员
     * @param companyId
     * @param keyword
     * @param current
     * @param pageSize
     * @param dbType
     * @param codes
     * @param names
     * @param descriptions
     * @param emails
     * @param phones
     * @param genders
     * @param statuses
     * @return
     */
    List<PersonAndLeaderPO> queryPeronsByCompanyIdAndCondition(@Param("companyId") Long companyId, @Param("keyword") String keyword, @Param("current") Integer current,
                                                         @Param("pageSize") Integer pageSize, @Param("dbType") String dbType,
                                                         @Param("codes") List<String> codes, @Param("names") List<String> names,
                                                         @Param("descriptions") List<String> descriptions, @Param("emails") List<String> emails,
                                                         @Param("phones") List<String> phones,
                                                         @Param("genders") List<String> genders, @Param("statuses") List<String> statuses);

    /**
     * 根据公司和条件查询人员數量
     * @param companyId
     * @param keyword
     * @param codes
     * @param names
     * @param descriptions
     * @param emails
     * @param phones
     * @param genders
     * @param statuses
     * @return
     */
    Integer queryPeronsByCompanyIdAndConditionCount(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                                                    @Param("codes") List<String> codes, @Param("names") List<String> names, @Param("descriptions") List<String> descriptions,
                                                    @Param("emails") List<String> emails, @Param("phones") List<String> phones,
                                                    @Param("genders") List<String> genders, @Param("statuses") List<String> statuses, @Param("dbType") String dbType);

    List<PersonAndLeaderPO> queryPeronsByPositionIdsAndCondition(@Param("positionIds") List<Long> positionIds, @Param("keyword") String keyword, @Param("current") Integer current,
                                                         @Param("pageSize") Integer pageSize, @Param("dbType") String dbType,
                                                         @Param("codes") List<String> codes, @Param("names") List<String> names,
                                                         @Param("descriptions") List<String> descriptions, @Param("emails") List<String> emails,
                                                         @Param("phones") List<String> phones,
                                                         @Param("genders") List<String> genders, @Param("statuses") List<String> statuses);

    Integer queryPeronsByPositionIdsAndConditionCount(@Param("positionIds") List<Long> positionIds, @Param("keyword") String keyword,
                                                    @Param("codes") List<String> codes, @Param("names") List<String> names, @Param("descriptions") List<String> descriptions,
                                                    @Param("emails") List<String> emails, @Param("phones") List<String> phones,
                                                    @Param("genders") List<String> genders, @Param("statuses") List<String> statuses, @Param("dbType") String dbType);

    /**
     * 根据人员id批量查询公司信息
     * @param personIds
     * @return
     */
    List<PersonSynchronizationCompanyPO> queryCompaniesByPersonIds(@Param("personIds") List<Long> personIds);

    /**
     *  查询人员所属公司列表
     */
    List<PersonSynchronizationCompanyPO> queryCompaniesByPersonId(@Param("personId") Long personId);

    /**
     * 根据人员id批量查询部门信息
     * @param personIds
     * @return
     */
    List<PersonSynchronizationDepartmentPO> queryDepartmentsByPersonIds(@Param("personIds") List<Long> personIds);

    /**
     *  查询人员所属部门列表
     */
    List<PersonSynchronizationDepartmentPO> queryDepartmentsByPersonId(@Param("personId") Long personId);


    /**
     * 条件查询公司下人员,并包含岗位部门公司信息
     * @param companyId
     * @param keyword
     * @param current
     * @param pageSize
     * @param dbType
     * @param codes
     * @param names
     * @param descriptions
     * @param emails
     * @param phones
     * @param genders
     * @param statuses
     * @return
     */
    List<PersonDetailBetterPO> queryPeronsOrgDetailByCompanyIdAndCondition(@Param("companyId") Long companyId, @Param("keyword") String keyword, @Param("current") Integer current,
                                                               @Param("pageSize") Integer pageSize, @Param("dbType") String dbType,
                                                               @Param("codes") List<String> codes, @Param("names") List<String> names,
                                                               @Param("descriptions") List<String> descriptions, @Param("emails") List<String> emails,
                                                               @Param("phones") List<String> phones,
                                                               @Param("genders") List<String> genders, @Param("statuses") List<String> statuses);

    /**
     * 条件查询岗位下人员,并包含岗位部门公司信息
     * @param positionIds
     * @param keyword
     * @param current
     * @param pageSize
     * @param dbType
     * @param codes
     * @param names
     * @param descriptions
     * @param emails
     * @param phones
     * @param genders
     * @param statuses
     * @return
     */
    List<PersonDetailBetterPO> queryPeronsOrgDetailByPositionIdsAndCondition(@Param("positionIds") List<Long> positionIds, @Param("keyword") String keyword, @Param("current") Integer current,
                                                                 @Param("pageSize") Integer pageSize, @Param("dbType") String dbType,
                                                                 @Param("codes") List<String> codes, @Param("names") List<String> names,
                                                                 @Param("descriptions") List<String> descriptions, @Param("emails") List<String> emails,
                                                                 @Param("phones") List<String> phones,
                                                                 @Param("genders") List<String> genders, @Param("statuses") List<String> statuses);
}
