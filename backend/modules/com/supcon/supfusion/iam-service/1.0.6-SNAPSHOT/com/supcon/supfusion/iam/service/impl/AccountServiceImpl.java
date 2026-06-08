package com.supcon.supfusion.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import com.supcon.supfusion.framework.scaffold.redis.external.SimpleStringRedisTemplate;
import com.supcon.supfusion.iam.common.exception.AccountIllegalException;
import com.supcon.supfusion.iam.common.exception.AccountNotExistsException;
import com.supcon.supfusion.iam.common.exception.IAMErrorEnum;
import com.supcon.supfusion.iam.common.exception.IAMExecption;
import com.supcon.supfusion.iam.dao.AccountMapper;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.manager.service.AuthService;
import com.supcon.supfusion.iam.service.AccountService;
import com.supcon.supfusion.iam.service.bo.AccountBO;
import com.supcon.supfusion.iam.service.support.AKSKGenerator;
import com.supcon.supfusion.iam.service.support.AdminKeyValue;
import com.supcon.supfusion.iam.service.support.KeyValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:00
 */
@Slf4j
@Setter
@Getter
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper             accountMapper;
    @Autowired
    private AuthService               authService;
    @Autowired
    private SimpleStringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBO create(String username, String description) {
        return create(username, description, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBO create(String username, String description, boolean isApp) {
        if (username.equals(AdminKeyValue.ADMIN)) {
            if (log.isDebugEnabled()) {
                log.info("==> user is admin");
            }
            return AccountBO.builder()
                    .accessKey(AdminKeyValue.AK)
                    .secretKey(AdminKeyValue.SK)
                    .username(AdminKeyValue.ADMIN)
                    .id(AdminKeyValue.ID)
                    .system(1)
                    .build();
        }

        KeyValue keyValue = AKSKGenerator.generate(username);

        AccountPO accountPO = AccountPO.builder()
                .id(IDGenerator.newInstance().generate().longValue())
                .username(username)
                .description(description)
                .accessKey(keyValue.getAccessKey())
                .secretKey(keyValue.getSecretKey())
                .createTime(DateTimeUtil.getUTC0())
                .system(isApp ? 1 : 0)
                .build();
        try {
            //save to db
            accountMapper.insert(accountPO);
            //add client to auth
            authService.create(keyValue.getAccessKey(), keyValue.getSecretKey());
            //put to redis
            stringRedisTemplate.opsForValue().setIfAbsent(keyValue.getAccessKey(), keyValue.getSecretKey());
        } catch (DuplicateKeyException e) {
            if (e.getMessage().toLowerCase().contains("udx_account_username")) {
                if (isApp) {
                    AccountPO po = accountMapper.selectOne(new QueryWrapper<AccountPO>().eq(AccountPO.getUserNameFieldName(), username));
                    return AccountBO.builder()
                            .id(po.getId())
                            .username(username)
                            .description(po.getDescription())
                            .accessKey(po.getAccessKey())
                            .secretKey(po.getSecretKey())
                            .system(po.getSystem())
                            .build();
                } else {
                    throw new IAMExecption(IAMErrorEnum.APPID_IS_EXIST);
                }

            }
            throw e;
        }

        return AccountBO.builder()
                .id(accountPO.getId())
                .username(username)
                .description(accountPO.getDescription())
                .accessKey(keyValue.getAccessKey())
                .secretKey(keyValue.getSecretKey())
                .system(accountPO.getSystem())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void destroy(String username) {
        if (!username.equals(AdminKeyValue.ADMIN)) {
            AccountPO po = accountMapper.selectOne(new QueryWrapper<AccountPO>().eq(AccountPO.getUserNameFieldName(), username));
            if (po != null) {
                log.info("delete access key:" + po.getAccessKey());
                //delete from db
                accountMapper.deleteById(po.getId());
                //delete from redis
                stringRedisTemplate.delete(po.getAccessKey());
                //delete from auth
                authService.delete(Lists.newArrayList(po.getAccessKey()));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void destroy(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            List<AccountPO> pos = accountMapper.selectList(Wrappers.<AccountPO>query().in(AccountPO.getIdFieldName(), ids));
            if (!CollectionUtils.isEmpty(pos)) {
                //delete from db
                accountMapper.deleteBatchIds(ids);
                //delete from redis
                List<String> aks = pos.stream().map(AccountPO::getAccessKey).collect(Collectors.toList());
                log.info("delete access key:" + aks.toString());
                stringRedisTemplate.delete(aks);
                //delete from auth
                authService.delete(aks);
            }
        }
    }

    @Override
    public void update(Long id, String description) {
        AccountPO accountPO = new AccountPO();
        accountPO.setId(id);
        accountPO.setDescription(description);
        accountMapper.updateById(accountPO);
    }

    @Override
    public List<AccountBO> find(String username) {
        if (username.equals(AdminKeyValue.ADMIN)) {
            return Lists.newArrayList(AccountBO.builder()
                    .id(AdminKeyValue.ID)
                    .username(AdminKeyValue.ADMIN)
                    .accessKey(AdminKeyValue.AK)
                    .secretKey(AdminKeyValue.SK)
                    .build());
        }

        QueryWrapper<AccountPO> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(username)) {
            queryWrapper.eq(AccountPO.getUserNameFieldName(), username);
        }
        List<AccountPO> accountPOS =  accountMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(accountPOS)) {
            return accountPOS.stream().map(po ->
                AccountBO.builder()
                        .id(po.getId())
                        .username(po.getUsername())
                        .description(po.getDescription())
                        .accessKey(po.getAccessKey())
                        .secretKey(po.getSecretKey())
                        .build()
            ).collect(Collectors.toList());
        }

        return Lists.newArrayList();
    }

    @Override
    public AccountBO findByAkAndSk(String ak, String sk) {
        if (ak.equals(AdminKeyValue.AK)) {
            if (log.isDebugEnabled()) {
                log.info("==> admin access key");
            }
            return AccountBO.builder()
                    .accessKey(AdminKeyValue.AK)
                    .secretKey(AdminKeyValue.SK)
                    .username(AdminKeyValue.ADMIN)
                    .id(AdminKeyValue.ID)
                    .build();
        }
        //
        AccountPO po = accountMapper.selectOne(new QueryWrapper<AccountPO>().eq(AccountPO.getAccessKeyFieldName(), ak)
                .eq(AccountPO.getSecretKeyFieldName(), sk));
        if (po == null) {
            throw new AccountIllegalException();
        }

        return AccountBO.builder()
                .id(po.getId())
                .username(po.getUsername())
                .description(po.getDescription())
                .accessKey(po.getAccessKey())
                .secretKey(po.getSecretKey())
                .build();
    }

    @Override
    public AccountBO findByAk(String ak) {
        if (ak.trim().equals(AdminKeyValue.AK)) {
            log.info("==> admin access key");
            return AccountBO.builder()
                    .accessKey(AdminKeyValue.AK)
                    .secretKey(AdminKeyValue.SK)
                    .username(AdminKeyValue.ADMIN)
                    .id(AdminKeyValue.ID)
                    .build();
        }
        //
        AccountPO po = accountMapper.selectOne(new QueryWrapper<AccountPO>().eq("access_key", ak));
        if (po == null) {
            throw new AccountNotExistsException();
        }

        return AccountBO.builder()
                .id(po.getId())
                .username(po.getUsername())
                .description(po.getDescription())
                .accessKey(po.getAccessKey())
                .secretKey(po.getSecretKey())
                .build();
    }

    @Override
    public List<AccountPO> findAll() {
        if (log.isDebugEnabled()) {
            log.info("==> find all account");
        }
        return accountMapper.selectList(null);
    }
}
