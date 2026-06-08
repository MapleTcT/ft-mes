package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
@TableName(value = "ec_button", autoResultMap = true)
public class EcButton extends Button{
}
