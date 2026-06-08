package com.supcon.supfusion.rbac.dao.po;

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
@Data
@TableName(value = "rbac_user_url_ref", autoResultMap=true)
public class UserUrlRefPO implements Serializable {


    private static final long serialVersionUID = -7054140336265700715L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;


    /**
     * 用户ID
     */
    @TableField("USER_ID")
    private Long userId;

    /**
     * 对应请求URL
     */
    @TableField("URL")
    private String url;

    /**
     * 请求方法，0 GET,1 POST,2 PUT,3 DELETE
     */
    @TableField(exist = false)
    private Integer methodType;

    /**
     * 服务名
     */
    @TableField(exist = false)
    private String app;

    /**
     * 公司ID
     */
    @TableField(exist = false)
    private Long cid;

}
