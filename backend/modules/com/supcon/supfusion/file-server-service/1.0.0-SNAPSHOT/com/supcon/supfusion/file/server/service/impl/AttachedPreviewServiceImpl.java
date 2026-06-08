package com.supcon.supfusion.file.server.service.impl;

import com.aspose.cells.Workbook;
import com.aspose.slides.Presentation;
import com.aspose.words.*;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.utils.AsposeLicenseUtil;
import com.supcon.supfusion.file.server.common.utils.EncodingDetect;
import com.supcon.supfusion.file.server.common.utils.SystemUtils;
import com.supcon.supfusion.file.server.service.AttachedPreviewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * @ClassName 附件预览，Aspose处理文件
 * @Description TODO
 * @Date 2019/11/21
 **/
@Service
@Slf4j
public class AttachedPreviewServiceImpl implements AttachedPreviewService, InitializingBean {
	@Value("${bap.home:}")
	private String BAP_HOME;
	private String TEMPPATH = "/bap-workspace/static/baseService/aspose/bapViewTemp.xml";

	@Override
	public Boolean doc2Pdf(InputStream inputStream, OutputStream outputStream) {
		try {
			if (SystemUtils.getOS().equals("LINUX")) {
				//这是重点
				FontSettings.getDefaultInstance().setFontsFolder("/usr/share/fonts/ttf-dejavu", true);
			}
			AsposeLicenseUtil.setWordsLicense();
			Document doc = new Document(inputStream);
			PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
			pdfSaveOptions.setSaveFormat(SaveFormat.PDF);
			pdfSaveOptions.getOutlineOptions().setHeadingsOutlineLevels(3); // 设置3级doc书签需要保存到pdf的heading中
			pdfSaveOptions.getOutlineOptions().setExpandedOutlineLevels(1); // 设置pdf中默认展开1级
			doc.save(outputStream, pdfSaveOptions);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public Boolean doc2Pdf2(String path, OutputStream outputStream) {
		try {
			AsposeLicenseUtil.setWordsLicense();
            String tempFile = System.getProperty(Constants.USER_DIR) + TEMPPATH;
			saveAsUTF8(path, tempFile);
			Document doc = new Document(tempFile);
			PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();
			pdfSaveOptions.setSaveFormat(SaveFormat.PDF);
			pdfSaveOptions.getOutlineOptions().setHeadingsOutlineLevels(3); // 设置3级doc书签需要保存到pdf的heading中
			pdfSaveOptions.getOutlineOptions().setExpandedOutlineLevels(1); // 设置pdf中默认展开1级
			doc.save(outputStream, pdfSaveOptions);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public Boolean doc2Html(InputStream inputStream, String savePath) {
		try {
			AsposeLicenseUtil.setWordsLicense();
			Document doc = new Document(inputStream);
			HtmlSaveOptions htmlSaveOptions = new HtmlSaveOptions();
			htmlSaveOptions.setExportImagesAsBase64(true);
			doc.save(savePath, htmlSaveOptions);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean doc2Html2(String path, String savePath) {
		try {
			AsposeLicenseUtil.setWordsLicense();

			String tempFilepath = savePath.substring(0, savePath.lastIndexOf("\\") + 1);
			saveAsUTF8(path, tempFilepath + "bapViewTemp.xml");
			Document doc = new Document(tempFilepath + "bapViewTemp.xml");
			HtmlSaveOptions htmlSaveOptions = new HtmlSaveOptions();
			htmlSaveOptions.setExportImagesAsBase64(true);
			doc.save(savePath, htmlSaveOptions);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean excel2Pdf(String srcPath, String savePath) {
		try {
			if (SystemUtils.getOS().equals("LINUX")) {
				//这是重点
				FontSettings.getDefaultInstance().setFontsFolder("/usr/share/fonts/ttf-dejavu", true);
			}
			AsposeLicenseUtil.setCellsLicense();
			Workbook workbook = new Workbook(srcPath);
			com.aspose.cells.PdfSaveOptions pdfSaveOptions = new com.aspose.cells.PdfSaveOptions();
			pdfSaveOptions.setOnePagePerSheet(true);
			workbook.save(savePath, pdfSaveOptions);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean excel2Html(String srcPath, String savePath) {
		try {
			AsposeLicenseUtil.setCellsLicense();
			Workbook workbook = new Workbook(srcPath);
			com.aspose.cells.HtmlSaveOptions htmlSaveOptions = new com.aspose.cells.HtmlSaveOptions();
			htmlSaveOptions.setExportImagesAsBase64(true);
			workbook.save(savePath, htmlSaveOptions);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean ppt2Pdf(String srcPath, OutputStream outputStream) {
		try {
			AsposeLicenseUtil.setSlidesLicense();
			long start = System.currentTimeMillis();
			Presentation presentation = new Presentation(srcPath);
			presentation.save(outputStream, com.aspose.slides.SaveFormat.Pdf);
			long end = System.currentTimeMillis();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean ppt2Html(String srcPath, String savePath) {
		try {
			AsposeLicenseUtil.setSlidesLicense();
			Presentation presentation = new Presentation(srcPath);
			presentation.save(savePath, com.aspose.slides.SaveFormat.Html);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Boolean image2Pdf(String srcPath, String savePath) {
//        Image image = null;
//        try {
//            image =  Image.load(srcPath);
//            PdfOptions exportOptions = new PdfOptions();
//            if (srcPath.lastIndexOf(".wmf") > -1) {
//                EmfRasterizationOptions emfRasterizationOptions = new EmfRasterizationOptions();
//                emfRasterizationOptions.setBackgroundColor(Color.getWhiteSmoke());
//                emfRasterizationOptions.setPageWidth(image.getWidth());
//                emfRasterizationOptions.setPageHeight(image.getHeight());
//                exportOptions.setVectorRasterizationOptions(emfRasterizationOptions);
//            } else {
//                exportOptions.setPdfDocumentInfo(new PdfDocumentInfo());
//            }
//            image.save(savePath, exportOptions);
//            return true;
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return false;
//        } finally {
//            if (image != null) {
//                image.close();
//            }
//        }
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
//		deleteTempFile();
	}

	public void deleteTempFile() {
		try {
			File file = new File(BAP_HOME + "/bap-workspace/static/baseService/aspose/tempFile");
			if (!file.exists()) {
				return;
			}
			FileUtils.deleteDirectory(file);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void saveAsUTF8(String inputFileUrl, String outputFileUrl) throws IOException {
	    //校验aspose文件夹是否存在
	    File tempFileDir = new File(outputFileUrl.substring(0,outputFileUrl.lastIndexOf("/")));
		if(!tempFileDir.exists()){
			tempFileDir.mkdirs();
		}
		String inputFileEncode = EncodingDetect.getJavaEncode(inputFileUrl);
		log.info("inputFileEncode===" + inputFileEncode);
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(inputFileUrl), inputFileEncode));
		BufferedWriter bufferedWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputFileUrl), "UTF-8"));
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			bufferedWriter.write(line + "\r\n");
		}
		bufferedWriter.close();
		bufferedReader.close();
		String outputFileEncode = EncodingDetect.getJavaEncode(outputFileUrl);
		log.info("outputFileEncode===" + outputFileEncode);
		log.info("txt文件格式转换完成");
	}
}

