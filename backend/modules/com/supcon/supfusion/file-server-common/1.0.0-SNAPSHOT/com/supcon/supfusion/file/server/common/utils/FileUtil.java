package com.supcon.supfusion.file.server.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class FileUtil {

    public static long getFileSize(File f){
        try {
            return !f.exists() ? 0 : f.length();
        } catch (Exception e) {
            log.error("get file  size  fail");
        }
        return 0;
    }

    /**
     * 文件流的形式copy
     *
     * @param  input
     * @param dest
     */
    public static void copyFileUsingFileStreams(InputStream input, File dest) {
        try (OutputStream output = new FileOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public static void createDir(String newPath) {
        File target = new File(newPath);
        if (!target.exists()) {
            target.mkdirs();
        }
    }
    public static void createNewFile(String newPathFile) {
        File file = new File(newPathFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error(newPathFile + " create new  file  fail!");
            }
        }
    }

}
