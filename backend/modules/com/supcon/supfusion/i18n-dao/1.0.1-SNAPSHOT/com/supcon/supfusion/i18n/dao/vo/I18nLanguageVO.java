package com.supcon.supfusion.i18n.dao.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.Date;


/**
 *
 * @Description:    * 国际化key对应的语言类型表实体
 *                  * 表名 supfusion_i18n_language
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
@ApiModel(description= "语言实体类VO")
public class I18nLanguageVO extends VO {
    private static final long serialVersionUID = 1L;

    //当前语言是否启用  false不使用 true使用
    @ApiModelProperty(value = "当前语言是否启用  false不使用 true使用")
    private boolean Used;

    @ApiModelProperty(value = "主键")
    private Long id;
    //语言code码 eg:zn_CH
    @ApiModelProperty(value = "语言code码 eg:zn_CH")
    private String languCode;
    //语言类型(中文描述)
    @ApiModelProperty(value = "语言类型(中文描述)")
    private String languType;
    //语言类型(code自己对应的语言描述)
    @ApiModelProperty(value = "语言类型(code自己对应的语言描述)")
    private String languName;
    //是否启用 0 不使用 1使用
    @ApiModelProperty(value = "是否启用 0 不使用 1使用")
    private String  hasUsed;
    //是否删除
    @ApiModelProperty(value = "是否删除")
    private String valid;
    //创建人
    @ApiModelProperty(value = "创建人")
    private String creator;
    //创建时间
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    //修改人
    @ApiModelProperty(value = "修改人")
    private String modifier;
    //修改时间
    @ApiModelProperty(value = "修改时间")
    private Date modifyTime;

    public boolean  hasUsed() {
        return Used;
    }

    public void setUsed(boolean used) {
        Used = used;
    }

}
