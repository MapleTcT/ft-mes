package com.supcon.supfusion.iam.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.iam.api.dto.AccountDTO;
import com.supcon.supfusion.iam.api.dto.AccountVerifyDTO;
import com.supcon.supfusion.iam.api.dto.CreateAccountDTO;
import com.supcon.supfusion.iam.api.dto.SignatureVerifyDTO;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-16 下午9:42
 */
@FeignClient("supos-iam")
public interface IdentityAndAccessService {

    String BASE_PATH = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "supos-iam/v1";

    /**
     * 申请
     *
     * @param dto
     * @return
     */
    @PostMapping(BASE_PATH + "/account")
    @ResponseBody
    Result<AccountDTO> create(@RequestBody @Validated CreateAccountDTO dto);

    /**
     * 注销
     *
     * @param username
     */
    @DeleteMapping(BASE_PATH + "/account/{username}")
    @ResponseStatus(HttpStatus.OK)
    void destroy(@PathVariable("username") @NotBlank(message = "user name must not be empty") String username);

    /**
     * 签名校验
     *
     * @param dto
     */
    @PostMapping(BASE_PATH + "/signature")
    @ResponseStatus(HttpStatus.OK)
    void verify(@RequestBody @Validated SignatureVerifyDTO dto);

    /**
     * 校验AK/SK是否合法
     *
     * @param dto
     */
    @PutMapping(BASE_PATH + "/account")
    @ResponseStatus(HttpStatus.OK)
    void verifyAccount(@RequestBody @Validated AccountVerifyDTO dto);

    /**
     * 通过AK查询
     *
     * @param accessKey
     * @return
     */
    @GetMapping(BASE_PATH + "/account")
    @ResponseBody
    Result<AccountDTO> findByAccessKey(@RequestParam("accessKey") String accessKey);
}
