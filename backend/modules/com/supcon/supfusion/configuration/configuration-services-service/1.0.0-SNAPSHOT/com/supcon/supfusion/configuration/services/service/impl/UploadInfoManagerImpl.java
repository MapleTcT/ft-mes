/**
 * 
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.supcon.supfusion.configuration.services.entity.DeployInfo;
import com.supcon.supfusion.configuration.services.entity.DeploymentTask;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.entity.UploadInfoBatch;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.enums.EcEntityEnum;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author zhushizhang
 *	主要是用来规划上载的管理类
 */
@ServiceApiService
@Transactional(propagation= Propagation.SUPPORTS, readOnly=true)
public class UploadInfoManagerImpl implements UploadInfoManager {
	
	private static final Logger logger = LoggerFactory.getLogger(UploadInfoManagerImpl.class);
	
	private static final Object endWaitLock = new Object();
	
	private static CopyOnWriteArrayList<UploadInfo> currentTasks = new CopyOnWriteArrayList<UploadInfo>(); //开始添加任务时候的任务总量
	
	private static LinkedBlockingQueue<UploadInfo> currentTasks2 = new LinkedBlockingQueue<UploadInfo>(); //主要用的是这个任务
	
	private static Long taskProggressSize;
	
	private Long batchStartTime = 0L; // 记录批量发布任务开始时间
	
	private static int CURRENTCPUCORES;	//计算过后的当前环境CPU核心数
	
	private static UploadTaskProggress taskProggress = new UploadTaskProggress();

	private static LinkedBlockingQueue<UploadInfo> tasksQueue;
	
	private static AtomicInteger maxTryCount;
	
	private static AtomicInteger errorCount;
	
	private static List<String> errorinfos = new ArrayList<String>();

	@Autowired
	private ModuleService moduleService;
	
	private ExecutorService executor;
	
	private ExecutorService uploadExecutor;//上载用的线程池
	
	private ExecutorService endUploadExecutor;//上载结束用的线程池
	
	private ExecutorService getMetaInfoExecutor;//获取元数据用的线程池
	
	private ExecutorService dealFileExecutor;//解压文件的线程池
	
	private ExecutorService deleteFileExecutor;//删除已经解压的文件文件的线程池
	
	private UploadInfo task;
	@Autowired
	private UploadInfoBatchService uploadInfoBatchService;
	@Autowired
	private UploadInfoService uploadInfoService;

	private AtomicBoolean shutdown = new AtomicBoolean(false);

	private AtomicBoolean uploadFinish = new AtomicBoolean(false); //使用线程安全类，查看是否成功结束
	
	private AtomicBoolean exceptionFinish = new AtomicBoolean(false); //当上载发生报错的时候通过该字段去阻塞线程，知道数据库中返回的数据一致
	
	private AtomicBoolean exceptionUsed = new AtomicBoolean(false); //当上载发生报错的时候,已经有一个线程进入了该报错处理，其他线程就跳过
	
	private AtomicBoolean exceptionExit = new AtomicBoolean(false); //使用线程安全类，一旦多线程中发生报错，通过这个标志来显示webservice返回日志信息失败
	
	private AtomicBoolean persistTaskFinish = new AtomicBoolean(false); //最后插入信息结束赋值为TRUE

	private Map<Long,String> taskStatus = new ConcurrentHashMap<>();//用于记录每个task的最终状态
	
	private long startTime;//每次添加任务的时候会记录开始的时间，最后用来记录此次批量上载的总时间
	
	private static Semaphore semaphore;
	
	public static final ConcurrentMap<String, String> deployTaskState = new ConcurrentHashMap<String, String>();
	
	@Value("${bap.upload.threadNum:1}")
	private Integer threadNum;
	
	@Value("${bap.upload.errorCheck:false}")
	private Boolean errorCheck;

	@Resource(name = "sessionFactory")
	private SessionFactory ecSessionFactory;
	
	public static final Map<String, Map<String, Map<String, Object>>> metaInfoMap = new ConcurrentHashMap<String, Map<String, Map<String, Object>>>();
	@Value("${masterSlave.enabled:false}")
	private boolean masterSlaveEnabled = false;
	/**
	 * 默认超时时间10s
	 */
	public static final Integer TIME_OUT = 10000;
	/**
	 * 初始化
	 */
	@PostConstruct
	public void init() {
		this.tasksQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
		CURRENTCPUCORES = Runtime.getRuntime().availableProcessors();
		errorinfos.add("Row was updated or deleted");
		errorinfos.add("No row with the given identifier exists");
		maxTryCount = new AtomicInteger(0);
		errorCount = new AtomicInteger(0);
		if (CURRENTCPUCORES >8) {
			CURRENTCPUCORES = 6;
		} else if(CURRENTCPUCORES >= 4){
			CURRENTCPUCORES = CURRENTCPUCORES - 2;
		}
		executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("BatchThread-%s").build());
		uploadExecutor = Executors.newFixedThreadPool(3,new ThreadFactoryBuilder().setNameFormat("UploadBatchThread-%s").build());
		endUploadExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("endUploadThread-%s").build());
		deleteFileExecutor = Executors.newFixedThreadPool(1,new ThreadFactoryBuilder().setNameFormat("deleteFileExecutor-%s").build());
		getMetaInfoExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("metaInfoThread-%s").build());
		dealFileExecutor = Executors.newFixedThreadPool(1,new ThreadFactoryBuilder().setNameFormat("dealFileExecutor-%s").build());
		if(null != threadNum){
			semaphore = new Semaphore(threadNum,true);
		}else{
			semaphore = new Semaphore(1,true);
		}
		if(null != errorCheck){
			errorCheck = false;
		}else{
			errorCheck = true;
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					batchUpload();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
		endUploadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					waitTaskEnd();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}
	public String sendMessageToHealthMonitor(int tasksize, String type, boolean isStart){
		StringBuilder responseMessage = new StringBuilder();
		HttpURLConnection yc=null;
			try {
				String serverIp = InetAddress.getLocalHost().getHostAddress();
				String url = "http://"+serverIp+":9080/bap/publish?XType="+type+"&XTaskSize="+tasksize+"&XIsStart="+isStart;
				URL oracle = new URL(url);
				yc = (HttpURLConnection) oracle.openConnection();;
				yc.setDoOutput(true);
				yc.setReadTimeout(TIME_OUT);
				yc.setConnectTimeout(TIME_OUT);
				yc.connect();
				int responseCode = yc.getResponseCode();
				if (responseCode >= 200 && responseCode < 300) {
					InputStream is = yc.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String sCurrentLine = "";
					while ((sCurrentLine = reader.readLine()) != null) {
						responseMessage.append(sCurrentLine);
					}
				} else if (responseCode >= 300) {
					responseMessage.append("connect error");
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), "utf-8"));// 防止乱码
				String inputLine = null;
				while ((inputLine = in.readLine()) != null) {
//					json.append(inputLine);
					responseMessage.append(inputLine);
				}
				in.close();
			} catch (Exception e) {
//				json.append(e.getMessage());
				responseMessage.append("connect error");
			} finally {
				if(yc !=null)
					yc.disconnect();
			}
			return responseMessage.toString();
		
	}
	private void batchUpload() {
		while(!shutdown.get()){
			final String localLanguage = EcUtils.uploadTask.get("localLanguage");
			try {
				Thread.sleep(500);
				//获取到当前的上载任务{}(前)
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.getCurrentTaskBefore"),task !=null ? task.getModuleName() : "");
				semaphore.acquire();
				//上载开始标志
				task = tasksQueue.take();
				//获取到当前的上载任务{}(后)
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.getCurrentTaskAfter"),task !=null ? task.getModuleName() : "");
				MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
				//当前线程并发数{}-------是否启动了乐观锁校验{}
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.currentThread"),threadNum,errorCheck);
				//{}获取上载文件编号后{}
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.getUploadFile"),task.getModuleName(), EcUtils.uploadTask.get("uploadTask"));
				uploadExecutor.execute(new Runnable() {
					public void run() {
						UploadInfo taskTemp = task;
						final String moduleCode = taskTemp.getModuleCode();
						final String moduleName = taskTemp.getModuleName();
						//String threadName = Thread.currentThread().getName() + task.getModuleCode();
						Thread.currentThread().setName(taskTemp.getModuleCode());
						String messageThread = "";
						MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
						try{
							if(task.getIsFirstImport()){
								//{}模块开始上载,进入executeUploadBatch()方法{}
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.moduleStartsToUploadNoExist"),task.getModuleName(),task.getModuleCode());
								messageThread = moduleService.executeUploadBatch(taskTemp);
							}else{
								//{}模块开始上载,进入executeUploadBatchExist()方法{}
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.moduleStartsToUploadExist"),task.getModuleName(),task.getModuleCode());
								messageThread = moduleService.executeUploadBatchExist(taskTemp);
								/*errorCount.incrementAndGet();
								if(errorCount.get() == 2){
									throw new RuntimeException("zhushizhang");
								}*/
							}
							if(messageThread == null || messageThread !=null && !"success".equals(messageThread.trim())){
								boolean isNormalEx = false;
								if(errorCheck){
									for(String info : errorinfos){
										if(messageThread.indexOf(info)>-1){
											isNormalEx = true;
											tasksQueue.add(taskTemp);
											currentTasks2.add(taskTemp);
											currentTasks.add(taskTemp);
										}
									}
								}
								if(!isNormalEx){
									//上载模块不正常退出！
									messageThread = InternationalResource.get("ec.module.uploadBatch.moduleAbnormalExit");
									exceptionExit.set(true);
									EcUtils.uploadTask.put("uploadTaskState", "fail");
								}
							}
						}
						catch (Exception e) {
							if(null != e.getMessage()){
								messageThread = e.getMessage();
							}else{
								if(e instanceof EcException){
									messageThread = ((EcException)e).getMessageKey();
								}
							}
							boolean isNormalEx = false;
							if(errorCheck){
								for(String info : errorinfos){
									if(messageThread.indexOf(info)>-1){
										isNormalEx = true;
										tasksQueue.add(taskTemp);
										currentTasks2.add(taskTemp);
										currentTasks.add(taskTemp);
									}
								}
							}
							if(!isNormalEx){
								exceptionExit.set(true);
								EcUtils.uploadTask.put("uploadTaskState", "fail");
							}
						}finally{
							try {
								//{}currentTasks2---前，当前的任务长度为{}
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.currentTaskSizeBefore"),moduleName,currentTasks2.size());
								currentTasks2.poll(30, TimeUnit.SECONDS);
								//{}currentTasks2---后，当前的任务长度为{}
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.currentTaskSizeAfter"),moduleName,currentTasks2.size());
								//一旦发生错误，清空  tasksQueue 中的任务
								//{}当前线程exceptionExit的状态为{}
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.currentTaskExceptionExit"),moduleName,exceptionExit.get());
								if(exceptionExit.get()){
									//{}模块上载进入报错处理程序
									EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.uploadIntoErrorProgram"),taskTemp.getModuleName());
									//这里开始判断是否已经有线程进入
									if(!exceptionUsed.get()){
										//{}当前线程exceptionUsed的状态为{}
										EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.exceptionUsedState"),taskTemp.getModuleName(),exceptionUsed.get());
										exceptionUsed.set(true);
										EcUtils.uploadFullLogger.info("currentTasks.size():{}----currentTasks2.size(){}",currentTasks.size(),currentTasks2.size());
										int usedTaskSize = currentTasks.size() - currentTasks2.size();
										tasksQueue.clear();
										//{}当前线程exceptionFinish的状态为{}
										EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.exceptionFinishState"),taskTemp.getModuleName(),exceptionFinish.get());
										while(!exceptionFinish.get()){
											//{}开始每秒循环查询uploadinfo的表信息
											EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.StartLoopingUploadinfo"),taskTemp.getModuleName());
											Page<UploadInfo> uploadPage = new Page<UploadInfo>(100);
											EcUtils.uploadFullLogger.info("{}start querying uploadinfo",taskTemp.getModuleName());
											List<UploadInfo> uploadList = new ArrayList<UploadInfo>();
											try {
												uploadList = uploadInfoService.findUploadInfo(" and uploadInfo.uploadInfoBatch.id = "+((UploadInfoBatch) EcUtils.uploadTaskBatch.get("uploadTaskBatch")).getId());
											} catch (Exception e2) {
												//{}查询uploadinfo表的时候的报错{}
												EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.errorQueryingUploadinfo"),taskTemp.getModuleName(),e2.getMessage());
											}
											EcUtils.uploadFullLogger.info("{} the uploadList length is {}",taskTemp.getModuleName(),uploadList.size());
											EcUtils.uploadFullLogger.info("{} usedTaskSize length is {}",taskTemp.getModuleName(),usedTaskSize);
											if(uploadList.size() == usedTaskSize || maxTryCount.get() == 4){
												if(maxTryCount.get() == 4){
													EcUtils.uploadFullLogger.info("{} Exit the loop after the fourth attempt to report an error",taskTemp.getModuleName());
												}
												EcUtils.uploadFullLogger.info("{} match the number information to the last insert table and exit the loop",taskTemp.getModuleName());
												exceptionFinish.set(true);
												break;
											}
											Thread.sleep(5000);
											EcUtils.uploadFullLogger.info("{} Trial in the error loop {} second",taskTemp.getModuleName(),maxTryCount.get());
											maxTryCount.incrementAndGet();
										}
										EcUtils.uploadFullLogger.info("Exit the while loop to clear");
										currentTasks2.clear();
										currentTasks.clear();
										tasksQueue.clear();
										//EcUtils.uploadLogger.info("getMetaInfoExecutor是否已经关闭{}",getMetaInfoExecutor.isShutdown());
										if(!getMetaInfoExecutor.isShutdown()){
											//EcUtils.uploadLogger.info("最后手动关闭getMetaInfoExecutor");
											getMetaInfoExecutor.shutdownNow();
										}
										//EcUtils.uploadLogger.info("dealFileExecutor是否已经关闭{}",dealFileExecutor.isShutdown());
										if(!dealFileExecutor.isShutdown()){
											//EcUtils.uploadLogger.info("最后手动关闭deleteFileExecutor");
											dealFileExecutor.shutdownNow();
										}
										EcUtils.uploadTask.put("uploadTaskState","fail");
										EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.errorUploadinfo"),taskTemp.getModuleName(),messageThread);
									}
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}finally{
								//{}模块上载结束,tasksQueue.size()=
								EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.endModuleUpload")+",tasksQueue.size()="+tasksQueue.size() +"currentTasks2.size()="+currentTasks2.size()+"exceptionExit.get()="+exceptionExit.get(),task.getModuleName());
								if(tasksQueue.size() == 0 && currentTasks2.size() == 0){
									uploadFinish.set(true);
									synchronized (endWaitLock) {
										endWaitLock.notifyAll();
									}
									//{}模块上载结束进入结束任务，此时的uploadFinish为{}
									EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.endModuleUploadIntoTask"),task.getModuleName(),uploadFinish.get());
									MDC.remove("uploadTask");
								}
								deleteFileExecutor.execute(new Runnable() {
									@Override
									public void run() {
										String base = PropertyHolder.get().getGeneratePath() + File.separator + "unziped";
										File file = file = new File(base + File.separator + moduleCode);
										if (file.exists()){
											try {
												FileUtils.deleteDirectory(file);
											} catch (IOException e) {
												EcUtils.uploadFullLogger.info(e.getMessage(),e);
											}
										}
									}
								});
								semaphore.release();
								EcUtils.uploadFullLogger.info("{} Module Upload End Release Semaphore",task.getModuleName());
							}
						}
						
					}
				});
			}catch (Exception e) {
				e.printStackTrace();
				EcUtils.uploadLogger.info("An error occurred while executing multithreaded upload. The error message is:{}",e.getMessage());
				EcUtils.uploadFullLogger.info("An error occurred while executing multithreaded upload. The error message is:{}",e.getMessage());
				uploadFinish.set(true);
			}
		}
	}
	private void waitTaskEnd(){
		while(!shutdown.get()){
			MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
			while(!uploadFinish.get()){
				MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
				try {
					synchronized (endWaitLock) {
						endWaitLock.wait();
					}
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
			//uploadFinish标记为{}
			EcUtils.uploadFullLogger.info("uploadFinish Marked as {}",uploadFinish.get());
			String localLanguage = EcUtils.uploadTask.get("localLanguage");
			//EcUtils.uploadFullLogger.info(localLanguage);
			try {
				double totalTime = (System.currentTimeMillis() - startTime) * 1.0/1000;
				UploadInfoBatch uploadTaskBatch = (UploadInfoBatch) EcUtils.uploadTaskBatch.get("uploadTaskBatch");
				uploadTaskBatch.setUploadState(EcUtils.uploadTask.get("uploadTaskState"));
				uploadTaskBatch.setUploadDate(new Date());
				uploadTaskBatch.setTotalTime(totalTime+"");
				uploadInfoBatchService.update(uploadTaskBatch);
				//成功插入批量信息{}
				EcUtils.uploadFullLogger.info("Successful insertion of batchInformation{}",task.getModuleName(),uploadTaskBatch.getDescribe());
				//状态记录为{}	
				EcUtils.uploadFullLogger.info("The status record is{}", EcUtils.uploadTask.get("uploadTaskState"));
				if (totalTime > 60) {	//上载时间超过60秒以分钟为单位显示，低于60秒以秒为单位显示
					EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.allTaskHasOver.minute"), String.format("%.1f", totalTime/60));
					EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.allTaskHasOver.minute"), String.format("%.1f", totalTime/60));
				} else {
					EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.allTaskHasOver.second"), String.format("%.1f", totalTime));
					EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.allTaskHasOver.second"), String.format("%.1f", totalTime));
				}
			} catch (Exception e2) {
				//{}模块上载在结束等待的时候报错,错误信息为{}
				EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.uploadReportsInWait"),task.getModuleName(),e2.getMessage());
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.uploadReportsInWait"),task.getModuleName(),e2.getMessage());
			} finally{
				startTime = 0;
				EcUtils.uploadTaskBatch.remove("uploadTaskBatch");
				EcUtils.uploadFullLogger.info("Delete uploadTask log at the end of {} {} module upload",task.getModuleName(),task.getModuleCode());
				//结束本次上载
				EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.endUploadTask"));
				MDC.remove("uploadTask");
				//冗余环境下发送上载结束标志到healthmonitor
				if(masterSlaveEnabled){
					String connectresult=sendMessageToHealthMonitor(0, "upload", false);
					if(connectresult.equals("connect error")){
						sendMessageToHealthMonitor(0, "upload", false);
					}
				}
				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				uploadFinish.set(false);
				persistTaskFinish.set(true);
				maxTryCount = new AtomicInteger(0);
				EcUtils.metaInfoTasksQueue.clear();
				//EcUtils.uploadTask.put("curCompany",null);
				
			}
		}
	}
	private void clearTask(){
		EcUtils.uploadFullLogger.info("clearTask()+currentTasks2.clear()");
		//结束之前先清理剩余文件
		String base = PropertyHolder.get().getGeneratePath() + File.separator + "unziped";
		File file = file = new File(base);
		if (file.exists()){
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				EcUtils.uploadFullLogger.info(e.getMessage(),e);
			}
		}
		currentTasks2.clear();
		currentTasks.clear();
		tasksQueue.clear();
		exceptionExit.set(false);
		persistTaskFinish.set(false);
		exceptionUsed.set(false);
		errorCount = new AtomicInteger(0);
		
		maxTryCount = new AtomicInteger(0);
		EcUtils.metaInfoTasksQueue.clear();
		EcUtils.dealFileTasksQueue.clear();
		/*int semaphoreLockLength = semaphore.getQueueLength();
		if(semaphore.availablePermits() == 0){
			semaphore.release();
		}*/
	}

	@Override
	public UploadLog getProgressiveLog(long taskId, String progressiveType, long start) throws IOException {
		String logfile = String.valueOf(taskId);
		String status = null;


		if("full".equalsIgnoreCase(progressiveType)) {
			logfile = logfile + "-full.log";
		} else {
			logfile = logfile + ".log";
		}

		File file = getLogfile(logfile);
		if(null == file) return null;

		if(file.length() == start) return new UploadLog(start, "", getTaskProggressAndTime());

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			reader.skip(start);
			StringBuilder sb = new StringBuilder();
			int buffSize = 4096;
			char[] bf = new char[buffSize];
			int len = reader.read(bf, 0, buffSize);
			if(len > 0) {
				for(int i = 0; i < len; i++) {
					if(bf[i] == '\r') continue;
					if(bf[i] == '\n') {
						sb.append("<br>");
					}
					sb.append(bf[i]);
				}
			}else {
				len = 0;
			}
			status = "RUNNING";
			if(!persistTaskFinish.get() ){
				status = "RUNNING";
			}else if(persistTaskFinish.get() && !exceptionExit.get()){
				status = "FINISHED";
				deployTaskState.put("success", taskId+"");
				clearTask();
			}else if(persistTaskFinish.get() && exceptionExit.get()){
				status = "FAILED";
				deployTaskState.put("fail", taskId+"");
				clearTask();
			}
			if (ObjectUtils.isEmpty(taskStatus.get(taskId)) ||  "RUNNING".equals(taskStatus.get(taskId))){
				taskStatus.put(taskId,status);
			}
			UploadLog log = new UploadLog(start + len, sb.toString(), taskStatus.get(taskId), getTaskProggressAndTime());
			return log;
		} finally {
			if(null != reader) reader.close();
		}
	}

	private Map<Long, Object> deployStatusMap = new HashMap<Long, Object>();

	private DeployInfo getHistoryDeployInfo(final Long taskId) {
		DeployInfo deployInfo = (DeployInfo) deployStatusMap.get(taskId);
		if (null == deployInfo) {

			List<DeployInfo> deployInfoList = moduleService.getDeployListBytaskId(taskId.toString());
			if (null != deployInfoList && deployInfoList.size() > 0) {
				deployInfo = deployInfoList.get(0);
				deployStatusMap.put(taskId, deployInfo);
			}
		}
		int size = deployStatusMap.size();
		if (size > 10) {
			deployStatusMap.clear();
		}
		return deployInfo;
	}

	@Override
	public boolean cancel(long taskId) {
		clearTask();
		return false;
	}

	@Override
	public CopyOnWriteArrayList<UploadInfo> getCurrentTasks() {
		// TODO Auto-generated method stub
		return currentTasks != null ? currentTasks : new CopyOnWriteArrayList<UploadInfo>();
	}

	@Value("${configuration-services.uploadLogPath:logs/appUpload}")
	private String logDirectoryPath;

	@Override
	public File getLogfile(String logfile) {
		File logdir = new File(logDirectoryPath);
		File file = new File(logdir, logfile);
		if(!file.exists()) return null;
		return file;
	}

	@Override
	public void clearMap() {
		taskStatus.clear();
	}

	private UploadTaskProggress getTaskProggressAndTime() {
		BigDecimal totalSize = new BigDecimal(currentTasks.size());
		BigDecimal progressSize = new BigDecimal(this.taskProggress.getNextProggress());
		UploadTaskProggress taskProggress = new UploadTaskProggress();
		if(currentTasks2.size() == 0 && tasksQueue.size() == 0 && uploadFinish.get()){ 
			taskProggress.setRemanentTtime(0);
			taskProggress.setNextProggress(100.0);
		}else{
			taskProggress.setRemanentTtime(currentTasks2.size());
			if(currentTasks2.size()==0){
				taskProggress.setNextProggress(Double.valueOf(100));
			}else{
				taskProggress.setNextProggress(Double.valueOf(progressSize.divide(totalSize,2, BigDecimal.ROUND_HALF_EVEN).multiply(new BigDecimal(100)).doubleValue()));
			}
		} 
		return taskProggress;
	}
	

	@Override
	public  void setTaskProggressSize(Long taskProggressSize) {
		UploadInfoManagerImpl.taskProggressSize = taskProggressSize;
	}

	@Override
	public void updatetaskProggress(Double type) {
		taskProggress.uploadProggress(type);
	}


	@Override
	public UploadInfo push(UploadInfo task) {
		// TODO Auto-generated method stub
		return null;
	}

	public enum TaskStatus {
		WAITTING, // 等待中
		RUNNING, // 执行中
		FINISHED, // 已完成
		FAILED, // 已失败
		CANCELED, // 已取消
	}

	/**
	 * 关闭
	 */
	@PreDestroy
	public void close() {
		this.tasksQueue.clear();
		if(null != executor) {
			executor.shutdown();
		}
		if(null != uploadExecutor) {
			uploadExecutor.shutdown();
		}
		if(null != endUploadExecutor) {
			endUploadExecutor.shutdown();
		}
		if(null != deployTaskState){
			deployTaskState.clear();
		}
		if(null != getMetaInfoExecutor) {
			getMetaInfoExecutor.shutdown();
		}
		if(null != dealFileExecutor) {
			dealFileExecutor.shutdown();
		}
		clearTask();
	}


	@Override
	public void batchPush(final List<UploadInfo> tasks) {
		// TODO Auto-generated method stub
		uploadFinish.set(false);
		persistTaskFinish.set(false);
		exceptionExit.set(false);
		exceptionUsed.set(false);
		currentTasks2.addAll(tasks);
		currentTasks.addAll(tasks);
		tasksQueue.addAll(tasks);
		taskProggress.setTaskSize(Double.valueOf(tasks.size()));
		taskProggress.setNextProggress(0.0);
		//冗余环境中发送上载状态到healthmonitor
		if(masterSlaveEnabled){
			String connectresult=sendMessageToHealthMonitor(tasks.size(), "upload", true);
			if(connectresult.equals("connect error")){
				sendMessageToHealthMonitor(tasks.size(), "upload", true);
			}
		}
		startTime = System.currentTimeMillis();
		errorCount = new AtomicInteger(0);
		maxTryCount = new AtomicInteger(0);
		EcUtils.metaInfoTasksQueue.clear();
		EcUtils.dealFileTasksQueue.clear();
		//开启一个线程去获取元数据
		openGetMetaInfoExecutor(tasks);
		//开启一个线程去解压文件
		OpenDealFileExecutor(tasks);
	}
	/**
	 * 
	* @Title: openGetMetaInfoExecutor
	* @Description: 通过getMetaInfoExecutor去获取所有的模块的元数据
	* @param @param tasks    参数
	* @return void    返回类型
	* @throws
	 */
	private void openGetMetaInfoExecutor(List<UploadInfo> tasks){
		if(getMetaInfoExecutor.isShutdown()){
			getMetaInfoExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("metaInfoThread-%s").build());
		}
		for (final UploadInfo task :tasks) {
			getMetaInfoExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						fetchMetaInfoInModule(task.getModuleCode());
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			});
		}
	}
	/**
	 * 
	* @Title: OpenDealFileExecutor
	* @Description: 通过OpenDealFileExecutor解压模块包
	* @param @param tasks    参数
	* @return void    返回类型
	* @throws
	 */
	private void OpenDealFileExecutor(List<UploadInfo> tasks){
		if(dealFileExecutor.isShutdown()){
			dealFileExecutor = Executors.newFixedThreadPool(1,new ThreadFactoryBuilder().setNameFormat("dealFileExecutor-%s").build());
		}
		for (final UploadInfo task :tasks) {
			dealFileExecutor.execute(new Runnable() {
				@Override
				public void run() {
					Long startTime = System.currentTimeMillis();
					MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
					String localLanguage = EcUtils.uploadTask.get("localLanguage");
					try {
						Date startDate=new Date();
						Long moduleUploadStart = System.currentTimeMillis();
						String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + task.getUploadFileName();
						File uploadedFile = new File(uploaded);
						if (!uploadedFile.exists()) {
							EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.notFindUploadFile"),task.getModuleName());
							EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.notFindUploadFile"),task.getModuleName());
							clearTask();
							Map<String, String> errorTaskFlags = new HashMap<String, String>();
							errorTaskFlags.put(task.getModuleCode(), "fail");
							EcUtils.dealFileTasksQueue.add(errorTaskFlags);
						}
						String base = PropertyHolder.get().getGeneratePath() + File.separator + "unziped";
						File file = new File(base + File.separator + task.getModuleCode());
						if (file.exists())
							FileUtils.deleteDirectory(file);
						file.mkdirs();
						UnZipFile.unzip(uploadedFile, file);
						Long timeA = System.currentTimeMillis();
						Map<String, String> taskFlags = new HashMap<String, String>();
						taskFlags.put(task.getModuleCode(), "success");
						EcUtils.dealFileTasksQueue.add(taskFlags);
						EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.allTimeOfFindTask"),task.getModuleCode(),(timeA-moduleUploadStart)*1.0/1000);
						EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.allTimeOfFindTask"),task.getModuleCode(),(timeA-moduleUploadStart)*1.0/1000);
					} catch (Exception e) {
						EcUtils.uploadFullLogger.info(e.getMessage(), e);
						EcUtils.uploadLogger.info(e.getMessage(), e);
						logger.error(e.getMessage(), e);
						clearTask();
					}finally{
						Long endTime = System.currentTimeMillis();
						EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.unzipFileTime"),(endTime-startTime)*1.0/1000);
						EcUtils.uploadLogger.info(InternationalResource.get("ec.module.uploadBatch.unzipFileTime"),(endTime-startTime)*1.0/1000);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
	
	/**
	 * 
	 * @param
	 * @return
	 */
	
	private void fetchMetaInfoInModule(String moduleCode) {
		final String localLanguage = EcUtils.uploadTask.get("localLanguage");
		Session session = ecSessionFactory.openSession();
		MDC.put("uploadTask", EcUtils.uploadTask.get("uploadTask"));
		try {
			if (moduleCode != null) {
				Long startTime = System.currentTimeMillis();
				Map<String, Map<String, Object>> retMap = new HashMap<>();
				Map<String, Object> tmpMap = new HashMap<>();
				Criterion criterion = Restrictions.or(Restrictions.like("code", moduleCode, MatchMode.START),
						Restrictions.like("code", "_" + moduleCode + "_", MatchMode.ANYWHERE));
				for (EcEntityEnum enumItem : EcEntityEnum.values()) {
					tmpMap = fetchMetaInfo(session, enumItem.clazz, criterion);
					retMap.put(enumItem.clazz.getName(), tmpMap);
				}
				Map<String, Map<String, Map<String, Object>>> metaInfoMap = new ConcurrentHashMap<String, Map<String, Map<String, Object>>>();
				Map<String, Map<String, Object>> resultMap = new HashMap<>();
				Map<String, Object> result =  new HashMap<String, Object>();
				result.put("result", "success");
				resultMap.put("result", result);
				metaInfoMap.put(moduleCode, retMap);
				metaInfoMap.put("result", resultMap);
				EcUtils.metaInfoTasksQueue.add(metaInfoMap);
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.module.uploadBatch.selectOfTimeModule")+((System.currentTimeMillis()-startTime)*1.0/1000),moduleCode);
			}
		} catch (Exception e) {
			// TODO: handle exception
			Map<String, Map<String, Map<String, Object>>> metaInfoMap = new ConcurrentHashMap<String, Map<String, Map<String, Object>>>();
			Map<String, Map<String, Object>> resultMap = new HashMap<>();
			Map<String, Object> result =  new HashMap<String, Object>();
			result.put("result", "fail");
			resultMap.put("result", result);
			metaInfoMap.put("result", resultMap);
			EcUtils.metaInfoTasksQueue.add(metaInfoMap);
			EcUtils.uploadFullLogger.info(e.getMessage(),e);
		}
		finally{
			session.close();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> fetchMetaInfo(Session session, Class<? extends AbstractCodeEntity> clazz, Criterion... criterions) {
		Map<String, Object> retMap = new HashMap<>();
		Criteria criteria = session.createCriteria(clazz);
		if (criterions != null && criterions.length > 0) {
			for (Criterion criterion : criterions) {
				criteria.add(criterion);
			}
		}
		Long startTime = System.currentTimeMillis();
		List<? extends AbstractCodeEntity> result = criteria.list();
		String clazzName = clazz.getName().toString().substring(clazz.getName().toString().lastIndexOf(".")+1,clazz.getName().toString().length());
	    //EcUtils.uploadFullLogger.info(clazzName+"------查询花费的时间为："+((System.currentTimeMillis()-startTime)*1.0/1000)+"-----查询条件是"+criteria.toString());
		if (result != null && !result.isEmpty()) {
			for (Object item : result) {
				AbstractCodeEntity tmp = (AbstractCodeEntity) item;
				retMap.put(tmp.getCode(), item);
			}
		}
	    return retMap;
	}

}
