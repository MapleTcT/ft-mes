package com.supcon.supfusion.file.server.service;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @ClassName 附件预览——aspose文件转换接口
 * @Description TODO
 * @Author jiangshiyi
 * @Date 2019/11/21
 **/
public interface AttachedPreviewService {
	public Boolean doc2Pdf(InputStream inputStream, OutputStream outputStream);
	public Boolean doc2Pdf2(String path, OutputStream outputStream);
	public Boolean doc2Html(InputStream inputStream, String savePath);
	public Boolean doc2Html2(String path, String savePath);
	public Boolean excel2Pdf(String srcPath, String savePath);
	public Boolean excel2Html(String srcPath, String savePath);
	public Boolean ppt2Pdf(String srcPath, OutputStream outputStream);
	public Boolean ppt2Html(String srcPath, String savePath);
	public Boolean image2Pdf(String srcPath, String savePath);
}
