package com.supcon.supfusion.auditlog.manager.impl;

import com.supcon.supfusion.auditlog.manager.FileServerApiServiceAdapter;
import com.supcon.supfusion.file.server.api.BapFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileServerApiServiceAdapterImpl implements FileServerApiServiceAdapter {

    @Autowired
    BapFileService bapFileService;

    @Override
    public ResponseEntity<byte[]> downloadFile(String filePath) {
        return bapFileService.downloadFile(filePath);
    }
}
