package com.supcon.supfusion.i18n.dao.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @Description:    * 国际化键值对表实体
 *                  * 表名 supfusion_i18n_resource
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Data
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description= "国际化键值对VO")
public class I18nResourceVO extends VO implements Serializable {
    private static final long serialVersionUID = 1L;

    //主键
    @ApiModelProperty(value = "主键")
    private Long id;
    //国际化主键
    @ApiModelProperty(value = "国际化key")
    private String i18nKey;
    //i18n_value
    @ApiModelProperty(value = "国际化value")
    private String i18nValue;
    //语言code码
    @ApiModelProperty(value = "语言code码")
    private String languCode;
    //模块code
    @ApiModelProperty(value = "模块code")
    private String moduleCode;
    //版本号code
    @ApiModelProperty(value = "版本号code")
    private String moduleVersionCode;
    //是否删除 1 使用 0删除
    @ApiModelProperty(value = "是否删除 1 使用 0删除")
    private String valid;
    //创建人
    @ApiModelProperty(value = "创建人")
    private String creator;
    //修改人
    @ApiModelProperty(value = "修改人")
    private Date modifier;
    //创建时间
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    //更新时间
    @ApiModelProperty(value = "更新时间")
    private Date modifyTime;


    //i18n_value 的集合
    @ApiModelProperty(value = "语言和该语言的国际化值, 例如: {zh_CN: \"mytestfolder\"}")
    private Map<String,String> i18nValues = new HashMap<>();

    @Override
    public String toString() {
        return "I18nResourceVO{" +
                "i18nValues=" + i18nValues +
                '}';
    }
}
