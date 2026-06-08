package com.supcon.supfusion.i18n.common.until;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.supcon.supfusion.i18n.common.config.I18nProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  10:42 2020/6/23
 * @Modified:
 */
@Slf4j
public class MyFileUtils {


    private MyFileUtils() {
        throw new IllegalStateException("MyFileUtils class");
    }

    public static void createDir(String newPath) {
        File target = new File(newPath);
        if (!target.exists()) {
            target.mkdirs();
        }
    }

    public static void copyFiles(String oldpath, String newpath) throws IOException {
        File file = new File(oldpath);
        if (file.listFiles() != null) {
            File[] fs = file.listFiles();
            for (File f : fs) {
                if (f.isDirectory()) {
                } else if (f.isFile()) {
                    File oldFile = new File(f.toString());
                    File newFile = new File(newpath + f.getName());
                    copyFileUsingFileStreams(oldFile, newFile);
                }
            }
        }
    }

    /**
     * 写入临时文件
     *
     * @param inputFileFullName
     * @param tempDir
     * @throws IOException
     */
    public static void writeAndClose(String inputFileFullName, String tempDir) {

        try (InputStream inputStream = locateStream(inputFileFullName);
             OutputStream outputStream = output(tempDir, inputFileFullName);) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                for (int i = 0; i < read; i++) {
                    outputStream.write(bytes[i]);
                }
            }
        } catch (IOException e) {
            log.error("write temp file fail! ", e);
        }
    }

    /**
     * 创建输出流
     *
     * @param tempDir
     * @param fileFullName
     * @return
     * @throws IOException
     */
    private static OutputStream output(String tempDir, String fileFullName) throws IOException {
        OutputStream outputStream;
        StringBuffer sb = new StringBuffer(tempDir);
        addFileSeperator(sb);
        String fileName = subStr(fileFullName);
        sb.append(fileName);
        File file = new File(sb.toString());
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream = new FileOutputStream(file);
        return outputStream;
    }

    private static String subStr(String fileFullName) {
        while (fileFullName.contains(Constants.PATH)) {
            fileFullName = fileFullName.substring(fileFullName.indexOf(Constants.PATH) + 1);
        }
        return fileFullName;
    }

    /**
     * 补目录分隔符
     *
     * @param sb
     * @return
     */
    public static StringBuffer addFileSeperator(StringBuffer sb) {
        if (!sb.toString().endsWith(File.separator)) {
            sb.append(File.separator);
        }
        return sb;
    }

    /**
     * 创建输入流
     *
     * @param fileName
     * @return
     */
    private static InputStream locateStream(String fileName) {
        InputStream inputStream = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (null != loader) {
            inputStream = loader.getResourceAsStream(fileName);
        }
        if (null == inputStream) {
            inputStream = ClassLoader.getSystemResourceAsStream(fileName);
        }
        return inputStream;
    }

    /**
     * 文件流的形式copy
     *
     * @param source
     * @param dest
     */
    public static void copyFileUsingFileStreams(File source, File dest) {
        try (InputStream input = new FileInputStream(source);
             OutputStream output = new FileOutputStream(dest);) {
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 删除目录下的所有文件
     *
     * @param dirName
     */
    public static void deleteAnyone(String dirName) {
        File file = new File(dirName);
        if (file.exists()) {
            if (file.isFile()) {
                deleteFile(dirName);
            } else {
                deleteDir(dirName);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param fileName
     */
    private static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (!file.delete()) {
                log.error(file.getName() + Constants.DELETE_ERROR);
            }
        }
    }

    /**
     * 删除目录
     *
     * @param dirName
     */
    private static void deleteDir(String dirName) {
        dirName = addFileSeperator(dirName);
        File file = new File(dirName);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteAnyone(files[i].getAbsolutePath());
            }
        }
    }


    /**
     * 补目录分隔符
     *
     * @param dir
     * @return
     */
    private static String addFileSeperator(String dir) {
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        return dir;
    }


    //i18nDir 目录下找到 名为 moduleCode 的文件夹
    public static String findDir(String i18nDir, String moduleCode) {
        File i18nDirFile = new File(i18nDir);
        File[] fs = i18nDirFile.listFiles();
        String moduleDor = "";
        for (File f : fs) {
            if (f.isDirectory()) {
                if (f.getName().equals(moduleCode)) {
                    moduleDor = f.toString();
                }
            }
        }
        if (moduleDor.equals(Constants.STR_NO_SPACE)) {
            //没有就新建一个返回
            moduleDor = i18nDir + moduleCode + Constants.PATH;
            createDir(moduleDor);
        }
        return moduleDor;
    }


    public static String getIndexToFile(I18nProperties i18nProperties) {

        String indexFilePath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_INDEX_PATH;
        MyFileUtils.createDir(indexFilePath);
        String indexFile = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_INDEX_PATH + Constants.INDEX_FILE_NAME + Constants.STR_POINT + Constants.PROPERTIES_LOW;
        File indexFiles = new File(indexFile);
        if (!indexFiles.exists()) {
            try {
                if (!indexFiles.createNewFile()) {
                    log.error(indexFiles.getName() + Constants.CREATE_ERROR);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return indexFile;
    }

    //保存数据库中所有变动的国际化key 到文件 用于定时任务来 更新文件中的键值对
    public static void saveI18nKeyCode(Set<String> i18nKeySet, I18nProperties i18nProperties) {
        String txtFileName = UUID.randomUUID() + Constants.STR_NO_SPACE;
        StringBuilder sb = new StringBuilder(FilePathUtil.getFilePath(i18nProperties));
        sb.append(Constants.EXCEL_FILE_IMPORT_UPDATE_PATH);
        String targetFolderPath = sb.toString();
        try (FileWriter fw = new FileWriter(sb.append(txtFileName).append(Constants.STR_POINT).append(Constants.TXT_LOW).toString())) {
            MyFileUtils.createDir(targetFolderPath);
            for (String key : i18nKeySet) {
                fw.write(key + "," + "\r\n");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    //保存 实体配置上载国际化资源 失败结果
//    public static void saveUploadFileErrorMessage(String moduleCode, String newVersionCode, String destDir,String language, I18nConfig i18nConfig) {
//        String txtFileName = UUID.randomUUID() + Constants.STR_NO_SPACE;
//        try (FileWriter fw = new FileWriter(FilePathUtil.getFilePath(i18nConfig) + Constants.EXCEL_FILE_IMPORT_UPDATE_PATH + txtFileName + Constants.STR_POINT + Constants.TXT_LOW)) {
//            MyFileUtils.createDir(FilePathUtil.getFilePath(i18nConfig) + Constants.EXCEL_FILE_IMPORT_UPDATE_PATH);
//            for (String key : i18nKeySet) {
//                fw.write(key + "," + "\r\n");
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//    }

    public static List<String> getStr(List<String> list, List<String> list2) {
        List<String> list3 = new ArrayList<>();
        Iterator<String> iter = list.iterator();//迭代器
        if (list2 != null && list2.size() > 0) {
            while (iter.hasNext()) {
                String str = (String) iter.next();
                for (String str2 : list2) {
                    if (str.equals(str2)) {
                        list3.add(str);
                    }
                }
            }
        }
        return list3;
    }

    /**
     * 得到本机时间并转换成 年-月-日 时:分:秒 24小时制
     */
    public static String DateTime() {
        Format format = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        return format.format(new Date());
    }

    /**
     * 判断文件大小
     */
    public static boolean checkFileSize(Long len, int size, String unit) {
        double fileSize = 0;
        if ("B".equals(unit.toUpperCase())) {
            fileSize = (double) len;
        } else if ("K".equals(unit.toUpperCase())) {
            fileSize = (double) len / 1024;
        } else if ("M".equals(unit.toUpperCase())) {
            fileSize = (double) len / 1048576;
        } else if ("G".equals(unit.toUpperCase())) {
            fileSize = (double) len / 1073741824;
        }
        if (fileSize > size) {
            return false;
        }
        return true;
    }

    public static void cleanUp(Path path) throws NoSuchFileException, DirectoryNotEmptyException, IOException {
        Files.delete(path);
    }

    /**
     * 加载资源目录
     */
    private static URL locateUrl(String resourcesPath) {
        URL url = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (null != loader) {
            url = loader.getResource(resourcesPath);
            System.out.println("loader.getResource(i18nConfig.getI18nResourcePath()):" + url);
            //jar:file:/app/bootstrap.jar!/i18nResource/i18n/
        }
        if (null == url) {
            url = ClassLoader.getSystemResource(resourcesPath);
            System.out.println("ClassLoader.getSystemResource(i18nConfig.getI18nResourcePath()):" + url);
            //jar:file:/app/bootstrap.jar!/i18nResource/i18n/
        }
        return url;
    }

    /**
     * 解析本地国际化资源
     */
    public static void execRescource(String messageDir, String moduleCode, String versionCode, I18nProperties i18nProperties) {
        URL url = locateUrl(messageDir);
        if (null != url) {
            String i18nPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + moduleCode + Constants.PATH;
            String newCustomPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
            //判断路径是否存在
            MyFileUtils.createDir(i18nPath);
            MyFileUtils.createDir(newCustomPath);
            String s = url.toString();
            if (s.startsWith(Constants.JAR_FILE_PREFIX)) {
                jarCopy(url, moduleCode, versionCode, i18nPath, messageDir);
            } else if (s.startsWith(Constants.FILE_PREFIX)) {
                fileCopy(url, moduleCode, versionCode, i18nPath);
            }
        }
    }

    /**
     * jar 包形式下的本地资源
     */
    private static void jarCopy(URL url, String moduleCode, String versionCode, String i18nPath, String messageDir) {
        String jarPath = url.toString().substring(0, url.toString().lastIndexOf(Constants.JAR_FILE_SUFFIX) + Constants.JAR_FILE_SUFFIX_LENGTH);
        URL jarURL = null;
        JarURLConnection jarCon = null;
        JarFile jarFile = null;
        try {
            jarURL = new URL(jarPath);
            jarCon = (JarURLConnection) jarURL.openConnection();
            jarFile = jarCon.getJarFile();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File temp = new File(i18nPath);
        if (null != jarFile && temp.exists()) {
            Enumeration<JarEntry> jarEntrys = jarFile.entries();
            while (jarEntrys.hasMoreElements()) {
                JarEntry entry = jarEntrys.nextElement();
                String name = entry.getName();
                if ((name.startsWith(moduleCode+Constants.STR_LINE) || name.startsWith(messageDir+moduleCode) || name.startsWith(Constants.MAVEN_RESOURCE_PATH + messageDir))
                        && !entry.isDirectory() && name.endsWith(Constants.PROPERTIES_LOW)) {
                    writeAndClose(name, i18nPath);
                }
            }
        }
    }

    /**
     * 文件 形式下的本地资源
     */
    private static void fileCopy(URL url, String moduleCode, String versionCode, String i18nPath) {
        String filePath = url.toString().substring(6);
        try {
            copyFiles(filePath, i18nPath);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 如果文件不存在 则创建
     *
     * @param dirName
     */
    public static void createFile(String dirName) {
        File file = new File(dirName);
        if (!file.exists()) {
            try {
                boolean is = file.createNewFile();
            } catch (IOException e) {
                log.error(file.getName() + ":" + e.getMessage());
            }
        }
    }


    /**
     * 删除 i18n/messages/ 目录下某个模块所有版本的国际化资源
     *
     * @param dirName
     */
    public static void deleteFileByModuleCodeNotCustom(String dirName, String moduleCode) {
        File file = new File(dirName);
        if (file.exists() && file.isDirectory() && file.listFiles().length > 0) {
            File[] files = file.listFiles();
            Arrays.asList(files).forEach(file1 -> {
                if (file1.getName().length() > 12) {
                    if (file1.getName().substring(0, file1.getName().length() - 12).equals(moduleCode)) {
                        deleteAnyone(file1.getAbsolutePath());
                        if (file1.exists()) {
                            file1.delete();
                        }
                    }
                }
            });
        }
    }

    /**
     * 删除 i18n/custom/ 目录下某个模块所有版本的国际化资源
     *
     * @param dirName
     */
    public static void deleteFileByModuleCodeCustom(String dirName, String moduleCode) {
        File file = new File(dirName);
        if (file.exists() && file.isDirectory() && file.listFiles().length > 0) {
            File[] files = file.listFiles();
            Arrays.asList(files).forEach(file1 -> {
                if (file1.getName().equals(moduleCode)) {
                    deleteAnyone(file1.getAbsolutePath());
                    if (file1.exists()) {
                        file1.delete();
                    }
                }
            });
        }
    }
}
