package com.supcon.supfusion.portal.manager.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author kk.C
 * @Description: 新增或修改国际化入参类
 * @Date 2020/10/24 13:50
 */
@Data
public class I18nParam implements Serializable {

    private static final long serialVersionUID = 5562342844007808764L;

    private  String en_US;
    private  String zh_HK;
    private  String zh_CN;
    private  String moduleCode;
}
