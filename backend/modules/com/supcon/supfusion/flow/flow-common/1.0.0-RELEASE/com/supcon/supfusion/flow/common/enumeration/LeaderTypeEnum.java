/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年8月21日 上午9:09:52
 */
public enum LeaderTypeEnum {
    /**
     * 直属领导
     */
    DIRECT_LEADER(1),
    /**
     * 隔级领导
     */
    SUPERIOR_LEADER(2);
    /**
     * leader类型
     */
    private final int type;
    
    private LeaderTypeEnum(final int type) {
        this.type = type;
    }

    public static LeaderTypeEnum getByLevel(int type) {
        for (LeaderTypeEnum leaderType : LeaderTypeEnum.values()) {
            if (type == leaderType.getType()) {
                return leaderType;
            }
        }
        return null;
    }
    
    public int getType() {
        return type;
    }
    
}
