package com.supcon.supfusion.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;


/**
 *
 * @Description:  * 模块表
 *                * 表名 supfusion_mod
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = ModulePO.TABLE_NAME, autoResultMap = true)
public class ModulePO implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "supfusion_mod";
    //主键
    private Integer id;
    //模块名code
    private String moduleCode;
    //模块名
    private String moduleName;
    //是否删除
    private String creator;
    //创建人
    private String valid;
    //修改人
    private String modifier;
    //创建时间
    private Date createTime;
    //更新时间
    private Date modifyTime;
    //创建人员id
    private Long createStaffId;
    //修改人员id
    private Long modifyStaffId;

}
