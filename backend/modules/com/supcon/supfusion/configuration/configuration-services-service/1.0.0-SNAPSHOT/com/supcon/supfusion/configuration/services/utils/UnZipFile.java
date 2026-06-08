package com.supcon.supfusion.configuration.services.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 解压ZIP压缩文件到指定的目录
 * 
 * @title: UnZipFile
 * @description:
 * @author wangxw
 * @since 2012-4-11下午01:55:22
 * @version 1.0
 */
public final class UnZipFile {

	private static final Logger logger = LoggerFactory.getLogger(UnZipFile.class);
	/**
	 * 
	 * 缓存区大小默认20480
	 * 
	 */
	private final static int FILE_BUFFER_SIZE = 20480;

	public static boolean unzip(File src, File dest) {
		boolean flag = false;
		// 1.判断压缩文件是否存在，以及里面的内容是否为空
		File file = src; // 压缩文件(带路径)
		ZipFile zipFile = null;
		String targetFileDir = dest.getAbsolutePath();
		if (false == file.exists()) {
			//EcUtils.uploadLogger.info(">>>>>>压缩文件【" + src.getAbsolutePath() + "】不存在<<<<<<");
			logger.info(">>>>>>压缩文件【" + src.getAbsolutePath() + "】不存在<<<<<<");
			return false;
		} else {
			// 2.开始解压ZIP压缩文件的处理
			//EcUtils.uploadLogger.info(">>>>>>解压文件【" + src.getAbsolutePath() + "】到【" + dest.getAbsolutePath() + "】目录下<<<<<<");
			logger.info(">>>>>>解压文件【" + src.getAbsolutePath() + "】到【" + dest.getAbsolutePath() + "】目录下<<<<<<");
			byte[] buf = new byte[FILE_BUFFER_SIZE];
			int readSize = -1;
			ZipInputStream zis = null;
			FileOutputStream fos = null;
			try {
				// 检查是否是zip文件
				zipFile = new ZipFile(file);
				zipFile.close();
				// 判断目标目录是否存在，不存在则创建
				File newdir = new File(targetFileDir);
				if (false == newdir.exists()) {
					newdir.mkdirs();
					newdir = null;
				}
				String fileEncode = EncodeUtil.getEncode(file.getPath(),true);
				//ZipFile zipFile = new ZipFile(new File(filePath), Charset.forName(fileEncode));
				zis = new ZipInputStream(new FileInputStream(file), Charset.forName(fileEncode));
				ZipEntry zipEntry = getNextEntry(zis);
				if(zipEntry == null){
					throw new RuntimeException("The uploaded file is not normal");
				}
				// 开始对压缩包内文件进行处理
				while (null != zipEntry) {
					String zipEntryName = zipEntry.getName().replace('\\', '/');
					// 判断zipEntry是否为目录，如果是，则创建
					if (zipEntry.isDirectory()) {
						int indexNumber = zipEntryName.lastIndexOf('/');
						File entryDirs = new File(targetFileDir + "/" + zipEntryName.substring(0, indexNumber));
						entryDirs.mkdirs();
						entryDirs = null;
					} else {
						try {
							File f = new File(targetFileDir + "/" + zipEntryName);
							File dir = f.getParentFile();
							if(!dir.exists()) {
								if(!dir.mkdirs()) {
									throw new RuntimeException("create directory:" + dir.getAbsolutePath() + " failed");
								}
							}
							fos = new FileOutputStream(targetFileDir + "/" + zipEntryName);
							while ((readSize = zis.read(buf, 0, FILE_BUFFER_SIZE)) != -1) {
								fos.write(buf, 0, readSize);
							}
							//EcUtils.uploadLogger.info("=============解压复制文件:" + zipEntryName);
							// 没有意义的日志
//							logger.info("=============解压复制文件:" + zipEntryName);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							throw new RuntimeException(e.getCause());
						} finally {
							try {
								if (null != fos) {
									fos.close();
								}
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
								throw new RuntimeException(e.getCause());
							}
						}
					}
					zipEntry = getNextEntry(zis);
				}
				flag = true;
			} catch (ZipException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getCause());
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getCause());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getCause());
			} finally {
				try {
					if (null != zis) {
						zis.close();
					}
					if (null != fos) {
						fos.close();
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e.getCause());
				}
			}
		}
		return flag;
	}

	/**
	 * 
	 * 将指定目录的ZIP压缩文件解压到指定的目录
	 * 
	 * @param zipFilePath
	 *            ZIP压缩文件的路径
	 * @param zipFileName
	 *            ZIP压缩文件名字
	 * @param targetFileDir
	 *            ZIP压缩文件要解压到的目录
	 * @return flag 布尔返回值
	 * 
	 */
	public static boolean unzip(String zipFilePath, String zipFileName, String targetFileDir) {
		return unzip(new File(zipFilePath + File.separatorChar + zipFileName), new File(targetFileDir));
	}

	private static ZipEntry getNextEntry(ZipInputStream zis) {
		ZipEntry zipEntry = null;
		try {
			zipEntry = zis.getNextEntry();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			return getNextEntry(zis);
		}
		return zipEntry;
	}

	/**
	 * 
	 * 测试用的Main方法
	 * 
	 */
	public static void main(String[] args) {
		String zipFilePath = "e:\\test";
		String zipFileName = "LIMSBasic_8.20.3.01.zip";
		String targetFileDir = "e:\\test";
		boolean flag = UnZipFile.unzip(zipFilePath, zipFileName, targetFileDir);
		if (flag) {
			logger.info(">>>>>>解压成功<<<<<<");
		} else {
			logger.info(">>>>>>解压失败<<<<<<");
		}
	}
}