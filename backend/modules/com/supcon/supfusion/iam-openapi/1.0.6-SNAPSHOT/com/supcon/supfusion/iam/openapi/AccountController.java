package com.supcon.supfusion.iam.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.iam.openapi.vo.AccountVO;
import com.supcon.supfusion.iam.openapi.vo.CreateAccountVO;
import com.supcon.supfusion.iam.service.AccountService;
import com.supcon.supfusion.iam.service.bo.AccountBO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:48
 */
@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "supos-iam/v1/account")
public class AccountController extends BaseController {

    @Autowired
    private AccountService accountService;

    /**
     * 账户申请
     *
     * @param createAccountVO
     * @return
     */
    @PostMapping
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Result<AccountVO> doCreate(@RequestBody @Validated CreateAccountVO createAccountVO) {
        if (log.isDebugEnabled()) {
            log.info("beginning to create account, username={}", createAccountVO.getUsername());
        }
        AccountBO accountBO = accountService.create(createAccountVO.getUsername(), createAccountVO.getDescription());
        if (log.isDebugEnabled()) {
            log.info("ending to create account, username={}, ak={}, sk={}", accountBO.getUsername(), accountBO.getAccessKey(), accountBO.getSecretKey());
        }
        return new Result<>(AccountVO.builder().accessKey(accountBO.getAccessKey()).secretKey(accountBO.getSecretKey()).build());
    }

    /**
     * 账户注销
     *
     * @param username
     */
    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void doDestroy(@PathVariable("username") @NotBlank(message = "user name must not be empty") String username) {
        if (log.isDebugEnabled()) {
            log.info("beginning to destroy account, username={}", username);
        }
        accountService.destroy(username);
    }

    /**
     * 账户查询
     *
     * @param username
     * @return
     */
    @GetMapping
    @ResponseBody
    public ListResult<AccountVO> get(@RequestParam(value = "username", required = false) String username) {
        if (log.isDebugEnabled()) {
            log.info("beginning to find account, username={}", username);
        }
        List<AccountBO> bos = accountService.find(username);
        return new ListResult<>(bos.stream().map(bo -> AccountVO.builder()
                .username(bo.getUsername())
                .description(bo.getDescription())
                .accessKey(bo.getAccessKey())
                .secretKey(bo.getSecretKey())
                .build()).collect(Collectors.toList()));
    }
}
