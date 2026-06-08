package com.supcon.supfusion.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.iam.api.IdentityAndAccessService;
import com.supcon.supfusion.iam.api.dto.AccountDTO;
import com.supcon.supfusion.iam.api.dto.AccountVerifyDTO;
import com.supcon.supfusion.iam.api.dto.CreateAccountDTO;
import com.supcon.supfusion.iam.api.dto.SignatureVerifyDTO;
import com.supcon.supfusion.iam.common.exception.AccountIllegalException;
import com.supcon.supfusion.iam.common.exception.AccountNotExistsException;
import com.supcon.supfusion.iam.dao.AccountMapper;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.AccountService;
import com.supcon.supfusion.iam.service.bo.AccountBO;
import com.supcon.supfusion.iam.service.support.AdminKeyValue;
import com.supcon.supfusion.iam.service.support.HmacUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-16 下午9:44
 */
@Slf4j
@Setter
@Getter
@ServiceApiService
public class IdentityAndAccessServiceImpl implements IdentityAndAccessService {

    @Autowired
    private AccountMapper  accountMapper;
    @Autowired
    private AccountService accountService;

    @Override
    public Result<AccountDTO> create(CreateAccountDTO dto) {
        if (log.isDebugEnabled()) {
            log.info("beginning to create account, username={}", dto.getUsername());
        }
        AccountBO accountBO = accountService.create(dto.getUsername(), dto.getDescription());
        if (log.isDebugEnabled()) {
            log.info("ending to create account, username={}, ak={}, sk={}", accountBO.getUsername(), accountBO.getAccessKey(), accountBO.getSecretKey());
        }
        return new Result<>(AccountDTO.builder().ak(accountBO.getAccessKey()).sk(accountBO.getSecretKey()).build());
    }

    @Override
    public void destroy(String username) {
        if (log.isDebugEnabled()) {
            log.info("beginning to destroy account, username={}", username);
        }
        accountService.destroy(username);
    }

    @Override
    public void verify(SignatureVerifyDTO dto) {
        if (log.isDebugEnabled()) {
            log.info("beginning to verify signature, detail={}", dto.toString());
        }
        String sk = "";
        if (dto.getAk().equals(AdminKeyValue.AK)) {
            sk = AdminKeyValue.SK;
        } else {
            AccountPO accountPO = accountMapper.selectOne(new QueryWrapper<AccountPO>().eq("access_key", dto.getAk()));
            if (accountPO == null) {
                log.error("the account not exists, ak={}", dto.getAk());
                throw new AccountNotExistsException();
            }
            sk = accountPO.getSecretKey();
        }
        //组装签名源字符串
        String canonicalRequestString = dto.getMetadata().getSchema() + "\n"
                + dto.getMetadata().getUri() + "\n"
                + dto.getMetadata().getContentType() + "\n"
                + dto.getMetadata().getCanonicalQueryString() + "\n"
                + dto.getMetadata().getCanonicalCustomHeaders() + "\n"
                + dto.getMetadata().getBodyPayload();
        if (log.isDebugEnabled()) {
            log.info("signature verify, req={}", canonicalRequestString);
        }
        //生成签名
        String sign = HmacUtil.hmacSha256(sk, canonicalRequestString);
        //比对签名
        if (!sign.equals(dto.getSignature())) {
            log.error("the signature is not match, src={}, curr={}, req={}", dto.getSignature(), sign, canonicalRequestString);
            throw new AccountIllegalException();
        }
        if (log.isDebugEnabled()) {
            log.info("ending to verify signature, result=ok");
        }
    }

    @Override
    public void verifyAccount(AccountVerifyDTO dto) {
        if (log.isDebugEnabled()) {
            log.info("beginning to verify account, ak={}, sk={}", dto.getAccessKey(), dto.getSecretKey());
        }
        accountService.findByAkAndSk(dto.getAccessKey(), dto.getSecretKey());
    }

    @Override
    public Result<AccountDTO> findByAccessKey(String accessKey) {
        log.info("beginning to find account by ak, ak={}", accessKey);
        AccountBO accountBO =  accountService.findByAk(accessKey);
        log.info("ending to find account by ak, username={}, ak={}, sk={}", accountBO.getUsername(), accountBO.getAccessKey(), accountBO.getSecretKey());
        return new Result<>(AccountDTO.builder().ak(accountBO.getAccessKey()).sk(accountBO.getSecretKey()).build());
    }
}
