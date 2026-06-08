package com.supcon.supfusion.organization.manager.impl;

import com.supcon.supfusion.file.server.api.BapFileService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.manager.FileServerApiServiceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileServerApiServiceAdapterImpl implements FileServerApiServiceAdapter {
    @Autowired
    private BapFileService bapFileService;

    @Override
    public Result fileUpload(MultipartFile uploadFile) {
        return bapFileService.fileUpload(uploadFile);
    }
}
