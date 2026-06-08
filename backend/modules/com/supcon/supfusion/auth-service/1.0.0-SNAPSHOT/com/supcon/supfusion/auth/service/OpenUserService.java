package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.UserBO;

import javax.servlet.http.HttpServletResponse;

public interface OpenUserService {

     void creatOpenUser(UserBO bo,HttpServletResponse response);

}
