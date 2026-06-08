package com.supcon.supfusion.portal.service;

import com.supcon.supfusion.portal.service.bo.EcPortletBO;
import com.supcon.supfusion.portal.service.entity.MyPortlet;

import java.util.List;

/**
 * @Author kk.C
 * @Description 首页门户有关service
 * @Date 2020/11/30 13:44
 * @Param
 * @return
 **/
public interface PortletHomePageService {

    List<EcPortletBO> queryHomePagePortlet();

    List<MyPortlet> queryMyPortal();

    void saveMyPortal(List<MyPortlet> myPortlet);
}
