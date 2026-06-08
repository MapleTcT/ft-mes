package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;


/**
 * pageData
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PageDataVO extends VO {

    /**
     * 页面来源
     */
    @ApiModelProperty(value = "页面来源")
    private Integer source;

    /**
     * 页面路径
     */
    @ApiModelProperty(value = "页面路径")
    private String path;

    /**
     * 页面级别
     */
    @ApiModelProperty(value = "标签名称")
    private Integer level;

    /**
     * 页面名称
     */
    @ApiModelProperty(value = "页面名称")
    private String name;

    /**
     * 页面编码
     */
    @ApiModelProperty(value = "标签名称")
    private String code;

    /**
     * 页面类型
     */
    @ApiModelProperty(value = "页面类型")
    private Integer type;

    /**
     * 页面父编码
     */
    @ApiModelProperty(value = "标签名称")
    private String pCode;

    /**
     * 模型编码
     */
    @ApiModelProperty(value = "模型编码")
    private String modelCode;

    /**
     * 下级目录
     */
    @ApiModelProperty(value = "下级目录")
    private List<PageDataVO> children;

}
