package com.supcon.supfusion.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.service.bo.RoleDetailBO;
import com.supcon.supfusion.auth.service.bo.UserBO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface SuposUserService extends IService<UserPO> {


    /**
     * 创建用户
     *
     * @param bo
     */
    void creatOpenUser(UserBO bo, String eamil, HttpServletResponse response);

    Page<UserBO> openGetUsers(Page<UserBO> page, String keyword, HttpServletResponse response);

    List<UserBO> openGetUsersByRoleCode(String roleCode, HttpServletResponse response);

    UserBO getUserInfo(String userName, HttpServletResponse response);

    void updateUserInfo(String username, String timeZone, String userDesc, String email, List<String> roleNameList, HttpServletResponse response);


    void deleteUsers(List<String> list);

     Page<RoleDetailBO> getRoles(Integer pageIndex,Integer pageSize, String keyword,HttpServletResponse response);

     void addRole(Long cid,String code,String name,String description);

     void deleteRoles(List<String> codes);

    RoleDetailBO getRoleDetail(String code);

    void update(String code,String name,String description);


}
