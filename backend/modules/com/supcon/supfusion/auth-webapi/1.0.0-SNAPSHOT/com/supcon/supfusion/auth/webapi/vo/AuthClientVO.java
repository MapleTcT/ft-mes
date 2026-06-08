package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
public class AuthClientVO extends VO {


    public AuthClientVO() {
        this.accessTokenLifespan = "1800";
        this.directAccessGrantsEnabled = true;
        this.enabled = true;
        this.implicitFlowEnabled = true;
        this.standardFlowEnabled = true;
        this.publicClient = true;
    }

    private String id;

    /**
     * 客户端标识
     */
    private String clientId;
    /**
     * 客户端秘钥
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String secret;
    /**
     * 认证回调url
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> redirectUris;
    /**
     * 客户端描述
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;

    /**
     * 是否启用
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean enabled;

    /**
     * 标准授权码模式
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean standardFlowEnabled;

    /**
     * 隐藏式授权码模式
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean implicitFlowEnabled;

    /**
     * 是否支持凭证式和密码式
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean directAccessGrantsEnabled;

    /**
     * token最长有效期
     */
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accessTokenLifespan;

    private Boolean publicClient;

}
