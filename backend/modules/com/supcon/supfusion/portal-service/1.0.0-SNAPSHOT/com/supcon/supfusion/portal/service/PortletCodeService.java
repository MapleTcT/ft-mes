package com.supcon.supfusion.portal.service;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.portal.service.bo.EcPortletBO;

/**
 * @Author kk.C
 * @Description 门户编码有关service
 * @Date 2020/10/21 13:44
 * @Param
 * @return
 **/
public interface PortletCodeService {

    PageResult<EcPortletBO> queryCodes(String moduleCode, String code, String category, Integer current, Integer pageSize);

    void addCode(EcPortletBO ecPortletBO);

    void deleteCode(EcPortletBO ecPortletBO);

    void updateCode(EcPortletBO ecPortletBO);
}
