/**
 * 
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.utils.UploadLog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本服务用于对模块的批量上载管理
 * <p>
 * 无论在哪个阶段，都会消耗很长的时间，为了更好的管理这种长时间任务，本管理器的实现将会采用异步的方式进行上载。
 * <p>
 * 本管理器将控制一个任务队列，采用先进先出策略逐一执行任务。
 * 考虑到目前BAP的上载逻辑较为复杂，目前暂时规定每次只会有一个线程执行发布任务。
 * <p>
 * 
 * @author zhushizhang
 *
 */
public interface UploadInfoManager {
	
	/**
	 * 提交一个上载任务到任务队列末尾。
	 * <p>
	 * 如果当前队列中已经存在一个相同模块的任务，但该任务还未执行，则会抛出异常。
	 * <p>
	 * 当发布任务被成功添加后，会自动给task的id赋值，并返回一个新的任务对象，不会覆盖原来的任务对象。
	 * 
	 * @param moduleCode
	 * @return 一个新的任务对象，并重新生成任务id。
	 */
	UploadInfo push(UploadInfo task); 
	
	/**
	 * 提交一组上载任务到任务队列末尾。
	 * 
	 * @param tasks
	 */
	void batchPush(List<UploadInfo> tasks);
	
	
	/**
	 * 取消任务。取消任务不会直接
	 * <p>
	 * 如果指定id的任务并没有执行，则直接从任务队列中删除；如果当前任务已经在执行了，则会在下一个环节之前停止执行。
	 * 可停止的任务环节由任务步骤具体定义。
	 * <p>
	 * 
	 * 
	 * @param taskId
	 * @return
	 */
	boolean cancel(long taskId);

	public CopyOnWriteArrayList<UploadInfo> getCurrentTasks();

	void setTaskProggressSize(Long taskProggressSize);

	void updatetaskProggress(Double type);

	UploadLog getProgressiveLog(long taskId, String progressiveType, long start) throws IOException;

	File getLogfile(String logfile);

	void clearMap();
}
