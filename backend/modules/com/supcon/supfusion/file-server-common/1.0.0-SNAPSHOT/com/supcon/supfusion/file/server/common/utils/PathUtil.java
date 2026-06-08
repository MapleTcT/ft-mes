package com.supcon.supfusion.file.server.common.utils;

import com.supcon.supfusion.file.server.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class PathUtil {

    public static String getFilePath(String folder, String timestamp, String fileName) {
        return "/" + folder + "/" + timestamp + "/" + fileName;
    }

    public static String getTempPath() {
        String tempPath = System.getProperty(Constants.USER_DIR) + Constants.PATH + Constants.TEMP_PATH;
        FileUtil.createDir(tempPath);
        return tempPath;
    }

    public static String getConvertTempPath() {
        String tempPath = System.getProperty(Constants.USER_DIR) + Constants.PATH + Constants.TEMP_PATH + Constants.CONVERT_PATH + Constants.PATH;
        FileUtil.createDir(tempPath);
        return tempPath;
    }

    public static String getStaticFilePath() {
        String staticFilePath = "";
        String userDir = System.getProperty(Constants.USER_DIR);
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            staticFilePath = "/data";
        } else {
             staticFilePath = userDir.substring(0, userDir.indexOf("bap-server\\base-Server")) + "bap-server/bap-workspace/bap-static/file";
        }
        FileUtil.createDir(staticFilePath);
        return staticFilePath;
    }

    public static String getProjectPath() {
        return  System.getProperty("user.dir");
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

//    public static URL locateUrl(String resourcesPath){
//        URL url = null;
//        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        if (null != loader) {
//            url = loader.getResource(resourcesPath);
//            //jar:file:/app/bootstrap.jar!/i18nResource/i18n/
//        }
//        if (null == url) {
//            url = ClassLoader.getSystemResource(resourcesPath);
//            //jar:file:/app/bootstrap.jar!/i18nResource/i18n/
//        }
//        return url;
//    }
//
//    public static void copyFfepegFile(URL url, String destPath) {
//        if (null != url) {
//            String urlStr = url.toString();
//            if (urlStr.startsWith("jar:")) {
//                jarCopy(url, destPath);
//            } else if (urlStr.startsWith("file:")) {
//                fileCopy(url, destPath);
//            }
//        }
//    }
//
//    /**
//     * jar 包形式下的本地资源
//     */
//    private static void jarCopy(URL url, String moduleCode, String versionCode, String i18nPath, String messageDir) {
//        String jarPath = url.toString().substring(0, url.toString().indexOf(Constants.JAR_FILE_SUFFIX) + Constants.JAR_FILE_SUFFIX_LENGTH);
//        URL jarURL = null;
//        JarURLConnection jarCon = null;
//        JarFile jarFile = null;
//        try {
//            jarURL = new URL(jarPath);
//            jarCon = (JarURLConnection) jarURL.openConnection();
//            jarFile = jarCon.getJarFile();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        File temp = new File(i18nPath);
//        if (null != jarFile && temp.exists()) {
//            Enumeration<JarEntry> jarEntrys = jarFile.entries();
//            while (jarEntrys.hasMoreElements()) {
//                JarEntry entry = jarEntrys.nextElement();
//                String name = entry.getName();
//                if ((name.startsWith(moduleCode+Constants.STR_LINE) || name.startsWith(messageDir+moduleCode) || name.startsWith(Constants.MAVEN_RESOURCE_PATH + messageDir))
//                        && !entry.isDirectory() && name.endsWith(Constants.PROPERTIES_LOW)) {
//                    writeAndClose(name, i18nPath);
//                }
//            }
//        }
//    }
//
//    /**
//     * 写入临时文件
//     *
//     * @param inputFileFullName
//     * @param tempDir
//     * @throws IOException
//     */
//    public static void writeAndClose(String inputFileFullName, String tempDir) {
//
//        try (InputStream inputStream = locateStream(inputFileFullName);
//             OutputStream outputStream = output(tempDir, inputFileFullName);) {
//            int read;
//            byte[] bytes = new byte[1024];
//            while ((read = inputStream.read(bytes)) != -1) {
//                for (int i = 0; i < read; i++) {
//                    outputStream.write(bytes[i]);
//                }
//            }
//        } catch (IOException e) {
//            log.error("write temp file fail! ", e);
//        }
//    }
//
//    /**
//     * 创建输入流
//     *
//     * @param fileName
//     * @return
//     */
//    private static InputStream locateStream(String fileName) {
//        InputStream inputStream = null;
//        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        if (null != loader) {
//            inputStream = loader.getResourceAsStream(fileName);
//        }
//        if (null == inputStream) {
//            inputStream = ClassLoader.getSystemResourceAsStream(fileName);
//        }
//        return inputStream;
//    }
//    /**
//     * 创建输出流
//     *
//     * @param tempDir
//     * @param fileFullName
//     * @return
//     * @throws IOException
//     */
//    private static OutputStream output(String tempDir, String fileFullName) throws IOException {
//        OutputStream outputStream;
//        StringBuffer sb = new StringBuffer(tempDir);
//        addFileSeperator(sb);
//        String fileName = subStr(fileFullName);
//        sb.append(fileName);
//        File file = new File(sb.toString());
//        File parent = file.getParentFile();
//        if (!parent.exists()) {
//            parent.mkdirs();
//        }
//        if (!file.exists()) {
//            file.createNewFile();
//        }
//        outputStream = new FileOutputStream(file);
//        return outputStream;
//    }
//
//    private static String subStr(String fileFullName) {
//        while (fileFullName.contains(Constants.PATH)) {
//            fileFullName = fileFullName.substring(fileFullName.indexOf(Constants.PATH) + 1);
//        }
//        return fileFullName;
//    }
//    /**
//     * 补目录分隔符
//     *
//     * @param sb
//     * @return
//     */
//    public static StringBuffer addFileSeperator(StringBuffer sb) {
//        if (!sb.toString().endsWith(File.separator)) {
//            sb.append(File.separator);
//        }
//        return sb;
//    }
//
//    /**
//     * 文件 形式下的本地资源
//     */
//    private static void fileCopy(URL url, String moduleCode, String versionCode, String i18nPath) {
//        String filePath = url.toString().substring(6);
//        try {
//            copyFiles(filePath, i18nPath);
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    public static void copyFiles(String oldpath, String newpath) throws IOException {
//        File file = new File(oldpath);
//        if (file.listFiles() != null) {
//            File[] fs = file.listFiles();
//            for (File f : fs) {
//                if (f.isDirectory()) {
//                } else if (f.isFile()) {
//                    File oldFile = new File(f.toString());
//                    File newFile = new File(newpath + f.getName());
//                    copyFileUsingFileStreams(oldFile, newFile);
//                }
//            }
//        }
//    }
//
//    /**
//     * 文件流的形式copy
//     *
//     * @param source
//     * @param dest
//     */
//    public static void copyFileUsingFileStreams(File source, File dest) {
//        try (InputStream input = new FileInputStream(source);
//             OutputStream output = new FileOutputStream(dest);) {
//            byte[] buf = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = input.read(buf)) != -1) {
//                output.write(buf, 0, bytesRead);
//            }
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }
}
