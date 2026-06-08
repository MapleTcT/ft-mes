package com.supcon.supfusion.organization.manager;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public interface FileServerApiServiceAdapter {

    /**
     *  附件 上传
     *
     * @param uploadFile
     * @return
     */
    Result fileUpload(@RequestPart("file") MultipartFile uploadFile);
}
