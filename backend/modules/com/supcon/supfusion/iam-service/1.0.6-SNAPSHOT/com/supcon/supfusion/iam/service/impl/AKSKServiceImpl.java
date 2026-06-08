package com.supcon.supfusion.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.iam.common.bean.Order;
import com.supcon.supfusion.iam.common.exception.IAMErrorEnum;
import com.supcon.supfusion.iam.common.exception.IAMExecption;
import com.supcon.supfusion.iam.dao.AccountMapper;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.AKSKService;
import com.supcon.supfusion.iam.service.AccountService;
import com.supcon.supfusion.iam.service.bo.AccountBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class AKSKServiceImpl implements AKSKService {
    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountService accountService;

    @Override
    public Page<AccountPO> queryListByKeyword(String appId, Order order, Integer pageNum, Integer pageSize) {
        QueryWrapper queryWrapper = Wrappers.query().eq(AccountPO.getSystemFieldName(), 0);
        if (StringUtils.hasText(appId)) {
            queryWrapper.eq(AccountPO.getUserNameFieldName(), appId);
        }
        if (order == Order.ASC) {
            queryWrapper.orderByAsc(AccountPO.getCreateTimeFieldName());
        } else {
            queryWrapper.orderByDesc(AccountPO.getCreateTimeFieldName());
        }
        return accountMapper.selectPage(new Page(pageNum, pageSize), queryWrapper);
    }

    @Override
    public long add(String appId, String description) {
        AccountBO bo = accountService.create(appId, description, false);
        return bo.getId();
    }

    @Override
    public void update(Long id, String description) {
        accountService.update(id, description);
    }

    @Override
    public void batchDelete(List<Long> id) {
        accountService.destroy(id);
    }

    @Override
    public AccountBO download(Long id) {
        AccountPO updatePO = new AccountPO();
        updatePO.setDownloadMark(1);
        Integer lock = accountMapper.update(updatePO, Wrappers.<AccountPO>update().eq(AccountPO.getIdFieldName(), id).eq(AccountPO.getDownloadMarkFieldName(), 0));
        if (lock != 1) {
            throw new IAMExecption(IAMErrorEnum.APPID_HAS_BEEN_DOWNLOADED);
        }
        AccountPO accountPO = accountMapper.selectById(id);

        AccountBO accountBO = new AccountBO();
        accountBO.setAccessKey(accountPO.getAccessKey());
        accountBO.setSecretKey(accountPO.getSecretKey());
        return accountBO;
    }
}
