package com.supcon.supfusion.iam.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午6:20
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SignatureVerifyDTO extends DTO {
    private static final long serialVersionUID = -7999271317948765421L;

    /**
     * AK
     */
    @NotBlank(message = "access key must not be empty")
    private String ak;
    /**
     * 签名
     */
    @NotBlank(message = "signature must not be empty")
    private String signature;
    /**
     * 签名元数据
     */
    @Valid
    private Metadata metadata;

    @Setter
    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static final class Metadata extends DTO {
        private static final long serialVersionUID = 1181294316429055575L;

        /**
         * 请求方法
         */
        @NotBlank(message = "schema must not be empty")
        private String schema;
        /**
         * 请求地址URI
         */
        @NotBlank(message = "uri must not be empty")
        private String uri;
        /**
         * 请求体类型
         */
        @NotBlank(message = "content type must not be empty")
        private String contentType;
        /**
         * QueryParams的字符串
         */
        private String canonicalQueryString;
        /**
         * 请求头字符串
         */
        private String canonicalCustomHeaders;
        /**
         * 请求体的SHA256签名
         */
        private String bodyPayload;
    }
}
