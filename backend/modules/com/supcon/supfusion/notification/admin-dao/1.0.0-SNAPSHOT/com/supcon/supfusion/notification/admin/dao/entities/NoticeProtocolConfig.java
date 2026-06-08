package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.notification.sharding.typehandler.AESEncryptHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;


/**
 * <p>
 * 协议配置表
 * </p>
 *
 * @author huangxin2
 * @since 2020/05/20 21:13
 */
@Getter
@Setter
@ToString
@TableName(value = "notice_protocol_config", autoResultMap = true)
@ApiModel(value = "协议配置对象", description = "用户信息")
public class NoticeProtocolConfig extends BaseEntity {

    @ApiModelProperty(value = "ID")
    @TableField(value = "id")
    private Long id;
    @ApiModelProperty(value = "协议ID")
    @TableField(value = "protocol")
    private String protocol;
    @ApiModelProperty(value = "配置项内容")
    @TableField(value = "config_value", typeHandler = AESEncryptHandler.class)
    private String configValue;

    public NoticeProtocolConfig() {
    }


    public static String getIdFieldName() {
        return "id";
    }

    public static String getProtocolFieldName() {
        return "protocol";
    }

    public static String getCVFieldName() {
        return "config_value";
    }

}
