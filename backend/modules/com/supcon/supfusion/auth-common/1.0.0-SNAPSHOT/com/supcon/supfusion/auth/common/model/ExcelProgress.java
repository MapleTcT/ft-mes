package com.supcon.supfusion.auth.common.model;

public enum ExcelProgress {
    IN_PROGRESS(1, "进行中"),
    FINISH(2, "完成"),
    FAIL(3, "失败");
    private Integer progress;
    private String description;

    ExcelProgress(Integer progress, String description) {
        this.progress = progress;
        this.description = description;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getDescription() {
        return description;
    }

}
