package com.supcon.supfusion.systemcode.openapi.vo;

import java.util.List;

import com.supcon.supfusion.systemcode.dao.po.SystemCodeDetailPO;

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
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemEntityDetailVO extends SystemEntityResultVO {

	
	private static final long serialVersionUID = -4487099921546786682L;

	/**
	 * 字典项的直接子编码节点
	 */
	private List<SystemCodeDetailPO> children;
}
