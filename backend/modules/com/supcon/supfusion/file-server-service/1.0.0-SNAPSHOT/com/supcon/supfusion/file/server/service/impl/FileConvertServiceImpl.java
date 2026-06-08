package com.supcon.supfusion.file.server.service.impl;

import com.supcon.supfusion.file.server.common.ConvertStatus;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.utils.*;
import com.supcon.supfusion.file.server.dao.DocumentDao;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.AttachedPreviewService;
import com.supcon.supfusion.file.server.service.FileConvertService;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Slf4j
@Service("fileConvertService")
public class FileConvertServiceImpl implements FileConvertService, InitializingBean {

    private LinkedBlockingQueue<DocumentPO> linkedBlockingQueue = new LinkedBlockingQueue<DocumentPO>(200);
    ExecutorService threadPoolExecutor1 = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(200));
    ExecutorService workExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(200));
    private static Map<String, String> FILE_TYPE_IMAGE = BaseUtil.FILE_TYPE_IMAGE;
    private static final Long excelSize = 10485760L;
    private static final Long FILE_MAX_SIZE = 104857600L;
    private static final Long videoSize = 1073741824L;

    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private FileService fileService;
    @Autowired
    private AttachedPreviewService attachedPreviewService;


    @Override
    public void fileConvert(DocumentPO documentPO) {
        documentPO = documentDao.selectByFileId(documentPO.getId());
        log.info("------附件转换开始------");
        //判断附件转换状态
        if(!checkDocStatus(documentPO)){
            log.info("当前文件无需再次转换,{}",documentPO.getFileName());
            return;
        }
        // 校验文件类型和大小
        Map<String, Object> result = checkDoc(documentPO);
        if (!(Boolean) result.get("flag")) {
            // 不支持转换 保存转换状态及原因
            log.info((String) result.get("noSupportReason"));
            documentPO.setConvertStatus(ConvertStatus.UNSUPPORTED.toString());
            documentPO.setReason((String) result.get("noSupportReason"));
            documentDao.updatedConvertReasonByOneId(documentPO);
            return;
        }
        //修改为正在转换状态
        documentPO.setConvertStatus(ConvertStatus.CONVERTING.toString());
        documentDao.updatedConvertReasonByOneId(documentPO);
        log.info("加入任务转换文件：" + documentPO.getFileName());

        documentPO.setTenantId(RpcContext.getContext().getTenantId());
        linkedBlockingQueue.add(documentPO);
    }


    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @PostConstruct
    public void init() {
        try {
            //构建一个线程用于处理附件转换，长期存活
            threadPoolExecutor1.execute(new Runnable() {
                @Override
                public void run() {
                    while (!threadPoolExecutor1.isShutdown()) {
                        try {
                            batchConvert();
                        } catch (Exception e) {
                            log.error("文件转换错误：" + e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void batchConvert() throws Exception {
        DocumentPO task = linkedBlockingQueue.take();
        log.info("开始转换文件：" + task.getFileName());
        if (!workExecutor.isShutdown()) {
            Future result = workExecutor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    convert(task);
                    return "success";
                }
            });
            try {
                Object callResult = result.get(20, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                log.error("执行超时，超时时间为20分钟。文件：{}", task);
                result.cancel(true);
            } catch (Exception e) {
                log.error("转换错误：{}", e.getMessage());
            }
        }
    }


    /***
     * 附件转换实际处理逻辑
     * @param documentPO
     */
    public void convert(DocumentPO documentPO) throws IOException {
        String originTenantId = documentPO.getTenantId();
        RpcContext.getContext().setTenantId(originTenantId);
        String tenantId = TenantUtil.getTenantId();
        log.info("转换附件中tenantId:{}", tenantId);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File outFile = null;
        String fileRealPath = "";
        String tempConvertDir = "";
        Boolean convertFlag = false;
        File tempConvertFile = null;
        try {
            // 校验文件类型和大小
            Map<String, Object> result = checkDoc(documentPO);
            if (!(Boolean) result.get("flag")) {
                // 不支持转换 保存转换状态及原因
                log.info((String) result.get("noSupportReason"));
                documentPO.setConvertStatus(ConvertStatus.UNSUPPORTED.toString());
                documentPO.setReason((String) result.get("noSupportReason"));
                documentDao.updatedConvertReasonByOneId(documentPO);
                return;
            }
            String docuName = (String) result.get("docuName");
            String fileType = (String) result.get("fileType");
            //从minio服务器下载文件到本地之后再转换
            String filePath = documentPO.getFilePath();
            String fileName = documentPO.getFileName();

            InputStream input = null;
            try {
                input = fileService.downLoad(tenantId, filePath);
            } catch (Exception e) {
                log.error("从minio服务器获取文件发生错误,filePath:{}", filePath, e);
            }

            //临时原始文件
            String tempDir = PathUtil.getTempPath() + "task/" + System.currentTimeMillis() + Constants.PATH;
            fileRealPath = tempDir + fileName;
            FileUtil.createDir(tempDir);
            FileUtil.createNewFile(fileRealPath);
            FileUtil.copyFileUsingFileStreams(input, new File(fileRealPath));
            input.close();

            String videoAndHtmlConvertPath = PathUtil.getStaticFilePath() + Constants.PATH + tenantId + documentPO.getFilePath().substring(0, documentPO.getFilePath().lastIndexOf("/")) + "/convert";
            if (/*"excel".equals(FILE_TYPE_IMAGE.get(fileType)) ||*/ "video".equals(FILE_TYPE_IMAGE.get(fileType))) {
                FileUtil.createDir(videoAndHtmlConvertPath);
            }

            // 转换文件存放路径
            tempConvertDir = PathUtil.getConvertTempPath();
            FileUtil.createDir(tempConvertDir);

            //指定转换的格式
            fileName = docuName + ".pdf";
            if ("image".equals(FILE_TYPE_IMAGE.get(fileType))) {
                fileName = docuName + "." + fileType;
            } else if ("excel".equals(FILE_TYPE_IMAGE.get(fileType))) {
//                fileName = docuName + ".html";
                fileName = docuName + ".pdf";
            } else if ("video".equals(FILE_TYPE_IMAGE.get(fileType))) {
                fileName = docuName + ".m3u8";
            }

            //附件转换
            //doc和ppt转为流对象
            if ("word".equals(FILE_TYPE_IMAGE.get(fileType)) || "word2".equals(FILE_TYPE_IMAGE.get(fileType)) || "ppt".equals(FILE_TYPE_IMAGE.get(fileType))) {
                outFile = new File(tempConvertDir + fileName);
                if (!outFile.exists()) {
                    outFile.createNewFile();
                }
                outputStream = new FileOutputStream(outFile);
            }
            switch (FILE_TYPE_IMAGE.get(fileType)) {
                case "image":
                case "pdf":
                    FileUtils.copyFile(new File(fileRealPath), new File(tempConvertDir + fileName));
                    convertFlag = true;
                    break;
                case "word":
                    inputStream = new FileInputStream(fileRealPath);
                    convertFlag = attachedPreviewService.doc2Pdf(inputStream, outputStream);
                    break;
                case "word2":
                    convertFlag = attachedPreviewService.doc2Pdf2(fileRealPath, outputStream);
                    break;
                case "excel":
//                    convertFlag = attachedPreviewService.excel2Html(fileRealPath, videoAndHtmlConvertPath + fileName);
                    convertFlag = attachedPreviewService.excel2Pdf(fileRealPath, tempConvertDir + fileName);
                    break;
                case "ppt":
                    convertFlag = attachedPreviewService.ppt2Pdf(fileRealPath, outputStream);
                    break;
                case "video":
//                    convertFlag = ConvertVideos(fileRealPath, tempConvertDir, docuName);
                    convertFlag = ConvertVideos(fileRealPath, videoAndHtmlConvertPath, docuName);
                    break;
                default:
                    break;
            }
            // 保存转换结果
            if (convertFlag) {
                log.info(docuName + "文件转换成功");
                documentPO.setConvertStatus(ConvertStatus.SUCCESS.toString());
            } else {
                log.info(docuName + "文件转换失败");
                documentPO.setConvertStatus(ConvertStatus.FAILURE.toString());
            }

            String convertFileMinIoPath = documentPO.getFilePath().substring(0, documentPO.getFilePath().lastIndexOf("/")) + "/convert/" + fileName;
            tempConvertFile = new File(tempConvertDir + fileName);
            if (!ObjectUtils.isEmpty(tempConvertFile)) {
                //将转换后的文件存放至minio
                if (/*!"excel".equals(FILE_TYPE_IMAGE.get(fileType)) &&*/ !"video".equals(FILE_TYPE_IMAGE.get(fileType))) {
                    fileService.upLoadStreamForConvert(tenantId, convertFileMinIoPath, new FileInputStream(tempConvertDir + fileName), fileType);
                }
            }
            //将文件转换结果，转换后路径存数据库
            documentPO.setConvertPath(convertFileMinIoPath);
            documentDao.updatedConvertReasonByOneId(documentPO);
        } catch (Exception e) {
            documentPO.setConvertStatus(ConvertStatus.FAILURE.toString());
            documentDao.updatedConvertReasonByOneId(documentPO);
            log.error(e.getMessage(), e);
        } finally {
            if (null != inputStream) {
                inputStream.close();
            }
            if (null != outputStream) {
                outputStream.close();
            }
            //删除临时原始文件，删除临时转换后的文件
            File file1 = new File(fileRealPath);
            if (!file1.delete()) {
                log.error("删除临时原始文件发生错误");
            }
            if (!ObjectUtils.isEmpty(tempConvertFile) && !tempConvertFile.delete()) {
                log.error("删除临时转换后的文件发生错误");
            }
        }
    }

    /***
     * @Description 校验文件是否可转换
     * @Date 2020/2/11
     * @Param [document, docClass]
     * @return java.util.Map
     **/
    public Map<String, Object> checkDoc(DocumentPO document) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            String docuName = document.getFileName();
            Long docuSize = document.getFileSize();
            String fileType = "";
            fileType = docuName.substring(docuName.lastIndexOf(".") + 1).toLowerCase();
            result.put("fileType", fileType);
            result.put("docuName", docuName.substring(0, docuName.lastIndexOf(".")));
            // 不支持的文件类型
            if (!FILE_TYPE_IMAGE.containsKey(fileType)) {
                result.put("flag", false);
                result.put("noSupportReason", "不支持的文件类型：" + fileType);
                return result;
            }
            // 文件大于100M不支持预览防止aspose服务压力过大
            if (docuSize > FILE_MAX_SIZE && !checkVideo(fileType)) {
                result.put("flag", false);
                result.put("noSupportReason", "预览文件大于100M，不支持预览");
                return result;
            }
            // excel文件大于10M抛出错误信息
            if ("xls".equals(fileType) || "xlsx".equals(fileType)) {
                if (docuSize > excelSize) {
                    result.put("flag", false);
                    result.put("noSupportReason", "excel文件大于10M，不支持预览");
                    return result;
                }
            }
            //视频文件大小限制为1G
            if (checkVideo(fileType) && docuSize > videoSize) {
                result.put("flag", false);
                result.put("noSupportReason", "视频文件大于1G，不支持预览");
                return result;
            }
//            //不支持视频文件转换
//            if (checkVideo(fileType)) {
//                result.put("flag", false);
//                result.put("noSupportReason", "视频文件，不支持预览");
//                return result;
//            }
//            //当前文件为空
//            if (0 == document.getFileSize()) {
//                result.put("flag", false);
//                result.put("noSupportReason", "当前文件无内容，不支持预览");
//                return result;
//            }
        } catch (Exception e) {
            result.put("flag", false);
            log.error(e.getMessage(), e);
            return result;
        }
        result.put("flag", true);
        return result;
    }

    /***
     * 判断文件类型是否为视频文件
     * @param fileType
     * @return
     */
    private Boolean checkVideo(String fileType) {
        String tempStr = StringUtils.lowerCase(fileType);
        String type = FILE_TYPE_IMAGE.get(tempStr);
        if (type != null) {
            if ("video".equals(type)) {
                return true;
            }
        }
        return false;
    }

    /***
     * @Description 视频转换
     * @Date 2020/11/09
     * @Param [path, savePath, strFileName]
     * @return java.lang.Boolean
     **/
    public Boolean ConvertVideos(String path, String savePath, String strFileName) {
        try {
            String hlsListPhyPath = String.format("%s%s%s%s", savePath, File.separator, strFileName, ".m3u8");//切片播放列表文件路径
            String hlsCutPhyPath = String.format("%s%s%s%s", savePath, File.separator, strFileName, "%03d.ts");//切片命名风格及地址
            String tempCutPhyPath = String.format("%s%s%s%s", savePath, File.separator, strFileName, "output.ts");//切片输出地址
            String picPath = String.format("%s%s%s%s", savePath, File.separator, strFileName, ".jpg");//封面图片地址
            //判断当前环境是什么环境
            log.info("===========操作系统是:" + System.getProperties().getProperty("os.name"));
            log.info("===========文件的分隔符为file.separator:" + System.getProperties().getProperty("file.separator"));
            Boolean flag = ConvertVideoToHls(path, hlsListPhyPath, hlsCutPhyPath, picPath, tempCutPhyPath);
            return flag;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    /**
     * 视频格式转换
     *
     * @param filePath       视频物理路径
     * @param hlsListPhyPath 列表存储路径
     * @param hlsCutPhyPath  切片风格路径
     * @param picPhyPath     封面截图路径
     * @return
     */
    public Boolean ConvertVideoToHls(String filePath, String hlsListPhyPath, String hlsCutPhyPath, String picPhyPath, String tempCutPhyPath) {
        String ffmpegDirPath = PathUtil.getProjectPath() + File.separator + "ffmpeg" + File.separator;
        FileUtil.createDir(ffmpegDirPath);
        File ffmpegFile = new File(ffmpegDirPath + "ffmpeg.exe");
        if (ffmpegFile == null) {
            //找到项目根路径下面的ffmpeg.exe 如果没有 从自己jar 包中直接拷贝到项目根路径使用
            String resourcesPath = File.separator + "ffmpeg" + File.separator + "ffmpeg.exe";
//            URL url = PathUtil.locateUrl(resourcesPath);
//            PathUtil.copyFfepegFile(url, ffmpegDirPath + "ffmpeg.exe");
        }
        String cmdExe = ffmpegDirPath + "ffmpeg.exe";
        List<String> convert = new ArrayList<String>();
        /*1. ffmpeg -re -i filePath -c:v libx264 -c:a aac -strict -2 -f hls -hls_list_size 0 -hls_time 8 hlsListPhyPath  -ss 20 -y -i filePath -f image2 -vframes 1 picPhyPath*/
        /*2. ffmpeg -y -i filePath -vcodec copy -acodec copy -vbsf h264_mp4toannexb tempCutPhyPath
         *    ffmpeg -i tempCutPhyPath  -c copy -map 0 -f segment -segment_list hlsListPhyPath -segment_time 10 hlsCutPhyPath -ss 20 -y -i filePath -f image2 -vframes 1 picPhyPath
         * */
        List<String> convertTemp = new ArrayList<String>();
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            convert.add("ffmpeg");
        } else {
            convert.add(cmdExe); // 添加转换工具路径
        }
        convert.add("-y"); // 该参数指定将覆盖已存在的文件
        convert.add("-i");
        convert.add(filePath);
        convert.add("-vcodec");
        convert.add("copy");
        convert.add("-acodec");
        convert.add("copy");
        convert.add("-vbsf");
        convert.add("h264_mp4toannexb");
        convert.add(tempCutPhyPath);
        if (RunCmd(convert)) {
            if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
                convertTemp.add("ffmpeg");
            } else {
                convertTemp.add(cmdExe); // 添加转换工具路径
            }
            convertTemp.add("-i");
            convertTemp.add(tempCutPhyPath);
            convertTemp.add("-c");
            convertTemp.add("copy");
            convertTemp.add("-map");
            convertTemp.add("0");
            convertTemp.add("-f");
            convertTemp.add("segment");
            convertTemp.add("-segment_list");
            convertTemp.add(hlsListPhyPath);
            convertTemp.add("-segment_time");
            convertTemp.add("10");
            convertTemp.add(hlsCutPhyPath);
            convertTemp.add("-ss");
            convertTemp.add("20");
            convertTemp.add("-y");
            convertTemp.add("-i");
            convertTemp.add(filePath);
            convertTemp.add("-f");
            convertTemp.add("image2");
            convertTemp.add("-vframes");
            convertTemp.add("1");
            convertTemp.add(picPhyPath);
            return RunCmd(convertTemp);
        }
        return false;
    }

    /**
     * 带视频压缩指令执行exe文件
     *
     * @param convert
     * @return
     */
    public static Boolean RunCmd(List<String> convert) {
        boolean mark = true;
        Process videoProcess = null;
        try {
            videoProcess = new ProcessBuilder(convert).redirectErrorStream(true).start();
            //new PrintStreamUtil(videoProcess.getInputStream()).start();
            dealStream(videoProcess);
            // 加上这句，系统会等待转换完成。不加，就会在服务器后台自行转换。
            videoProcess.waitFor();
            videoProcess.destroy();

        } catch (Exception e) {
            mark = false;
            log.error(e.getMessage(), e);
        }
        return mark;
    }

    /**
     * 处理process输出流和错误流，防止进程阻塞
     * 在process.waitFor();前调用
     *
     * @param process
     */
    private static void dealStream(Process process) {
        if (process == null) {
            return;
        }
        // 处理InputStream的线程
        new Thread() {
            @Override
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                try {
                    while ((line = in.readLine()) != null) {
                        log.info("output: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    process.destroy();
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        // 处理ErrorStream的线程
        new Thread() {
            @Override
            public void run() {
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line = null;
                try {
                    while ((line = err.readLine()) != null) {
                        log.error("err: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
//					process.destroy();
                    try {
                        err.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /***
     * 文件转换状态，当前文件是否可转换
     * @param document
     * @return
     */
    private Boolean checkDocStatus(DocumentPO document){
        // 存入开始转换标记
        if (document == null) {
            return false;
        }
        // 正在转换、已成功、不支持的文件，无需转换
        if (/*ConvertStatus.CONVERTING.toString().equals(document.getConvertStatus())
                || */ConvertStatus.SUCCESS.toString().equals(document.getConvertStatus())
                || ConvertStatus.UNSUPPORTED.toString().equals(document.getConvertStatus())) {
            return false;
        }
        return true;
    }

}
