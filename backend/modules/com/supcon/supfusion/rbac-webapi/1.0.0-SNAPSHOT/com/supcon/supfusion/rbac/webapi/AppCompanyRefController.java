package com.supcon.supfusion.rbac.webapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.po.AppCompanyRefPO;
import com.supcon.supfusion.rbac.service.IAppCompanyRefService;
import com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany.AppCompanyRefVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "APP公司关联相关接口")
public class AppCompanyRefController extends BaseController {

    @Autowired
    IAppCompanyRefService appCompanyRefService;

    @PostMapping("/app/companies")
    @ApiOperation(value = "保存菜单公司关联")
    public void addAppCompanyRef(@RequestBody AppCompanyRefVO appCompanyRefVO) {
        log.info("inter-api/rbac/v1/app/companies==param:appCompanyRefVO***********************************{}",appCompanyRefVO);
        String appId = appCompanyRefVO.getAppId();
        List<String> cidList = appCompanyRefVO.getCidList();
        List<AppCompanyRefPO> appCompanyRefPOList = new ArrayList<>();
        for (String cid : cidList) {
            AppCompanyRefPO appCompanyRefPO = new AppCompanyRefPO();
            appCompanyRefPO.setAppId(appId);
            appCompanyRefPO.setCid(Long.parseLong(cid));
            appCompanyRefPOList.add(appCompanyRefPO);
        }
        appCompanyRefService.addAppCompanyRef(appCompanyRefPOList);
    }

    @GetMapping("/app/{appId}/companies")
    @ApiOperation(value = "保存菜单公司关联")
    public ListResult<Long> queryAppCompanyRef(@PathVariable("appId") String appId) {
        log.info("inter-api/rbac/v1/app/{appId}/companies==param:appId***********************************{}",appId);
        List<AppCompanyRefPO> appCompanyRefPOList = appCompanyRefService.queryAppCompanyRefList(appId);
        if (CollectionUtils.isEmpty(appCompanyRefPOList)) {
            return new ListResult<>();
        }
        log.info("inter-api/rbac/v1/app/{appId}/companies==response: ListResult<Long>***********************************{}",appCompanyRefPOList.stream().map(AppCompanyRefPO::getCid).collect(Collectors.toList()));
        return new ListResult<>(appCompanyRefPOList.stream().map(AppCompanyRefPO::getCid).collect(Collectors.toList()));
    }

}
