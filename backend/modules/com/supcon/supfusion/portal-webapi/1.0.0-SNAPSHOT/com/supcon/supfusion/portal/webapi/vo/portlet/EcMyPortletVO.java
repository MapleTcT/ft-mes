package com.supcon.supfusion.portal.webapi.vo.portlet;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author kk.C
 * @Description: 用户自定义首页门户组态返回类
 * @Date 2020/11/30 16:31
 */
@Data
public class EcMyPortletVO implements Serializable {
    private static final long serialVersionUID = -8925193974379374134L;

    private Long id;
    @NotNull
    private Long userId;
    @NotNull
    private String config;

}
