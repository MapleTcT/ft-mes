/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年9月22日 下午1:27:49
 */
public enum RecipientSelection {
    /**
     * 发起者
     */
    INITIATOR("initiator"),
    /**
     * 执行者是上下文变量
     */
    VARIABLE("variable"),
    /**
     * 选择部门
     */
    DEPART("department"),
    /**
     * 选择岗位
     */
    POSITION("position"),
    /**
     * 选择角色
     */
    ROLE("role"),
    /**
     * 选择人员
     */
    PERSON("staff"),
    /**
     * 发起者直属领导
     */
    INITIATOR_DIRECT_LEADER("initiatorDirectLeader"),
    /**
     * 发起者隔级领导
     */
    INITIATOR_SUPERIOR_LEADER("initiatorSuperiorLeader"),
    /**
     * 提交者直属领导
     */
    SUBMITTER_DIRECT_LEADER("submitterDirectLeader"),
    /**
     * 提交者隔级领导
     */
    SUBMITTER_SUPERIOR_LEADER("submitterSuperiorLeader");
    
    final String name;
    
    private RecipientSelection(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
    public static RecipientSelection getByName(String name) {
        for (RecipientSelection selection : RecipientSelection.values()) {
            if (selection.getName().equals(name)) {
                return selection;
            }
        }
        return null;
    }
}
