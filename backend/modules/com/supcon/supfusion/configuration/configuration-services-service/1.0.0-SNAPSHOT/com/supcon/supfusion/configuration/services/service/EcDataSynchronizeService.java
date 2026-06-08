package com.supcon.supfusion.configuration.services.service;


/**
 * 
 * 实体配置元数据同步服务。
 * <p>
 * 对运行期元数据做修改时会触发相应的事件，并发出相应的消息。
 * 本服务会使用 {@linkplain org 服务发送事件。
 * <p>
 * 考虑到整体性能影响，本服务将采用异步的方式发送事件，也就是调用 {@lin} 方法。
 * 
 * @author yaowei
 *
 */
public interface EcDataSynchronizeService {
	
	/**
	 * 把指定模块的开发库同步到运行库以及工程库中
	 * 
	 * @param moduleCode
	 *            指定的模块CODE，若为null，则同步所有数据
	 */
	void synchronizeEcDataFromDevToRumtime(String moduleCode);
	
   /**
	* 把指定视图的数据从开发库同步到运行时和工程库中
	*
	* @param viewCode
	* @param syncFlag
	*            指定的视图CODE
	*/
	void synchronizeViewDataFromEC(String viewCode, int syncFlag);
	/**
	 * 把指定视图的数据从工程库同步到运行时库中
	 *
	 * @param viewCode
	 *            指定的视图CODE
	 */
	void synchronizeViewDataFromProj(String viewCode);


	void forceSynchronizeECViewDataToRuntime(String viewCode);

}
