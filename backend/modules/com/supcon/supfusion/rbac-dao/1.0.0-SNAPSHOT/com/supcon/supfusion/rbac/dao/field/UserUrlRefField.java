package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 用户与请求URL关联表
 * </p>
 */
public class UserUrlRefField {


    /**
     * 主键ID
     */
    public static String id="ID";


    /**
     * 用户ID
     */
    public static String userId="USER_ID";

    /**
     * 对应请求URL
     */
    public static String url="URL";
}
