<@frameset id="ec_scheduler_datasource">
	<@loadpanel />
	<@frame id="ec_scheduler_datasource_main" offsetH=4>
		<@datatable hidekey="[]" transMethod="post" dtPage="page" id="ec_scheduler_datasource_datatable" dataUrl="/msService/ec/scheduler/getDataSourceList?isProj=true&moduleCode=${entity.module.code}" dblclick="ec.scheduler.datasource.edit" paginator=true>
			<@operatebar operates="code:ec_print_template_btn_add||name:${getText('ec.scheduler.datasource.add')}||iconcls:add||onclick:ec.scheduler.datasource.add();
				code:ec_print_template_btn_mod||iconcls:edit||name:${getText('ec.scheduler.datasource.edit')}||onclick:ec.scheduler.datasource.edit();
				code:ec_print_template_btn_del||iconcls:del||name:${getText('ec.scheduler.datasource.delete')}||onclick:ec.scheduler.datasource.del()"
				  operateType="noPower" resultType="json">
			</@operatebar>
			<@datacolumn textalign="left" key="code" label="${getText('ec.scheduler.datasource.code')}" width=160 />
			<@datacolumn textalign="left" key="nameInternational" label="${getText('ec.scheduler.datasource.name')}" width=160 />
			<@datacolumn textalign="left" key="datasourceType" label="${getText('ec.scheduler.datasource.datasourceType')}" width=280 />
		</@datatable>
	</@frame>
</@frameset>

<script type="text/javascript">
(function(){
	CUI.ns("ec.scheduler.datasource"); 
	var fastQuerySetting_dialog;
	ec.scheduler.datasource.config = function(evt,row){
		row.inheritType = 1;
		if(row.inheritType==1){
			var url = '/msService/ec/printManage/edit?printTemplate.code=' + row.code + '&entity.code=${entity.code}&isProj=true';
			ec.scheduler.datasource.editDlg = new CUI.Dialog({
				title: "${getText('ec.project.view_setting')}",
				modal:true,
				width : 550,
				height : 330,
				url:url,
				type:4,
				buttons:[{	name:"${getText('common.button.save')}",
					handler:function(){
						$("#SubmitschedulerDatasourceForm").attr('action', '/msService/ec/scheduler/datasourceSave?isProj=true');
						$("#SubmitschedulerDatasourceForm").submit();
					}
				},
				{	name:"${getText('ec.scheduler.datasource.publish')}",
					handler:function(){
						$("#SubmitschedulerDatasourceForm").attr('action', '/msService/ec/scheduler/datasourceSave?isProj=true');
						$("#SubmitschedulerDatasourceForm").submit();
					}
				},
				{	name:"${getText('ec.common.cancel')}",
					handler:function(){
						this.close();
					}
				}]
			});
			ec.scheduler.datasource.editDlg.show();
		}else{
			if(row.isShadow) {
				CUI.Dialog.alert("${getText('ec.scheduler.datasource.shadowViewNotEdit')}");
				return;
			} 
			if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
				window.open('/msService/ec/printManage/edit?isProj=true&printTemplate.code=' + row.code + '&entity.code=' + row.entity.code,'ecviewwin','height=800, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, status=no');
			} else {
				openFullScreen('/msService/ec/printManage/edit?isProj=true&printTemplate.code=' + row.code + '&entity.code=' + row.entity.code);
			}
		}
	}
	function _dialog(title,url,callback,width,height){
		return new CUI.Dialog({
			title : title,
			width :550,
			height :250,
			modal : true,
			url : url,
			buttons:[
				{	name:"${getText('ec.scheduler.datasource.testConnection')}",
					handler:function(){
						datasourceTestConnection(true);
					}
				},
				{	name:"${getText('common.button.save')}",
					handler:function(){callback(this);}
				},
				{	name:"${getText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
	}
	
	 /**
	 * 数据源添加
	 * @method ec.scheduler.datasource.add
	 */
	ec.scheduler.datasource.editDlg;
	ec.scheduler.datasource.add=function(){
		var dlg = ec.scheduler.datasource.editDlg=_dialog("${getText('ec.scheduler.datasource.addview')}","/msService/ec/scheduler/addDatasource?moduleCode=${entity.module.code}&artifact=${entity.module.artifact}&isProj=true",function(d){
			$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.moduleCode']").val("${entity.module.code}");
			var code = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']").val();
			if (ec.scheduler.datasource.requiredVerification()) {
				if(ec.scheduler.datasource.datasourceValidate()){
					if(querySchedulerDatasourceCodeUnique(code)){
						var loading = new CUI.loading({  
						"head": "数据处理中, 请稍等",  
						"opacity":50,  
						"bgColor":"#666666",  
						"show": true                  
						}); 
						CUI.ajax({
							data : {"schedulerDatasource.datasourceType":$("#SubmitschedulerDatasourceForm select[name='schedulerDatasource.datasourceType']").val(),
									"schedulerDatasource.datasourceAddress":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']").val(),
									"schedulerDatasource.datasourceName":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']").val(),
									"schedulerDatasource.port":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']").val(),
									"schedulerDatasource.username":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']").val(),
									"schedulerDatasource.password":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']").val(),
									"isProj":true
									},
							url: "/msService/ec/scheduler/datasourceTestConnection",
							type: 'post',
							async: true,
							success : function(msg){
								//关闭遮罩  
								loading.close();
								if(msg && msg.success){
									$("#SubmitschedulerDatasourceForm").attr('action', '/msService/ec/scheduler/datasourceSave?moduleCode=${entity.module.code}&isProj=true');
									$("#SubmitschedulerDatasourceForm").submit();
								}else{
									CUI.Dialog.alert("${getText('ec.scheduler.connection.datasource.failure')}");
									return false;
								}
							}		
						});
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
	 * 数据源信息修改
	 * @method ec.scheduler.datasource.edit
	 * @public
	 */
	ec.scheduler.datasource.edit=function(){
		if(datatable_ec_scheduler_datasource_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.datasource.choice.edit')}");
			return;
		}
		var dlg = ec.scheduler.datasource.editDlg = _dialog("${getText('ec.scheduler.datasource.editview')}", "/msService/ec/scheduler/modifyDatasource?schedulerDatasource.moduleCode=${entity.module.code}&artifact=${entity.module.artifact}&isProj=true&schedulerDatasource.code="+datatable_ec_scheduler_datasource_datatable.selectedRows[0].code,function(d){
				$("#SubmitschedulerDatasourceForm input[name='printTemplate.entity.code']").val("${entity.code}");
				if(ec.scheduler.datasource.requiredVerification()){
					if(ec.scheduler.datasource.datasourceValidate()){
						var loading = new CUI.loading({  
						"head": "数据处理中, 请稍等",  
						"opacity":50,  
						"bgColor":"#666666",  
						"show": true                  
						}); 
						CUI.ajax({
							data : {"schedulerDatasource.datasourceType":$("#SubmitschedulerDatasourceForm select[name='schedulerDatasource.datasourceType']").val(),
									"schedulerDatasource.datasourceAddress":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']").val(),
									"schedulerDatasource.datasourceName":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']").val(),
									"schedulerDatasource.port":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']").val(),
									"schedulerDatasource.username":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']").val(),
									"schedulerDatasource.password":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']").val(),
									"isProj":true
									},
							url: "/msService/ec/scheduler/datasourceTestConnection",
							type: 'post',
							async: true,
							success : function(msg){
								//关闭遮罩  
								loading.close();
								if(msg && msg.success){
									$("#SubmitschedulerDatasourceForm").attr('action', '/msService/ec/scheduler/datasourceSave?moduleCode=${entity.module.code}&isProj=true');
									$("#SubmitschedulerDatasourceForm").submit();
								}else{
									CUI.Dialog.alert("${getText('ec.scheduler.connection.datasource.failure')}");
									return false;
								}
							}		
						});
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
	 * 数据源删除
	 * @method ec.scheduler.datasource.del
	 * @public
	 */
	 ec.scheduler.datasource.deleteMethod = function(url) {
		$.ajax({
			//data : {"view.code":datatable_ec_scheduler_datasource_datatable.selectedRows[0].code,
			//		"view.version":datatable_ec_scheduler_datasource_datatable.selectedRows[0].version
			//},
			url : url , 
			success : function(subInfos){
				if(undefined != subInfos && null != subInfos && subInfos != "" && subInfos.flag){
				
					datatable_ec_scheduler_datasource_datatable.setRequestDataUrl("/msService/ec/scheduler/getDataSourceList?isProj=true&moduleCode=${entity.module.code}");
					CUI.Dialog.alert("${getText('common.delete.success')}");
				}else{
					if(subInfos && subInfos.jobStr){CUI.Dialog.alert("${getText('ec.scheduler.datasource.datasource')}"+subInfos.jobStr+"${getText('ec.scheduler.datasource.scheduler')}");}
					else{CUI.Dialog.alert("${getText('common.delete.failure')}");}
				}
			}	
		});
	}
	ec.scheduler.datasource.del=function(){
		if(datatable_ec_scheduler_datasource_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.scheduler.datasource.choice.del')}");
			return;
		}
		CUI.Dialog.confirm(  
				"${getHtmlText('ec.scheduler.datasource.deleteConfirm')}",  // 提示消息  
				function(){	ec.scheduler.datasource.deleteMethod("/msService/ec/scheduler/datasourceDelete?isProj=true&schedulerDatasource.code=" +
						datatable_ec_scheduler_datasource_datatable.selectedRows[0].code + "&moduleCode=${entity.module.code}");
						//CUI.Dialog.closeAll();
						//$("#load_mask").css(
						//{'z-index':0,
						//'block':'none'});
						} // 确定后事件   
			);
	}
	
	ec.scheduler.datasource.enableCallBackInfo=function(res){
		if(res.success){
			ec.scheduler.datasource.configDialog.close();
			CUI.Dialog.alert("${getHtmlText('ec.engine.view.dealsuccess')}");
			datatable_ec_scheduler_datasource_datatable.setRequestDataUrl("/msService/ec/scheduler/getDataSourceList?isProj=true&moduleCode=${entity.module.code}");
		}
	}
	/**
	 * 数据源信息提交返回信息及回列表
	 * @method ec.scheduler.datasource.callBack
	 * @public
	 SubmitschedulerDatasourceFormDialogErrorBar
	 */
	ec.scheduler.datasource.callBack=function(msg){
		if(msg && msg.success){
			if(window.SubmitschedulerDatasourceFormDialogErrorBarWidget){
				SubmitschedulerDatasourceFormDialogErrorBarWidget.show("${getText('ec.scheduler.datasource.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.scheduler.datasource.editDlg.close();}catch(e){}
					datatable_ec_scheduler_datasource_datatable.setRequestDataUrl("/msService/ec/scheduler/getDataSourceList?isProj=true&moduleCode=${entity.module.code}");
				},1500);
			}else if(window.SubmitschedulerDatasourceFormDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.scheduler.datasource.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.scheduler.datasource.inheritconfigdialog.close();}catch(e){}
					datatable_ec_scheduler_datasource_datatable.setRequestDataUrl("/msService/ec/scheduler/getDataSourceList?isProj=true&moduleCode=${entity.module.code}");
				},1500);
			}
		}else{
			if(window.SubmitschedulerDatasourceFormDialogErrorBarWidget){
    	        SubmitschedulerDatasourceFormDialogErrorBarWidget.show("${getText('ec.scheduler.datasource.submitfailure')}");
			} else if (window.ec_view_inheritconfig_formDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.scheduler.datasource.submitfailure')}");
			}
			setTimeout(function(){
				try{ec.scheduler.datasource.editDlg.close();}catch(e){}
				foundation.workbench.refresh();
			},1500);
        }
	}
})();
	
</script>