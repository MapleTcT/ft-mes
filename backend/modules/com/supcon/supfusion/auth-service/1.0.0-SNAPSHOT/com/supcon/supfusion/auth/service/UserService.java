package com.supcon.supfusion.auth.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.auth.dao.po.AuthExcelPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.service.bo.*;
import com.supcon.supfusion.auth.service.bo.bap.BapUserInfoBO;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author lifangyuan
 */
public interface UserService extends IService<UserPO> {

    /**
     * 批量删除
     *
     * @param ids
     * @return 大于0 删除成功 小于等于0 用户不存在
     */
    void batchDelet(List<Long> ids);

    /**
     * 批量删除
     *
     * @param ids
     * @return 大于0 删除成功 小于等于0 用户不存在
     */
    void batchDeletByNames(List<String> userNames,HttpServletResponse response);

    /**
     * 创建用户
     *
     * @param bo
     */
    void creatUser(UserBO bo, List<UserRoleBO> userRoleBO);

    /**
     * 创建用户
     *
     * @param bo
     */
    void creatOpenUser(UserBO bo, HttpServletResponse response);


//    /**
//     * 创建用户
//     *
//     * @param bo
//     */
//    void batchInsert(List<UserBO> bo, List<UserRoleBO> userRoleBOS);

    /**
     * 修改用户
     *
     * @param bo
     */
    void updateUser(UserBO bo, List<UserRoleBO> userRoleBOS);

    /**
     * 搜索用户
     *
     * @param bo
     * @return List<UserBO>
     */
    Page<UserBO> searchUser(Page<UserBO> page, UserBO bo);


    /**
     * 根据id获取用户信息
     *
     * @param id
     * @return List<UserBO>
     */
    UserBO getUserById(Long id);

    /**
     * 根据id获取用户信息
     *
     * @param ids
     * @return List<UserBO>
     */
    List<UserBO> batchGetByIds(List<Long> ids);

    /**
     * 根据id获取用户信息
     *
     * @param ids
     * @return List<UserBO>
     */
    List<UserBO> batchGetByPersonIds(List<Long> ids);

    /**
     * 根据id获取用户信息
     *
     * @param personId
     * @return List<UserBO>
     */
    UserBO selectByPersonId(Long personId);


    /**
     * 根据id获取用户信息
     *
     * @param userId
     * @return List<UserBO>
     */
    List<UserRoleBO> batchGetByUserId(Long userId);

    /**
     * 根据id获取用户信息
     *
     * @param userName
     * @return List<UserBO>
     */
    UserBO findByUserName(String userName);


    /**
     * 根据id获取用户信息
     *
     * @param userName
     * @return List<UserBO>
     */
    List<UserBO> findBatchUserName(String[] userName);

    List<UserBO> findCompanyAdminUser(Long companyId);

    Boolean deleteUserByPersonIds(Long[] personIds);

    Boolean deleteUserByCompanyId(Long companyId);


    void excuteExcelState(AuthExcelPO excelPO);

    /**
     * 根据id查询用户
     *
     * @param id       用户id
     * @param includes
     * @return 用户信息json
     */
    String getBapUserById(Long id, String includes);

    /**
     * 获取当前登陆人信息
     */
    BapUserInfoBO getCurrentLoginInfo();

    /**
     * 切换用户当前公司
     *
     * @param userName  用户名称
     * @param companyId 企业id
     */
    void changeCurrentCompany(String userName, Long companyId);

    /**
     * 修改用户联系方式
     *
     * @param personId 人员id
     * @param email    邮件
     * @param phone    电话
     */
    void updateEmailOrPhone(Long personId, String email, String phone);

    void modifyCurrUserPassword(String password, String rePassword, String prePassword);

    UserBO selectUserName(String userName);

    LoginResponseBO login(LoginBO loginBO, String realIp, String quatoName, String deviceType, String cookieValue, String ticket);

    void verifyLoginResult(JSONObject jsonObject);

    void logout(String ticket);

    LoginResponseBO companyChange(Long companyId, String ticket);

    void refreshToken(String ticket, HttpServletResponse response);


    LoginResponseBO accessToken(String ticket);

    Page<UserBO> openGetUsers(Page<UserBO> page, String keyword, String companyCode, String roleCode, String createTime,HttpServletResponse response);

    List<UserBO> openGetAllUsers(UserBO bo);

    void loadAllUser(Long userDirectoryId, Long companyId, NamingEnumeration<NameClassPair> list);

    void openUpdateUser(UserBO userBO,List<String> addRoleCodes,List<String> deleteRoleCodes, HttpServletResponse response);

    Page<UserBO> search(String keyword,Integer current,Integer pageSize);

    /**
     * 根据用户名获取id
     * @param userName
     */
    Long getIdByUserName(String userName);

    /**
     * 获取所有人员用户信息
     */
    List<UserBO> getAllPersonsUsers();

    JSONObject loginKeycloak(LoginBO loginBO, String realIp, String deviceType);

    LoginResponseBO convertLoginResponse(LoginBO loginBO, UserBO userBO);

    OnlineUserBO buildOnlineUserBO(String realIp, String deviceType, String uuid, LoginResponseBO loginResponseBO);

    void cacheTicket(JSONObject jsonObject, String key, LoginResponseBO loginResponseBO);

    void addSystemAdmin(String userName);

    /**
     * openapi 重置admin password
     * @param authorization
     * @return
     */
    String resetAdminPassword(String authorization);

    void unBindUserThridIdentitys(String username, List<String> identityIds);

    LoginResponseBO simulateLogin(String userName, Long companyId, String realIp, String quatoName, String deviceType);
}
