<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss />
<@mainjs />

<title>${getText('foundation.inter.uploadPage')}</title>
</head>
<body id="entityList_page">
<script type="text/javascript">
	var body;
	var publishPercent;
	var progressBar;
	var uploadUser;//上载人
	var uploadModuleText;//上载的模块
	var publishLast;//上载内容,进度条下面的那个内容
	var uploadDescription;//上载日志
	$(function(){
		body = $('#top_upload_body');
		publishPercent = body.find('.top-publishLog-bar');
		progressBar = new CUI.progressBar();
		publishPercent.append(progressBar._el);
		//progressBar.moveBar(80)
		uploadUser = body.find('.top-publishLog-DeployUser');//上载人
		uploadModuleText = body.find('.top-publishLog-modulList-text');//上载的模块
		publishLast = body.find('.top-publishLog-description');//上载内容,进度条下面的那个内容
		uploadDescription = body.find('.top-publishLog-content');//上载日志
		publishLast.text("");

		//开始给信息赋值
		uploadUser.append("${uploadBaptchUser}");
		uploadModuleText.append("${uploadBaptchUserModuleMessage}".replace('${getText("foundation.inter.uploadModule")}','${getText("foundation.inter.uploading")}'));
		uploadModuleText.attr('title',"${uploadBaptchUserModuleMessage}".replace('${getText("foundation.inter.uploadModule")}',''));
		//publishLast.append("${uploadBaptchUserLastTime!''}");//显示还有多少时间，在下面的ajax中及时更新时间
		uploadDescription.append("${uploadBaptchUserProcessLog!''}");//用来更新日志
		$("#progressivebatchLog").append("${uploadBaptchUserModuleMessage!''}");
		$("#progressivebatchLog").append("</br>");
		setTimeout(function(){
			progressiveBatchLog(0,'${uploadTaskId}');
		},2000)
	})
	function uploadSuccess(){
		progressBar.finishedCallBack(function(){
			//ec.module.publishLast.text('所有模块上载完成！').css({'color':'#111', 'font-weight':'bold', 'font-size':'14px'});
			
			$(progressBar._el).hide();
			publishLast.text('').addClass("uploadDone");			
		
		})
		progressBar.finished('100%');
		/*clearTimeout(ec.module.timeout);
		ec.module.deployModules = [];
		ec.module.lastPercent = 0;
    	ec.module.deployModulesName = '';
		foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'));
		ec.module.refreshModuleTree();*/
	}

	function progressiveBatchLog(i,taskId){
		if(i == undefined){
			i = 0;
		}
		var start = $("#progressivebatchLog").attr("start") || 0;
		if(taskId != null) {
			$.ajax({
				url : '/msService/ec/module/upload/progressiveLog?taskId=' + taskId + "&progressiveType=simple&start=" + start,
				dataType : 'json',
 				cache: false,
 				async: false,
				success : function(data, textStatus, request) {
					i++;
					$("#progressivebatchLog").attr("start", data.position);
					if(data.status == 'WAITTING'){
						$("#progressivebatchLog").append("${getText("ec.model.upload.trylater")}");
					}else if(data.status == 'RUNNING'){
						var lastPercent = data.taskProggress.nextProggress == 100 ? 99 : data.taskProggress.nextProggress;
						if(progressBar._percent.innerText == '0%' && lastPercent == 99){
							progressBar.moveTo(50);
						}else{
							progressBar.moveTo(lastPercent);
						}
						var lastTime = data.taskProggress.remanentTtime;
						if(lastPercent == 99) {
							publishLast.text("${getText("ec.model.upload.uploadcomplete")}");
						}else if(isNaN(lastTime)) {
							//publishLast.text("预计还需" + lastTime +"上载完成");
							publishLast.text("${getText("ec.model.upload.drinkcoffee")}");
						}
						$("#progressivebatchLog").append(data.content);
						setTimeout(function(){
							progressiveBatchLog(i, taskId);
						}, 2000);
					}else if(data.status == 'FAILED'){
						$("#progressivebatchLog").attr("start", 0);
						$("#progressivebatchLog").append("${getText("ec.model.upload.uploadfail")}")
						progressBar.finishedCallBack(function(){
							$(progressBar._el).hide();
							publishLast.text('').addClass("uploadFail");	
						})
						progressBar.finished('100%');
						//foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'));
						parent.$('div[id ^="dialog_"] .bt').find('.ewc-dialog-button-left').attr('style','');
						parent.$('div[id ^="dialog_"] .bt').find('.ewc-dialog-button-left').html('<a class="cui-link-dialog" href="/msService/ec/module/upload-down-log?uploadTaskId=${uploadTaskId}">${getText("foundation.inter.xzxxrz")}</a>');//日志下载
						parent.ec.module.refreshModuleTree();
					}else if(data.status == 'FINISHED'){
						progressBar.finishedCallBack(function(){
							$(progressBar._el).hide();
							publishLast.text('').addClass("uploadDone");			
						})
						progressBar.finished('100%');
						$("#progressivebatchLog").append(data.content);
						parent.$('div[id ^="dialog_"] .bt').find('.ewc-dialog-button-left').attr('style','');
						parent.$('div[id ^="dialog_"] .bt').find('.ewc-dialog-button-left').html('<a class="cui-link-dialog" href="/msService/ec/module/upload-down-log?uploadTaskId=${uploadTaskId}">${getText("foundation.inter.xzxxrz")}</a>');//日志下载
						parent.ec.module.refreshModuleTree();
						return false;
					}
				},
				error: function(){
					console.log("${getText('foundation.inter.determineerrormessage')}");
					//为了解决测试环境最后结束的时候无法结束的问题
					setTimeout(function(){
							progressiveBatchLog(i, taskId);
						}, 2000);
				}
			})
		}
	}
</script>
<div id="top_upload_body">
<div id="top_publishLog_hd"><div class="top-publishLog-info"><span class="top-publishLog-DeployUser"></span><span class="top-publishLog-modulList-text"></span></div><div class="top-publishLog-bar"></div><p class="top-publishLog-description"></p></div><div id="top_publishLog_bd"><div class="top-publishLog-details">${getText("foundation.inter.uploadlogdetail")}</div><div class="top-publishLog-content"><div id="progressivebatchLog" start = "0" ></div></div></div>
</div>
</body>
</html>