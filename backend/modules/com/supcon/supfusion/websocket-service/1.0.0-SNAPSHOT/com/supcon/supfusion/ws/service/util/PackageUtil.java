package com.supcon.supfusion.ws.service.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageUtil {


    /**
     * 获取某包下（包括该包的所有子包）所有类
     *
     * @param packageName 包名
     * @return 类的完整名称
     */
    public static List<String> getClassName(String packageName) {
        return getClassName(PackageUtil.class.getClassLoader(), packageName, true);
    }

    /**
     * 获取某包下（包括该包的所有子包）所有类
     *
     * @param loader      ClassLoader
     * @param packageName 包名
     * @return 类的完整名称
     */
    public static List<String> getClassName(ClassLoader loader, String packageName) {
        return getClassName(loader, packageName, true);
    }

    /**
     * 获取某包下所有类
     *
     * @param loader      ClassLoader
     * @param packageName 包名
     * @param recur       是否遍历子包
     * @return 类的完整名称
     */
    public static List<String> getClassName(ClassLoader loader, String packageName, boolean recur) {
        List<String> classNames = null;
        String packagePath = packageName.replace(".", "/");
        URL url = loader.getResource(packagePath);
        if (url != null) {
            String type = url.getProtocol();
            if (type.equals("file")) {
                String classPath = loader.getResource("./").getPath();
                classNames = getClassNameByFile(classPath, url.getPath(), recur);
                System.out.println("asdadasdas" + classPath);
            } else if (type.equals("jar")) {
                System.out.println("31232432424");
                classNames = getClassNameByJar(url.getPath(), recur);
            }
        } else {
            classNames = getClassNameByJars(((URLClassLoader) loader).getURLs(), packagePath, recur);
        }
        return classNames;
    }

    /**
     * 从项目文件获取某包下所有类
     *
     * @param filePath  文件路径
     * @param classPath
     * @param recur     是否遍历子包
     * @return 类的完整名称
     */
    private static List<String> getClassNameByFile(String classPath, String filePath, boolean recur) {
        List<String> classNames = new ArrayList<String>();
        File file = new File(filePath);
        File[] childFiles = file.listFiles();
        for (File childFile : childFiles) {
            if (childFile.isDirectory()) {
                if (recur) {
                    classNames.addAll(getClassNameByFile(classPath, childFile.getPath(), recur));
                }
            } else {
                String childFilePath = childFile.toURI().getPath();
                if (childFilePath.endsWith(".class")) {
                    childFilePath = childFilePath.substring(classPath.length(), childFilePath.lastIndexOf("."));
                    childFilePath = childFilePath.replace("/", ".");
                    classNames.add(childFilePath);
                }
            }
        }

        return classNames;
    }

    /**
     * 从jar获取某包下所有类
     *
     * @param jarPath jar文件路径
     * @param recur   是否遍历子包
     * @return 类的完整名称
     */
    private static List<String> getClassNameByJar(String jarPath, boolean recur) {
        List<String> myClassName = new ArrayList<String>();
        String[] jarInfo = jarPath.split("!");
        String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
        String packagePath = jarInfo[1].substring(1);
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarFilePath);
            Enumeration<JarEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                JarEntry jarEntry = entrys.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    if (recur) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    } else {
                        int index = entryName.lastIndexOf("/");
                        String myPackagePath;
                        if (index != -1) {
                            myPackagePath = entryName.substring(0, index);
                        } else {
                            myPackagePath = entryName;
                        }
                        if (myPackagePath.equals(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return myClassName;
    }

    /**
     * 从所有jar中搜索该包，并获取该包下所有类
     *
     * @param urls        URL集合
     * @param packagePath 包路径
     * @param recur       是否遍历子包
     * @return 类的完整名称
     */
    private static List<String> getClassNameByJars(URL[] urls, String packagePath, boolean recur) {
        List<String> myClassName = new ArrayList<String>();
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                URL url = urls[i];
                String urlPath = url.getPath();
                // 不必搜索classes文件夹
                if (urlPath.endsWith("classes/")) {
                    continue;
                }
                String jarPath = urlPath + "!/" + packagePath;
                myClassName.addAll(getClassNameByJar(jarPath, recur));
            }
        }
        return myClassName;
    }
}