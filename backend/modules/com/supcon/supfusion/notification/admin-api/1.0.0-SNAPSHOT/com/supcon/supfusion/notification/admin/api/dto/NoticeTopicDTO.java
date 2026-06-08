package com.supcon.supfusion.notification.admin.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@ToString
public class NoticeTopicDTO {

    @NotEmpty(message = "编码不能为空")
    @Length(max = 50, message = "编码长度不能超过50")
    private String code;

    @NotEmpty(message = "名称不能为空")
    @Length(max = 50, message = "名称长度不能超过50")
    private String name;

    @ApiModelProperty(value = "主题类型编码", example = "默认类型请填写：defaultType；待办类型请填写：defaultType002；")
    @NotEmpty(message = "主题类型编码不能为空")
    private String topicTypeCode;

    @ApiModelProperty(value = "模板编码")
    @NotEmpty
    @Size(min = 1)
    private List<String> templateCodes;

}
