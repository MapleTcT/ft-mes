package com.supcon.supfusion.signature.dao.enums;
/**
 * 按钮操作类型 
 * @author zhuyuyin
 * @version $Id$
 */
public enum OperateType {
	/**
	 * 增加
	 */
	ADD, 
	/**
	 * 修改
	 */
	MODIFY,
	/**
	 * 删除
	 */
	DELETE, 
	/**
	 * 自定义
	 */
	CUSTOM, 
	/**
	 * 分隔符
	 */
	SEPARATE,
	/**
	 * 增加子节点
	 */
	ADDCHILD, 
	/**
	 * 删除节点
	 */
	DELETENODE, 
	/**
	 * 向前增加兄弟节点
	 */
	ADDPREV, 
	/**
	 * 向后增加兄弟节点
	 */
	ADDNEXT, 
	/**
	 * 上移
	 */
	MOVEUP,
	/**
	 * 下移
	 */
	MOVEDOWN,
	/**
	 * 升级
	 */
	LEVELUP, 
	/**
	 * 降级
	 */
	LEVELDOWN,
	/**
	 * 树节点移动
	 */
	MOVE,
	/**
	 * 排序
	 */
	SORT,
	/**
	 * 导入
	 */
	IMPORT,
	/**
	 * 加行
	 */
	ADDROW,
	/**
	 * 刪行
	 */
	DELETEROW,
	/**
	 * 插行
	 */
	INSERTROW,
	/**
	 * 还原
	 */
	RESTORE,
	/**
	 * 打印
	 */
	PRINT,
	/**
	 * 批量打印
	 */
	BATCH_PRINT,
	/**
	 * 批量打印预览
	 */
	BATCH_PRINT_PREVIEW,
	/**
	 * 参照
	 */
	REF
}
