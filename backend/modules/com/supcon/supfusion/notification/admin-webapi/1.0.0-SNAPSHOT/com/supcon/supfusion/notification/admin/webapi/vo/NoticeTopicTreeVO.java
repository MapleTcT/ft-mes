package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTopicTreeVO extends NoticeBaseVO {

    @ApiModelProperty(value = "父级消息主题类型")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long parentId;
    @ApiModelProperty(value = "父级消息主题对象")
    private NoticeTopicTreeVO parentObj;
    //层级结构
    @ApiModelProperty(value = "模板默认参数", example = "1000-1010-1050")
    private Integer layRec;


}
