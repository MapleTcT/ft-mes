<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.inter.cfg')}</title>
<@maincss/>
<@mainjs/>
<@adpSkin />
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.elm-layout-doc-in-wrap .elm-layout-wrap-in-west{border-right:0px;}
#ec_module_entity_datatable{margin-top:10px!important;margin-bottom:3px!important;}
#ec_module_entity_datatable .helptip-customwrap {
	float: left;
	margin-right: 7px;
}
#ec_module_search_box{
	padding: 5px 0;
    width: inherit;
	background: #fff;
}
#left_in{
	overflow:hidden;
}
#ec_module_search_box{
	padding: 5px 0;
    width: inherit;
	background: #fff;
}
#ec_module_search_input_wrap{
    position: relative;
    width: calc(100% - 10px);
    height: 26px;
}
#ec_module_search_input_wrap .icon{
	position: absolute;
    right: 10px;
    top: 7px;
    width: 13px;
    height: 13px;
    background-image: url(/bap/static/new/img/search_all.png?t=20171208);
    background-position: -21px -5px;
}
#ec_module_search_input{
	border: 1px solid #ddd;
    outline: 0;
    background-color: transparent;
    height: 24px;
    box-sizing: border-box;
    margin-left: 10px;
    margin-top: 2px;
    width: inherit;
    line-height: 24px;
    border-radius: 12px;
	padding: 0 25px 0 10px;
}
#ec_module_search_list{
	position: absolute;
    top: 34px;
    background-color: #ffffff;
    width: 100%;
    z-index: 21;
    padding: 5px;
    box-sizing: border-box;
    border-bottom: 1px solid #d8d8d8;
    display: none;
    overflow: hidden;
	box-shadow:0px 3px 6px 0px rgba(0, 0, 0, 0.08);
}
#ec_module_search_list li{
	padding: 4px 0 4px 10px;
	cursor: pointer;
	overflow: hidden;
	white-space: nowrap;
    text-overflow: ellipsis;
}
#ec_module_search_list li:hover{
	background: #F7F7F7;
}
#ec_module_entity_datatable a.help-link {
	display: inline-block;
	width: 15px;
	height: 15px;
	background: url(/bap/struts/css/edit_20150318.png) 0px -3553px no-repeat;
}
#ec_module_Tree{
	overflow-y: auto;
    padding-top: 0;
	height:calc(100% - 40px);
}
</style>
<@frameset id="ec_module_manage">
	<@frame id="left_in" region="west" width=200 resize=true>
	<div id="ec_module_search_box">
                <div id="ec_module_search_input_wrap">
                    <span class="icon"></span>
                    <input type="text" placeholder="${getText('ec.module.placeholder')}" autocomplete="off" id="ec_module_search_input" oninput="moduleSearch();" onfocus="moduleSearch();" onblur="onSearchBlur();" onkeydown="return moduleKeyDown(event)">
                </div>
                <ul id="ec_module_search_list" onclick="searchClick(event)"></ul>
            </div>
		<@tree resizeHeight="false" id="ec_module_Tree" dataUrl="/msService/ec/engine/datalist" rootName="${getText('ec.module.List')}" nameCol="nameInternational"
			 callback="{onClick:function(event,treeId,node){runtime.module.showModuleInfo(node);}}" />
	</@frame>
	<@frame id="ec_module_manage_main" offsetH=4>
		<#assign colAdmFlag = checkUserPermisition('ec_ptManage_list_view')>
		<@datatable hidekey="['code','version']
		" firstLoad=false id="ec_module_entity_datatable" dataUrl="/msService/ec/engine/entities" dblclick="runtime.entity.dblclick" helpInfo=helpInfo >
			<@operatebar operates="code:ec_module_entity_btn_mod||name:${getHtmlText('ec.module.editEntity')}||iconcls:edit||onclick:runtime.entity.mod();
				code:ec_module_entity_btn_backup||name:${getHtmlText('ec.engine.backupEntity')}||iconcls:eighteen-dt-op-download-sourcecode||onclick:runtime.entity.backup();
				code:ec_module_entity_btn_backupAll||name:${getHtmlText('ec.engine.backupEntityAll')}||iconcls:eighteen-dt-bulk-export||onclick:runtime.entity.backupAll();
				code:ec_module_entity_btn_restore||name:${getHtmlText('ec.engine.restoreEntity')}||iconcls:eighteen-dt-op-upload-sourcecode||onclick:runtime.entity.restore();
				code:ec_module_entity_btn_publish||name:${getHtmlText('ec.module.publish')}||iconcls:eighteen-dt-op-publish||onclick:runtime.entity.publish();"
				operateType="noPower" resultType="json" >
			</@operatebar>
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=30 />
			<@datacolumn key="entityName" label="${getHtmlText('ec.entity.entityCode')}" width=100 />
			<@datacolumn key="nameInternational" label="${getHtmlText('ec.entity.entityName')}" width=150 />
			<@datacolumn key="prefix" label="${getHtmlText('ec.entity.prefix')}" width=120 />
			<@datacolumn textalign="center" type="boolean" key="workflowEnabled" label="${getHtmlText('ec.entity.workflowEnabled')}" width=120 />
			<@datacolumn textalign="center" type="boolean" key="groupEnabled" label="${getHtmlText('ec.entity.groupEnabled')}" width=120 />
			<@datacolumn textalign="center" type="boolean" key="isBase" label="${getHtmlText('ec.entity.isBase')}" width = 120/>
			<@datacolumn textalign="center" type="boolean" key="isInherentedBase" label ="${getText('ec.entity.isInherentedBase')}" width=120 />
			<@datacolumn textalign="center" type="boolean" key="crossCompanyFlag" label ="${getText('ec.entity.crossCompanyFlag')}" width=120 />
		</@datatable>
	</@frame>

</@frameset>
<script type="text/javascript" >
function moduleSearch(){
        			var input = document.getElementById("ec_module_search_input").value;
        			var filter = input.toUpperCase();
        			var search_result = '';
        			var times = 0;
        			var moduleData = ec_module_Tree.getNodesByFilter(function(node){
        				if(!node.isParent) return true;
        			});
        			if(input!=='') {
        				moduleData.forEach(function(data){
        					if((data.nameInternational+data.artifact).toUpperCase().indexOf(filter) > -1 && times < 6) {
        						++times;
        						search_result +='<li  data-code='+ data.code + '>'+ data.nameInternational +' ('+ data.artifact +')</li>';
        					}
        				})
        				if(search_result) {
        					$('#ec_module_search_list').show();
        					$('#ec_module_search_list').html(search_result);
        				} else {
        					document.getElementById("ec_module_search_list").innerHTML = '';
        					document.getElementById("ec_module_search_list").style.display = 'none';
        				}
        			} else {
        				document.getElementById("ec_module_search_list").innerHTML = '';
        				document.getElementById("ec_module_search_list").style.display = 'none';
        			}
        		}
        		function moduleKeyDown(e){
                        			var lis = $('#ec_module_search_list').children();
                        			// enter
                        			if (e && e.keyCode === 13) {
                        				var li = lis[0];
                        				for(var i=0;i<lis.length;i++){
                        					if(lis[i].isChecked ){
                        						li = lis[i];
                        						break;
                        					}
                        				}
                        				document.getElementById("ec_module_search_input").value = '';
                        				document.getElementById("ec_module_search_list").innerHTML = '';
                        				document.getElementById("ec_module_search_list").style.display = 'none';
                        				var node = ec_module_Tree.getNodeByParam("code", li.dataset.code);
                        				ec_module_Tree.selectNode(node);
                        				ec.module.showModuleInfo(node);
                        				document.getElementById("ec_module_search_input").focus();
                        			}
                        			// down
                        			if (e && e.keyCode === 40) {
                        				var index = -1;
                        				for(var i=0;i<lis.length;i++){
                        					if(lis[i].isChecked ){
                        						lis[i].style.background = 'none';
                        						lis[i].isChecked = false;
                        						index = i;
                        						break;
                        					}
                        				}
                        				if(index == lis.length-1) index = -1;
                        				if(index < lis.length -1){
                        					lis[index + 1].style.background = '#F7F7F7';
                        					lis[index + 1].isChecked = true;
                        				}
                        			}
                        			// up
                        			if (e && e.keyCode === 38) {
                        				var index = -1;
                        				for(var i=0;i<lis.length;i++){
                        					if(lis[i].isChecked){
                        						lis[i].style.background = 'none';
                        						lis[i].isChecked = false;
                        						index = i;
                        						break;
                        					}
                        				}
                        				if(index == 0) index = lis.length;
                        				if(index > 0){
                        					lis[index - 1].style.background = '#F7F7F7';
                        					lis[index - 1].isChecked = true;
                        				}
                        			}
                        		}

                        		function searchClick(e){
                        			var lis = $('#ec_module_search_list').children();
                        			for(var i=0;i<lis.length;i++){
                        				lis[i].style.background='none';
                        				lis[i].isChecked = false;
                        			}
                        			e.target.style.background='#F7F7F7';
                        			e.target.isChecked = true;
                        			document.getElementById("ec_module_search_input").value = '';
                        			document.getElementById("ec_module_search_list").innerHTML = '';
                        			document.getElementById("ec_module_search_list").style.display = 'none';
                        			var node = ec_module_Tree.getNodeByParam("code",e.target.dataset.code);
                        			ec_module_Tree.selectNode(node);
                        			runtime.module.showModuleInfo(node);
                        			document.getElementById("ec_module_search_input").focus();
                        		}

                        		function onSearchBlur(){
                        			setTimeout(function(){
                        				document.getElementById("ec_module_search_list").style.display = 'none';
                        			},200)
                        		}
(function(){
	//注册命名空间
	CUI.ns("runtime.module");
	CUI.ns("runtime.entity");
	var _dialog=function(title,url,callback,width,height){
		return new CUI.Dialog({
			title : title,
			width : width?width:550,
			height : height?height:330,
			modal : true,
			url : url,
			buttons:[
				{	name:"${getHtmlText('common.button.save')}",
					handler:function(){callback(this);}
				},
				{	name:"${getHtmlText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
	}
	/**
	 * 点击模块树节点，显示实体详细信息
	 * @method runtime.module.showModuleInfo
	 * @param {Node} oNode
	 * @public
	 */
	runtime.module.showModuleInfo = function(oNode){
		if(oNode && !oNode.id && !oNode.code) {//分类
			runtime.module.handleModuleCategory(oNode);
			return;
		}
		// 处理
		var code = '';
		if(oNode != null && oNode != 'undefined' ){
			code = oNode.code;
		}
		if (code != '' && ec_module_Tree.getSelectedNodes()[0].level>0) {
			$('span[id ^="ec_module_"]','#ec_module_entity_datatable').parent().attr('style',"");
			$('#buttonbar_more_button').show();
			var pageSize=CUI('input[name="ec_module_entity_datatable_PageLink_PageCount"]').val();
			datatable_ec_module_entity_datatable.setRequestDataUrl('/msService/ec/engine/entities?module.code='+oNode.code, (pageSize? 'pageSize='+pageSize : ''));
		}
	};

	runtime.module.handleModuleCategory = function(oNode) {
		var pageSize=CUI('input[name="ec_module_entity_datatable_PageLink_PageCount"]').val();
		datatable_ec_module_entity_datatable.setRequestDataUrl('/msService/ec/entity/list?module.code='+oNode.code, (pageSize? 'pageSize='+pageSize : ''));
		$('span[id ^="ec_module_"]','#ec_module_entity_datatable').parent().attr('style',"display:none");
		$('#buttonbar_more_button').hide();
		$('#ec_module_btn_batchdeploy').parent().show();
	}

	/**
	 * 模块信息修改
	 * @method runtime.module.mod
	 * @public
	 */
	runtime.module.mod=function(){
	if(ec_module_Tree.getSelectedNodes()[0]==null || ec_module_Tree.getSelectedNodes()[0].level==0){
			CUI.Dialog.alert("${getHtmlText('ec.module.edit.alert')}");
			return;
		}
	if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true){
			CUI.Dialog.alert("${getHtmlText('ec.module.modifyalert')}");
			return;
	}
	var dlg=runtime.module.editDlg = _dialog("${getHtmlText('ec.module.edit')}","/ec/module/edit.action?module.code="+ ec_module_Tree.getSelectedNodes()[0].code,function(){
			$("#ec_module_edit_form").submit();
		});
		dlg.show();
	}

	runtime.module.refresh=function(){
		if (ec_module_Tree.getSelectedNodes()[0] != null && ec_module_Tree.getSelectedNodes()[0].level > 0) {
			runtime.module.showModuleInfo(ec_module_Tree.getSelectedNodes()[0]);
		}else{
			foundation.workbench.refresh();
		}
	}

	/**
	 * 模块信息提交返回信息及回刷列表
	 * @method runtime.module.addCallback
	 * @public
	 */
	runtime.module.addCallback=function(msg){
		if(msg && msg.success){
			ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitsuccessful')}","s");
			try{runtime.module.editDlg.close();}catch(e){}
		}else{ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitfailure')}");}

	}
	runtime.module.refresh=function(){
		if (ec_module_Tree.getSelectedNodes()[0] != null && ec_module_Tree.getSelectedNodes()[0].level > 0) {
						runtime.module.showModuleInfo(ec_module_Tree.getSelectedNodes()[0]);
		}else{
			foundation.workbench.refresh();
		}
	}
	runtime.module.refreshModuleTree=function(){
		var rootNode = ec_module_Tree.getNodeByParam("id", -1);
		ec_module_Tree.reAsyncChildNodes(rootNode, "refresh");
	}
	/**
	 * 实体信息修改
	 * @method runtime.entity.mod
	 * @public
	 */
	runtime.entity.mod=function(){
		if(datatable_ec_module_entity_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceEntityEdit')}");
			return;
		}
		if(ec_module_entity_datatableWidget.selectedRows.length > 1){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselectedmore')}", "f");
			return;
		}
		var moduleCode=ec_module_Tree.getSelectedNodes()[0].code;
		runtime.entity.editDlg =new CUI.Dialog({
			title : "${getHtmlText('ec.module.editEntity')}",
			width : 550,
			height : 450,
			modal : true,
			url : "/msService/ec/engine/view?moduleCode="+moduleCode+"&entity.code="+datatable_ec_module_entity_datatable.selectedRows[0].code+"&isView=true",
			buttons:[
				{	name:"${getHtmlText('common.button.save')}",
					handler:function(){
						if (runtime.entity.codeValidate()){
							$("#ec_entity_edit_form").submit();
						}
					}
				},
				{	name:"${getHtmlText('ec.view.close')}",
					handler:function(){this.close();}
				}]
		});
		runtime.entity.editDlg.show();
	}

	runtime.entity.dblclick=function(evt,row){
		if(ec_module_entity_datatableWidget.selectedRows.length > 1){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselectedmore')}", "f");
			return;
		}
		if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
			window.open('/msService/ec/engine/config?entity.code=' + row.code,'','height=700, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=yes,location=no, status=no');
		} else {
			window.open('/msService/ec/engine/config?entity.code=' + row.code);
		}
	}

	runtime.entity.addCallback=function(msg){
		if(msg && msg.success){
			ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitsuccessful')}","s");
			setTimeout(function(){
				try{runtime.entity.editDlg.close();}catch(e){}
				runtime.module.refresh();
			}
			,500);
		}else{ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitfailure')}");}

	}
	runtime.entity.backup=function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
			CUI.Dialog.alert("<span i18n='ec.engine.choiceModulePublish'>${getHtmlText('ec.engine.choiceModulePublish')}</span>");
		return;
	}
	var moduleCode = ec_module_Tree.getSelectedNodes()[0].code;
		new CUI.Dialog({
			title : "${getHtmlText('ec.module.packageMonitor')}",
			width : 600,
			height : 350,
			modal : true,
			html : '<iframe frameborder="0" scrolling="auto" width="100%" height="300" src="/msService/ec/engine/backup?module.code=' + moduleCode + '"></iframe>',
			buttons:[
				{	name:"${getHtmlText('common.button.Confirm')}",
					handler:function(){this.close();}
				}]
		}).show();
	}

	runtime.entity.restore=function(){
		if( !runtime.module.uploadWizardDialog ){
			runtime.module.uploadWizardDialog = new CUI.WizardDialog({
					title: "${getHtmlText('ec.module.uploadMonitor')}",
					size: 3,
					close: true, // 关闭后再打开时时重新开始
					steps :	[

							{
								iframe : 'wizar_iframe_step1',
								onload:function(){
									wizar_iframe_step1.$( '#changeFile' ).val( 'false' )
									$( '#dialog_btn_migrate', runtime.module.uploadWizardDialog.dialog._buttonbar ).attr('canclick','false').addClass( 'cui-btn-gray' );
								},
								url: '/msService/ec/engine/upload',
								buttons : [
									{
										name : "${getHtmlText('ec.module.upload.uploadbutton')}",
										id : 'dialog_btn_upload',
										handler : function(){
											if(wizar_iframe_step1.uploadForm.onsubmit()){
												CUI.Dialog.confirm(
													"${getHtmlText('ec.engine.restoreEntity.confirm')}",
													function(){
														var view3 = runtime.module.uploadWizardDialog.getView( 'proj_wizar_iframe_step2' );
														view3.show();
														view3.load();
														wizar_iframe_step1.uploadForm.target = "proj_wizar_iframe_step2";
														wizar_iframe_step1.uploadForm.action = "/msService/ec/engine/receive";
														wizar_iframe_step1.uploadForm.submit();
													},
													function(){},
													"",
													70,
													400
												);
											}
										}
									},
									{
										name : "${getHtmlText('ec.module.upload.cancelbutton')}",
										handler : function(){
											runtime.module.uploadWizardDialog.hide();
										}
									}

								]
							},
							{
								iframe : 'proj_wizar_iframe_step2',
								url: 'about:blank1',
								buttons : [

									{
										name : "${getHtmlText('ec.module.upload.closebutton')}",
										id : 'closeBtn',
										handler : function(){
											runtime.module.uploadWizardDialog.hide();
										}
									}
								]
							}
						]
			});
		}else{
			var view1 = runtime.module.uploadWizardDialog.getView( 'wizar_iframe_step1' );
			view1.url = view1._url = '/msService/ec/engine/upload';
			runtime.module.uploadWizardDialog.show();
		}
	}
})();
/*
工程期配置中备份复选的功能 2017.6.8 by zhushizhang
*/
 var ec_engine_backupEntity;
 runtime.entity.backupAll = function(){
 	setTimeout(function(){
 		$('#ec_module_entity_btn_backupAll').unbind('click').bind('click', function(e){
        ev = e || window.event;
        if (ev) { // 停止事件冒泡
            ev.stopPropagation();
        } else {
            window.event.cancelBubble = true;
        }///ec/engine/modules
        showOverLayer_ec_engine_backupEntity(this,"/msService/ec/engine/backupModule")
    })
 	},100)

$('body').click(
        function(){
            if(ec_engine_backupEntity && ec_engine_backupEntity.isShow) {
                ec_engine_backupEntity.hide();
            }

        }
    );
 }

function showOverLayer_ec_engine_backupEntity(obj,url){
	if( ec_engine_backupEntity ){
		ec_engine_backupEntity.show();
		return;
	}

ec_engine_backupEntity = new CUI.Overlay({
  align:obj,
    el:'ec_module_entity_btn_backup_form',
    title:"${getText('ec.module.List')}",
    width:180,
    height:282,
    zIndex:9999,
    shadow:false,
    alignCentertrue:true,
  buttons:[
      { name:"<span i18n='foundation.workbench.mainPage.sure'>${getText('foundation.workbench.mainPage.sure')}</span>",
        id:"ec_engine_backupEntitySubmit",
        handler:function(){runtime.entity.backupAll.submit()}
      },
      { name:"<span i18n='calendar.common.cancal'>${getText('calendar.common.cancal')}</span>",
        id:"ec_engine_backupEntityCancel",
        handler:function(){ec_engine_backupEntity.hide()}
      }]

});
ec_engine_backupEntity.render();
ec_engine_backupEntity._overlay.style.left=YUD.getX(obj)+20+'px';
$("#overlay-idec_module_entity_btn_backup_form").click(
		function(e){

			ev = e || window.event;
			if (ev) { // 停止事件冒泡
				ev.stopPropagation();
    		} else {
    			window.event.cancelBubble = true;
    		}
		}
	);
ec_engine_backupEntity.show();
$("#ec_module_entity_btn_backup_form").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
CUI('#ec_module_entity_btn_backup_form').load(url);
}
setTimeout(function(){
 		$('#ec_module_entity_btn_backupAll').trigger('click');
 	},100)

//提交按钮的提交事件 by zhushizhang
runtime.entity.backupAll.submit = function(){
var checkedValue='';
$("#module_backup input[name='ec.module']:checked").each(function(index){
    if($(this).attr("value")!=""){
        checkedValue+=','+$(this).attr("value");
    }
});
var moduleCode = checkedValue.substring(1);
//未选择模块的时候进行提醒
if(moduleCode==''){
		CUI.Dialog.alert("<span i18n='ec.module.choiceModule'>${getHtmlText('ec.module.choiceModule')}</span>!");
		return;
	}
new CUI.Dialog({
	title : "${getHtmlText('ec.module.packageMonitor')}",
	width : 600,
	height : 350,
	modal : true,
	html : '<iframe frameborder="0" scrolling="auto" width="100%" height="300" src="/msService/ec/engine/backup?module.code=' + moduleCode + '"></iframe>',
	buttons:[
		{	name:"${getHtmlText('common.button.Confirm')}",
			handler:function(){this.close();}
		}]
}).show();
ec_engine_backupEntity.hide();
}
runtime.entity.publish=function(){

	if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
		CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");
		return;
	}

	CUI.Dialog.confirm("${getText('foundation.infoSet.publishInfoSetComfirn')}", function(){
			runtime.entity.publishDialog=new CUI.Dialog({
					title : "${getHtmlText('ec.module.monitorDeploy')}",
					width : 600,
					height : 350,
					modal : true,
					elementId : 'generateSelectOutDiv',
					buttons:[
						{	name:"${getHtmlText('foundation.common.closed')}",
							handler:function(){this.close();}
						}]
				});
				$('#progressiveLog').empty();
                $('#progressiveLog').show();
                var moduleName = ec_module_Tree.getSelectedNodes()[0].nameInternational;
                $("#progressiveLog").append(moduleName + "${getHtmlText('开始发布')}"+"<br>");
				$.ajax({
                	url: '/msService/servicemanager/msModule/projgenerate',
                	//dataType: 'json',
                	type: 'POST',
                	data:{
                		"moduleCode": ec_module_Tree.getSelectedNodes()[0].code
                	},
                	success : function(res) {
                		$("#progressiveLog").attr("start", 0);
                		if (res && res == 'success') {
                		   $("#progressiveLog").append(moduleName+"${getHtmlText('发布成功')}");
                		   CUI.Dialog.alert("${getHtmlText('发布成功')}");
                		} else {
                		   $("#progressiveLog").append(res);
                		   CUI.Dialog.alert("${getHtmlText('ec.generateErrorModule')}");
                		}
                	}
                });
			runtime.entity.publishDialog.show();
		});


/*
if(!confirm("${getText("foundation.infoSet.publishInfoSetComfirn")}")){
		return false;
	}
	runtime.entity.publishDialog=new CUI.Dialog({
			title : "${getHtmlText('ec.module.monitorDeploy')}",
			width : 600,
			height : 350,
			modal : true,
			url : "/msService/servicemanager/msModule/projgenerate?moduleCode="+ec_module_Tree.getSelectedNodes()[0].code,
			buttons:[
				{	name:"${getHtmlText('foundation.common.closed')}",
					handler:function(){this.close();}
				}]
		});
	runtime.entity.publishDialog.show();
*/

}
</script>
<div id="generateSelectOutDiv" style="overflow:auto" >
<div id="generateSelectDiv" style="padding:2px 20px 0 20px;display:none;">
<form id="generateForm" target="generateWaitForm" onSubmit="javascript:return beforeDeploySubmit();" method="post">
<input type="hidden" name="module.code" value="" id="module_code"/>
<input type="hidden" name="isDeploy" value="false" id="isDeploy"/>
<input type="hidden" name="isGenerate" />
<input type="hidden" name="isUpdateTable" />
<input type="hidden" name="isCopy" />
<input type="hidden" name="generatorV3" value="true" />
<input type="hidden" name="generate_full" value="true" />
<input type="hidden" id="tasks" name="tasks" value="16" />
<input type="hidden" id="taskId" name="taskId" value="-1" />
<input type="hidden" id="taskType" name="taskType" value="0" />

<#--<span id="generate_full_span">
<label for="generate_full" style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.inter.mkscfs')}</label>
</span>
<select name="generate_full" id="generate_full">
	<option value="true">${getHtmlText('foundation.inter.total')}</option>
</select>-->
<table class="edit-table" style="">
<tr>
	<td style='height:0px;border:none;width:13%'></td>
	<td style='height:0px;border:none;width:17%'></td>
	<td style='height:0px;border:none;width:13%'></td>
	<td style='height:0px;border:none;width:17%'></td>
	<td style='height:0px;border:none;width:13%'></td>
	<td style='height:0px;border:none;width:17%'></td>
	<td style='height:0px;border:none;width:10%'></td>
<tr>
	<td align="right" style="text-align: right;">
		<label id="init_type_label" for="init_type" style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.inter.fbfs')}</label>
	</td>
	<td align="left" style="text-align: left;">
		<select name="init_type" id="init_type" onchange="deployTypeChange();">
			<option value="0">${getText('foundation.inter.ptfb')}</option>
			<option value="1">${getText('foundation.inter.ksfb')}</option>
		</select>
	</td>
	<td>
		<input type="button" onclick="submitForm()" value="${getText('foundation.common.checked')}" />
	</td>
	<td></td>
	<td></td>
	<td></td>
	<td></td>
</tr>
<#if isDev?string=='true'>
<tr id="deployOperate_tr">
	<td align="left" style="text-align: left;" colspan="6">
		<input type="checkbox" value="true" id="isGenerate" checked/>
		<label style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.inter.scydm')}</label>&nbsp;&nbsp;&nbsp;
		<input type="checkbox" value="true" id="isUpdateTable" checked/>
		<label style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.inter.gxbjgptfb')}</label>&nbsp;&nbsp;&nbsp;
		<input type="checkbox" value="true" id="isCopy" checked/>
		<label style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.inter.kbzywj')}</label>
	</td>
	<td></td>
</tr>
</#if>
</table>
</form>
<form id="uform" target="proj_wizar_iframe_step2" action="/msService/ec/engine/receive" method="post" enctype="multipart/form-data">
		<input type="file" id="receiveFile" name="receiveFile"  onclick="return clickReceive()" title="" style="position: absolute; width: 100%; left: 0; text-indent: -9999px; cursor: pointer; opacity: 0; z-index: 2; filter: alpha(opacity=0);">
	</form>
</div>
<iframe style="display:none;" id="generateWaitForm" name="generateWaitForm" frameborder="0" scrolling="auto" width="100%" height="313" src="about:blank"></iframe>
<div id="progressiveLog" style="display:none;" ></div>
</div>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>