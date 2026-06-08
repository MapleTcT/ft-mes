<style>
.cui-btn-schedule-bs {
    background: url(/bap/static/images/schedule/icon_bs.png) no-repeat 0 1px;
}

.cui-btn-schedule-ckrz {
    background: url(/bap/static/images/schedule/icon_ckrz.png) no-repeat 0 1px;
}

.cui-btn-schedule-hf {
    background: url(/bap/static/images/schedule/icon_hf.png) no-repeat 0 1px;
}

.cui-btn-schedule-jc {
    background: url(/bap/static/images/schedule/icon_jc.png) no-repeat 0 1px;
}

.cui-btn-schedule-ljzx {
    background: url(/bap/static/images/schedule/icon_ljzx.png) no-repeat 0 1px;
}

.cui-btn-schedule-zt {
    background: url(/bap/static/images/schedule/icon_zt.png) no-repeat 0 1px;
}
</style>

<@frameset id="ec_scheduler_job">
	<@loadpanel />
	<@frame id="ec_scheduler_job_main" offsetH=4>
		<@datatable hidekey="['code','inheritFlag','endTime']" transMethod="post" dtPage="jobPage" pageInitMethod="ec.scheduler.job.ec_scheduler_job_datatable_initmethod" id="ec_scheduler_job_datatable" dataUrl="/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}" dblclick="ec.scheduler.job.edit" paginator=true>
			<@operatebar operates="code:ec_print_template_btn_add||name:${getText('ec.scheduler.datasource.add')}||iconcls:add||onclick:ec.scheduler.job.add();
				code:ec_scheduler_job_btn_extendJob||name:${getText('ec.scheduler.job.extendJob')}||isHide:true||iconcls:schedule-jc||onclick:ec.scheduler.job.extendJob();
				code:ec_print_template_btn_add||name:${getText('ec.scheduler.job.schedule')}||iconcls:schedule-bs||onclick:ec.scheduler.job.schedule();
				code:ec_print_template_btn_add||name:${getText('ec.scheduler.job.stop')}||iconcls:schedule-zt||onclick:ec.scheduler.job.stop();
				code:ec_print_template_btn_add||name:${getText('ec.scheduler.job.immediateExecution')}||iconcls:schedule-ljzx||onclick:ec.scheduler.job.immediateExecution();
				code:ec_print_template_btn_add||name:${getText('ec.scheduler.job.lookjoblog')}||iconcls:schedule-ckrz||onclick:ec.scheduler.job.lookjoblog();
				code:ec_print_template_btn_mod||iconcls:edit||name:${getText('ec.scheduler.datasource.edit')}||onclick:ec.scheduler.job.edit();
				code:ec_print_template_btn_del||iconcls:del||name:${getText('ec.scheduler.datasource.delete')}||onclick:ec.scheduler.job.del();
                code:ec_print_template_btn_fresh||iconcls:schedule-refresh||name:${getText('foundation.common.reflesh')}||onclick:ec.scheduler.job.refresh()"
				  operateType="noPower" resultType="json">
			</@operatebar>
			<#if isProj?? && isProj>
			<@datacolumn textalign="left" key="inheritFlag" type="select" options="{'true':'${getText('ec.model.isExtends')}','false':'${getText('ec.engine.enginenew')}'}" label="${getText('ec.engine.source')}" width=160 />
			</#if>
			<@datacolumn textalign="left" key="nameInternational" label="${getText('ec.scheduler.job.name')}" width=160 />
			<@datacolumn textalign="left" key="startTime" type="datetime" label="${getText('ec.scheduler.job.startTime')}" width=160 />
			<@datacolumn textalign="left" key="jobType" type="select" options="{'1':'${getText('ec.scheduler.job.jobtype1')}','2':'${getText('ec.scheduler.job.jobtype2')}'}" label="${getText('ec.scheduler.job.jobType')}" width=160 />
			<@datacolumn textalign="left" key="nextRunTime" type="datetime" label="${getText('ec.scheduler.job.nextRunTime')}" width=160 />
			<@datacolumn textalign="left" key="hasRunTimes" label="${getText('ec.scheduler.job.hasruntimes')}" width=160 />
			<@datacolumn textalign="left" key="status" type="select" options="{'UNSCHEDULED':'${getText('ec.scheduler.job.unscheduled')}','SCHEDULED':'${getText('ec.scheduler.job.scheduler')}','WAITNEXTTRIGGER':'${getText('ec.scheduler.job.waitnexttrigger')}','JOBPAUSED':'${getText('ec.scheduler.job.jobpaused')}','JOBOVER':'${getText('ec.scheduler.job.jobover')}','SCHEDULERERROR':'${getText('ec.scheduler.job.schedulererror')}'}" label="${getText('ec.scheduler.job.jobStatus')}" width=160 />
		</@datatable>
	</@frame>
</@frameset>

<script type="text/javascript">
(function(){
	CUI.ns("ec.scheduler.job"); 
	var fastQuerySetting_dialog;
	ec.scheduler.job.ec_scheduler_job_datatable_initmethod = function(){
		<#if (isProj?? && !isProj) || (entity?? && entity.module?? && entity.module.code == 'sysbase_1.0')>
		$('#ec_scheduler_job_btn_extendJob').parent().hide();
		</#if>
	}
	ec.scheduler.job.config = function(evt,row){
		row.inheritType = 1;
		if(row.inheritType==1){
			var url = '/msService/ec/printManage/edit?printTemplate.code=' + row.code + '&entity.code=${entity.code}&isProj=${isProj?string('true', 'false')}';
			ec.scheduler.job.editDlg = new CUI.Dialog({
				title: "${getText('ec.project.view_setting')}",
				modal:true,
				width : 550,
				height : 300,
				url:url,
				type:4,
				buttons:[{	name:"${getText('common.button.save')}",
					handler:function(){
						$("#SubmitschedulerJobForm").attr('action', '/msService/ec/scheduler/jobSave?isProj=${isProj?string('true', 'false')}');
						$("#SubmitschedulerJobForm").submit();
					}
				},
				{	name:"${getText('ec.scheduler.job.publish')}",
					handler:function(){
						$("#SubmitschedulerJobForm").attr('action', '/msService/ec/scheduler/jobSave?isProj=${isProj?string('true', 'false')}');
						$("#SubmitschedulerJobForm").submit();
					}
				},
				{	name:"${getText('ec.common.cancel')}",
					handler:function(){
						this.close();
					}
				}]
			});
			ec.scheduler.job.editDlg.show();
		}else{
			if(row.isShadow) {
				CUI.Dialog.alert("${getText('ec.scheduler.job.shadowViewNotEdit')}");
				return;
			} 
			if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
				window.open('/msService/ec/printManage/edit?isProj=${isProj?string('true', 'false')}&printTemplate.code=' + row.code + '&entity.code=' + row.entity.code,'ecviewwin','height=800, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, status=no');
			} else {
				openFullScreen('/msService/ec/printManage/edit?isProj=${isProj?string('true', 'false')}&printTemplate.code=' + row.code + '&entity.code=' + row.entity.code);
			}
		}
	}
	function _dialog(title,url,callback,width,height){
		return new CUI.Dialog({
			title : title,
			width :width||550,
			height :height||350,
			modal : true,
			url : url,
			buttons:[
				{	name:"${getText('common.button.save')}",
					handler:function(){callback(this);}
				},
				{	name:"${getText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
	}

	ec.scheduler.job.refresh = function(){
        datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList.action?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
    }

	 /**
	 * 任务调度添加
	 * @method ec.scheduler.job.add
	 */
	ec.scheduler.job.editDlg;
	ec.scheduler.job.add=function(){
		var dlg = ec.scheduler.job.editDlg=_dialog("${getText('ec.scheduler.job.addview')}","/msService/ec/scheduler/addJob?moduleCode=${entity.module.code}&artifact=${entity.module.artifact}&isProj=${isProj?string('true', 'false')}",function(d){
			$("#SubmitschedulerJobForm input[name='schedulerJob.moduleCode']").val("${entity.module.code}");
			var shortCode = $("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']").val();
			var moduleCode = $("#SubmitschedulerJobForm input[name='schedulerJob.moduleCode']").val();
			if (ec.scheduler.job.requiredVerification()) {
				if (ec.scheduler.job.jobValidate()) {
					if(querySchedulerJobCodeUnique(shortCode,moduleCode)){
						$("#SubmitschedulerJobForm").attr('action', '/msService/ec/scheduler/jobSave?moduleCode=${entity.module.code}&artifact=${entity.module.artifact}&isProj=${isProj?string('true', 'false')}');
						$("#SubmitschedulerJobForm").submit();
					}else{
						return false;
					}
				}else{
					return false;
				}
			}else{
				return false;
			}
		});
		dlg.show();
	}
	
	/**
	 * 任务调度信息修改
	 * @method ec.scheduler.job.edit
	 * @public
	 */
	ec.scheduler.job.edit=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice.edit')}");
			return;
		}
		var dlg = ec.scheduler.job.editDlg = _dialog("${getText('ec.scheduler.job.editview')}", "/msService/ec/scheduler/modifyJob?moduleCode=${entity.module.code}&isProj=${isProj?string('true', 'false')}&schedulerJob.code="+datatable_ec_scheduler_job_datatable.selectedRows[0].code,function(d){
				$("#SubmitschedulerJobForm input[name='schedulerJob.moduleCode']").val("${entity.module.code}");
				//if (ec.scheduler.job.codeValidate()) {
					if(ec.scheduler.job.requiredVerification()){
						$("#SubmitschedulerJobForm").attr('action', '/msService/ec/scheduler/jobUpdate?moduleCode=${entity.module.code}&isProj=${isProj?string('true', 'false')}');
						$("#SubmitschedulerJobForm").submit();
					}else{
						return false;
					}
				//}else{
				//	return false;
				//}		
		});
		dlg.show();
	}
	
	
		/**
	 * 任务调度删除
	 * @method ec.scheduler.job.del
	 * @public
	 */
	 ec.scheduler.job.deleteMethod = function(url,successMsg,failureMsg) {
		$.ajax({
			//data : {"view.code":datatable_ec_scheduler_job_datatable.selectedRows[0].code,
			//		"view.version":datatable_ec_scheduler_job_datatable.selectedRows[0].version
			//},
			url : url , 
			success : function(msg){
				
				if(msg && msg.success){
				
					datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
					CUI.Dialog.alert(successMsg||"${getText('common.delete.success')}");
				}else{
					if(msg && msg.exceptionMsg){alert(msg.exceptionMsg);}
					else{CUI.Dialog.alert(failureMsg||"${getText('ec.scheduler.datasource.delete.failure')}");}
				}
			}	
		});
	}
	ec.scheduler.job.del=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice.delete')}");
			return;
		}
		CUI.Dialog.confirm(  
				"${getHtmlText('ec.scheduler.job.deleteConfirm')}",  // 提示消息  
				function(){	ec.scheduler.job.deleteMethod("/msService/ec/scheduler/jobDelete?isProj=${isProj?string('true', 'false')}&schedulerJob.code=" +
						datatable_ec_scheduler_job_datatable.selectedRows[0].code);
						CUI.Dialog.closeAll();
						$("#load_mask").css(
						{'z-index':0,
						'block':'none'});
						} // 确定后事件   
			);
	}
	
	ec.scheduler.job.enableCallBackInfo=function(res){
		if(res.success){
			ec.scheduler.job.configDialog.close();
			CUI.Dialog.alert("${getHtmlText('ec.engine.view.dealsuccess')}");
			datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
		}
	}
	/**
	 * 数据源信息提交返回信息及回列表
	 * @method ec.scheduler.job.callBack
	 * @public
	 SubmitschedulerJobFormDialogErrorBar
	 */
	ec.scheduler.job.callBack=function(msg){
		if(msg && msg.success){
			if(window.SubmitschedulerJobFormDialogErrorBarWidget){
				SubmitschedulerJobFormDialogErrorBarWidget.show("${getText('ec.scheduler.job.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.scheduler.job.editDlg.close();}catch(e){}
					datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
				},1500);
			}else if(window.SubmitschedulerJobFormDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.scheduler.job.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.scheduler.job.inheritconfigdialog.close();}catch(e){}
					datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
				},1500);
			}
		}else{
			if(window.SubmitschedulerJobFormDialogErrorBarWidget){
    	        SubmitschedulerJobFormDialogErrorBarWidget.show("${getText('ec.scheduler.job.submitfailure')}");
			} else if (window.ec_view_inheritconfig_formDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.scheduler.job.submitfailure')}");
			}
			setTimeout(function(){
				try{ec.scheduler.job.editDlg.close();}catch(e){}
				foundation.workbench.refresh();
			},1500);
        }
	}
	
	/**
	 * 任务调度继承
	 * @method ec.scheduler.job.extendJob
	 * @public
	 */
	ec.scheduler.job.extendJob= function(){
		var extendJobdlg = ec.scheduler.job.extendJob.editDlg=_dialog("${getText('ec.scheduler.job.extendJobScheduler')}","/msService/ec/scheduler/extendJob?moduleCode=${entity.module.code}",function(d){
			$("#ec_scheduler_inherit_form").submit();
				},'100px','120px');
				extendJobdlg.show();
	}
	
	/**
	 * 任务调度继承回调
	 * @method ec.scheduler.job.extendJob.inheritcallBack
	 * @public
	 */
	ec.scheduler.job.extendJob.inheritcallBack=function(msg){
		if(msg && msg.success){
			ec_view_inherit_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.scheduler.job.extendJob.editDlg.close();}catch(e){}
				datatable_ec_scheduler_job_datatable.setRequestDataUrl("/msService/ec/scheduler/getJobList?isProj=${isProj?string('true', 'false')}&moduleCode=${entity.module.code}");
			},1500);
		}else{
    	    ec_view_inherit_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
			setTimeout(function(){
				try{ec.scheduler.job.extendJob.editDlg.close();}catch(e){}
					foundation.workbench.refresh();
				},1500);
        }
    }
	
	/**
	* 部署
	*/
	ec.scheduler.job.schedule=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.scheduler.job.choice.scheduler')}");
			return;
		}
		<#if isProj?? && !isProj>
		var inheritFlag = datatable_ec_scheduler_job_datatable.selectedRows[0].inheritFlag;
		if (inheritFlag == true) {
			CUI.Dialog.alert("${getText('ec.scheduler.job.scheduler.inherit')}");
			return;
		}
		</#if>
		var status = datatable_ec_scheduler_job_datatable.selectedRows[0].status;
		if (status == 'UNSCHEDULED') {
			var endTime = datatable_ec_scheduler_job_datatable.selectedRows[0].endTime;
			var curDate = new Date();
			if(null != endTime && endTime<curDate){
				CUI.Dialog.alert("${getText('ec.scheduler.job.endTime.range')}");
				return;
			}
			CUI.Dialog.confirm(  
					"${getHtmlText('ec.scheduler.job.scheduleConfirm')}",  // 提示消息  
					function(){	ec.scheduler.job.deleteMethod("/msService/ec/scheduler/schedule?isProj=${isProj?string('true', 'false')}&schedulerJob.code=" +
							datatable_ec_scheduler_job_datatable.selectedRows[0].code+"&schedulerJob.status="+datatable_ec_scheduler_job_datatable.selectedRows[0].status,"${getHtmlText('ec.scheduler.job.schedulesuccess')}","${getHtmlText('ec.scheduler.job.schedulefailure')}");
							CUI.Dialog.closeAll();
							$("#load_mask").css(
							{'z-index':0,
							'block':'none'});
							}, // 确定后事件
							null,"${getHtmlText('ec.scheduler.job.scheduleHintInfo')}"   
				);
		}else{
			CUI.Dialog.alert("${getText('ec.scheduler.job.scheduler.unscheduled')}");
			return;
		}
	}
	
	/**
	* 停止
	*/
	ec.scheduler.job.stop=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice.stop')}");
			return;
		}
		var status = datatable_ec_scheduler_job_datatable.selectedRows[0].status;
		if (status == 'SCHEDULED' || status == 'WAITNEXTTRIGGER') {
			CUI.Dialog.confirm(  
					"${getHtmlText('ec.scheduler.job.stopConfirm')}",  // 提示消息  
					function(){	ec.scheduler.job.deleteMethod("/msService/ec/scheduler/stop?isProj=${isProj?string('true', 'false')}&schedulerJob.code=" +
							datatable_ec_scheduler_job_datatable.selectedRows[0].code+"&schedulerJob.status="+datatable_ec_scheduler_job_datatable.selectedRows[0].status,"${getHtmlText('ec.scheduler.job.stopsuccess')}","${getHtmlText('ec.scheduler.job.stopfailure')}");
							CUI.Dialog.closeAll();
							$("#load_mask").css(
							{'z-index':0,
							'block':'none'});
							}, // 确定后事件
							null,"${getHtmlText('ec.scheduler.job.stopHintInfo')}"   
				);
		}else{
			CUI.Dialog.alert("${getText('ec.scheduler.job.scheduler.or.waitnexttrigger')}");
			return;
		}
	}
	
	/**
	* 恢复
	*/
	ec.scheduler.job.resume=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice')}");
			return;
		}
		var status = datatable_ec_scheduler_job_datatable.selectedRows[0].status;
		if (status == 'JOBPAUSED') {
			CUI.Dialog.confirm(  
					"${getHtmlText('ec.scheduler.job.resumeConfirm')}",  // 提示消息  
					function(){	ec.scheduler.job.deleteMethod("/msService/ec/scheduler/resume?isProj=${isProj?string('true', 'false')}&schedulerJob.code=" +
							datatable_ec_scheduler_job_datatable.selectedRows[0].code+"&schedulerJob.status="+datatable_ec_scheduler_job_datatable.selectedRows[0].status,"${getHtmlText('ec.scheduler.job.resumesuccess')}","${getHtmlText('ec.scheduler.job.resumefailure')}");
							CUI.Dialog.closeAll();
							$("#load_mask").css(
							{'z-index':0,
							'block':'none'});
							}, // 确定后事件
							null,"${getHtmlText('ec.scheduler.job.resumeHintInfo')}"   
				);
		}else{
			CUI.Dialog.alert("${getText('ec.scheduler.job.scheduler.stop')}");
			return;
		}
	}
	
	/**
	* 立即执行
	*/
	ec.scheduler.job.immediateExecution=function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice.immediateExecution')}");
			return;
		}
		var status = datatable_ec_scheduler_job_datatable.selectedRows[0].status;
		if (status == 'SCHEDULED' || status == 'WAITNEXTTRIGGER') {
			CUI.Dialog.confirm(  
					"${getHtmlText('ec.scheduler.job.immediateExecutionConfirm')}",  // 提示消息  
					function(){	ec.scheduler.job.deleteMethod("/msService/ec/scheduler/immediateExecution?isProj=${isProj?string('true', 'false')}&schedulerJob.code=" +
							datatable_ec_scheduler_job_datatable.selectedRows[0].code,"${getHtmlText('ec.scheduler.job.immediatesuccess')}","${getHtmlText('ec.scheduler.job.immediatefailure')}");
							CUI.Dialog.closeAll();
							$("#load_mask").css(
							{'z-index':0,
							'block':'none'});
							}, // 确定后事件
							null,"${getHtmlText('ec.scheduler.job.immediateHintInfo')}"   
			);
		}else{
			CUI.Dialog.alert("${getText('ec.scheduler.job.immediateExecution.scheduler.or.waitnexttrigger')}");
			return;
		}
	}
	
	/**
	* 查看日志
	*/
	ec.scheduler.job.lookjoblog = function(){
		if(datatable_ec_scheduler_job_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.job.choice.lookjoblog')}");
			return;
		}
		//if(datatable_ec_scheduler_job_datatable.selectedRows[0]){
			var url='/msService/ec/scheduler/remindlookjoblog?isProj=${isProj?string('true', 'false')}&code=' + datatable_ec_scheduler_job_datatable.selectedRows[0].code;
			CUI(function(){
				ec.scheduler.job.lookjoblog.editDlg = new CUI.Dialog({
					title: "${getText('ec.scheduler.job.lookjoblog')}",
					url:url, 
					modal:true,
					height:390,
					width: 630,
					dragable:true,
					buttons:[{name:"${getText('ec.view.close')}",id:"close",handler:function(){this.close();}}]
				})
				ec.scheduler.job.lookjoblog.editDlg.show();
			});
		//}else {
		//	workbenchErrorBarWidget.showMessage("${getText('ec.view.recoview')}");
		//}	
	}
	
})();
	
</script>