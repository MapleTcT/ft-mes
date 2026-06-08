package com.supcon.supfusion.printer.service.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


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
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageDataBO {
    /**
     * page编号
     */
    private Long id;

    /**
     * 页面来源
     */
    private Integer source;

    /**
     * 页面路径
     */
    private String path;

    /**
     * 页面级别
     */
    private Integer level;

    /**
     * 页面名称
     */
    private String name;

    /**
     * 页面编码
     */
    private String code;

    /**
     * 页面类型
     */
    private Integer type;

    /**
     * 页面父编码
     */
    private String parentCode;

    /**
     * 模型编码
     */
    private String modelCode;
}
