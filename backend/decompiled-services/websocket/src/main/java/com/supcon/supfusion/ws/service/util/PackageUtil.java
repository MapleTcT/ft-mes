/*
 * Decompiled with CFR 0.152.
 */
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
    public static List<String> getClassName(String packageName) {
        return PackageUtil.getClassName(PackageUtil.class.getClassLoader(), packageName, true);
    }

    public static List<String> getClassName(ClassLoader loader, String packageName) {
        return PackageUtil.getClassName(loader, packageName, true);
    }

    public static List<String> getClassName(ClassLoader loader, String packageName, boolean recur) {
        List<String> classNames = null;
        String packagePath = packageName.replace(".", "/");
        URL url = loader.getResource(packagePath);
        if (url != null) {
            String type = url.getProtocol();
            if (type.equals("file")) {
                String classPath = loader.getResource("./").getPath();
                classNames = PackageUtil.getClassNameByFile(classPath, url.getPath(), recur);
                System.out.println("asdadasdas" + classPath);
            } else if (type.equals("jar")) {
                System.out.println("31232432424");
                classNames = PackageUtil.getClassNameByJar(url.getPath(), recur);
            }
        } else {
            classNames = PackageUtil.getClassNameByJars(((URLClassLoader)loader).getURLs(), packagePath, recur);
        }
        return classNames;
    }

    private static List<String> getClassNameByFile(String classPath, String filePath, boolean recur) {
        File[] childFiles;
        ArrayList<String> classNames = new ArrayList<String>();
        File file = new File(filePath);
        for (File childFile : childFiles = file.listFiles()) {
            if (childFile.isDirectory()) {
                if (!recur) continue;
                classNames.addAll(PackageUtil.getClassNameByFile(classPath, childFile.getPath(), recur));
                continue;
            }
            String childFilePath = childFile.toURI().getPath();
            if (!childFilePath.endsWith(".class")) continue;
            childFilePath = childFilePath.substring(classPath.length(), childFilePath.lastIndexOf("."));
            childFilePath = childFilePath.replace("/", ".");
            classNames.add(childFilePath);
        }
        return classNames;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static List<String> getClassNameByJar(String jarPath, boolean recur) {
        ArrayList<String> myClassName = new ArrayList<String>();
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
                if (!entryName.endsWith(".class")) continue;
                if (recur) {
                    if (!entryName.startsWith(packagePath)) continue;
                    entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                    myClassName.add(entryName);
                    continue;
                }
                int index = entryName.lastIndexOf("/");
                String myPackagePath = index != -1 ? entryName.substring(0, index) : entryName;
                if (!myPackagePath.equals(packagePath)) continue;
                entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                myClassName.add(entryName);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return myClassName;
    }

    private static List<String> getClassNameByJars(URL[] urls, String packagePath, boolean recur) {
        ArrayList<String> myClassName = new ArrayList<String>();
        if (urls != null) {
            for (int i = 0; i < urls.length; ++i) {
                URL url = urls[i];
                String urlPath = url.getPath();
                if (urlPath.endsWith("classes/")) continue;
                String jarPath = urlPath + "!/" + packagePath;
                myClassName.addAll(PackageUtil.getClassNameByJar(jarPath, recur));
            }
        }
        return myClassName;
    }
}

