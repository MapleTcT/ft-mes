<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.inter.cfg')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.elm-layout-doc-in-wrap .elm-layout-wrap-in-west{border-right:0px;}
#ec_module_entity_datatable{margin-top:10px!important;margin-bottom:3px!important;}
.batchpublish-extra-checkbox {
	margin-top: 7px;
	margin-right: 9px;
	display: inline-block;
}
.batchpublish-extra-checkbox input {
	vertical-align: middle;
	margin-right: 1px;
}
#ec_module_entity_datatable .helptip-customwrap {
	float: left;
	margin-right: 7px;
}
#ec_module_entity_datatable a.help-link {
	display: inline-block;
	width: 15px;
	height: 15px;
	background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
}
#ec_module_Tree{
	overflow-y: auto;
    padding-top: 0;
	height:calc(100% - 40px);
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
		<@tree id="ec_module_Tree" dataUrl="/msService/ec/module/datalist?isMsService=${isMsService}" rootName="${getText('ec.module.List')}" nameCol="nameInternational"
			 callback="{onClick:function(event,treeId,node){ec.module.showModuleInfo(node);}}" resizeHeight="false"/>
	</@frame>
	<div style="position:absolute;bottom:0px;z-index:55;">
		<div id="opratebar" class="opratebar">
			<a id="base_roleManage_add" class="cui-btn mr10 cui-btn-add" href="#" onclick='ec.module.add()'>${getHtmlText('ec.module.add')}</a>
			<a id="base_roleManage_modify" class="cui-btn mr10 cui-btn-edit" href="#" onclick='ec.module.mod()'>${getHtmlText('ec.module.edit')}</a>
			<a id="base_roleManage_delete" class="cui-btn mr10 cui-btn-del" href="#" onclick='ec.module.del()'>${getHtmlText('ec.module.del')}</a>
			<a id="base_upload_deploy_info" class="cui-btn mr10 cui-btn-eighteen-dt-display-histor-version" href="#" onclick='ec_upload_deploy_info()'>${getHtmlText('foundation.inter.history')}</a>
		</div>
	</div>
	<@frame id="ec_module_manage_main" offsetH=4>
		<#assign colAdmFlag = checkUserPermisition('ec_ptManage_list_view')>
		<#assign helpInfo = '<a href="/help/" target="_blank" title="${getText("foundation.inter.helpfile")}" class="help-link"></a>'>
		<#assign helpInfoConfig = '{isCustom: true}'>
		<@datatable hidekey="['code','version']" pageInitMethod="dgInitEvent" renderOverEvent="dgMangRenderOverEvent" firstLoad=false id="ec_module_entity_datatable" dataUrl="/msService/ec/entity/list" dblclick="ec.entity.dblclick" helpInfo=helpInfo helpInfoConfig=helpInfoConfig colAdminFlag=colAdmFlag>
			<#if '${isMsService}'!='Mis' >
			<@operatebar operates="code:ec_module_btn_generate||name:${getHtmlText('ec.module.create')}||iconcls:eighteen-dt-op-generate||onclick:ec.module.generate();
					code:ec_module_btn_build||name:${getHtmlText('ec.module.package')}||iconcls:eighteen-dt-op-install||onclick:ec.module.build();
					code:ec_module_btn_batchdeploy||name:${getHtmlText('ec.module.publish')}||iconcls:eighteen-dt-op-publish||onclick:ec.module.batchdeploywait();
					code:ec_module_btn_synchronize||name:${getHtmlText('ec.module.synchronize')}||useInMore:true||iconcls:eighteen-dt-op-synchronous||onclick:ec.module.synchronize();"
					 operateType="noPower" resultType="json" >
			</@operatebar>
			<#else>
			<@operatebar operates="code:ec_module_btn_generate||name:${getHtmlText('ec.module.create')}||iconcls:eighteen-dt-op-generate||onclick:ec.module.generate();
					code:ec_module_btn_build||name:${getHtmlText('ec.module.package')}||iconcls:eighteen-dt-op-install||onclick:ec.module.build();
					code:ec_module_btn_synchronize||name:${getHtmlText('ec.module.synchronize')}||useInMore:true||iconcls:eighteen-dt-op-synchronous||onclick:ec.module.synchronize();"
					 operateType="noPower" resultType="json" >
			</@operatebar>
			</#if>
			<@operatebar operates="code:ec_module_btn_upload_batch||name:${getHtmlText('foundation.inter.plsz')}||iconcls:eighteen-dt-op-upload-sourcecode"
					 operateType="noPower" resultType="json" >
			</@operatebar>
			<@operatebar operates="code:ec_module_btn_download||name:${getHtmlText('ec.module.download')}||iconcls:eighteen-dt-op-download-sourcecode||onclick:ec.module.download();
					code:ec_module_entity_btn_add||name:${getHtmlText('ec.module.addEntity')}||iconcls:own-tjst||onclick:ec.entity.add();
					code:ec_module_entity_btn_mod||name:${getHtmlText('ec.module.editEntity')}||iconcls:own-stxg||useInMore:true||onclick:ec.entity.mod();
					code:ec_module_entity_btn_del||name:${getHtmlText('ec.module.delEntity')}||useInMore:true||iconcls:del||onclick:ec.entity.del();
					code:ec_module_btn_mne_sync||name:${getHtmlText('ec.module.syncmnecode')}||useInMore:true||iconcls:eighteen-dt-op-mnemonic-synchronous||onclick:ec.module.syncMnecode()"
					 operateType="noPower" resultType="json" >
			</@operatebar>
			<@operatebar operates="code:ec_module_btn_updateScript||name:${getHtmlText('ec.module.exportexcel')}||useInMore:true||iconcls:eighteen-dt-op-export-entity||onclick:ec.module.exportEntity();"
				operateType="noPower" resultType="json" >
			</@operatebar>
			<#if isDev?string=='true'>
			<@operatebar operates="code:ec_module_btn_updateScript||name:${getHtmlText('ec.module.exec.updateScript')}||useInMore:true||iconcls:eighteen-dt-op-send-requisition||onclick:ec.module.updateScript();"
				operateType="noPower" resultType="json" >
			</@operatebar>
			</#if>
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
	<form id="QueryForm">
	<input type="hidden" id="excelfalg" name="module.code" value="" />
	</form>
	<form id="uform" target="wizar_iframe_batch_step2" action="/msService/ec/module/uploadBatchModules?isMsService=${isMsService}" method="post" enctype="multipart/form-data">
		<input type="file" id="receiveFile" name="receiveFile"  onclick="return clickReceive()" title="" style="position: absolute; width: 100%; left: 0; text-indent: -9999px; cursor: pointer; opacity: 0; z-index: 2; filter: alpha(opacity=0);">
	</form>
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
        			ec.module.showModuleInfo(node);
        			document.getElementById("ec_module_search_input").focus();
        		}

        		function onSearchBlur(){
        			setTimeout(function(){
        				document.getElementById("ec_module_search_list").style.display = 'none';
        			},200)
        		}
        //var customThemeCode = '${customThemeCode!}';
		//在点击file之前需要触发click事件来校验是否有任务在上载，如果有的话直接打开上载工程页面
		function clickReceive(){
			var isUploading = true;
			$.ajax({
				type : 'post',
				url : '/msService/ec/module/uploadBatchHasTask',
				async:false,
				success : function(res){
					console.log(res);
					if(res.success==false){
						isUploading = false;
						if(res.data == 1){
							ec.module.uploadWizardDialogBatch = new CUI.WizardDialog({
							title: "${getHtmlText('foundation.inter.plszjk')}",
							size: 4,
							close: true,
							steps :	[
										{
											iframe : 'wizar_iframe_batch_step3',
											url: '/msService/ec/module/uploadBatchProcessUsed',
											buttons : [
												{
													name : '${getText("foundation.inter.close")}',
													id : 'closeBtn',
													handler : function(){
														ec.module.uploadWizardDialogBatch.hide();
													}
												}
											]
										}
									]

							});
						}
						else if(res.data == 2 && res.exceptionMsg != ''){
							CUI.Dialog.alert(res.exceptionMsg);
						}
					}
				},
				error:function(res){
					CUI.Dialog.alert('${getText("foundation.inter.szztyc")}');
					isUploading = false;
				}
			});
			return isUploading;
		}
		$('#receiveFile').on('change', function(){
			//进行简单的文件名格式校验
			if ($('#uform').find('#receiveFile').length) return
			var fileName = $('#receiveFile').val();
			if(!fileName || fileName.length == 0) {
				CUI.Dialog.alert("${getHtmlText('ec.module.generate.upload.select')}");
				return false;
			}else if(!fileName.endsWith('.zip')){
				CUI.Dialog.alert("${getHtmlText('ec.module.upload.fileTypeError')}");
				return false;
			}
			$('[name="static"]').remove();//删除原本存在页面上的dialog框
			$('#uform').append( $('#receiveFile') );
			ec.module.uploadWizardDialogBatch = new CUI.WizardDialog({
				title: "${getHtmlText('foundation.inter.plszjk')}",
				size: 4,
				close: true, // 关闭后再打开时时重新开始
				steps :	[
				{
							iframe : 'wizar_iframe_batch_step2',
							buttons : [
								{
									name : "${getText('foundation.inter.sz')}",
									id : 'dialog_btn_upload_batch2',
									handler : function(){
										if(wizar_iframe_batch_step2.uploadBatch.onsubmit()){
											var view3 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step3' );

											view3.show();
											view3.load();

											wizar_iframe_batch_step2.uploadBatch.target = "wizar_iframe_batch_step3";
											wizar_iframe_batch_step2.uploadBatch.submit();
										}
									}

								},

								{
									name : '${getText("foundation.inter.close")}',
									id : 'dialog_btn_upload_close2',
									handler : function(){
										ec.module.uploadWizardDialogBatch.hide();
									}
								}

							]
							},

							{
								iframe : 'wizar_iframe_batch_step3',
								url: '/msService/ec/module/uploadwait',
								buttons : [
									{
										name : '${getText("foundation.inter.close")}',
										id : 'closeBtn',
										handler : function(){
											ec.module.uploadWizardDialogBatch.hide();
										}
									}

								]
							}
						]
			});

			$('#uform').submit();
			ec.module.loading = new CUI.loading({
			    "head": "${getText('foundation.inter.loadingwait')}",
			    "opacity":50,
			    "bgColor":"#666666",
			    "show": true
			});
			$('#loading_wrap').css('zIndex', 100000);
			$('#uform').append($('#receiveFile'));
			$('#receiveFile')[0].value= '';//每次结束的时候重新制空

		})

		function dgInitEvent(){
		 	$('#ec_module_btn_upload_batch').hover(function(){
		  		$(this).before($('#receiveFile'));
		  		$('#ec_module_btn_upload_batch').closest('a').css('position', 'relative');
		 	},
		 	function(){
		  		//$('#uform').append( $('#tfile') )
		  		//console.log('b')
		 	})
		}
	</script>
</@frameset>
<div id="ec_module_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div><#--text-indent:2em  不要缩进了，在英文下难看！！！-->
	${getHtmlText('ec.entity.checkDeleteModul')}
	</div>
</div>
</div>
<div id="ec_entity_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div><#--text-indent:2em  不要缩进了，在英文下难看！！！-->
	${getHtmlText('ec.entity.checkDeleteEntity')}
	</div>
</div>
</div>
<form id="exportForm" name="exportForm" method="post" target="_blank" action="/msService/ec/entity/list?exportFlag=true">
<input type="hidden" name="module.code" value="">
</form>
<script type="text/javascript" >
(function(){
	//注册命名空间
	CUI.ns("ec.module");
	CUI.ns("ec.entity");
	ec.module.timeout = null;
	ec.module.isMsService = '${isMsService}';
	var _dialog=function(title,url,callback,width,height){
		return new CUI.Dialog({
			title : title,
			width : width?width:618,
			height : height?height:360,
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
	var _dialog2=function(title,url,callback,width,height){
		var msService = '${isMsService}';
		var needButton;
		if(msService=='Mis'){
			needButton=true;
		}else{
			needButton=false;
		}
		return new CUI.Dialog({
			title : title,
			width : width?width:550,
			height : height?height:360,
			modal : true,
			url : url,
			beforeCloseEvent: ec.module.beforeCloseBatchPublish,
			onload: 'ec.module.beforeBatchPublish',
			close: true,
			hideCloseBtn: true,
			buttons:[
				{	id:"btn_publish",
					name:"${getHtmlText('ec.module.publish')}",
					handler:function(){callback(this);}
				},
				{	name:"${getHtmlText('foundation.common.closed')}",
					handler:function(){this.close();clearTimeout(ec.module.timeout);ec.module.deployModules = [];ec.module.lastPercent = 0;ec.module.deployModulesName = '';}
				}]
		});
	}
	var _dialog_top = function(title,url,callback,width,height) {
		return new top.CUI.Dialog({
			title : title,
			elementId: "top_PublishLog",
			width : width?width:550,
			height : height?height:360,
			modal : true,
			url : url,
			hideCloseBtn: true,
			buttons:[
				{	name:"${getHtmlText('foundation.common.closed')}",
					handler:function(){this.close();}
				}]
		})
	}
	ec.module.handleModuleCategory = function(oNode) {
		var pageSize=CUI('input[name="ec_module_entity_datatable_PageLink_PageCount"]').val();
		datatable_ec_module_entity_datatable.setRequestDataUrl('/msService/ec/entity/list?module.code='+oNode.code, (pageSize? 'pageSize='+pageSize : ''));
		$('span[id ^="ec_module_"]','#ec_module_entity_datatable').parent().hide();
		$('#buttonbar_more_button').hide();
		$('#ec_module_btn_batchdeploy').parent().show();
        $('#ec_module_btn_download').parent().show();
        $('#ec_module_btn_upload_batch').parent().show();
	     if ( $('#ec_module_btn_download', '#buttonbar_more_button').length) {
		   ec.module.downloadInMoreBtn = true;
		   $('#ec_module_btn_download', '#buttonbar_more_button').parent().appendTo($('.paginatorbar-operatebar')).show();
		  }
	}
	/**
	 * 点击模块树节点，显示实体详细信息
	 * @method ec.module.showModuleInfo
	 * @param {Node} oNode
	 * @public
	 */
	ec.module.showModuleInfo = function(oNode){
		if(oNode && !oNode.id && !oNode.code) {//分类
			ec.module.handleModuleCategory(oNode);
			return;
		}
		if (ec.module.downloadInMoreBtn){
		   $('.white-corner', '#buttonbar_more_button').prepend( $('#ec_module_btn_download').parent() );
		}
		// 处理
		var code = '';
		var isReadOnly = false;
		if(oNode != null && oNode != 'undefined' ){
			code = oNode.code;
			if(oNode.isReadOnly != null){
				isReadOnly = oNode.isReadOnly;
			}
			if(oNode.level==0){
				isReadOnly = true;
			}
		}
		if (code != '') {
			var pageSize=CUI('input[name="ec_module_entity_datatable_PageLink_PageCount"]').val();
			datatable_ec_module_entity_datatable.setRequestDataUrl('/msService/ec/entity/list?module.code='+oNode.code, (pageSize? 'pageSize='+pageSize : ''));
			if(isReadOnly){
				$('span[id ^="ec_module_"]','#ec_module_entity_datatable').parent().hide();
				$('#buttonbar_more_button').hide();
				if(code != 'sysbase_1.0'){
					$('#ec_module_btn_upload').parent().show();
					$('#ec_module_btn_batchdeploy').parent().show();
					$('#ec_module_btn_download').parent().show();
					$('#ec_module_btn_upload_batch').parent().show();

				}
			} else {
				$('span[id ^="ec_module_"]','#ec_module_entity_datatable').parent().show();
				$('#buttonbar_more_button').show();
			}
			$("#excelfalg").val(oNode.code);
		}
	};
	/**
	 * 模块信息添加
	 * @method ec.module.add
	 * @public
	 */
	ec.module.editDlg;
	ec.module.add=function(){
		var dlg=ec.module.editDlg = _dialog("${getHtmlText('ec.module.add')}","/msService/ec/module/edit?isMsService=${isMsService}",function(){
			if (ec.module.artifactValidate()){
				$("#ec_module_edit_form").submit();
			}else{
				return false;
			}
		}, null, "530");
		dlg.show();
	}


	/**
	 * 模块信息修改
	 * @method ec.module.mod
	 * @public
	 */
	ec.module.mod=function(){
	if(ec_module_Tree.getSelectedNodes()[0]==null || ec_module_Tree.getSelectedNodes()[0].level==0 || !ec_module_Tree.getSelectedNodes()[0].code){
			CUI.Dialog.alert("${getHtmlText('ec.module.edit.alert')}");
			return;
		}
	if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true){
			CUI.Dialog.alert("${getHtmlText('ec.module.modifyalert')}");
			return;
	}
	var dlg=ec.module.editDlg = _dialog("${getHtmlText('ec.module.edit')}","/msService/ec/module/edit?isMsService=${isMsService}&module.code="+ ec_module_Tree.getSelectedNodes()[0].code,function(){
			if (ec.module.acronymValidate()){
				$("#ec_module_edit_form").submit();
			}else{
				return false;
			}
		}, null, "600");
		dlg.show();
	}
	/**
	 * 模块信息删除  add by yubo20171221
	 * @method ec.module.del
	 * @public
	 */
	 ec.module.del=function(){
	 	if(ec_module_Tree.getSelectedNodes()[0]==null){
			//workbenchErrorBarWidget.showMessage('<li>' + "${getHtmlText('ec.module.choicedelete')}"+ '</li>', "f");
			//workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.choicedelete')}", "f");
			CUI.Dialog.alert("${getHtmlText('ec.module.choicedelete')}");
			return;
		}
		if(ec_module_Tree.getSelectedNodes()[0]==null || !ec_module_Tree.getSelectedNodes()[0].code || ec_module_Tree.getSelectedNodes()[0].level==0){
			//workbenchErrorBarWidget.showMessage('<li>' + "${getHtmlText('ec.module.choicedelete')}" + '</li>', "f");
			//workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.choicedelete')}", "f");
			CUI.Dialog.alert("${getHtmlText('ec.module.choicedelete')}");
			return;
		}
		if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true){
			//workbenchErrorBarWidget.showMessage('<li>' + "${getHtmlText('ec.module.delalert')}" + '</li>',"f");
			//workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.delalert')}", "f");
			CUI.Dialog.alert("${getHtmlText('ec.module.delalert')}");
			return;
		}

		ec.module.dialog = new CUI.Dialog({
				title: "${getHtmlText('ec.common.delChoise')}",
				elementId: "ec_module_manage_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
						//{	name:"${getHtmlText('ec.common.void')}",
						//	handler:function(){
						//		this.close();
						//		ec.module.deleteMthod("/msService/ec/module/ordinaryDelete");
						//	}
						//},

						{	name:"${getHtmlText('ec.view.delete')}",
							handler:function(){
								this.close();
								ec.module.deleteMthod("/msService/ec/module/delete");

							}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.module.dialog.show();

	}

	ec.module.deleteMthod = function(url) {
		closeLoadPanel();
		createLoadPanel(false, null,{head:"${getText('ec.common.dealing')}", show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"module.code":ec_module_Tree.getSelectedNodes()[0].code,
						"module.version":ec_module_Tree.getSelectedNodes()[0].version
					},
				url : url ,
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success){
						CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
						setTimeout(function() {
							var rootNode = ec_module_Tree.getNodeByParam("id", -1);
							ec_module_Tree.reAsyncChildNodes(rootNode, "refresh");
							//ec.module.refresh();
							ec.module.showModuleInfo(rootNode);
						},1000);

					}else{
						if (msg && msg.exceptionMsg) {
							var arr = JSON.parse(msg.exceptionMsg);
							if($('div#workbenchErrorBar').length == 0){
								$('div#layout-center').append("<div id='workbenchErrorBar'></div>")
							}
							var workbenchErrorBarWidget2 = new CUI.ErrorBar('workbenchErrorBar',{
							    offsetY:20
							});
	        				var str = '${getText("foundation.inter.deletefailed")}';
	        				for (var i=0; i<arr.length; i++){
	        					str += '<li>' + arr[i] + '</li>';
	        				}
	        				workbenchErrorBarWidget2.showMessage(str);

							//workbenchErrorBarWidget.showMessage(msg.exceptionMsg, "f");
						} else {
							workbenchErrorBarWidget.showMessage('<li>'+"${getHtmlText('common.delete.failure')}"+ '</li>', "f");
						}
					}

				}
			});
		},1500);
	}

	ec.module.refresh=function(){
		if (ec_module_Tree.getSelectedNodes()[0] != null && ec_module_Tree.getSelectedNodes()[0].level > 0) {
			ec.module.showModuleInfo(ec_module_Tree.getSelectedNodes()[0]);
		}else{
			foundation.workbench.refresh();
		}
	}

	/**
	 * 模块信息提交返回信息及回刷列表
	 * @method ec.module.addCallback
	 * @public
	 */
	ec.module.addCallback=function(msg){
		if(msg && msg.success){
			ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitsuccessful')}","s");
			try{ec.module.editDlg.close();}catch(e){}
			var rootNode = ec_module_Tree.getNodeByParam("id", -1);
			ec_module_Tree.reAsyncChildNodes(rootNode, "refresh");
		}else{ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.submitfailure')}");}

	}
	ec.module.refreshModuleTree=function(){
		var rootNode = ec_module_Tree.getNodeByParam("id", -1);
		ec_module_Tree.reAsyncChildNodes(rootNode, "refresh");
	}
	ec.module.dblclick=function(evt,row){
		loadPage({url : "/msService/ec/entity/manage?module.code=" + row.code});
	}


	/**
	 * 实体信息新增
	 * @method ec.entity.add
	 * @public
	 */
	ec.entity.editDlg;
	ec.entity.add=function(){
		if(ec_module_Tree.getSelectedNodes()[0]==null || ec_module_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}");
			return;
		}
		var moduleCode=ec_module_Tree.getSelectedNodes()[0].code;
		var dlg = ec.entity.editDlg = _dialog("${getHtmlText('ec.module.addEntity')}","/msService/ec/entity/edit?isMsService=${isMsService}&moduleCode="+moduleCode,function(d){
			$("#ec_entity_edit_form input[name='entity.module.code']").val(ec_module_Tree.getSelectedNodes()[0].code);
			if (ec.entity.codeValidate() && ec.entity.isBaseValidate()){
				$("#ec_entity_edit_form").submit();
			}else{
				return false;
			}
		},"","510");
		dlg.show();
	}
	/**
	 * 实体信息修改
	 * @method ec.entity.mod
	 * @public
	 */
	ec.entity.mod=function(){
		if(datatable_ec_module_entity_datatable.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceEntityEdit')}");
			return;
		}
		if(ec_module_entity_datatableWidget.selectedRows.length > 1){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselectedmore')}", "f");
			return;
		}
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
			CUI.Dialog.alert("${getHtmlText('ec.module.entity.modify.choicemodule')}");
			return;
		}
		var moduleCode=ec_module_Tree.getSelectedNodes()[0].code;
		var dlg = ec.entity.editDlg = _dialog("${getHtmlText('ec.module.editEntity')}","/msService/ec/entity/edit?isMsService=${isMsService}&moduleCode="+moduleCode+"&entity.code="+datatable_ec_module_entity_datatable.selectedRows[0].code,function(d){
			if (ec.entity.codeValidate()){
				$("#ec_entity_edit_form").submit();
			}else{
				return false;
			}
		},"","510");
		dlg.show();
	}

	/**
	 * 实体信息删除 add by yubo20171221
	 * @method ec.entity.del
	 * @public
	 */
	 ec.entity.del=function(){
		if(datatable_ec_module_entity_datatable.selectedRows.length==0){
				CUI.Dialog.alert("${getHtmlText('ec.module.choiceEntityDel')}");
			return;
		}
		if(ec_module_entity_datatableWidget.selectedRows.length > 1){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselectedmore')}", "f");
			return;
		}
		ec.entity.dialog = new CUI.Dialog({
				title: "${getHtmlText('ec.common.delChoise')}",
				elementId: "ec_entity_manage_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
						//{	name:"${getHtmlText('ec.common.void')}",
						//	handler:function(){
						//		this.close();
						//		ec.entity.deleteMthod('/msService/ec/entity/ordinaryDelete');
						//	}
						//},

						{	name:"${getHtmlText('ec.view.delete')}",
							handler:function(){
								this.close();
								ec.entity.deleteMthod('/msService/ec/entity/delete');

							}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.entity.dialog.show();


	}

	ec.entity.deleteMthod = function(url) {
	    closeLoadPanel();
		createLoadPanel(false, null,{head:"${getText('ec.common.dealing')}", show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"entity.code":datatable_ec_module_entity_datatable.selectedRows[0].code,
						"entity.version":datatable_ec_module_entity_datatable.selectedRows[0].version
				},
				url : url ,
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success){
						CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
						datatable_ec_module_entity_datatable.setRequestDataUrl('/msService/ec/entity/list?module.code='+ec_module_Tree.getSelectedNodes()[0].code);
					}else{
						if($('.datagrid-loading')) {
							$('.datagrid-loading').hide();
						}
						if(msg && msg.exceptionMsg){
							//alert(msg.exceptionMsg);
							var arr = JSON.parse(msg.exceptionMsg);
							if($('div#workbenchErrorBar').length == 0){
								$('div#layout-center').append("<div id='workbenchErrorBar'></div>")
							}
							var workbenchErrorBarWidget2 = new CUI.ErrorBar('workbenchErrorBar',{
								offsetY:20
							});
							var str = '${getText("foundation.inter.deletefailed")}';
							for (var i=0; i<arr.length; i++){
								str += '<li>' + arr[i] + '</li>';
							}
							workbenchErrorBarWidget2.showMessage(str);
						}
						else{CUI.Dialog.alert("${getHtmlText('ec.module.submitfailure')}");}
					}
				}
			});
		},1500);
	}



	/**
	 * 获取待操作的记录ID，从树或者列表上获取
	 * @method getOperateRecordId
	 * @return {string} 待操作的记录ID
	 * @private
	 */
	var getOperateRecordId = function(){
		var code;
		if(ec_module_Tree.getSelectedNodes()[0] != null){
			code = ec_module_Tree.getSelectedNodes()[0].code;
			return code;
		};
	}

	ec.entity.dblclick=function(evt,row){
		var isReadOnly = false;
		if(ec_module_Tree.getSelectedNodes()[0]!=null && ec_module_Tree.getSelectedNodes()[0].level>0) {
			isReadOnly = ec_module_Tree.getSelectedNodes()[0].isReadOnly;
		}
		if(!isReadOnly) {
			if(ec_module_entity_datatableWidget.selectedRows.length > 1){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselectedmore')}", "f");
				return;
			}
			if(window.navigator.userAgent.indexOf('MSIE 6.0') != -1){
				window.open('/msService/ec/entity/config?entity.code=' + row.code,'','height=700, width=1000, top=0,left=0, toolbar=no, menubar=no, scrollbars=no, resizable=yes,location=no, status=no');
			} else {
				window.open('/msService/ec/entity/config?entity.code=' + row.code);
			}
		} else {
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.module.isReadOnly')}", "f");
			return;
		}
	}
	ec.entity.addCallback=function(msg){
		var moduleCode = "";
		if(ec_module_Tree.getSelectedNodes()[0]!=null) {
			moduleCode = ec_module_Tree.getSelectedNodes()[0].code;
		}
		if(msg && msg.success){
			ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.entity.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.entity.editDlg.close();}catch(e){}
				ec.module.refresh();
			},1500);
		}else{ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.entity.submitfailure')}");}
	}
	ec.module.refresh=function(){
		if (ec_module_Tree.getSelectedNodes()[0] != null && ec_module_Tree.getSelectedNodes()[0].level > 0) {
						ec.module.showModuleInfo(ec_module_Tree.getSelectedNodes()[0]);
		}else{
			foundation.workbench.refresh();
		}
	}
	ec.module.generate=function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceModulePublish')}");
			return;
		}
		if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0'){
			CUI.Dialog.alert("${getHtmlText('ec.module.fundation')}");
			return;
		}
		if ('Mis' != ec.module.isMsService) {
			$.ajax({
		        url: '/msService/ec/deploy/batchTask',
		        dataType: 'json',
		        type: 'POST',
		        success : function(res) {
		        	if(res != null && res.length > 0){
		        		CUI.Dialog.alert("${getHtmlText('foundation.inter.zzfbshcs')}");
		    			return;
		        	}else{
		        		var moduleCode = ec_module_Tree.getSelectedNodes()[0].code;
		        		if(!ec_module_Tree.getSelectedNodes()[0]){	CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");return;}
		        		CUI.Dialog.confirm("${getText('ec.module.createAlert')}", function(){
							$('#generateForm').attr('action', '/msService/ec/module/generate?generate_full=true');
			        		$('[name="isDeploy"]').val(false);
			        		$('#init_type_label').hide();
			        		$('#init_type').hide();
			        		$('[name="module.code"]').val(ec_module_Tree.getSelectedNodes()[0].code);
			        		$('#generateSelectDiv').hide();
			        		$('#generateWaitForm').show();
							ec.module.generateDlg = new CUI.Dialog({
			        			title : "${getHtmlText('ec.module.Monitor')}",
			        			width : 600,
			        			height : 350,
			        			modal : true,
			        			elementId : 'generateSelectOutDiv',
			        			buttons:[
			        				{	name:"${getHtmlText('foundation.common.closed')}",
			        					handler:function(){$('#generateWaitForm').attr('src', 'about:blank');this.close();}
			        				}]
			        		});
							ec.module.generateDlg.show();
			        		$('#generateWaitForm').hide();
			        		$("#tasks").val(16);
			        		$("#taskType").val(0);
			        		addTask(false, false);
			        		$(ec.module.generateDlg._buttonLeft).show();
			        		return ec.module.generateDlg;
						});
		        	}
		        }
			});
		} else {
			var moduleCode = ec_module_Tree.getSelectedNodes()[0].code;
			var moduleName = ec_module_Tree.getSelectedNodes()[0].nameInternational;
    		if(!ec_module_Tree.getSelectedNodes()[0]){
    			CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");return;
    		}
    		CUI.Dialog.confirm("${getText('ec.module.createAlert')}", function(){
        		$('[name="isDeploy"]').val(false);
        		$('#init_type_label').hide();
        		$('#init_type').hide();
        		$('[name="module.code"]').val(ec_module_Tree.getSelectedNodes()[0].code);
        		$('#generateSelectDiv').hide();
        		$('#generateWaitForm').show();
				ec.module.generateDlg = new CUI.Dialog({
        			title : "${getHtmlText('ec.module.Monitor')}",
        			width : 600,
        			height : 350,
        			modal : true,
        			elementId : 'generateSelectOutDiv',
        			buttons:[
        				{	name:"${getHtmlText('foundation.common.closed')}",
        					handler:function(){$('#generateWaitForm').attr('src', 'about:blank');this.close();}
        				}]
        		});
				ec.module.generateDlg.show();
				$('#generateSelectDiv').hide();
        		$('#generateWaitForm').hide();
		    	$('#progressiveLog').empty();
				$('#progressiveLog').show();
        		$("#progressiveLog").append(moduleName +"${getHtmlText('ec.module.generate.startCode')}");
        		$.ajax({
		            url: '/msService/servicemanager/msModule/generate',
		            //dataType: 'json',
		            type: 'POST',
		            data:{
		            	"moduleCode": moduleCode
		            },
		            success : function(res) {
		            	$("#progressiveLog").attr("start", 0);
		            	if (res && res == 'success') {
		            		$("#progressiveLog").append(moduleName + "${getHtmlText('ec.generateSuccessModule')}");
		            		CUI.Dialog.alert("${getHtmlText('ec.generateSuccessModule')}");
		            	} else {
		            		$("#progressiveLog").append(res);
		            		CUI.Dialog.alert("${getHtmlText('ec.generateErrorModule')}");
		            	}
		            }
		    	});
        		$(ec.module.generateDlg._buttonLeft).show();
        		generateTimer();
        		return ec.module.generateDlg;
			});
		}
	}
	ec.module.upload = function(){

		var moduleCode = (ec_module_Tree.getSelectedNodes()[0]) ? ec_module_Tree.getSelectedNodes()[0].code : '-1';

		if(moduleCode != -1 && (ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0')){
			CUI.Dialog.alert("${getHtmlText('ec.module.uploadInherent')}");
			return;
		}

		ec.module.uploadWizardDialog_prev = null;

		if( !ec.module.uploadWizardDialog ){
			ec.module.uploadWizardDialog = new CUI.WizardDialog({
				title: "${getHtmlText('ec.module.uploadMonitor')}",
				size: 4,
				close: true, // 关闭后再打开时时重新开始
				steps :	[

							{
								iframe : 'wizar_iframe_step1',
								onload:function(){
									wizar_iframe_step1.$( '#changeFile' ).val( 'false' )
									$( '#dialog_btn_migrate', ec.module.uploadWizardDialog.dialog._buttonbar ).attr('canclick','false').addClass( 'cui-btn-gray' );
								},
								url: '/msService/ec/module/upload?module.code=' + moduleCode,
								buttons : [
									{
										name : '${getText("foundation.inter.sz")}',
										id : 'dialog_btn_upload',
										handler : function(){
											ec.module.uploadWizardDialog_prev = 'wizar_iframe_step1';
											if(wizar_iframe_step1.uploadForm.onsubmit()){
												var view3 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step3' );
												view3.show();
												view3.load();
												wizar_iframe_step1.uploadForm.target = "wizar_iframe_step3";
												wizar_iframe_step1.uploadForm = "/msService/ec/module/receive"
												wizar_iframe_step1.uploadForm.submit();
											}
										}
									},
									/*{
										name : '${getText("foundation.inter.nextstep")}',
										id : 'dialog_btn_migrate',
										handler : function(){
											wizar_iframe_step1.uploadForm = "/msService/ec/module/listEntity"
											var view2 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step2' );
											if(!view2.loaded || wizar_iframe_step1.uploadForm.changeFile.value == 'true'){
												if(wizar_iframe_step1.uploadForm.onsubmit()){
													wizar_iframe_step1.uploadForm.submit();
												}

											}else{
												view2.show();
											}

										}
									},*/
									{
										name : '${getText("calendar.common.cancal")}',
										handler : function(){
											ec.module.uploadWizardDialog.hide();
										}
									}

								]
							},

							{
								iframe : 'wizar_iframe_step2',
								buttons : [

									{
										name : '${getText("foundation.inter.prevstep")}',
										handler : function(){
											wizar_iframe_step1.changeFile.value=false
											var view1 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step1' );
											view1.show();
											$( '#dialog_btn_upload', ec.module.uploadWizardDialog.dialog._buttonbar ).attr('canclick','false').addClass( 'cui-btn-gray' );
										}
									},

									{
										name : '${getText("foundation.inter.nextstep")}',
										handler : function(){
											ec.module.uploadWizardDialog_prev = 'wizar_iframe_step2';
											if(wizar_iframe_step2.uploadEntity.onsubmit()){
												var view3 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step3' );

												view3.show();
												view3.load();

												wizar_iframe_step2.uploadEntity.target = "wizar_iframe_step3";
												wizar_iframe_step2.uploadEntity.submit();
											}

										}
									},

									{
										name : '${getText("calendar.common.cancal")}',
										handler : function(){
											ec.module.uploadWizardDialog.hide();
										}
									}

								]
							},

							{
								iframe : 'wizar_iframe_step3',
								url: '/msService/ec/module/uploadwait',
								buttons : [

									{
										name : '${getText("foundation.inter.prevstep")}',
										id : 'frame3_preStep_Btn',
										handler : function(){
											var view3 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step3' );
											view3.iframe.prop( 'src','about:blank' );
											if(ec.module.uploadWizardDialog_prev == 'wizar_iframe_step1'){
												var view1 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step1' );
												wizar_iframe_step1.uploadForm.target = "transfer";
												view1.show();
												$( '#dialog_btn_migrate', ec.module.uploadWizardDialog.dialog._buttonbar ).attr('canclick','false').addClass( 'cui-btn-gray' );
											}else{
												var view2 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step2' );
												view2.show();
											}
										}
									},

									{
										name : '${getText("foundation.inter.close")}',
										id : 'closeBtn',
										handler : function(){
											ec.module.uploadWizardDialog.hide();
										}
									}

								]
							}




						]


			});

		}else{
			var view1 = ec.module.uploadWizardDialog.getView( 'wizar_iframe_step1' );
			view1.url = view1._url = '/msService/ec/module/upload?module.code=' + moduleCode;
			ec.module.uploadWizardDialog.show();
		}




		/*
		return new CUI.Dialog({
			title : "${getHtmlText('ec.module.uploadMonitor')}",
			width : 600,
			height : 350,
			iframe : 'uploadFrame',
			modal : true,
			url : '/msService/ec/module/upload?module.code=' + moduleCode,
			buttons:[
				{	name:"${getHtmlText('common.button.close')}",
					handler:function(){this.close();}
				}]
		}).show();
		*/


	};
ec.module.download = function(){
    if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id == '-1'){
            ec.module.download.batch();
        return;
    }
    if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0'){
        CUI.Dialog.alert("${getText('ec.module.downloadInherent')}");
        return;
    }

    if(!ec_module_Tree.getSelectedNodes()[0]){	CUI.Dialog.alert("${getText('ec.module.choiceModule')}!");return;}
    var code = '';
    if(ec_module_Tree.getSelectedNodes()[0].isParent != undefined && ec_module_Tree.getSelectedNodes()[0].isParent == true){
        var children = ec_module_Tree.getSelectedNodes()[0].children;
        for(var i = 0 ;i<children.length ; i++){
            code += ','+children[i].code;
        }
    }else{
        code += ','+ec_module_Tree.getSelectedNodes()[0].code;
    }
    if(code != ''){
        var excludeCustomFile = "true";
        $.ajax({
            data : {"moduleCodes": code.substring(1)},
            url : '/msService/ec/module/checkModifyState',
            success : function(msg){
                if(msg){
                    if(msg.success) {
                        CUI.Dialog.confirm(msg.exceptionMsg, function(){
                            ec.module.download.confirm(code.substring(1));
                        });
                    }else {
                        ec.module.download.confirm(code.substring(1));
                    }
                }
            }
        });
    }else{
        alert('${getText("foundation.inter.codeisnull")}');
    }

};
ec.module.download.confirm = function(moduleCodes) {
    if(moduleCodes == ''){
		moduleCode = '';
		if(ec_module_Tree.getSelectedNodes()[0].category != undefined && ec_module_Tree.getSelectedNodes()[0].category != ''){
			var children = ec_module_Tree.getSelectedNodes()[0].children;
			for(var i = 0 ;i<children.length ; i++){
				moduleCode += ','+children[i].code;
			}
		}else{
			moduleCode = ec_module_Tree.getSelectedNodes()[0].code;
		}
	}
	new CUI.Dialog({
                title : "${getText('ec.module.packageMonitor')}",
                width : 600,
                height : 350,
                modal : true,
                html : '<iframe frameborder="0" scrolling="auto" width="100%" height="300" src="/msService/ec/module/down-source?excludeCustomFile=true&moduleCodes=' + moduleCodes + '&isMsService=${isMsService}"></iframe>',
                buttons:[
                    {	name:"${getText('common.button.Confirm')}",
                        handler:function(){this.close();}
                    }]
            }).show();
    /*CUI.Dialog.customConfirm("是否包含自定义资源(国际化文件等)？",
        function(){
            return new CUI.Dialog({
                title : "${getText('ec.module.packageMonitor')}",
                width : 600,
                height : 350,
                modal : true,
                html : '<iframe frameborder="0" scrolling="auto" width="100%" height="300" src="/msService/ec/module/down-source?excludeCustomFile=false&moduleCodes=' + moduleCodes + '"></iframe>',
                buttons:[
                    {	name:"<span i18n='common.button.Confirm'>确 定</span>",
                        handler:function(){this.close();}
                    }]
            }).show();
        },
        function(){
            return new CUI.Dialog({
                title : "<span i18n='ec.module.packageMonitor'>压缩下载状态监控</span>",
                width : 600,
                height : 350,
                modal : true,
                html : '<iframe frameborder="0" scrolling="auto" width="100%" height="300" src="/msService/ec/module/down-source?excludeCustomFile=true&moduleCodes=' + moduleCodes + '"></iframe>',
                buttons:[
                    {	name:"<span i18n='common.button.Confirm'>确 定</span>",
                        handler:function(){this.close();}
                    }]
            }).show();
        },
        '',null,null,"<span i18n='ec.module.download.custom.include'>包含</span>","<span i18n='ec.module.download.custom.exclude'>不包含</span>"
    );*/
}

ec.module.download.batch = function() {
	ec.module.batchDownloadDlg = new CUI.Dialog({
		title : "${getText('ec.module.batchDownload')}",
		width : 770,
		height : 520,
		modal : true,
		close: true,
		hideCloseBtn: true,
		url:'/msService/ec/module/downloadModule?isMsService=${isMsService}',
		buttons:[
		    {	name:"${getText('ec.module.responseDownload')}",
				handler:function(){
					var moduleCodes = '';
					ec_modules_download_datatableWidget.getSelectedRow().forEach(function(obj){
						console.log(obj.code);
						moduleCodes += ','+obj.code;
					})
					if(moduleCodes.length>0){
						moduleCodes = moduleCodes.substring(1);
					}else{
						CUI.Dialog.alert('${getText("foundation.inter.qxzxyxzdmk")}');
						return ;
					}
					var code = moduleCodes;
					if(code != ''){
					    var excludeCustomFile = "true";
					    $.ajax({
					        data : {"moduleCodes": code},
					        url : '/msService/ec/module/checkModifyState',
					        success : function(msg){
					            if(msg){
					                if(msg.success) {
					                    CUI.Dialog.confirm(msg.exceptionMsg, function(){
					                        ec.module.download.confirm(code);
					                        ec.module.batchDownloadDlg.close();
					                    });
					                }else {
					                    ec.module.download.confirm(code);
					                    ec.module.batchDownloadDlg.close();
					                }
					            }
					        }
					    });
					}

				}
			},
			{	name:"${getText('foundation.inter.close')}",
				handler:function(){this.close();}
			}]
		});
	ec.module.batchDownloadDlg.show();
}
	/**
	 * 编译打包当前模块
	 */
	ec.module.build=function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");
			return;
		}
		if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0'){
			CUI.Dialog.alert("${getHtmlText('ec.module.packageInherent')}");
			return;
		}
		CUI.Dialog.confirm("${getText('ec.module.isPackage')}", function(){
			if ('Mis' != ec.module.isMsService) {
				$.ajax({
			        url: '/msService/ec/deploy/batchTask',
			        dataType: 'json',
			        type: 'POST',
			        success : function(res) {
			        	if(res != null && res.length > 0){
			        		CUI.Dialog.alert("${getHtmlText('foundation.inter.zzfbshcs')}");
			    			return;
			        	}else{
			        		ec.module.packageDlg = new CUI.Dialog({
			        			title : "${getHtmlText('ec.module.monitorPackage')}",
			        			width : 600,
			        			height : 350,
			        			modal : true,
			        			elementId : 'generateSelectOutDiv',
			        			buttons:[
			        				{	name:"${getHtmlText('foundation.common.closed')}",
			        					handler:function(){$('#generateWaitForm').attr('src', 'about:blank');this.close();}
			        				}]
			        		});
							ec.module.packageDlg.show();
			        		$('#generateWaitForm').hide();
			        		$("#tasks").val(4);
			        		$("#taskType").val(1);
			        		addTask(false, false);
							$(ec.module.packageDlg._buttonLeft).show();
			        		return ec.module.packageDlg;;
			        	}
			        }
				});
			} else {
				ec.module.packageDlg = new CUI.Dialog({
        			title : "${getHtmlText('ec.module.monitorPackage')}",
        			width : 600,
        			height : 350,
        			modal : true,
        			elementId : 'generateSelectOutDiv',
        			buttons:[
        				{	name:"${getHtmlText('foundation.common.closed')}",
        					handler:function(){$('#generateWaitForm').attr('src', 'about:blank');this.close();}
        				}]
        		});
        		ec.module.packageDlg.show();
        		$('#generateWaitForm').hide();
				$('#generateSelectDiv').hide();
        		$('#generateWaitForm').hide();
		    	$('#progressiveLog').empty();
				$('#progressiveLog').show();
				var moduleName = ec_module_Tree.getSelectedNodes()[0].nameInternational;
        		$("#progressiveLog").append(moduleName + "${getHtmlText('ec.module.generate.startPackage')}");
				$.ajax({
			        url: '/msService/servicemanager/msModule/buidPackage',
			        data : {
			        	"moduleCode" : ec_module_Tree.getSelectedNodes()[0].code
			        },
			        type: 'POST',
			        success : function(res) {
		    			$("#progressiveLog").attr("start", 0);
		            	if (res && res == 'success') {
		            		$("#progressiveLog").append(moduleName + "${getHtmlText('ec.module.generate.compilePackageSucessed')}");
		            		CUI.Dialog.alert("${getHtmlText('ec.module.generate.compilePackageSucessed')}");
		            	} else {
		            		$("#progressiveLog").append(res);
		            		CUI.Dialog.alert("${getHtmlText('ec.module.generate.compilePackageFailed')}");
		            	}
			        }
				});
        		$(ec.module.packageDlg._buttonLeft).show();
        		return ec.module.packageDlg;
			}
		});
	}
	/**
	 * 发布当前模块
	 */
	ec.module.deploy = function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id=='-1'){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");
			return;
		}
		if(ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0'){
			CUI.Dialog.alert("${getHtmlText('ec.module.deployInherent')}");
			return;
		}
		$.ajax({
			data : {"module.code": ec_module_Tree.getSelectedNodes()[0].code},
			url : '/msService/ec/module/checkDependency',
			async: false,
			cache: false,
			success:function(res){
				if(res.dealSuccessFlag){
					$('#generateForm').attr('action', '/msService/ec/module/deploy');
					$('[name="isDeploy"]').val(true);
					$('#init_type_label').show();
					$('#init_type').show();
					$('[name="module.code"]').val(ec_module_Tree.getSelectedNodes()[0].code);
					$('#generateSelectDiv').show();
					$('#generateWaitForm').hide();
					$('#progressiveLog').empty();
					var dialog = new CUI.Dialog({
						title : "${getHtmlText('ec.module.monitorDeploy')}",
						width : 600,
						height : 350,
						modal : true,
						elementId : 'generateSelectOutDiv',
						buttons:[
							{	name:"${getHtmlText('foundation.common.closed')}",
								handler:function(){$('#generateWaitForm').attr('src', 'about:blank');$('#progressiveLog').hide();this.close();}
							}]
					}).show();
				} else {
					CUI.Dialog.alert(res.errorMsg);
					return;
				}
			}
		});
	}

	/**
	 * 点发布进去，首先判断是否有发布任务，有发布任务直接显示发布进度，没有发布任务显示模块列表
	 */
	ec.module.batchdeploywait = function(){
		ec.module.deployModulesName = '';
		ec.module.other = false;
		var customThemeCode = '${isMsService}';
		if(customThemeCode=='Mis'){
			var dlg = ec.module.batchdeployDlg = _dialog2("${getHtmlText('ec.module.monitorDeploy')}","/msService/ec/module/batchDeployWait?isMsService=${isMsService}",function(){ec.module.batchdeploy()},"770","520");
						dlg.show();
		}else{
			$.ajax({
		        url: '/msService/ec/deploy/batchTask',
		        dataType: 'json',
		        type: 'POST',
		        async: false,
		        success : function(res) {
		        	if(res != null && res.length > 0){
		        		ec.module.batchdeployDlg = new CUI.Dialog({
						title : "${getHtmlText('ec.module.monitorDeploy')}",
						width : 770,
						height : 520,
						modal : true,
						close: true,
						hideCloseBtn: true,
						beforeCloseEvent: ec.module.beforeCloseBatchPublish,
						onload: 'ec.module.beforeBatchPublish',
						buttons:[
							{	name:"${getText('foundation.inter.close')}",
								handler:function(){this.close();clearTimeout(ec.module.timeout);ec.module.deployModules = [];ec.module.lastPercent = 0;ec.module.deployModulesName = '';}
							}]
						});
						ec.module.batchdeployDlg.show();
			        	runtimeTaskId = [res[0].id];
			        	ec.module.other = true;
			        	deployUser = res[0].deployUser;
			        	var deployModules = res[0].moduleName;
			        	ec.module.deployModulesName = deployModules;
			        	ec.module.showPublishLogReady();
			        	if(deployUser != "") {
							if(deployUser == "system") deployUser = "${getText('foundation.inter.system')}";
							ec.module.deployUser.text(deployUser);
						}
			        	progressiveBatchLog(0,runtimeTaskId[0]);
			        	$(ec.module.batchdeployDlg._buttonLeft).show();
		        	}else{
		        		ec.module.btn_publish_disabled = false;
			        	var dlg = ec.module.batchdeployDlg = _dialog2("${getHtmlText('ec.module.monitorDeploy')}","/msService/ec/module/batchDeployWait?isMsService=${isMsService}",function(){ec.module.batchdeploy()},"770","520");
						dlg.show();
		        	}

		        }
			});
		}
	}

	ec.module.moduleMap = {};
	ec.module.deployModules = new Array();
	ec.module.btn_publish_disabled = false;

	ec.module.batchdeploy = function(){
		ec.module.deployModules = [];
		var rows = ec_modules_datatableWidget.getAllRows();
		var modules = new Array();
		var checkmodules = '';
		//生成源码：16, 编译打包：4, 部署启动：8, 同步数据：32；组合任务可以将上面的数字相加
		var deploy_type_fast=8;
		var deploy_type_general=60;
		var isNoModule = true;
		for(var i=0; i<rows.length; i++){
			var checkboxFast = datatable_ec_modules_datatable.getCellValue(rows[i].rowHtmlObj.rowIndex,"checkboxFast");
			var checkboxNormal = datatable_ec_modules_datatable.getCellValue(rows[i].rowHtmlObj.rowIndex,"checkboxNormal");
			if(checkboxFast == 'true'){
				modules.push(rows[i].code+','+deploy_type_fast);
				checkmodules = checkmodules + ',' + rows[i].code
				isNoModule = false;
			}
			if(checkboxNormal == 'true'){
				modules.push(rows[i].code+','+deploy_type_general);
				checkmodules = checkmodules + ',' + rows[i].code;
				isNoModule = false;
			}
			ec.module.moduleMap[rows[i].code] = rows[i].nameInternational;
		}
		if(isNoModule){
			CUI.Dialog.alert('${getText("foundation.inter.qxzxyfbmk")}');
			return false;
		}
		var hasUploadTask = false;
		//校验是否有任务在上载
		$.ajax({
			url : '/msService/ec/module/getUploadState',
			async: false,
			success : function(msg){
				if(msg.success) {
					CUI.Dialog.alert(msg.exceptionMsg);
					hasUploadTask = true;
				}
			}
		});
		if(hasUploadTask){
			return false;
		}
		// 防止多次点击
		if (ec.module.btn_publish_disabled) {
			return false;
		}
		ec.module.btn_publish_disabled = true;
		$.ajax({
			data : {"moduleCodes": checkmodules.substring(1)},
			url : '/msService/ec/module/getDeployModulesForsort',
			success : function(res){
				if(res != null && res.length > 0){
					var dependenciesName='';
					var relationsModules = [];
					var relationsName = '';
					for(var i=0; i<res.length; i++){
						var b = false;
						for(var n=0; n<modules.length; n++){
							if(modules[n].split(',')[0] == res[i].code){
								ec.module.deployModules.push(modules[n]);
								b=true;
								break;
							}
						}
						if(!b && (null == res[i].isRelation || !res[i].isRelation)) {
							if(res[i] != null && res[i].deployType==2){
								ec.module.deployModules.push(res[i].code + ',' + deploy_type_general);
							}else{
								ec.module.deployModules.push(res[i].code + ',' + deploy_type_fast);
							}
							dependenciesName = dependenciesName + "," + '"' + res[i].nameInternational + '"';
						} else if(!b && null != res[i].isRelation && res[i].isRelation){
							if(res[i].deployType==2){
								relationsModules.push(res[i].code + ',' + deploy_type_general);
							}else{
								relationsModules.push(res[i].code + ',' + deploy_type_fast);
							}
							relationsName = relationsName + "," + '"' + res[i].nameInternational + '"';
						}
					}

					if(dependenciesName != ''){
						CUI.Dialog.confirm(
								"${getText('foundation.inter.ylmk')} "+dependenciesName.substring(1)+"${getText('foundation.inter.ylmkwfb')}",
								function(){
									$('#btn_publish').hide();
									ec.module.addRelation(relationsModules, relationsName);
								},
								function(){$('#btn_publish').show();},
								"",
								70,
								400
							);
					} else {
						ec.module.addRelation(relationsModules, relationsName);
					}
				}else {
					$.ajax({
			            url: '/msService/ec/deploy/batchTask',
			            dataType: 'json',
			            type: 'POST',
			            async: false,
			            success : function(res) {
			            	runtimeTaskId = [res[0].id];
			            	deployUser = res[0].deployUser;
			            	var deployModules = res[0].moduleName;
		        			ec.module.deployModulesName = deployModules;
			            	ec.module.showPublishLogReady();
			            	if(deployUser != "") {
								if(deployUser == "system") deployUser = "${getText('foundation.inter.system')}";
								ec.module.deployUser.text(deployUser);
							}
			            	$('#btn_publish').hide();
			            	progressiveBatchLog(0,runtimeTaskId[0]);
			            }
			    	});
				}
				ec.module.btn_publish_disabled = false;
			},
			error:function(res){
				if(undefined != res.responseText){
					try {
						var errorMsg = $.parseJSON(res.responseText).exceptionMsg;
						batchDeployWaitErrorBarWidget.showMessage(errorMsg,'f');
					}catch(err){
						console.log(err);
					}
				}
				ec.module.btn_publish_disabled = false;
			}
		});
	}

	ec.module.addRelation = function(relationsModules, relationsName){
		if(relationsName != ''){
			CUI.Dialog.confirm(
				"${getText('foundation.inter.rxmk')} "+relationsName.substring(1)+"${getText('foundation.inter.rxmkhbxz')}"+relationsName.substring(1)+"?",
				function(){
					$('#btn_publish').hide();
					foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'),true);
					while(relationsModules.length > 0){
						ec.module.deployModules.push(relationsModules.pop());
					}
					foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'),true);
					ec.module.showPublishLogReady();
					addTasks(0);
				},
				function(){
					foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'),true);
					ec.module.showPublishLogReady();
					addTasks(0);
				},
				"",
				70,
				400
			);
		}else{
			foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'),true);
			ec.module.showPublishLogReady();
			addTasks(0);
		}
	}


	/**
	 * 遮罩和进度条log
	 */
	ec.module.hidePublishLog = function() {
		$(ec.module._topMask).hide();
		$(ec.module._topOverlayFrame).hide();
		$(ec.module._topPublishLog).hide();
	}

	/**
	 * 全屏遮罩和log关闭
	 */
	ec.module.closePublishLog = function() {
		$(ec.module._topMask).remove();
		$(ec.module._topOverlayFrame).remove();
		$(ec.module._topPublishLog).remove();
	}

	/**
	 * ec与runtime数据同步
	 */
	ec.module.synchronize = function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id == '-1'){   CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");return;}
		if ('Mis' != ec.module.isMsService) {
			$.ajax({
				data : {"module.code": ec_module_Tree.getSelectedNodes()[0].code},
				url : '/msService/ec/module/synchronize',
				success : function(msg){
					if(msg && msg.success){
						workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}", 's')
					} else {
						CUI.showErrorInfos(msg);
					}
				}
			});
		} else {
			$.ajax({
				data : {"moduleCode": ec_module_Tree.getSelectedNodes()[0].code},
				url : '/msService/servicemanager/msModule/ecDataSynchronize',
				success : function(msg){
					if(msg && msg == 'success'){
						workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}", 's')
					} else {
						CUI.showErrorInfos(msg);
					}
				}
			});
		}
	}



	ec.module.syncMnecode = function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id == '-1'){   CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");return;}
		$.ajax({
			data : {"module.code": ec_module_Tree.getSelectedNodes()[0].code},
			url : '/msService/ec/module/syncMnecode',
			success : function(msg){
				if(msg && msg.success){
					workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}", 's')
				} else {
					CUI.showErrorInfos(msg);
				}
			}
		});
	}


	//迁移
	ec.module.entityMigrate = function(){
		if (ec_module_entity_datatableWidget.selectedRows.length == 0) {
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.entity.checkMigrate')}", "f");
			return;
		}
		if(ec_module_Tree.getSelectedNodes().length == 0){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.choiceModule')}", "f");
			return;
		}
		var entityCodes = '';
		$.each(ec_module_entity_datatableWidget.selectedRows, function(i, row){
			entityCodes += entityCodes.length == 0 ? row.code : "," + row.code;
		});

		if( !ec.module.migrateWizardDialog ){
			ec.module.migrateWizardDialog = new CUI.WizardDialog({
				title: "${getHtmlText('ec.entity.migrate')}",
				size: 3,
				close: true, // 关闭后再打开时时重新开始
				steps :	[
							{
								iframe : 'migwizar_iframe_step1',
								url: '/msService/ec/module/migrate?srcmodule.code='+ec_module_Tree.getSelectedNodes()[0].code+'&entityCodes=' + entityCodes,
								buttons : [
									{
										name : '${getText("foundation.common.checked")}',
										id : 'dialog_btn_migrate',
										handler : function(){
											if(migwizar_iframe_step1.migrateForm.onsubmit()){
												var view2 = ec.module.migrateWizardDialog.getView( 'migwizar_iframe_step2' );
												if(!view2.loaded){
													view2.load();
												}
												view2.show();
												migwizar_iframe_step1.migrateForm.target = "migwizar_iframe_step2";
												migwizar_iframe_step1.migrateForm.submit();
											}
										}
									},
									{
										name : '${getText("calendar.common.cancal")}',
										handler : function(){
											ec.module.migrateWizardDialog.hide();
										}
									}

								]
							},
							{
								iframe : 'migwizar_iframe_step2',
								url: '/msService/ec/module/uploadwait',
								buttons : [
									{
										name : '${getText("foundation.inter.close")}',
										handler : function(){
											ec.module.migrateWizardDialog.hide();
										}
									}

								]
							}
						]
			});
		}else{
			var view1 = ec.module.migrateWizardDialog.getView( 'migwizar_iframe_step1' );
			view1.url = view1._url = '/msService/ec/module/migrate?srcmodule.code='+ec_module_Tree.getSelectedNodes()[0].code+'&entityCodes=' + entityCodes;
			ec.module.migrateWizardDialog.show();
		}




		/*return new CUI.Dialog({
			title : "${getHtmlText('ec.entity.migrate')}",
			iframe : "migrateFrame",
			width : 600,
			height : 350,
			modal : true,
			url : '/msService/ec/module/migrate?srcmodule.code='+ec_module_Tree.getSelectedNodes()[0].code+'&entityCodes=' + entityCodes,
			buttons:[
				{	name:"${getHtmlText('foundation.common.checked')}",
					handler:function(){console.log(1111);}
				},

				{	name:"${getHtmlText('common.button.close')}",
					handler:function(){this.close();}
				}
				]
		}).show();*/
	}


	ec.module.updateField = function(){
		if (ec_module_entity_datatableWidget.selectedRows.length == 0) {
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.checkFieldUpdate')}", "f");
			return;
		}
		var entityCodes = '';
		$.each(ec_module_entity_datatableWidget.selectedRows, function(i, row){
			entityCodes += entityCodes.length == 0 ? row.code : "," + row.code;
		});
		$.ajax({
			type : 'POST',
			url : '/msService/ec/module/updateField',
			data : {"entityCodes" : entityCodes},
			success : function(msg){
				if (msg && msg.success) {
					workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.updateFieldSuccessful')}", "s");
				} else {
					CUI.showErrorInfos(msg);
				}
			}
		});
	}
	ec.module.updateScript = function(){
		if(!ec_module_Tree.getSelectedNodes()[0] || ec_module_Tree.getSelectedNodes()[0].id == '-1'){   CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}!");return;}
		$.ajax({
			data : {"module.code": ec_module_Tree.getSelectedNodes()[0].code},
			url : '/msService/ec/module/execScript',
			success : function(msg){
				if(msg && msg.success){
					workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.exec.updateScriptSuccessfully')}", "s");
				} else {
					workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.exec.updateScriptFailed')}", "s");
				}
			}
		});
	}
	ec.module.exportEntity = function(){
	if(ec_module_Tree.getSelectedNodes().length<=0||ec_module_Tree.getSelectedNodes()[0].parentTId==null){
		workbenchErrorBarWidget.showMessage("${getHtmlText('ec.module.checkselected')}",'f');
		return;
	}
	CUI.Dialog.confirm(
	 "${getHtmlText('ec.module.isexporting')}",
		function(){
			$('#exportForm').find("input:hidden[name='module.code']").val(ec_module_Tree.getSelectedNodes()[0].code);
			CUI('#exportForm').submit();
		},
		function(){},
		"${getHtmlText('ec.view.hint')}",
		70,
		400
	);
	}
})();
</script>
<script type="text/javascript">
	$(function(){
		deployTypeChange();
	});

	function submitForm(){
		beforeDeploySubmit();
		var isDeploy = $('[name="isDeploy"]').val();
		var tasks = 8;//生成源码：16, 编译打包：4, 部署启动：8, 同步数据：32；组合任务可以将上面的数字相加
		var isUpdateTable = $("input[name='isUpdateTable']").val();
		var isCopy = $("input[name='isCopy']").val();
		if(isDeploy=='true'){
			var content = "";
			var initType = $('#init_type').val();
			if(initType=="0"){
				content = "${getText('ec.module.deploy.normal')}";
				tasks = tasks + 52;
			} else {
				content = "${getText('ec.module.deploy.fast')}";
				isUpdateTable = false;
				isCopy = false;
			}
			$("#tasks").val(tasks);
			$("#taskType").val(2);
			if(!confirm(content))return false;
			var envMode = "${getConfigProperty('orchid.env')}";
			if(envMode == "product" || (initType=="0" && $('#isGenerate').prop('checked'))){
				addTask(isUpdateTable, isCopy);
			} else {
				$.ajax({
					data : {"module.code": ec_module_Tree.getSelectedNodes()[0].code},
					url : '/msService/ec/module/checkModifyState',
					success : function(msg){
						if(msg){
							if(msg.success) {
								CUI.Dialog.confirm("${getText('ec.module.download.ungenerate')}", function(){
									addTask(isUpdateTable, isCopy);
								});
							} else {
								addTask(isUpdateTable, isCopy);
							}
						}
					}
				});
			}
		}
	}

	function addTask(isUpdateTable, isCopy){
		var taskList = [{
						moduleCode:ec_module_Tree.getSelectedNodes()[0].code,
						tasks:$("#tasks").val(),
						needUpdateDatabaseTables:isUpdateTable,
						needUpdateStaticResources:isCopy
					}];
		$.ajax({
            url: '/msService/ec/deploy/addTask',
            dataType: 'json',
            type: 'POST',
            data:{taskList: JSON.stringify(taskList)},
            success : function(res) {
            	if(res.length > 0){
            		$("#taskId").val(res[0].id).attr("start", res[0].position);
					$("#progressiveLog").attr({"start":res[0].position,"taskId":res[0].id});
            	}
            }
    	});

    	$('#generateSelectDiv').hide();
    	$('#progressiveLog').empty();
		$('#progressiveLog').show();
		generateTimer();
	}



	function deploySuccess(){
		ec.module.progressBar.finishedCallBack(function(){
			//ec.module.publishLast.text('所有模块发布完成！').css({'color':'#111', 'font-weight':'bold', 'font-size':'14px'});

			$(ec.module.progressBar._el).hide();
			ec.module.publishLast.text('').addClass("done");

		})
		ec.module.progressBar.finished('100%');
		clearTimeout(ec.module.timeout);
		ec.module.deployModules = [];
		ec.module.lastPercent = 0;
    	ec.module.deployModulesName = '';
		foundation.common.showPublishWaiting($('[name="ec_module_batchDeployWait"]'));
		ec.module.refreshModuleTree();
	}

	var isUpdateTable = false;
	var isCopy = true;
	var runtimeTaskId;
	var deployUser = "";
	function addTasks(i){
		$("#progressivebatchLog").attr({"start":0});
		$('#batchGenerateSelectDiv').hide();
		$('#btn_publish').hide();
    	$('#progressivebatchLog').empty();
		$('#progressivebatchLog').show();
		//$(ec.module.batchdeployDlg._buttonLeft).html('');

		var taskList = [];
		for(var j=0;j<ec.module.deployModules.length;j++){
			var module = ec.module.deployModules[j].split(",");
			taskList[j]= {
						moduleCode:module[0],
						tasks:module[1],
						needUpdateDatabaseTables:isUpdateTable,
						needUpdateStaticResources:isCopy
					};
		}
		$.ajax({
            url: '/msService/ec/deploy/addTask',
            dataType: 'json',
            async: false,
            type: 'POST',
            data:{taskList: JSON.stringify(taskList)},
            success : function(res) {
				runtimeTaskId = res;
				if(runtimeTaskId.length > 0){
					deployUser = runtimeTaskId[0].deployUser;
					if(deployUser != "") {
						if(deployUser == "system") deployUser = "${getText('foundation.inter.system')}";
						ec.module.deployUser.text(deployUser);
					}
					progressiveBatchLog(0,runtimeTaskId[0].id);
				}
            }
    	});
	}

	function getKeysRelation(ptid, k1, k2) {
        function genC(k){
            return ptid + '_checkall_' + k;
        }
        var o = {}
        var c1 = genC(k1);
        var c2 = genC(k2);
        o[c1] = c2;
        o[c2] = c1;
        return o;
    }

    ec.module.beforeCloseBatchPublish = function() {
      	$('#ec_modules_datatable').off('change');
    };

	ec.module.beforeBatchPublish = function () {
        var keyM = getKeysRelation('ec_modules_datatable', 'checkboxNormal', 'checkboxFast');
	    var extraCheckbox = '<span class="batchpublish-extra-checkbox"  ><a href="#" id="advancedSet" onclick="advancedSet(this)">${getText("foundation.inter.gjms")}</a></span> <span class="batchpublish-extra-checkbox" style = "display:none"><input type="checkbox" checked id="isUpdateTable_" name="isUpdateTable_" >${getText("foundation.inter.gxbjg")}</span>'

	    $(ec.module.batchdeployDlg._buttonLeft).html(extraCheckbox).show();
	    $('#ec_modules_datatable').on('change', 'input', function(e){
	        var target = e.target;
	        var $target = $(target);
	        if (target.checked) {
	            var name = $(target).attr('name');
	            var otherName = keyM[name];
	            var otherCkb = $target.parents('tr').find('input[name='+ otherName +']');
	            if (otherCkb.prop('checked')) {
	                otherCkb.click();
	            }else{
	            	$target.parents('tr').find('input[name="ec_modules_datatable_checkall_autoDeploy"]').attr('checked',true)
	            }
	        }else if(!target.checked && ($(target).attr('name') == 'ec_modules_datatable_checkall_checkboxNormal' || $(target).attr('name') == 'ec_modules_datatable_checkall_checkboxFast')){
	        		var name = $(target).attr('name');
		        	var otherName = keyM[name];
		            var otherCkb = $target.parents('tr').find('input[name='+ otherName +']');
			        if (!otherCkb.prop('checked')) {
		               $target.parents('tr').find('input[name="ec_modules_datatable_checkall_autoDeploy"]').attr('checked',false);
		               $('input[id="ec_modules_datatable_checkall_id_autoDeploy"]').attr('checked',false);
		            }else{
		            	var notCheckedSize = $('td[key="autoDeploy"][truevalue="false"]').size();
		            	if(notCheckedSize==0){
		            		$('input[id="ec_modules_datatable_checkall_id_autoDeploy"]').attr('checked',true);
		            	}
		            }
	        }

		})
	};
	function advancedSet(obj){
	var name = $(obj).text();
	if(name=='${getText("foundation.inter.ptms")}'){
       $('td[key="checkboxNormal"]').hide();
       $('td[key="checkboxFast"]').hide();
       $('td[key="autoDeploy"]').show();
       $(obj).text('${getText("foundation.inter.gjms")}');
    }else{
       $('td[key="checkboxNormal"]').show();
       $('td[key="checkboxFast"]').show();
       $('td[key="autoDeploy"]').hide();
       $(obj).text('${getText("foundation.inter.ptms")}');
    }
}
	function progressiveLog(){
		var start = $("#progressiveLog").attr("start") || 0;
		var id = $("#progressiveLog").attr("taskId");
		var taskType = $("#taskType").val()==0?"${getText('ec.module.create')}":$("#taskType").val()==1?"${getText('ec.module.package')}":"${getText('ec.scheduler.job.schedule')}";
		if(id > 0) {
			$.ajax({
				url : '/services/public/foundation/deploy/progressiveLog?taskId=' + id + "&progressiveType=simple&start=" + start,
				dataType : 'html',
 				async: false,
 				cache: false,
				success : function(data, textStatus, request) {
					$("#progressiveLog").attr("start",request.getResponseHeader('X-Text-Position'));
					if(request.getResponseHeader('X-Task-Status') == "WAITTING"){
						$("#progressiveLog").html("${getText('foundation.inter.zzfbshcs')}</br>");
					}
					$("#progressiveLog").append(data);
					if(request.getResponseHeader('X-Task-Status') === 'FINISHED') {
						clearInterval(timer);
						$("#progressiveLog").attr("start", 0);
						showButtonLeft($("#taskType").val(),id);
						CUI.Dialog.alert('${getText("foundation.inter.model")} '+taskType+' ${getText("foundation.inter.success")}');
					}else if(request.getResponseHeader('X-Task-Status') === 'FAILED'){
						clearInterval(timer);
						$("#progressiveLog").attr("start", 0);
						showButtonLeft($("#taskType").val(),id);
						CUI.Dialog.alert('${getText("foundation.inter.model")} '+taskType+' ${getText("foundation.inter.failed")}');
					}
				},
				error: function(){
					clearInterval(timer);
					$("#progressiveLog").attr("start", 0);
					showButtonLeft($("#taskType").val(),id);
					CUI.Dialog.alert('${getText("foundation.inter.model")} '+taskType+' ${getText("foundation.inter.failed")}');
				}
			})
		}
	}
	function showButtonLeft(taskType,taskId){
		if(undefined == taskId || "" == taskId){
			return;
		}
		if(taskType == 1){
			$(ec.module.packageDlg._buttonLeft).html('<a class="cui-link-dialog" href="/msService/ec/deploy/down-log?taskId='+taskId+'">${getText("foundation.inter.xzxxrz")}</a>');
		}else if(taskType == 0){
			$(ec.module.generateDlg._buttonLeft).html('<a class="cui-link-dialog" href="/msService/ec/deploy/down-log?taskId='+taskId+'">${getText("foundation.inter.xzxxrz")}</a>');
		}
	}
	var timer;
	function generateTimer() {
		if(timer){
			clearInterval(timer);
		}
		timer = setInterval(progressiveLog, 1000);
		return timer;
	}

	function dgMangRenderOverEvent(){

	}
	function beforeDeploySubmit(){
		<#if isDev?string=='false'>
		$("input[name='isGenerate']").val("true");
		$("input[name='isUpdateTable']").val("true");
		$("input[name='isCopy']").val("true");
		<#else>
		$("input[name='isGenerate']").val($('#isGenerate').prop('checked'));
		$("input[name='isUpdateTable']").val($('#isUpdateTable').prop('checked'));
		$("input[name='isCopy']").val($('#isCopy').prop('checked'));
		</#if>
		return true;
	}
	function deployTypeChange(){
		var deployType = $('#init_type').val();
		var showTr = $('#deployOperate_tr');
		if(deployType == 0){
			if(showTr){
				showTr.show();
			}
		} else {
			if(showTr){
				showTr.hide();
			}
		}
	}

	 /*
	发布记录
	*/
	ec_upload_deploy_info = function (){
		var dialog  = new CUI.Dialog({
				title: '${getText("foundation.inter.history")}',
				url :'/msService/ec/module/history',
				width:850 || 460,
				height: 630 || 330
			});
		dialog.show();
	}

	ec.module.uploadBatch = function(){

		var moduleCode = (ec_module_Tree.getSelectedNodes()[0]) ? ec_module_Tree.getSelectedNodes()[0].code : '-1';

		if(moduleCode != -1 && (ec_module_Tree.getSelectedNodes()[0].isInherentedBase==true || ec_module_Tree.getSelectedNodes()[0].code == 'sysbase_1.0')){
			CUI.Dialog.alert("${getHtmlText('ec.module.uploadInherent')}");
			return;
		}

		ec.module.uploadWizardDialogBatchBatch_prev = null;
		ec.module.loading = null;
		$.ajax({
				type : 'post',
				url : '/msService/ec/module/uploadBatchHasTask',
				async:false,
				success : function(res){
					console.log(res);
					if(res.success==true){
							if(ec.module.uploadWizardDialogBatch){
								ec.module.uploadWizardDialogBatch.show();
								return false;
							}
							ec.module.uploadWizardDialogBatch = new CUI.WizardDialog({
							title: "${getHtmlText('foundation.inter.plszjk')}",
							size: 4,
							close: true, // 关闭后再打开时时重新开始
							steps :	[

										{
											iframe : 'wizar_iframe_batch_step1',
											onload:function(){
												wizar_iframe_batch_step1.$( '#changeFile' ).val( 'false' );
												console.log(wizar_iframe_batch_step1.$('#receiveFile'));
												setTimeout(function() {
													wizar_iframe_batch_step1.$('#receiveFile').click();
												}, 1000);
											},
											url: '/msService/ec/module/uploadBatch',
											buttons : [
												{
													name : '${getText("foundation.inter.nextstep")}',
													id : 'dialog_btn_upload_manage',
													handler : function(){
														var view2 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step2' );
														if(!view2.loaded || wizar_iframe_batch_step1.uploadBatchForm.changeFile.value == 'true'){
															if(wizar_iframe_batch_step1.uploadBatchForm.onsubmit()){
																view2.show();
																view2.load();
																$('#dialog-iframe-loading').remove();
																ec.module.loading = new CUI.loading({
																    "head": "${getText('foundation.inter.loadingwait')}",
																    "opacity":50,
																    "bgColor":"#666666",
																    "show": true
																});
																$('#loading_wrap').css('zIndex', 100000);
																wizar_iframe_batch_step1.uploadBatchForm.target = "wizar_iframe_batch_step2";
																wizar_iframe_batch_step1.uploadBatchForm = "/msService/ec/module/uploadBatchModules?isMsService=${isMsService}";
																wizar_iframe_batch_step1.uploadBatchForm.submit();
															}
														}else{
															view2.show();
														}

													}
												},
												{
													name : '${getText("calendar.common.cancal")}',
													handler : function(){
														ec.module.uploadWizardDialogBatch.hide();
													}
												}

											]
										},

										{
											iframe : 'wizar_iframe_batch_step2',
											buttons : [
												{
													name : '${getText("foundation.inter.sz")}',
													id : 'dialog_btn_upload_batch2',
													handler : function(){
														ec.module.uploadWizardDialogBatchBatch_prev = 'wizar_iframe_batch_step2';
														if(wizar_iframe_batch_step2.uploadBatch.onsubmit()){
															var view3 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step3' );

															view3.show();
															view3.load();

															wizar_iframe_batch_step2.uploadBatch.target = "wizar_iframe_batch_step3";
															wizar_iframe_batch_step2.uploadBatch.submit();
														}
													}

												},

												{
													name : '${getText("foundation.inter.close")}',
													id : 'dialog_btn_upload_close2',
													handler : function(){
														ec.module.uploadWizardDialogBatch.hide();
													}
												}

											]
										},

										{
											iframe : 'wizar_iframe_batch_step3',
											url: '/msService/ec/module/uploadwait',
											buttons : [
												/*{
													name : '${getText("foundation.inter.prevstep")}',
													id : 'frame3_preStep_Btn',
													handler : function(){
														var view3 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step3' );
														view3.iframe.prop( 'src','about:blank' );
														if(ec.module.uploadWizardDialogBatchBatch_prev == 'wizar_iframe_batch_step1'){
															var view1 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step1' );
															wizar_iframe_batch_step1.uploadBatchForm.target = "transfer";
															view1.show();
															$( '#dialog_btn_migrate', ec.module.uploadWizardDialogBatch.dialog._buttonbar ).attr('canclick','false').addClass( 'cui-btn-gray' );
														}else{
															var view2 = ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step2' );
															view2.show();
														}
													}
												},		*/

												{
													name : '${getText("foundation.inter.close")}',
													id : 'closeBtn',
													handler : function(){
														ec.module.uploadWizardDialogBatch.hide();
													}
												}

											]
										}




									]

						});

					}else{

						ec.module.uploadWizardDialogBatch = new CUI.WizardDialog({
						title: "${getHtmlText('foundation.inter.plszjk')}",
						size: 4,
						close: true,
						steps :	[
									{
										iframe : 'wizar_iframe_batch_step3',
										url: '/msService/ec/module/uploadBatchProcessUsed',
										buttons : [
											{
												name : '${getText("foundation.inter.close")}',
												id : 'closeBtn',
												handler : function(){
													ec.module.uploadWizardDialogBatch.hide();
												}
											}
										]
									}
								]

						});
					}
				},
				error:function(res){
					CUI.Dialog.alert('${getText("foundation.inter.systemerror")}')
				}
			});
		}


	function checkupload(){
		$.ajax({
				type : 'post',
				url : '/msService/ec/module/uploadBatchHasTask',
				async:false,
				success : function(res){
					if(res.success==true){
						console.log('准备上载')
					}else{
						console.log('有任务在上载')
					}
				},
				error:function(res){
					CUI.Dialog.alert('${getText("foundation.inter.systemerror")}')
				}
			});
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
</div>
<iframe style="display:none;" id="generateWaitForm" name="generateWaitForm" frameborder="0" scrolling="auto" width="100%" height="313" src="about:blank"></iframe>
<div id="progressiveLog" style="display:none;" ></div>
</div>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>