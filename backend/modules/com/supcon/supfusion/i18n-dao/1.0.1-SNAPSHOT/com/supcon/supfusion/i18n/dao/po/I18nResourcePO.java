package com.supcon.supfusion.i18n.dao.po;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;



/**
 *
 * @Description:    * 国际化键值对表实体
 *                  * 表名 supfusion_i18n_resource
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = I18nResourcePO.TABLE_NAME, autoResultMap = true)
public class I18nResourcePO implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "supfusion_i18n_resource";

    //主键
    private Long id;
    //国际化主键
    private String i18nKey;
    //i18n_value
    private String i18nValue;
    //语言code码
    private String languCode;
    //模块名主键
    private String moduleCode;
    //版本号表主键
    @TableField("module_version_code")
    private String moduleVersionCode;
    //是否删除 1 使用 0 删除
    private String valid;
    @TableField(value="tenant_id", fill = FieldFill.INSERT)
    private String tenantId;
    //创建人
    private String creator;
    //修改人
    private Date modifier;
    //创建时间
    private Date createTime;
    //更新时间
    private Date modifyTime;
    //创建人员id
    private Long createStaffId;
    //修改人员id
    private Long modifyStaffId;

    /**
     *  重写hashCode，方便集合的操作
     */
    @Override
    public int hashCode() {
        return  (this.i18nKey+this.i18nValue+this.languCode).hashCode();
    }
    /**
     *  重写equals，只有i18nKey 和i18nValue languCode都相等，才认为这个对象是想等的
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof I18nResourcePO)){
            return false;
        }
        I18nResourcePO  rp = (I18nResourcePO) obj;
        return ((this.i18nKey.equals(rp.i18nKey)) && (this.i18nValue.equals(rp.i18nValue)) &&  (this.languCode.equals(rp.languCode)));
    }

}
