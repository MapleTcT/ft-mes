package com.supcon.supfusion.iam.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:35
 */
@TableName(value = "iam_account", autoResultMap = true)
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountPO extends BaseEntity {
    private static final long serialVersionUID = 4683865250200531385L;

    /**
     * 主健
     */
    private Long id;
    /**
     * AK
     */
    private String accessKey;
    /**
     * SK
     */
    private String secretKey;
    /**
     * 用户名
     */
    private String username;
    /**
     * 用户说明
     */
    private String description;

    /**
     * 是否为app安装时生成，0 页面创建 1 app安装时生成
     */
    private Integer system;

    /**
     * 下载标志
     */
    private Integer downloadMark;


    public static String getIdFieldName() {
        return "id";
    }

    public static String getSystemFieldName() {
        return "system";
    }

    public static String getUserNameFieldName() {
        return "username";
    }

    public static String getDownloadMarkFieldName() {
        return "download_mark";
    }

    public static String getCreateTimeFieldName() {
        return "create_time";
    }

    public static String getAccessKeyFieldName() {
        return "access_key";
    }

    public static String getSecretKeyFieldName() {
        return "secret_key";
    }
}
