package com.supcon.supfusion.file.server.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * ${description}
 *
 * @author szq
 * @create 2020/11/9 16:42
 */
public class BaseUtil {
    public static Map<String, String> FILE_TYPE_IMAGE = new HashMap<String, String>();
    static {
        FILE_TYPE_IMAGE.put("jpg", "image");
        FILE_TYPE_IMAGE.put("jpeg", "image");
        FILE_TYPE_IMAGE.put("gif", "image");
        FILE_TYPE_IMAGE.put("bmp", "image");
        FILE_TYPE_IMAGE.put("png", "image");
        FILE_TYPE_IMAGE.put("tif", "image");
        FILE_TYPE_IMAGE.put("tiff", "image");
        FILE_TYPE_IMAGE.put("wmf", "image");
        FILE_TYPE_IMAGE.put("jfif", "image");
        FILE_TYPE_IMAGE.put("doc", "word");
        FILE_TYPE_IMAGE.put("docx", "word");
        FILE_TYPE_IMAGE.put("wps", "word");
        FILE_TYPE_IMAGE.put("wpt", "word");
        FILE_TYPE_IMAGE.put("md", "word");
        FILE_TYPE_IMAGE.put("bat", "word2");
        FILE_TYPE_IMAGE.put("txt", "word2");
        FILE_TYPE_IMAGE.put("xml", "word2");
        FILE_TYPE_IMAGE.put("log", "word2");
        FILE_TYPE_IMAGE.put("properties", "word2");
        FILE_TYPE_IMAGE.put("html", "word2");
        FILE_TYPE_IMAGE.put("htm", "word2");
        FILE_TYPE_IMAGE.put("xls", "excel");
        FILE_TYPE_IMAGE.put("xlsx", "excel");
        FILE_TYPE_IMAGE.put("et", "excel");
        FILE_TYPE_IMAGE.put("ett", "excel");
        FILE_TYPE_IMAGE.put("ppt", "ppt");
        FILE_TYPE_IMAGE.put("pptx", "ppt");
        FILE_TYPE_IMAGE.put("pdf", "pdf");
        FILE_TYPE_IMAGE.put("mp4", "video");
        FILE_TYPE_IMAGE.put("avi", "video");
        FILE_TYPE_IMAGE.put("mov", "video");
        FILE_TYPE_IMAGE.put("rmvb", "video");
        FILE_TYPE_IMAGE.put("rm", "video");
        FILE_TYPE_IMAGE.put("flv", "video");
    }
}
