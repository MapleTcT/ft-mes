package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.exception.OAuthClientErrorEnum;
import com.supcon.supfusion.auth.common.exception.OAuthClientException;
import com.supcon.supfusion.auth.dao.mapper.OAuthClientMapper;
import com.supcon.supfusion.auth.dao.po.OAuthClientPO;
import com.supcon.supfusion.auth.service.OAuthClientService;
import com.supcon.supfusion.auth.service.bo.OAuthClientBO;
import com.supcon.supfusion.auth.service.bo.OAuthClientQueryBO;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OAuthClientServiceImpl implements OAuthClientService {
    @Autowired
    private OAuthClientMapper oAuthClientMapper;

    @Override
    public OAuthClientBO queryOAuthClient(Long id) {
        OAuthClientPO oauthClientPO = queryById(id);
        OAuthClientBO oAuthClientBO = new OAuthClientBO();
        BeanUtils.copyProperties(oauthClientPO, oAuthClientBO);
        return oAuthClientBO;
    }

    @Override
    public OAuthClientBO createOAuthClient(OAuthClientBO oAuthClientBO) {
        Integer sameNameCount = oAuthClientMapper.selectCount(Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getName, oAuthClientBO.getName())
                .eq(OAuthClientPO::getValid, true));
        if (sameNameCount > 0) {
            throw new OAuthClientException(OAuthClientErrorEnum.NAME_IS_EXIST);
        }
        Integer sameClientIdCount = oAuthClientMapper.selectCount(Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getClientId, oAuthClientBO.getClientId())
                .eq(OAuthClientPO::getValid, true));
        if (sameClientIdCount > 0) {
            throw new OAuthClientException(OAuthClientErrorEnum.CLIENT_ID_EXIST);
        }
        OAuthClientPO oauthClientPO = new OAuthClientPO();
        BeanUtils.copyProperties(oAuthClientBO, oauthClientPO);
        oAuthClientMapper.insert(oauthClientPO);
        BeanUtils.copyProperties(oauthClientPO, oAuthClientBO);
        return oAuthClientBO;
    }

    @Override
    public OAuthClientBO updateOAuthClient(OAuthClientBO oAuthClientBO) {
        checkIsExist(oAuthClientBO.getId());
        Integer sameNameCount = oAuthClientMapper.selectCount(Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getName, oAuthClientBO.getName())
                .ne(OAuthClientPO::getId, oAuthClientBO.getId())
                .eq(OAuthClientPO::getValid, true));
        if (sameNameCount > 0) {
            throw new OAuthClientException(OAuthClientErrorEnum.NAME_IS_EXIST);
        }
        Integer sameClientIdCount = oAuthClientMapper.selectCount(Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getClientId, oAuthClientBO.getClientId())
                .ne(OAuthClientPO::getId, oAuthClientBO.getId())
                .eq(OAuthClientPO::getValid, true));
        if (sameClientIdCount > 0) {
            throw new OAuthClientException(OAuthClientErrorEnum.CLIENT_ID_EXIST);
        }
        OAuthClientPO oauthClientPO = new OAuthClientPO();
        BeanUtils.copyProperties(oAuthClientBO, oauthClientPO);
        oAuthClientMapper.updateById(oauthClientPO);
        BeanUtils.copyProperties(oauthClientPO, oAuthClientBO);
        return oAuthClientBO;
    }

    @Override
    public void removeOAuthClients(List<Long> ids) {
        checkIsExist(ids.toArray(new Long[0]));
        OAuthClientPO oauthClientPO = new OAuthClientPO();
        oauthClientPO.setValid(false);
        oauthClientPO.setEnabled(false);
        oAuthClientMapper.update(oauthClientPO, Wrappers.lambdaUpdate(OAuthClientPO.class).in(OAuthClientPO::getId, ids));
    }

    @Override
    public void enableOAuthClient(Long id, Boolean enabled) {
        checkIsExist(id);
        OAuthClientPO oauthClientPO = new OAuthClientPO();
        oauthClientPO.setId(id);
        oauthClientPO.setEnabled(enabled);
        oAuthClientMapper.updateById(oauthClientPO);
    }

    @Override
    public PageResult<OAuthClientBO> queryOAuthClients(OAuthClientQueryBO queryParams, Pagination pagination) {
        LambdaQueryWrapper<OAuthClientPO> lambdaQueryWrapper = Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getValid, true)
                .orderByAsc(OAuthClientPO::getId);
        if (!StringUtils.isEmpty(queryParams.getName())) {
            lambdaQueryWrapper.likeRight(OAuthClientPO::getName, queryParams.getName());
        }
        Page<OAuthClientPO> page = new Page<>();
        page.setCurrent(pagination.getCurrent()).setSize(pagination.getPageSize());
        Page<OAuthClientPO> directoryPOPage = oAuthClientMapper.selectPage(page, lambdaQueryWrapper);
        List<OAuthClientBO> collect = directoryPOPage.getRecords().stream().map(entity -> {
            OAuthClientBO userDirectoryBO = new OAuthClientBO();
            BeanUtils.copyProperties(entity, userDirectoryBO);
            return userDirectoryBO;
        }).collect(Collectors.toList());
        return new PageResult<>(collect, directoryPOPage.getTotal(), directoryPOPage.getSize(), directoryPOPage.getCurrent());
    }

    /**
     * 通过ID获取身份提供者
     *
     * @param id 主键ID
     */
    private OAuthClientPO queryById(Long id) {
        OAuthClientPO oauthClientPO = oAuthClientMapper.selectOne(Wrappers.lambdaQuery(OAuthClientPO.class)
                .eq(OAuthClientPO::getId, id)
                .eq(OAuthClientPO::getValid, true));
        if (oauthClientPO == null) {
            throw new OAuthClientException(OAuthClientErrorEnum.NOT_EXIST);
        }
        return oauthClientPO;
    }

    /**
     * 判断身份提供者是否存在
     *
     * @param ids 主键ID数组
     */
    private void checkIsExist(Long... ids) {
        Integer count = oAuthClientMapper.selectCount(Wrappers.lambdaQuery(OAuthClientPO.class)
                .in(OAuthClientPO::getId, ids)
                .eq(OAuthClientPO::getValid, true));
        if (count != ids.length) {
            throw new OAuthClientException(OAuthClientErrorEnum.NOT_EXIST);
        }
    }
}
