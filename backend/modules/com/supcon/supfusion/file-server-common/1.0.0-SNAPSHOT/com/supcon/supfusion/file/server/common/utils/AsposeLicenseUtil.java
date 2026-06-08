package com.supcon.supfusion.file.server.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * @ClassName AsposeLicenseUtil
 * @Description Aspose的授权认证类
 * @Date 2020/11/09
 **/

@Slf4j
public class AsposeLicenseUtil {

	private static ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
	/**
	 * 获取License的输入流
	 *
	 * @return
	 */
	private static InputStream getLicenseInput() {
		InputStream inputStream = null;
		try {
			//inputStream = new FileInputStream(contextClassLoader.getResource("license.xml").getPath());
			inputStream = contextClassLoader.getResourceAsStream("license.xml");
		} catch (Exception e) {
			log.error("license not found!", e);
		}
		return inputStream;
	}

	private static InputStream getDocLicenseInput() {
		InputStream inputStream = null;
		try {
			//inputStream = new FileInputStream(contextClassLoader.getResource("license.xml").getPath());
			inputStream = contextClassLoader.getResourceAsStream("license.xml");
		} catch (Exception e) {
			log.error("license not found!", e);
		}
		return inputStream;
	}

	private static InputStream getExcelLicenseInput() {
		InputStream inputStream = null;
		try {
//            inputStream = new FileInputStream(contextClassLoader.getResource("excelLicense.xml").getPath());
			inputStream = contextClassLoader.getResourceAsStream("license.xml");
		} catch (Exception e) {
			log.error("license not found!", e);
		}
		return inputStream;
	}

	private static InputStream getPPTLicenseInput() {
		InputStream inputStream = null;
		try {
//            inputStream = new FileInputStream(contextClassLoader.getResource("pptLicense.xml").getPath());
			inputStream = contextClassLoader.getResourceAsStream("license.xml");
		} catch (Exception e) {
			log.error("license not found!", e);
		}
		return inputStream;
	}

	/**
	 * 设置License
	 *
	 * @return true表示已成功设置License, false表示失败
	 */
	public static boolean setWordsLicense() {
		InputStream licenseInput = getLicenseInput();
		if (licenseInput != null) {
			try {
				com.aspose.words.License aposeLic = new com.aspose.words.License();
				aposeLic.setLicense(licenseInput);
				return true;//aposeLic.getIsLicensed();
			} catch (Exception e) {
				log.error("set words license error!", e);
			} finally {
				try {
					licenseInput.close();
				} catch (Exception e) {

				}
			}
		}
		return false;
	}

	/**
	 * 设置License
	 *
	 * @return true表示已成功设置License, false表示失败
	 */
	public static boolean setCellsLicense() {
		InputStream licenseInput = getLicenseInput();
		if (licenseInput != null) {
			try {
				com.aspose.cells.License aposeLic = new com.aspose.cells.License();
				aposeLic.setLicense(licenseInput);
				return true;
			} catch (Exception e) {
				log.error("set cells license error!", e);
			} finally {
				try {
					licenseInput.close();
				} catch (Exception e) {

				}
			}
		}
		return false;
	}

	/**
	 * 设置License
	 *
	 * @return true表示已成功设置License, false表示失败
	 */
	public static boolean setSlidesLicense() {
		InputStream licenseInput = getLicenseInput();
		if (licenseInput != null) {
			try {
				com.aspose.slides.License aposeLic = new com.aspose.slides.License();
				aposeLic.setLicense(licenseInput);
				return aposeLic.isLicensed();
			} catch (Exception e) {
				log.error("set ppt license error!", e);
			} finally {
				try {
					licenseInput.close();
				} catch (Exception e) {

				}
			}
		}
		return false;
	}

	/**
	 * 设置Aspose PDF的license
	 * @return true表示设置成功，false表示设置失败
	 */
	public static boolean setPdfLicense() {
		InputStream licenseInput = getLicenseInput();
		if (licenseInput != null) {
			try {
				com.aspose.pdf.License aposeLic = new com.aspose.pdf.License();
				aposeLic.setLicense(licenseInput);
				return true;
			} catch (Exception e) {
				log.error("set pdf license error!", e);
			}
		}
		return false;
	}
}

