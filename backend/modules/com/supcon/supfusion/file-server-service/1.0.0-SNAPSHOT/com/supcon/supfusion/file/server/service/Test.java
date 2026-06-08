package com.supcon.supfusion.file.server.service;

import java.io.File;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        File file1 = new File("D:\\a\\b\\");
        file1.mkdirs();
        File file = new File("D:\\a\\b\\c.txt");
        file.createNewFile();
        removeDirAndFile("D:\\a");
    }

    public static void removeDirAndFile(String path) {
        File orgFile = new File(path);
        File[] files = orgFile.listFiles();
        if (null != files) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else {
                    removeDirAndFile(file.getPath());
                }
            }
        }
        orgFile.delete();
    }
}
