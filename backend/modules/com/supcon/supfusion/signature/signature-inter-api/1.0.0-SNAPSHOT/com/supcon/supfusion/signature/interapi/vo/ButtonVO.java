package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.signature.dao.entity.Button;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import java.io.Serializable;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ButtonVO extends VO {

    private static final long serialVersionUID = 8897451125254281213L;

    private Button button;

    @ApiModelProperty("按钮名字")
    private String buttonName;

    @ApiModelProperty("是否开启电子签名")
    private Boolean signatureEnabled;

    @ApiModelProperty("签名类型(singleSign,doubleSign)")

    private String signatureType;
    @ApiModelProperty("双签时,权限类型")
    private String powerType;

    @ApiModelProperty("人员id集合")
    private List<String> staffMultiIDs;
    private String staff;

    @ApiModelProperty("岗位id集合")
    private List<String> positionMultiIDs;
    private String position;

    @ApiModelProperty("角色id集合")
    private List<String> roleMultiIDs;
    private String role;

    @ApiModelProperty("按钮code")
    private String buttonCode;

    @ApiModelProperty("描述")
    @Max(value = 255, message = "字符长度不能大于255位")
    private String signatureDescrible;

}
