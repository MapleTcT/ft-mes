package com.supcon.supfusion.portal.service.entity;

import com.supcon.supfusion.portal.service.bo.EcPortletBO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author kk.C
 * @Description: 用户自定义门户配置
 * @Date 2020/12/1 13:38
 */
@Data
public class MyPortlet implements Serializable {

    private static final long serialVersionUID = 141902645926570548L;

    private String width;
    private List<EcPortletBO> portlets;
}
