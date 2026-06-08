package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.dao.mapper.AuthCenterMapper;
import com.supcon.supfusion.auth.dao.mapper.OAuthClientMapper;
import com.supcon.supfusion.auth.dao.po.AuthCenterPO;
import com.supcon.supfusion.auth.dao.po.OAuthClientPO;
import com.supcon.supfusion.auth.service.AuthCenterService;
import com.supcon.supfusion.auth.service.bo.AuthCenterBO;
import com.supcon.supfusion.auth.service.bo.IdentityProviderQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthCenterServiceImpl implements AuthCenterService {
    @Autowired
    private AuthCenterMapper authCenterMapper;
    @Autowired
    private OAuthClientMapper oAuthClientMapper;

    @Override
    public PageResult<AuthCenterBO> queryAuthCenters(IdentityProviderQueryBO queryParams, Pagination pagination) {
        LambdaQueryWrapper<AuthCenterPO> lambdaQueryWrapper = Wrappers.lambdaQuery(AuthCenterPO.class)
                .eq(AuthCenterPO::getValid, true)
                .orderByAsc(AuthCenterPO::getId);
        if (StringUtils.isNotEmpty(queryParams.getName())) {
            lambdaQueryWrapper.likeRight(AuthCenterPO::getName, queryParams.getName());
        }
        Page<AuthCenterPO> page = new Page<>();
        page.setCurrent(pagination.getCurrent()).setSize(pagination.getPageSize());
        Page<AuthCenterPO> authCenterPOPage = authCenterMapper.selectPage(page, lambdaQueryWrapper);
        List<AuthCenterBO> collect = authCenterPOPage.getRecords().stream().map(entity -> {
            AuthCenterBO authCenterBO = new AuthCenterBO();
            BeanUtils.copyProperties(entity, authCenterBO);
            LambdaQueryWrapper<OAuthClientPO> oAuthClientWrapper = Wrappers.lambdaQuery(OAuthClientPO.class)
                    .eq(OAuthClientPO::getAuthCenterId, entity.getId())
                    .eq(OAuthClientPO::getValid, true);
            Integer count = oAuthClientMapper.selectCount(oAuthClientWrapper);
            authCenterBO.setClientNumber(count);
            return authCenterBO;
        }).collect(Collectors.toList());
        return new PageResult<>(collect, authCenterPOPage.getTotal(), authCenterPOPage.getSize(), authCenterPOPage.getCurrent());
    }
}
