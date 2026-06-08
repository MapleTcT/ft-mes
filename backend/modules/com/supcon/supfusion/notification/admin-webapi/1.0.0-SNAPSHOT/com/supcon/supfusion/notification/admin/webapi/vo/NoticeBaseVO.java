package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeBaseVO implements Serializable {
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long id;

    private String code;
    //名称
    private String name;
    //版本
//    private Integer version;
    //排序
//    private Integer sort;
    //描述
    private String memo;
}
