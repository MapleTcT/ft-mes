package com.supcon.supfusion.file.server.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class DocumentSaveDTO implements Serializable {
    private static final long serialVersionUID = 7097916839851820035L;
    Long[] LinkIds;
    String[] filePaths;
    String[] fileTypes;
    Long[] mainModelId;
    String[] sizeDis;
    String[] memo;
    String[] propertyCode;
    String[] showType;
    String[] opener;
    Date[] openTime;
    Long[] deploymentId;
    String[] activityName;
    String[] taskDescription;
    String[] fileIcon;
    Boolean[] isFileView;
    String[] docContent;
    String[] docSummary;
    String[] convertStatus;
    String[] reason;
    String[] convertPath;
    String[] name;
    String[] type;
    String[] size;
    Long[] downloadTimes;
    Long[] previewTimes;
}

