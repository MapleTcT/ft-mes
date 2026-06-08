package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 基础信息
 * </p>
 *
 * @author 袁阳
 * @since 2020-07-13
 */
public class InitVersionInfoField{

    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 初始化版本号
     */
    public static String initVersion="INIT_VERSION";

    /**
     * 服务名
     */
    public static String app="APP";

}
