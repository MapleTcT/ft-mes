package com.supcon.supfusion.auditlog.manager;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface FileServerApiServiceAdapter {
    /**
     * 下载附件
     * @param filePath 附件相对路径
     * @return
     */
    ResponseEntity<byte[]> downloadFile(@RequestParam(value = "filePath") String filePath);
}
