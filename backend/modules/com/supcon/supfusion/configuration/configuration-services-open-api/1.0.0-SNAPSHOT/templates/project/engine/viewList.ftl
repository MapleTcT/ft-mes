<@frameset id="ec_view_manage">
	<@loadpanel />
	<#--
	<@frame id="ec_view_manage_top" region="north" height=50>
		<div class="cui-main-title">
			<div class="cui-main-title-r">
				<@loadpanel text="${getText('ec.project.view_setting')}" />
				<@operatebar operates="code:ec_view_btn_add||name:${getText('ec.view.addview')}||iconcls:add||onclick:ec.view.add();
					code:ec_view_btn_mod||iconcls:edit||name:${getText('ec.view.editview')}||onclick:ec.view.mod();
					code:ec_view_btn_del||iconcls:del||name:${getText('ec.view.delview')}||onclick:ec.view.del();
					code:ec_view_btn_restore||iconcls:publishlist||name:${getText('ec.view.publishrecord')}||onclick:ec.view.restoreList()"
					  operateType="noPower">
				</@operatebar>
			</div>
		</div>
	</@frame>
	-->
	<@frame id="ec_view_manage_main" offsetH=4>
		<@datatable hidekey="['code','entity.code','version','isShadow','modifyTime']" transMethod="post" dtPage="viewPage" id="ec_view_datatable" dataUrl="/msService/ec/engine/getviews?entity.code=${entity.code}" dblclick="ec.view.config" onclick="ec.view.onclick"  paginator=false>
			<@operatebar operates="code:ec_view_btn_add||name:${getText('ec.view.addview')}||iconcls:add||onclick:ec.view.add();
				code:ec_view_btn_inherit||name:${getText('ec.engine.inheritview')}||iconcls:eighteen-dt-extend-view||onclick:ec.view.inherit();
				code:ec_view_btn_copy||name:${getText('ec.view.copyview')}||iconcls:copy||onclick:ec.view.copy();
				code:ec_view_btn_mod||iconcls:ec-view-modify-view||name:${getText('ec.view.editview')}||onclick:ec.view.mod();
				code:ec_view_btn_start||name:${getHtmlText('ec.businessCenter.StartUse')}||iconcls:eighteen-dt-op-enable||onclick:ec.view.start();
				code:ec_view_btn_stop||name:${getHtmlText('ec.businessCenter.Stop')}||iconcls:eighteen-dt-stop||onclick:ec.view.stop();
				code:ec_view_btn_del||iconcls:del||name:${getText('ec.view.delview')}||onclick:ec.view.del();
				code:ec_view_btn_restore||iconcls:publishlist||name:${getText('ec.view.publishrecord')}||onclick:ec.view.restoreList()"
				  operateType="noPower" resultType="json">
			</@operatebar>
			<@datacolumn textalign="center" label="" checkall="true"  key="checkbox" type="checkbox" width=60  />
			<@datacolumn key="inheritType" type="select" defaultDisplay="${getText('ec.engine.enginenew')}" options="{1:'${getText('ec.engine.halfinherit')}',2:'${getText('ec.engine.totalinherit')}'}" label="${getText('ec.engine.source')}" width=100 />
			<@datacolumn key="name" label="${getText('ec.view.name')}" width=150 />
			<@datacolumn key="titleInternational" label="${getText('ec.view.title')}" width=150 />
			<@datacolumn key="displayNameInternational" label="${getText('ec.view.displayName')}" width=150 />
			<@datacolumn key="publishTime" type="datetime" label="${getText('ec.view.createTime')}" width=130 />
			<@datacolumn key="publishstste" label="${getText('ec.engine.publishstste')}" width=100  showFormatFunc="publishststeFunc"/>
			<@datacolumn textalign="center" key="projEnabled" type="select" label="${getHtmlText('ec.model.startup')}" options="{'true':'${getText('ec.businessCenter.StartUse')}','false':'${getText('ec.businessCenter.Stop')}'}" width=60 />
			<@datacolumn key="type" label="${getText('ec.view.type')}" width=50 />
			<@datacolumn key="showType" type="select" options="{'SINGLE':'${getText('ec.view.single')}','PART':'${getText('ec.view.part')}','LAYOUT':'${getText('ec.view.layout')}','LAYOUT2':'${getText('ec.view.layout2')}'}" label="${getText('ec.view.pagetype')}" width=60 />
			<#--<@datacolumn key="editViewType" type="select" options="{'0':'${getText('ec.view.viewtype.Extra0')}','1':'${getText('ec.view.viewtype.Extra1')}'}" label="${getText('ec.view.isExtra')}" width=60 />-->
			<@datacolumn textalign="center" type="boolean" key="usedForWorkFlow" label="${getText('ec.view.usedForWorkFlow')}" width=60 />
			<@datacolumn textalign="center" type="boolean" key="mainRef" label="${getText('ec.view.mainRef')}" width=60 />
			<@datacolumn key="vieworigin" label="${getText('ec.engine.vieworigin')}" width=60  showFormatFunc="vieworiginFunc"/>
			<@datacolumn key="description" label="${getText('ec.view.description')}" width=200 />
		</@datatable>
	</@frame>
</@frameset>
<div id="ec_view_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div>
	${getText('ec.engine.checkDeleteView')}
	</div>
</div>
</div>
<script type="text/javascript">
(function(){
	CUI.ns("ec.view");
	var fastQuerySetting_dialog;
	ec.view.config = function(evt,row){
		if(row.inheritType==1){
			var url = '/msService/ec/engine/halfInheritConfig?view.code=' + row.code + '&entity.code=' + row.entity.code;
			ec.view.editDlg = new CUI.Dialog({
				title: "${getText('ec.project.view_setting')}",
				modal:true,
				url:url,
				type:4,
				buttons:[{	name:"${getText('common.button.save')}",
					handler:function(){
						ec.view.savedataprepare();
						$("#ec_view_halfinheritconfig_form").submit();
					}
				},
				{	name:"${getText('ec.view.publish')}",
					handler:function(){
						$("#ec_view_halfinheritconfig_form").attr('action', '/msService/ec/engine/inheritConfigPublish');
						ec.view.savedataprepare();
						$("#ec_view_halfinheritconfig_form").submit();
					}
				},
				{	name:"${getText('ec.common.cancel')}",
					handler:function(){
						this.close();
					}
				}]
			});
			ec.view.editDlg.show();
		}else{
			if(row.isShadow) {
				CUI.Dialog.alert("${getText('ec.view.shadowViewNotEdit')}");
				return;
			}
			if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
				window.open('/msService/ec/view/config?isProj=true&view.code=' + row.code + '&entity.code=' + row.entity.code,'ecviewwin','height=800, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, status=no');
			} else {
				openFullScreen('/msService/ec/view/config?isProj=true&view.code=' + row.code + '&entity.code=' + row.entity.code);
			}
		}
	}
	ec.view.onclick=function(evt){
		if(evt.srcElement.tagName=="TD"){
			var rowIndex=evt.srcElement.parentElement.rowIndex;
		}else{
			var rowIndex=evt.srcElement.parentElement.parentElement.rowIndex;
		}
		var inheritType=datatable_ec_view_datatable.getCellValue(rowIndex,"inheritType");
		if(inheritType==1){
			$("#ec_view_btn_restore").hide();
		}else{
			$("#ec_view_btn_restore").show();
		}
	}
	ec.view.savedataprepare=function(){
		$("#beforeSaveStr").val($("#beforeSave").val());
		$("#afterSaveStr").val($("#afterSave").val());
		$("#beforeSubmitStr").val($("#beforeSubmit").val());
		$("#afterSubmitStr").val($("#afterSubmit").val());
		if($("#dataGrid_public_event")){
			$("#listptrendovereventhidden").val($("#dataGrid_public_event").val());
		}
		if($("#dataGrid_init_event")){
			$("#listptiniteventhidden").val($("#dataGrid_init_event").val());
		}
		if($("#dataGrid_dbclick_event")){
			$("#listptdbclickeventhidden").val($("#dataGrid_dbclick_event").val());
		}
		$("textarea[id^='dg_renderover_area_']").each(function(){
			document.getElementById("dgConfig_"+$(this).attr("dgcode")+"_renderOver_project").value=$(this).val();
		});
		$("textarea[id^='dg_init_area_']").each(function(){
			document.getElementById("dgConfig_"+$(this).attr("dgcode")+"_ptPageInit_project").value=$(this).val();
		});
		if($("#isFirstLoad",$("#tab_common"))){
			$("#ec_view_halfinheritconfig_form").append("<input type='hidden' name='isFirstLoad' value='"+$("#isFirstLoad",$("#tab_common")).is(":checked")+"'/>");
		}
		if($("#selectFirstRow",$("#tab_common"))){
			$("#ec_view_halfinheritconfig_form").append("<input type='hidden' name='selectFirstRow' value='"+$("#selectFirstRow",$("#tab_common")).is(":checked")+"'/>");
		}
		if($("#isExportExcel",$("#tab_common"))){
			$("#ec_view_halfinheritconfig_form").append("<input type='hidden' name='isExportExcel' value='"+$("#isExportExcel",$("#tab_common")).is(":checked")+"'/>");
		}
	}
	function _dialog(title,url,callback,width,height){
		return new CUI.Dialog({
			title : title,
			width : width?width:550,
			height : height?height:500,
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

	 /**
	 * 视图添加
	 * @method ec.view.add
	 */
	ec.view.add=function(){
		var dlg = ec.view.editDlg=_dialog("${getText('ec.view.addview')}","/msService/ec/view/edit?entity.code=${entity.code}&isProj=true",function(d){
			$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
				if(ec.view.requiredVerification() && ec.view.checkBatchControlPrint()&&ec.view.requiredViewmenu()){
					if(!ec.view.requiredShadowView()) {
						return false;
					}
					$("#ec_view_edit_form").attr('action', '/msService/ec/view/save?isProj=true');
					$("#ec_view_edit_form").submit();
				}else{
					return false;
				}
			}else{
				return false;
			}
		});
		dlg.show();
	}
	ec.view.copy=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.copy.srcview')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows[0].isShadow){
			CUI.Dialog.alert("${getText('ec.view.shadowViewNotCopy')}");
			return;
		}
		var dlg = ec.view.editDlg=_dialog("${getText('ec.view.copyview')}","/msService/ec/view/copyInit?isProj=true&srcView.code="+datatable_ec_view_datatable.selectedRows[0].code,function(d){
			$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
				$("#ec_view_edit_form").attr('action', '/msService/ec/view/copy?isProj=true');
				$("#ec_view_edit_form").submit();
			}else{
				return false;
			}
		}, '200px','180px');
		dlg.show();
	}

	// 去除移动视图配置
	ec.view.removeMobile=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.choiceview')}");
			return;
		}

		CUI.Dialog.confirm("${getText('common.button.suredelete')}", function(){
			var view = datatable_ec_view_datatable.selectedRows[0];
			datatable_ec_view_datatable.selectedRows[0].existMobileConfig
			if(view.existMobileConfig && view.existMobileConfig){
				$.ajax({
					data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code + "__mobile__"},
					url : '/msService/ec/view/delete' ,
					success : function(msg){
						if(msg && msg.success){
							CUI.Dialog.alert("${getText('common.delete.success')}");
							datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/view/list?entity.code=${entity.code}");
						}else{
							if(msg && msg.exceptionMsg){alert(msg.exceptionMsg);}
                            else{CUI.Dialog.alert("${getText('common.delete.failure')}");}
						}
					}
				});
			}
		});
	}
	// 移动视图配置
	ec.view.configMobile=function(){
		if(datatable_ec_view_datatable.selectedRows[0].isShadow) {
				CUI.Dialog.alert("${getText('ec.view.shadowViewNotEdit')}");
				return;
			}
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.choiceview')}");
			return;
		}
		var viewType = datatable_ec_view_datatable.selectedRows[0].type;
		if(viewType == 'EDIT' || viewType == 'VIEW' || viewType == 'LIST' || viewType == 'REFERENCE'){
			$.ajax({
				data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code},
				url : '/msService/ec/view/configMobileInit',
				success : function(msg){
					if(msg && msg.success){
						ec.view.config(null, {"code" : datatable_ec_view_datatable.selectedRows[0].code + "__mobile__", "entity" : {"code" : "${entity.code}"}});
					}
				}
			});
		}
	}
	/**
	 * 视图信息修改
	 * @method ec.view.mod
	 * @public
	 */
	ec.view.mod=function(){
	if(datatable_ec_view_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.view.choiceview')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows.length>1){
			// FIXME 国际化
				CUI.Dialog.alert('请选择一条记录修改');
			return;
		}
	var dlg = ec.view.editDlg = _dialog("${getText('ec.view.editview')}","/msService/ec/view/edit?isProj=true&view.code="+datatable_ec_view_datatable.selectedRows[0].code,function(d){
		$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
				if(ec.view.requiredVerification() && ec.view.checkBatchControlPrint()&&ec.view.requiredViewmenu()){
					if(!ec.view.requiredShadowView()) {
						return false;
					}
					$("#ec_view_edit_form").attr('action', '/msService/ec/view/save?isProj=true');
					$("#ec_view_edit_form").submit();
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
	 * 视图删除
	 * @method ec.view.del
	 * @public
	 */
	ec.view.del=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.view.choiceviewdel')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows.length>1){
		    CUI.Dialog.alert("${getText('ec.view.choiceviewdelonlyone')}");
        	return;
		}
        var inheritType= datatable_ec_view_datatable.selectedRows[0].inheritType;
		ec.view.dialog = new CUI.Dialog({
				title: "${getText('ec.common.delChoise')}",
				elementId: "ec_view_manage_del_div",
				modal:true,
				//type : 1,
				width:376,
				height:95,
				buttons:[
						{	name:"${getText('ec.view.delete')}",
							handler:function(){
								CUI.Dialog.toggleAllButton("ec_view_manage_del_div","true");
								ec.view.deleteMethod("/msService/ec/view/delete?isProj=true&&inheritType="+inheritType);
							}
						},
						{	name:"${getText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.view.dialog.show();

	}

	ec.view.deleteMethod = function(url) {

		ec.view.dialog.close();
		closeLoadPanel();
		createLoadPanel(false, null, {head: "${getText('foundation.modelmanagement.dealing')}", show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code,
						"view.version":datatable_ec_view_datatable.selectedRows[0].version
				},
				url : url ,
				success : function(msg){
					closeLoadPanel();
					//CUI.Dialog.toggleAllButton("ec_view_manage_del_div",false);
					if(msg && msg.success){
						CUI.Dialog.alert("${getText('common.delete.success')}");
						//ec.model.showModuleInfo(ec_model_Tree.getSelectedNodes()[0]);
						datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
					}else{
						if(msg && msg.exceptionMsg){
							var arr = JSON.parse(msg.exceptionMsg);
							if($('div#workbenchErrorBar').length == 0){
								$('div#layout-center').append("<div id='workbenchErrorBar'></div>")
							}
							var workbenchErrorBarWidget = new CUI.ErrorBar('workbenchErrorBar',{
							    offsetY:20
							});
	        				var str = '删除失败，存在依赖：';
	        				for (var i=0; i<arr.length; i++){
	        					str += '<li>' + arr[i] + '</li>';
	        				}
	        				workbenchErrorBarWidget.showMessage(str);
						}
						else{CUI.Dialog.alert("${getHtmlText('common.delete.failure')}");}
					}
				}
			});
		},1500);
	}


	/* CUI.Dialog.confirm = function(msg, yesCallback, noCallback, title, height, width){
		if( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ){
			return parent.CUI.Dialog.confirm(msg, yesCallback, noCallback, title, height, width);
		}
		width = width || 400;
		width = CUI('body').width() -10 > width  ? width : CUI('body').width() -10;
		var randomId = CUI.Dialog.createMsgElement(msg);
		CUI('#msgInfoDiv_' + randomId).width(width - 22 - 10);
		height = height || 70;
		if(height == 70) {
			height += CUI('#msgInfoDiv_' + randomId).height();
		}
		ec.view.configDialog = new CUI.Dialog({
			title: title || "${getText('ec.view.hint')}" ,
			elementId: "msgInfoDiv_" + randomId,
			modal:true,
			height: height,
			width: width,
			dragable:true,
			blankCloseEnable:true,
			enterCloseEnable:true,
			buttons:[{	name:"${getText('ec.common.confirm')}",
						handler:function(){
							if (yesCallback) {
								CUI.Dialog.toggleAllButton();
								CUI(_dialog).each(yesCallback);
							}
						}
					}, {name:"${getText('ec.common.cancel')}",
						handler:function(){
							if (noCallback) {
								CUI.Dialog.toggleAllButton();
								CUI(_dialog).each(noCallback);
							}
							ec.view.configDialog.close();
						}
					}]
		});
		ec.view.configDialog.show();
	}; */

	ec.view.start=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.business.SelectRow')}");
			return;
		}
		//if(datatable_ec_view_datatable.selectedRows[0].projEnabled==true){
			//CUI.Dialog.alert("${getText('ec.engine.view.aleadyenable')}");
			//return;
		//}
		CUI.Dialog.confirm("${getHtmlText('ec.engine.view.confirmenable')}", function(){
		var viewCodes="";
		for(var i=0;i<datatable_ec_view_datatable.selectedRows.length;i++){
            viewCodes+=datatable_ec_view_datatable.selectedRows[i].code+",";
		}
			CUI.post("/msService/ec/engine/changeEnabled?viewCodes="+viewCodes.substring(0,viewCodes.lastIndexOf(','))+"&enableFlag=true",ec.view.enableCallBackInfo, "json")
		});
	}

	ec.view.stop=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.business.SelectRow')}");
			return;
		}
		//if(datatable_ec_view_datatable.selectedRows[0].projEnabled==false){
			//CUI.Dialog.alert("${getText('ec.engine.view.aleadydisenable')}");
			//return;
		//}
		CUI.Dialog.confirm("${getHtmlText('ec.engine.view.confirmdisenable')}", function(){
		var viewCodes="";
        		for(var i=0;i<datatable_ec_view_datatable.selectedRows.length;i++){
                    viewCodes+=datatable_ec_view_datatable.selectedRows[i].code+",";
        		}

			CUI.post("/msService/ec/engine/changeEnabled?viewCodes="+viewCodes.substring(0,viewCodes.lastIndexOf(','))+"&enableFlag=false",ec.view.enableCallBackInfo, "json")
		});
	}

	ec.view.enableCallBackInfo=function(res){
		if(res.success){
			// ec.view.configDialog.close();
			CUI.Dialog.alert("${getHtmlText('ec.engine.view.dealsuccess')}");
			datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
		}
	}
	/**
	 * 视图信息提交返回信息及回列表
	 * @method ec.view.callBack
	 * @public
	 ec_view_edit_formDialogErrorBar
	 */
	ec.view.callBack=function(msg){
		if(msg && msg.success){
			if(window.ec_view_edit_formDialogErrorBarWidget){
				ec_view_edit_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.view.editDlg.close();}catch(e){}
					datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
				},1500);
			}else if(window.ec_view_inheritconfig_formDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
				setTimeout(function(){
					try{ec.view.inheritconfigdialog.close();}catch(e){}
					datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
				},1500);
			}
		}else{
			if(window.ec_view_edit_formDialogErrorBarWidget){
    	        ec_view_edit_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
			}else if(window.ec_view_inheritconfig_formDialogErrorBar){
				ec_view_inheritconfig_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
			}
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
				foundation.workbench.refresh();
				},1500);
        	}
	}

	ec.view.halfinheritcallBack=function(msg){
		if(msg && msg.success){
			ec_view_halfinheritconfig_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
				datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
			},1500);
		}else{
    	    ec_view_halfinheritconfig_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
					foundation.workbench.refresh();
				},1500);
        }
    }

    ec.view.inheritcallBack=function(msg){
		if(msg && msg.success){
			ec_view_inherit_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
				datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
			},1500);
		}else{
    	    ec_view_inherit_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
					foundation.workbench.refresh();
				},1500);
        }
    }

	ec.view.restoreList = function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.choicerecord')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows[0].isShadow) {
			CUI.Dialog.alert("${getText('ec.view.shadowHaveNoBackupView')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows[0]){
			var url='/msService/ec/view/remindBackupViewList?isProj=true&code=' + datatable_ec_view_datatable.selectedRows[0].code;
			CUI(function(){
				ec.view.remindBackupViewList = new CUI.Dialog({
					title: "${getText('ec.view.publishrecord')}",
					url:url,
					modal:true,
					height:390,
					width: 630,
					dragable:true,
					buttons:[{name:"${getText('ec.view.recovery')}",id:"restore",handler:function(){ec.view.restore();}},
							<#--{name:"${getText('ec.view.delete')}",id:"delBackupView",handler:function(){ec.view.delBackupView();}},
					         {name:"$(getText('ec.view.moreInfo'))",id:"preview",handler:function(){ec.view.preview();}},-->
					         {name:"${getText('ec.view.close')}",id:"close",handler:function(){this.close();}}]
				})
				ec.view.remindBackupViewList.show();
			});
		}else {
			workbenchErrorBarWidget.showMessage("${getText('ec.view.recoview')}");
		}
	}
	/**
	 * 视图继承
	 * @method ec.view.del
	 * @public
	 */
	ec.view.inherit= function(){
		ec.view.editDlg=_dialog("${getText('ec.engine.inheritview')}","/msService/ec/engine/inheritView?entity.code=${entity.code}",function(){
			CUI("#ec_view_inherit_form").submit();
				},'200px','180px');
				ec.view.editDlg.show();
			};
	})();

	publishststeFunc=function(value,nRow){
		var modifyTime= datatable_ec_view_datatable.getCellValue(nRow,"modifyTime");
		var publishTime= datatable_ec_view_datatable.getCellValue(nRow,"publishTime");
		if(!publishTime||publishTime==""){
			return "${getText('ec.engine.publishstste.not')}";
		}
		var publishDate=new Date(publishTime.replace(/-/g,"/"));
		if(Math.abs(publishDate.getTime()-modifyTime)<1000){
			return "${getText('ec.engine.publishstste.published')}";
		}else{
			return "${getText('ec.engine.publishstste.waittopublish')}";
		}
	}

	vieworiginFunc=function(value,nRow){
		var inheritType= datatable_ec_view_datatable.getCellValue(nRow,"inheritType");
		var viewcode= datatable_ec_view_datatable.getCellValue(nRow,"code");
		var isShadow= datatable_ec_view_datatable.getCellValue(nRow,"isShadow");
		var entitycode= datatable_ec_view_datatable.getCellValue(nRow,"entity.code");
		if(inheritType&&!isShadow){
			return '<a href="#" onclick="ec.view.originconfig(\''+viewcode+'\',\''+entitycode+'\')">${getText("ec.engine.clicktoview")}</a>';
		}else{
			return "";;
		}
	}
	ec.view.originconfig = function(viewcode,entitycode){
		if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
			window.open('/msService/ec/view/config-readonly?view.code=' + viewcode+"&entity.code=${entity.code}",'ecviewwin','height=800, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, status=no');
		} else {
			openFullScreen('/msService/ec/view/config-readonly?view.code=' + viewcode+"&entity.code=${entity.code}");
		}
	}
	ec.view.reloadDataGrid=function(){
		datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/engine/getviews?entity.code=${entity.code}");
	}
</script>