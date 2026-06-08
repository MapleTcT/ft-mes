package com.supcon.supfusion.iam.service.support;

import lombok.*;

import java.io.Serializable;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:11
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KeyValue implements Serializable {
    private static final long serialVersionUID = -7479998508795482933L;

    private String accessKey;

    private String secretKey;
}
