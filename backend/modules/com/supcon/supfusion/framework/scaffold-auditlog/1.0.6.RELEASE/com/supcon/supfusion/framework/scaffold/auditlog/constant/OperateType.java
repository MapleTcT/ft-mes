package com.supcon.supfusion.framework.scaffold.auditlog.constant;

/**
 * 操作类型
 *
 * @author caokele
 */
public enum OperateType {
    /**
     * 新增
     */
    ADD,
    /**
     * 修改
     */
    MODIFY,
    /**
     * 新增或修改
     * 根据方法参数中是否包含主键自行判断
     */
    ADD_OR_MODIFY,
    /**
     * 删除
     */
    DELETE,
    /**
     * 其他
     */
    OTHER,
    /**
     * 导入
     */
    IMPORT,
    /**
     * 导出
     */
    EXPORT,
    /**
     * 作废
     */
    INVALID,
    /**
     * 驳回
     */
    REJECT,
    /**
     * 打印
     */
    PRINT,
    /**
     * 批量打印
     */
    BATCH_PRINT,
    /**
     * 还原
     */
    ROLLBACK
}
