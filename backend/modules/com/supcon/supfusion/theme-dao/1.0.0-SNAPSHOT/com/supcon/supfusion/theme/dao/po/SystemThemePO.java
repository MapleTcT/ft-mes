package com.supcon.supfusion.theme.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = SystemThemePO.TABLE_NAME, autoResultMap = true)
public class SystemThemePO extends PO {

    public static final String TABLE_NAME = "system_theme";

    @TableId(value = "id")
    private Long id;

    @TableField(value = "theme")
    private String theme;

    @TableField(value = "logo")
    private String logo;

    @TableField(value = "font")
    private Integer font;

    @TableField(value = "status")
    private Integer status;

    public static String getIdFieldName() {
        return "id";
    }

    public static String getUserIdFieldName() {
        return "user_id";
    }

    public static String getThemeFieldName() {
        return "theme";
    }

    public static String getLogoFieldName() {
        return "logo";
    }

    public static String getFontFieldName() {
        return "font";
    }

    public static String getStatusFieldName() {
        return "status";
    }
}
