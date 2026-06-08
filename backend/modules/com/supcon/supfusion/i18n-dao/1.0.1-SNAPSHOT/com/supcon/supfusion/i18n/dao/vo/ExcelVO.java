package com.supcon.supfusion.i18n.dao.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.Date;

/**
 * Excel导入记录类
 */
@Data
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description= "导出导出状态类")
public class ExcelVO extends VO {
    private static final long serialVersionUID = 1L;


    //Excel导入记录id
    @ApiModelProperty(value = "主键ID")
    private Long id;
    //导入、导出状态,1进行中, 2成功, 3失败
    @ApiModelProperty(value = "导入、导出状态,1进行中, 2成功, 3失败")
    private Integer status;
    //导入/导出文件名
    @ApiModelProperty(value = "导入/导出文件名")
    private String fileName;
    //生成的错误文件名
    @ApiModelProperty(value = "生成的错误文件名")
    private String errorFile;
    //错误消息
    @ApiModelProperty(value = "错误消息")
    private String errorMessage;
    //类型,import 或 export
    @ApiModelProperty(value = "类型,import 或 export")
    private String type;
    //不符合要求的行数
    @ApiModelProperty(value = "不符合要求的行数")
    private Integer errorNum;
    //新增的行数
    @ApiModelProperty(value = "新增的行数")
    private Integer addNum;
    //更新的行数
    @ApiModelProperty(value = "更新的行数")
    private Integer updateNum;
    //总行数
    @ApiModelProperty(value = "总行数")
    private Integer allNum;
    //是否删除  0不使用 1 使用
    @ApiModelProperty(value = "是否删除  0不使用 1 使用")
    private String valid;
    //创建人
    @ApiModelProperty(value = "创建人")
    private String creator;
    //修改人
    @ApiModelProperty(value = "修改人")
    private String modifier;
    //创建时间
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    //更新时间
    @ApiModelProperty(value = "更新时间")
    private Date modifyTime;

}
