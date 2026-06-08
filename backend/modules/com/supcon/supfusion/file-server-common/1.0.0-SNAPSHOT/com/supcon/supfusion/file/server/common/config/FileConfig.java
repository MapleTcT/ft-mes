package com.supcon.supfusion.file.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Configuration
@ConfigurationProperties(prefix = "supfusion.file")
public class FileConfig {

    /*
     *环境 dev  or pro
     */
    @Value("${spring.profiles.active:prod}")
    private String profile;


    @Value("${bap.list.maxPageSize:500}")
    private Integer maxPageSize;


    @Value("${bap.import.excel.maxsize:500}")
    private String maxsize;

    @Value("${filePreview:true}")
    private Boolean isFileView;


    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Integer getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(Integer maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public String getMaxsize() {
        return maxsize;
    }

    public void setMaxsize(String maxsize) {
        this.maxsize = maxsize;
    }

    public Boolean getFileView() {
        return isFileView;
    }

    public void setFileView(Boolean fileView) {
        isFileView = fileView;
    }
}
