package com.supcon.supfusion.portal.service.entity;

import lombok.Data;

import java.io.Serializable;
import java.security.KeyStore;

/**
 * @Author kk.C
 * @Description: 菜单
 * @Date 2020/10/21 10:02
 */
@Data
public class MenuInfo implements Serializable {
    private static final long serialVersionUID = 7023674686566108962L;

    private int id;
    private String code; // 编码
    private String name; // 名称
    private String memo; // 备注
    private String url;// 程序URL
}
