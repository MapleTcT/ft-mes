package com.supcon.supfusion.systemcode.dao.po;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 字典项的的树型结构详情类
 * @author root
 *
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class SystemEntityDetailPO extends SystemEntityPO {


	private static final long serialVersionUID = -4755016261515947576L;

	/**
	 * 字典项的直接子编码节点
	 */
	private List<SystemCodeDetailPO> children;

}
