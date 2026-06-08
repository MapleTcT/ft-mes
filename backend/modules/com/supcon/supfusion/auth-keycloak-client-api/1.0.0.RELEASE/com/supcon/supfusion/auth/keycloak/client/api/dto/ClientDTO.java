package com.supcon.supfusion.auth.keycloak.client.api.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClientDTO extends DTO {


    public ClientDTO() {
        this.accessTokenLifespan = "600";
        this.directAccessGrantsEnabled = true;
        this.enabled = true;
        this.implicitFlowEnabled = true;
        this.standardFlowEnabled = true;
    }

    /**
     * 客户端标识
     */
    private String clientId;
    /**
     * 客户端秘钥
     */
    private String secret;
    /**
     * 认证回调url
     */
    private List<String> redirectUris;
    /**
     * 客户端描述
     */
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
    private String accessTokenLifespan;

}
