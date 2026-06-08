package com.supcon.supfusion.notification.admin.service.bo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
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
public class NoticeBaseBO implements Serializable {
    @JsonDeserialize(using = IDJsonDeserializer.class)
    private Long id;
    @NotEmpty(message = "编码不能为空")
    @Length(max = 32, message = "编码长度不能超过256")
    private String code;
    @NotEmpty(message = "名称不能为空")
    @Length(max = 32, message = "名称长度不能超过256")
    private String name;
    //版本
    private Integer version;
    //排序
    private Integer sort;
    //描述
    private String memo;

}
