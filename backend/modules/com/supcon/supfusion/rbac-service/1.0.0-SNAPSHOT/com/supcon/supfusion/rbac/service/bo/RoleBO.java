package com.supcon.supfusion.rbac.service.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.List;

@Data
public class RoleBO {

    /**
     * 版本
     */
    private Integer version;
    /**
     * 全部标签
     */
    private List<TagBO> tagBOs;

    /**
     * 第一个标签
     */
    private TagBO tagBO;
}
