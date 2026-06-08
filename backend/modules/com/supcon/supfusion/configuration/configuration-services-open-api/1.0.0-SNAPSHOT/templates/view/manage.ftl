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
		<@datatable hidekey="['code','entity.code','version','isShadow']" transMethod="post" dtPage="page" id="ec_view_datatable" dataUrl="/msService/ec/view/list?entity.code=${entity.code}" dblclick="ec.view.config" paginator=false>
			<@operatebar operates="code:ec_view_btn_add||name:${getText('ec.view.addview')}||iconcls:add||onclick:ec.view.add();
			    code:ec_view_btn_copy||name:${getText('ec.view.copyview')}||iconcls:copy||onclick:ec.view.copy();
				code:ec_view_btn_mod||iconcls:ec-view-modify-view||name:${getText('ec.view.editview')}||onclick:ec.view.mod();
				code:ec_view_btn_config||iconcls:ec-view-config-view||name:${getText('ec.view.config')}||onclick:ec.view.config.button();
				code:ec_view_btn_mobile||iconcls:ec-view-config-mobile-view||name:${getText('ec.view.config.mobile')}||onclick:ec.view.configMobile();
				code:ec_view_btn_remove_mobile||iconcls:ec-view-remove-mobile-view||name:${getText('ec.view.config.remove.mobile')}||onclick:ec.view.removeMobile();
				code:ec_view_btn_del||iconcls:del||name:${getText('ec.view.delview')}||onclick:ec.view.del();
				code:ec_view_btn_restore||iconcls:publishlist||name:${getText('ec.view.publishrecord')}||onclick:ec.view.restoreList()"
				  operateType="noPower" resultType="json">
			</@operatebar>
			<@datacolumn key="name" label="${getText('ec.view.name')}" width=150 />
			<@datacolumn key="titleInternational" label="${getText('ec.view.title')}" width=150 />
			<@datacolumn key="displayNameInternational" label="${getText('ec.view.displayName')}" width=150 />
			<@datacolumn key="type" label="${getText('ec.view.type')}" width=80 />
			<@datacolumn key="showType" type="select" options="{'SINGLE':'${getText('ec.view.single')}','PART':'${getText('ec.view.part')}','LAYOUT':'${getText('ec.view.layout')}','LAYOUT2':'${getText('ec.view.layout2')}'}" label="${getText('ec.view.pagetype')}" width=60 />
			<@datacolumn textalign="center" type="boolean" key="usedForWorkFlow" label="${getText('ec.view.usedForWorkFlow')}" width=60 />
			<@datacolumn textalign="center" type="boolean" key="mainRef" label="${getText('ec.view.mainRef')}" width=60 />
			<@datacolumn textalign="center" type="boolean" key="projFlag" label="${getText('ec.view.beInherited')}" width=80 />
			<@datacolumn textalign="center" type="boolean" key="existMobileConfig" label="${getText('ec.view.config.mobile')}" width=80 />
			<@datacolumn textalign="center" type="boolean" key="customFlag" label="${getText('ec.view.customView')}" width=60 />
			<#-- @datacolumn key="editViewType" type="select" options="{'0':'${getText('ec.view.viewtype.Extra0')}','1':'${getText('ec.view.viewtype.Extra1')}'}" label="${getText('ec.view.isExtra')}" width=60 /> -->
			<@datacolumn key="description" label="${getText('ec.view.description')}" width=200 />
		</@datatable>
	</@frame>
</@frameset>
<div id="ec_view_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div>
	${getText('ec.entity.checkDeleteView')}
	</div>
</div>
</div>
<script type="text/javascript">
(function(){
	CUI.ns("ec.view"); 
	var fastQuerySetting_dialog;
	ec.view.config = function(evt,row){
		if(row.type==='DIGEST'){
			var url = '/msService/ec/view/config?view.code=' + row.code + '&entity.code=' + row.entity.code;
			fastQuerySetting_dialog = new CUI.Dialog({
				title: "${getText('ec.view.digest.config')}",
				modal:true,
				url:url,
				type:4,
				buttons:[{	name:"${getText('ec.flow.save')}",
					handler:function(){
						if(digestec.save()){
							$(this._el).html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("ec.view.loading")}</label></td></tr></table>');
							CUI(this._el).load(url,null,function(){
								closeLoadPanel();
							});
						}
					}
				},
				{	name:"${getText('ec.flow.cancal')}",
					handler:function(){
						this.close();
					}
				}]
			});
			fastQuerySetting_dialog.show();
		}else{
			if(row.isShadow) {
				CUI.Dialog.alert("${getText('ec.view.shadowViewNotEdit')}");
				return;
			} 
			if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
				window.open('/msService/ec/view/config?view.code=' + row.code + '&entity.code=' + row.entity.code,'ecviewwin','height=800, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=no,location=no, status=no');
			} else {
				openFullScreen('/msService/ec/view/config?view.code=' + row.code + '&entity.code=' + row.entity.code);
			}
		}
	}
	
	ec.view.config.button = function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.choiceview')}");
			return;
		}
		ec.view.config(null, datatable_ec_view_datatable.selectedRows[0]);
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
	  ec.view.editDlg;
	ec.view.add=function(){
		var dlg = ec.view.editDlg=_dialog("${getText('ec.view.addview')}","/msService/ec/view/edit?entity.code=${entity.code}",function(d){
			$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
				if(ec.view.requiredVerification() && ec.view.checkBatchControlPrint()){
					if(!ec.view.requiredShadowView()) {
						return false;
					}
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
		var dlg = ec.view.editDlg=_dialog("${getText('ec.view.copyview')}","/msService/ec/view/copyInit?srcView.code="+datatable_ec_view_datatable.selectedRows[0].code,function(d){
			$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
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
		var view = datatable_ec_view_datatable.selectedRows[0];
		if(!view.existMobileConfig){
			CUI.Dialog.alert("${getText('ec.view.notconfig.mobile')}");
			return;
		}
		CUI.Dialog.confirm("${getText('common.button.suredelete')}", function(){
			closeLoadPanel();
			createLoadPanel(false, null, {head: '正在处理，请稍等', show: true, opacity:(50), bgColor:("#666666")});
			setTimeout(function(){
			$.ajax({
				data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code + "__mobile__"},
				url : '/msService/ec/view/delete' ,
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success){
						CUI.Dialog.alert("${getText('common.delete.success')}");
						datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/view/list?entity.code=${entity.code}");
					}else{
						if(msg && msg.exceptionMsg){alert(msg.exceptionMsg);}
						else{CUI.Dialog.alert("${getText('common.delete.failure')}");}
					}
				}	
			});
			},1500);
		});
	}
	var configMobileButton = false;
	// 移动视图配置
	ec.view.configMobile=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.view.choiceview')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows[0].isShadow) {
			CUI.Dialog.alert("${getText('ec.view.shadowViewNotEdit')}");
			return;
		}
		if(datatable_ec_view_datatable.selectedRows[0].type != "VIEW" && datatable_ec_view_datatable.selectedRows[0].type != "EDIT" && datatable_ec_view_datatable.selectedRows[0].type != "LIST" && datatable_ec_view_datatable.selectedRows[0].type != "REFERENCE"||datatable_ec_view_datatable.selectedRows[0].showType=="LAYOUT2"||datatable_ec_view_datatable.selectedRows[0].showType=="LAYOUT"){
			CUI.Dialog.alert("${getText('ec.view.notsupport.mobile')}");
			return;
		}

				
		if(configMobileButton) {
			return;
		}
		configMobileButton = true;
		var viewType = datatable_ec_view_datatable.selectedRows[0].type;
		if(viewType == 'EDIT' || viewType == 'VIEW' || viewType == 'LIST' || viewType == 'REFERENCE' || viewType == 'EXTRA' ){
			$.ajax({
				data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code},
				url : '/msService/ec/view/configMobileInit',
				success : function(msg){
					if(msg && msg.success){
						ec.view.config(null, {"code" : datatable_ec_view_datatable.selectedRows[0].code + "__mobile__", "entity" : {"code" : "${entity.code}"}});
						datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/view/list?entity.code=${entity.code}");
					}
				}
					
			});
		}
		configMobileButton = false;
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
	var dlg = ec.view.editDlg = _dialog("${getText('ec.view.editview')}","/msService/ec/view/edit?view.code="+datatable_ec_view_datatable.selectedRows[0].code,function(d){
		$("#ec_view_edit_form input[name='view.entity.code']").val("${entity.code}");
			if (ec.view.nameValidate()) {
				if(ec.view.requiredVerification() && ec.view.checkBatchControlPrint()){
					if(!ec.view.requiredShadowView()) {
						return false;
					}
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
	 * 视图删除   add by yubo20171221
	 * @method ec.view.del
	 * @public
	 */
	ec.view.del=function(){
		if(datatable_ec_view_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getText('ec.view.choiceviewdel')}");
			return;
		}
		
		ec.view.dialog = new CUI.Dialog({
				title: "${getText('ec.common.delChoise')}",
				elementId: "ec_view_manage_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
						//{	name:"${getText('ec.common.void')}",
						//	handler:function(){
						//		this.close();
						//		ec.view.deleteMethod("/msService/ec/view/ordinaryDelete");
						//	
						//	}
						//},
						
						{	name:"${getText('ec.view.delete')}",
							handler:function(){
								this.close();
								ec.view.deleteMethod("/msService/ec/view/delete");
							
							}
						},
						{	name:"${getText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.view.dialog.show();
		
	}
	
	ec.view.deleteMethod = function(url) {
		
		closeLoadPanel();
		createLoadPanel(false, null, {head: '正在处理，请稍等', show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"view.code":datatable_ec_view_datatable.selectedRows[0].code,
						"view.version":datatable_ec_view_datatable.selectedRows[0].version
				},
				url : url , 
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success && (!msg.exceptionMsg||msg.exceptionMsg=="")){
						CUI.Dialog.alert("${getText('common.delete.success')}");
						datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/view/list?entity.code=${entity.code}");
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
						} else {
							CUI.Dialog.alert("${getText('common.delete.failure')}");
						}
					}
				}	
			});	
	
		},1500);
	}
	
	/**
	 * 视图信息提交返回信息及回列表
	 * @method ec.view.callBack
	 * @public
	 ec_view_edit_formDialogErrorBar
	 */
	ec.view.callBack=function(msg){
		if(msg && msg.success){
			ec_view_edit_formDialogErrorBarWidget.show("${getText('ec.view.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.view.editDlg.close();}catch(e){}
				datatable_ec_view_datatable.setRequestDataUrl("/msService/ec/view/list?entity.code=${entity.code}");
			},1500);
			
		}else{
            ec_view_edit_formDialogErrorBarWidget.show("${getText('ec.view.submitfailure')}");
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
			var url='/msService/ec/view/remindBackupViewList?code=' + datatable_ec_view_datatable.selectedRows[0].code;
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
	
})();
function setScript(objArr){

	var code =objArr[0].scriptCode;
	CUI('#scriptCode').val(code);
}
</script>