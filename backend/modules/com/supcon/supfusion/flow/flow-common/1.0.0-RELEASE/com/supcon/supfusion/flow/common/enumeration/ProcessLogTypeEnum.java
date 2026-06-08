/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年11月19日 上午9:46:21
 */
public enum ProcessLogTypeEnum {
    /**
     * 任务提交
     */
    TASK_COMPLETE,
    /**
     * 任务驳回
     */
    TASK_REJECT,
    /**
     * 任务委托
     */
    TASK_DELEGATE,
    /**
     * 代理任务委托
     */
    TASK_PROXY_DELEGATE,
    /**
     * 任务撤回
     */
    TASK_ROLLBACK,
    /*
     * 加签
     */
    TASK_ADD,
    /**
     * 发起流程
     */
    PROCESS_START,
    /**
     * 终止流程
     */
    PROCESS_TERMINATE,
    /**
     * 暂停流程
     */
    PROCESS_SUSPEND,
    /**
     * 恢复流程
     */
    PROCESS_RESUME,
    /**
     * 取消委托
     */
    TASK_CANCEL_DELEGATE,
    /**
     * 定时取消
     */
    TIMER_CANCEL_ACTIVITI;

}
