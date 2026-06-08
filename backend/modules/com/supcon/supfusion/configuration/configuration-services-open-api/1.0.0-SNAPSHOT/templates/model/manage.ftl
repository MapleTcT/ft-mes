<style>
<!--
#ec_project_model_list{margin:5px 10px 0 5px;}
#ec_project_model_list li{height:20px;line-height: 20px;padding-left:30px;background-repeat: no-repeat;background-position: 10px center;}
.ic0{background-image: url("/bap/static/ec/images/table_row_delete.gif");}
.ic1{background-image: url("/bap/static/ec/images/table_row_insert.gif");}
.cui-btn{border:1px solid #e3f1f9;}
.elm-layout-unit-center .cui-btn{border:1px solid #E3F1F9;}
.elm-layout-unit-south .cui-btn{border:1px solid #f2f9ff;}
.model_selected,.model_hover{background-color:#0269A3; color:#FFF;cursor:default;}
-->
</style>
<@frameset id="ec_model_manage">
	<@frame id="ec_model_manage_top" region="north" height=40 region="north">
		<div class="cui-main-title">
	     	<@loadpanel text="${getText('ec.project.datamodel_setting')}" />
			<div class="cui-main-title-r">
				<#--
				<@operatebar operates="code:ec_model_btn_add||name:${getHtmlText('ec.model.add')}||iconcls:add||onclick:ec.model.add();
						code:ec_model_btn_mod||iconcls:edit||name:${getHtmlText('ec.model.edit')}||onclick:ec.model.mod();
						code:ec_model_btn_del||iconcls:del||name:${getHtmlText('ec.model.del')}||onclick:ec.model.del();
						code:ec_property_btn_add||name:${getHtmlText('ec.model.propertyAdd')}||iconcls:add||onclick:ec.property.add();
						code:ec_property_btn_mod||iconcls:edit||name:${getHtmlText('ec.model.propertyEdit')}||onclick:ec.property.mod();
						code:ec_property_btn_del||iconcls:del||name:${getHtmlText('ec.model.propertyDel')}||onclick:ec.property.del()"
						生成固有字段按钮
						code:ec_property_btn_ini||iconcls:del||name:生成字段||onclick:ec.property.addInherented()"				
						operateType="noPower">
				</@operatebar>
				${getText('ec.model.showInherent')}:<input type="checkbox" id="showInherent" name="showInherent"  onclick="ec.model.showInherent(this)"/>
				-->
				<input type="hidden" value="false" id="showInherent_hide"/>
				<input type="hidden" id="showCustom_hide" value="false" />
				<input type="hidden" id="isBase" value="${entity.isBase?c}" />
			</div>
		</div>
	</@frame>
	<@frame id="left_in" region="west" width=200 resize=true style="overflow:auto">
		<@tree id="ec_model_Tree" nameCol="nameInternational" dataUrl="/msService/ec/model/list?entity.code="+entity.code rootName="${getText('ec.model.list')}"
			 callback="{onClick:function(event,treeId,node){ec.model.showModuleInfo(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
	</@frame>
	<@frame id="ec_project_datamodel_container_main" region="center">
	 	<div align="right" style="line-height:25px;">
		
		</div>
		<@datatable caption="${getText('ec.model.property')}"  dtPage="properties" hidekey="['code','isInherent','version']" id="ec_model_property_table" editable=false dblclick="ec.property.mod">
			<@operatebar operates="code:ec_model_btn_add||name:${getHtmlText('ec.model.add')}||iconcls:add||onclick:ec.model.add();
					code:ec_model_btn_mod||iconcls:edit||name:${getHtmlText('ec.model.edit')}||onclick:ec.model.mod();
					code:ec_model_btn_del||iconcls:del||name:${getHtmlText('ec.model.del')}||onclick:ec.model.del();
					code:ec_sqlmodel_btn_add||name:${getHtmlText('ec.model.addsql')}||iconcls:add||onclick:ec.model.addsql();
					code:ec_property_btn_add||name:${getHtmlText('ec.model.propertyAdd')}||iconcls:add||onclick:ec.property.add();
					code:ec_property_btn_mod||iconcls:edit||name:${getHtmlText('ec.model.propertyEdit')}||onclick:ec.property.mod();
					code:ec_property_btn_del||iconcls:del||name:${getHtmlText('ec.model.propertyDel')}||onclick:ec.property.del();
					code:ec_property_btn_Inherent||iconcls:eighteen-dt-show-inherent-field||name:${getHtmlText('ec.model.showInherent')}||onclick:ec.model.showInherent();
					code:ec_property_btn_sort||iconcls:eighteen-dt-sort-field||name:${getHtmlText('ec.model.sort')}||onclick:ec.model.sort();
					code:ec_property_btn_addCustom||iconcls:eighteen-dt-generate-custom-field||name:${getHtmlText('ec.model.addCustom')}||onclick:ec.model.editCustomProps();
					code:ec_property_btn_showCustom||iconcls:eighteen-dt-show-custom-field||name:${getHtmlText('ec.model.showCustom')}||onclick:ec.model.showCustomProps()"
					<#-- 生成固有字段按钮
						code:ec_property_btn_ini||iconcls:del||name:生成字段||onclick:ec.property.addInherented()" -->
					operateType="noPower" resultType="json" >
			</@operatebar>
			<@datacolumn key="name" label="${getHtmlText('ec.property.name')}" width=150 />
			<@datacolumn key="displayNameInternational" label="${getHtmlText('ec.property.displayName')}" width=150 />
			<@datacolumn key="type" label="${getHtmlText('ec.property.type')}" width=130 />
			<@datacolumn textalign="center" type="boolean" key="isIndex" sortable=false label="${getHtmlText('ec.property.isIndex')}" width=100 />
			<@datacolumn textalign="center" type="boolean" key="nullable" sortable=false label="${getHtmlText('ec.property.nullable')}" width=100 />
			<@datacolumn textalign="center" type="boolean" key="isInherent" sortable=false label="${getHtmlText('ec.property.isInherent')}" width=100 />
			<@datacolumn textalign="center" type="boolean" key="isCustom" sortable=false label="${getHtmlText('ec.property.customFields')}" width=100 />
			<@datacolumn key="columnName" label="${getHtmlText('ec.property.columnName')}" width=130 />
		</@datatable>
	</@frame>
</@frameset>
<div id="ec_model_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div>
	${getHtmlText('ec.entity.checkDeleteModel')}
	</div>
</div>
</div>
<div id="ec_property_manage_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div>
	${getHtmlText('ec.entity.checkDeleteProperty')}
	</div>
</div>
</div>
<script type="text/javascript">
function _dialog(title,url,callback,callback2,width,height){
	var dialogButtons = [
			{	name:"${getHtmlText('common.button.save')}",
				handler:function(){callback(this);}
			},
			{	name:"${getHtmlText('common.button.cancel')}",
				handler:function(){this.close();}
			}];
	if(callback2) {
		dialogButtons[2] = dialogButtons[1];
		dialogButtons[1] = {name:"${getText('common.button.saveadd')}",
		handler:function(){callback2(this);}}
	}
	return new CUI.Dialog({
		title : title,
		width : width?width:550,
		height : height?height:350,
		modal : true,
		url : url,
		buttons:dialogButtons
	});
}

function _sqldialog(title,url,callback1,callback2,callback3,width,height){
	var dialogButtons = [
			{	name:"${getHtmlText('common.button.next')}",
				id:'buttonNext',
				handler:function(){callback1(this);}
			},
			{	name:"${getHtmlText('common.button.back')}",
				id:'buttonBack',
				handler:function(){callback2(this);}
			},
			{	name:"${getHtmlText('common.button.save')}",
				id:'buttonSave',
				handler:function(){callback3(this);}
			},
			{	name:"${getHtmlText('common.button.cancel')}",
				handler:function(){this.close();}
			}];
	return new CUI.Dialog({
		title : title,
		width : width?width:550,
		height : height?height:350,
		modal : true,
		url : url,
		onload : 'hideCustomPropEl',
		buttons:dialogButtons
	});
}

/**
* 修改自定义字段，隐藏某些表单元素
*/
function hideCustomPropEl() {
	if (ec_model_property_tableWidget.selectedRows[0].isCustom == true) {
		$('#baseproperty_1_2').hide();
		$('input[name="property_isUsedForList"]').parent().hide();
		$('input[name="property_isUsedForList"]').parent().prev().hide();
		$('#mneControl').hide();
		$('#baseproperty_base_sen').hide();
		$('input[name="property_isMainDisplay"]').parent().hide();
		$('input[name="property_isMainDisplay"]').parent().prev().hide();
		$('#baseproperty_base_7').hide();
		$('#baseproperty_enum_systemcode_1_1').hide();
		$('#ec_select_module').prop('disabled',true);
		$('#ec_select_entity').prop('disabled',true);
		$('#ec_select_model').prop('disabled',true);
		$('#ec_select_property').prop('disabled',true);
		$('[name="property.fillcontent"]').prop('disabled', true);
		$('[id="associated_1"]').hide();
	}
}

(function(){
	//注册命名空间
	CUI.ns("ec.model");
	CUI.ns("ec.property");
	CUI.ns("ec.ass");
	CUI.ns("ec.entity");
	ec.model.showInherent=function(){
		if (ec_model_Tree.getSelectedNodes()[0] != null && ec_model_Tree.getSelectedNodes()[0].level > 0) {
			var code =ec_model_Tree.getSelectedNodes()[0].code;
			var hide_value = $("#showInherent_hide").val();
			if(hide_value==='true'){
				$("#showInherent_hide").val("false");
				$('#ec_property_btn_Inherent').text("${getText('ec.model.showInherent')}");
			}else{
				$("#showInherent_hide").val("true");
				$('#ec_property_btn_Inherent').text("${getText('ec.model.hideInherent')}");
			}
			datatable_ec_model_property_table.setRequestDataUrl('/msService/ec/property/list?model.code=' + code + '&showInherent=' + $("#showInherent_hide").val() + '&showCustom=' + $('#showCustom_hide').val());
		}
	}
	/**
	 * 字段排序
	 * @method ec.model.sort
	 * @author mkp 
	 */
	ec.model.sort=function(){
	    if(ec_model_Tree.getSelectedNodes()[0]==null || ec_model_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.model.choiceModel')}");
			return;
		}
		
		var url = "/msService/ec/property/sortitem?model.code="+ec_model_Tree.getSelectedNodes()[0].code+"&openType=frame";
		if( ec.model.dialog_sort ){
			ec.model.dialog_sort._config.url = url
		} else{	
			ec.model.dialog_sort = new CUI.Dialog({
				title: "${getHtmlText('ec.model.sort')}",
				url: url,
				modal:true,
				//type:3,
				width:660,
				height:500,
				
				iframe:'ec_model_sortitem_iframe',
				
				dragable:true,
				onload: 'ec.model.initDialog',
				buttons:[{	name:"${getHtmlText('common.button.save')}",
			            id:"saveButton",
						handler:function(){ec.model.SaveColOrder()}
					},
					{	name:"${getHtmlText('common.button.cancel')}",
						handler:function(){this.close()}
					}]
			})
		}
		ec.model.dialog_sort.show();
	}
	/**
	 * 字段排序结果保存
	 * @method ec.model.SaveColOrder
	 * @author mkp 
	 */
	ec.model.SaveColOrder = function(){
	    CUI.Dialog.toggleAllButton();
		var orderModel='';
		var arr = ec_model_sortitem_iframe.CUI('li[id^="propertyColOrder_"]');
		for(var i=0;i<arr.length;i++){
			var id=CUI(arr[i]).attr("colid");
			orderModel+=";"+id+","+i;
		}
		orderModel=orderModel.substr(1);
		ec_model_sortitem_iframe.CUI("#orderModelID").val(orderModel);
		CUI.post("/msService/ec/property/sortitem/orderModelColSave",
		ec_model_sortitem_iframe.CUI('#SubmitModelColOrderForm').serialize(), function(res){
			CUI.Dialog.toggleAllButton();
			try{ec.model.dialog_sort.close();}catch(e){}
			if(res.dealSuccessFlag == true){
				setTimeout(function(){ec.model.showModuleInfo(ec_model_Tree.getSelectedNodes()[0]);},500);
				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.infoSet.orderSuccess')}","s");
			}else{
				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.infoSet.orderfailure')}","s");
			}
		});
	
  }
	/**
	 * 点击模型树节点，显示字段详细信息
	 * @method ec.model.showModuleInfo
	 * @param {Node} oNode
	 */
	ec.model.showModuleInfo = function(oNode){
		// 处理
		var id = '';
		if(oNode != null && oNode != 'undefined'){
			id = oNode.id;
		}
		var selectedNode = ec_model_Tree.getSelectedNodes()[0];
		if(selectedNode.type == 3) { /*sql模型隐藏字段操作按钮*/
			$("#ec_property_btn_add").parent().css("display",'none');
			$("#ec_property_btn_del").parent().css("display",'none');
			$("#ec_property_btn_mod").parent().css("display",'none');
			$("#ec_property_btn_addCustom").parent().css("display",'none');
			$("#ec_property_btn_showCustom").parent().css("display",'none');
		} else {
			$("#ec_property_btn_add").parent().css("display",'inline-block');
			$("#ec_property_btn_del").parent().css("display",'inline-block');
			$("#ec_property_btn_mod").parent().css("display",'inline-block');
			$("#ec_property_btn_addCustom").parent().css("display",'inline-block');
			$("#ec_property_btn_showCustom").parent().css("display",'inline-block');
		}		
		datatable_ec_model_property_table.setRequestDataUrl('/msService/ec/property/list?model.code=' + oNode.code + '&showInherent=' + $('#showInherent_hide').val() + '&showCustom=' + $('#showCustom_hide').val());
	};
	ec.model.editDlg;
	/**
	 * sql模型信息添加
	 * @method ec.model.addsql
	 * @public
	 */
	 ec.model.addsql = function() {
	 	var isBase = $("#isBase").val();
	 	if (isBase==='false') {
	 		CUI.Dialog.alert("${getHtmlText('ec.model.sql.nosupport')}");
	 		return false;
	 	}
	 	var dlg= ec.model.sqlDialog = _sqldialog("${getText('ec.model.addsql')}","/msService/ec/sqlmodel/edit?entity.code=${entity.code!}",
	 		function(){
	 			$("#ec_model_editSql_form input[name='model.entity.code']").val("${entity.code}");
				if (ec.model.codeValidate()){
					ec.model.loadSqlProperties();
				} else{
					return false;
				}
	 		},
	 		function(){
	 			$("#sqlModelDlg").css('display','block');
				$("#sqlPropertiesDlg").css('display','none');
				$("#buttonNext").show();
				$("#buttonBack").hide();
				$("#buttonSave").hide();
	 		},
	 		function(){
	 			if (ec.model.beforesubmit()) {
	 				$("#ec_model_editSql_form").submit();
	 			}
	 		}, 750, 500);
	 	$("#buttonSave").hide();
		$("#buttonBack").hide();
		dlg.show();
	}
	
	/**
	 * sql模型信息修改
	 * @method ec.model.modsql
	 * @public
	 */
	 ec.model.modsql = function() {
		var dlg= ec.model.sqlDialog = _sqldialog("${getText('ec.model.edit')}","/msService/ec/sqlmodel/edit?entity.code=${entity.code!}&model.code="+ ec_model_Tree.getSelectedNodes()[0].code,
	 		function(){
	 			$("#ec_model_editSql_form input[name='model.entity.code']").val("${entity.code}");
				if (ec.model.codeValidate()){
					ec.model.loadSqlProperties();
				} else{
					return false;
				}
	 		},
	 		function(){
	 			$("#sqlModelDlg").css('display','block');
				$("#sqlPropertiesDlg").css('display','none');
				$("#buttonNext").show();
				$("#buttonBack").hide();
				$("#buttonSave").hide();
	 		},
	 		function(){
	 			if (ec.model.beforesubmit()) {
	 				$("#ec_model_editSql_form").submit();
	 			}
	 		}, 750, 500);
	 	$("#buttonSave").hide();
		$("#buttonBack").hide();
		dlg.show();
	}
	/**
	 * 模型信息添加
	 * @method ec.model.add
	 * @public
	 */
	ec.model.add=function(){
		var dlg=ec.model.editDlg = _dialog("${getText('ec.model.add')}","/msService/ec/model/edit?entity.code=${entity.code}",function(){
			$("#ec_model_edit_form input[name='model.entity.code']").val("${entity.code}");
			if (ec.model.codeValidate()){
				$("#ec_model_edit_form").submit();
			}else{
				return false;
			}
		});
		dlg.show();
		
		
	}
	/**
	 * 模型信息修改
	 * @method ec.model.mod
	 * @public
	 */
	 ec.model.mod=function(){
	 	var selectedNode = ec_model_Tree.getSelectedNodes()[0];
		if(ec_model_Tree.getSelectedNodes()[0]==null || ec_model_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.model.alert.choiceedit')}");
			return;
		}
		if(selectedNode.type == 3) {
			ec.model.modsql();
		}else {
			var dlg=ec.model.editDlg = _dialog("${getText('ec.model.edit')}","/msService/ec/model/edit?entity.code=${entity.code}&model.code="+ ec_model_Tree.getSelectedNodes()[0].code,function(){
				if($("input[name='xmlString']").val()!=""&&$("[name='model.configBussinePermissionRight']").val()!=null&& $("[name='model.configBussinePermissionRight']").val()=="false")  {
					ec_model_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.specialpermission.mustHoldOneLevel')}");
					return false;
				}
				$("#ec_model_edit_form").submit();
			},"",null,400);
			dlg.show();
		}
	}
	/**
	 * 模型信息删除 add by yubo20171221
	 * @method ec.model.del
	 */
	 ec.model.del=function(){
	 	if(ec_model_Tree.getSelectedNodes()[0]==null || !ec_model_Tree.getSelectedNodes()[0].code || ec_model_Tree.getSelectedNodes()[0].level==0){
			CUI.Dialog.alert("${getHtmlText('ec.model.alert.choicedel')?js_string}");
			return;
		}
		
		ec.model.dialog = new CUI.Dialog({
				title: "${getHtmlText('ec.model.del')}",
				elementId: "ec_model_manage_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
						
						//{	name:"${getHtmlText('ec.common.void')}",
						//	handler:function(){
						//		this.close();
						//		ec.model.deleteMethod("/msService/ec/model/ordinaryDelete");
						//	}
						//},

						{	name:"${getHtmlText('ec.view.delete')}",
							handler:function(){
								this.close();
								ec.model.deleteMethod("/msService/ec/model/delete");
							
							}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.model.dialog.show();
		
	 }
	 
	 
	 ec.model.deleteMethod = function(url) {
		closeLoadPanel();
		createLoadPanel(false, null, {head: '正在处理，请稍等', show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"model.code":ec_model_Tree.getSelectedNodes()[0].code,
						"model.version":ec_model_Tree.getSelectedNodes()[0].version
						},
				url : url , 
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success){
						CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
						var rootNode = ec_model_Tree.getNodeByParam("id", -1);
						ec_model_Tree.reAsyncChildNodes(rootNode, "refresh");
						ec.model.refresh();
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
	 
	 
	 /**
	 * 模型信息提交返回信息及回刷树
	 * @method ec.module.Callback
	 */
	ec.model.Callback=function(msg){
		if(msg && msg.success){
			ec_model_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.model.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.model.editDlg.close();}catch(e){}
				var rootNode = ec_model_Tree.getNodeByParam("id", -1);
				ec_model_Tree.reAsyncChildNodes(rootNode, "refresh");
			},1500);
		}else{
			ec_model_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.model.submitfailure')}");
			setTimeout(function(){
				try{ec.model.editDlg.close();}catch(e){}
			},1500);
         }
		
	}
	
	/**
	 * SQL模型信息提交返回信息及回刷树
	 * @method ec.module.Callback
	 */
	ec.model.SqlCallback=function(msg){
		if(msg && msg.success){
			ec_model_editSql_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.model.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.model.sqlDialog.close();}catch(e){}
				var rootNode = ec_model_Tree.getNodeByParam("id", -1);
				ec_model_Tree.reAsyncChildNodes(rootNode, "refresh");
			},1500);
		}else{
			ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.submitfailure')}");
			setTimeout(function(){
				try{ec.model.sqlDialog.close();}catch(e){}
			},1500);
         }
		
	}
	
	 /**
	 * 字段添加
	 * @method ec.property.add
	 */
	ec.property.add=function(){
		if(ec_model_Tree.getSelectedNodes()[0]==null || ec_model_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.model.choiceModel')}");
			return;
		}
		var dlg = ec.property.editDlg=_dialog("${getHtmlText('ec.model.propertyAdd')}","/msService/ec/property/edit?entity.code=${entity.code}&model.code="+ec_model_Tree.getSelectedNodes()[0].code,function(d){
			$("#ec_property_edit_form input[name='property.model.code']").val(ec_model_Tree.getSelectedNodes()[0].code);
			if (ec.property.nameValidate()){
				$("#ec_property_edit_form").submit();
			}else{
				return false;
			}
		}, function(){
			$("#ec_property_edit_form input[name='property.model.code']").val(ec_model_Tree.getSelectedNodes()[0].code);
			if (ec.property.nameValidate()){
				ec.property.renew = true;
				$("#ec_property_edit_form").submit();
			}else{
				return false;
			}
		},null,540,500);
		dlg.show();
	}
	 /**
	 * 字段修改
	 * @method ec.property.mod
	 */
	ec.property.mod=function(){
		if(datatable_ec_model_property_table.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.model.choicePro')}");
			return;
		}
		if(ec_model_Tree.getSelectedNodes()[0].type == 3) {
			CUI.Dialog.alert("${getHtmlText('ec.model.sqlproperty.cannotedit')}");
		} else {
			//if(datatable_ec_model_property_table.selectedRows[0].isInherent==true){
			//	CUI.Dialog.alert("${getHtmlText('ec.model.cannotedit')}");
			//	return;
			//}
			var dlg = ec.property.editDlg = _dialog("${getHtmlText('ec.model.propertyEdit')}", "/msService/ec/property/edit?entity.code=${entity.code}&property.code=" + datatable_ec_model_property_table.selectedRows[0].code,
				function(d){
					if (datatable_ec_model_property_table.selectedRows[0].isInherent || ec.property.nameValidate()){
						$("#ec_property_edit_form").submit();
					}else{
						return false;
					}
				}, 
				function(){
					$("#ec_property_edit_form input[name='property.model.code']").val(ec_model_Tree.getSelectedNodes()[0].code);
					if (datatable_ec_model_property_table.selectedRows[0].isInherent || ec.property.nameValidate()){
						ec.property.renew = true;
						$("#ec_property_edit_form").submit();
					}else{
						return false;
					}
				}, null, 540, 500);
			dlg.show();
		}
	}
	
	 /**
	 * 字段删除 add by yubo20171221
	 * @method ec.property.del
	 */
	ec.property.del=function(){
		if(datatable_ec_model_property_table.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.model.choicedelpro')}");
			return;
		}
		
		ec.property.dialog = new CUI.Dialog({
				title: "${getHtmlText('ec.common.delChoise')}",
				elementId: "ec_property_manage_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
				
						//{	name:"${getHtmlText('ec.common.void')}",
						//	handler:function(){
						//		this.close();
						//		ec.property.deleteMethod("/msService/ec/property/ordinaryDelete");
						//	}
						//},
						
						{	name:"${getHtmlText('ec.view.delete')}",
							handler:function(){
								this.close();
								ec.property.deleteMethod("/msService/ec/property/delete");
							}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.property.dialog.show();
		
	}
	
	
	ec.property.deleteMethod = function(url) {
		closeLoadPanel();
		createLoadPanel(false, null, {head: '正在处理，请稍等', show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {"property.code":datatable_ec_model_property_table.selectedRows[0].code,
						"property.version":datatable_ec_model_property_table.selectedRows[0].version
				},
				url : url , 
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.success){
						CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
						ec.model.showModuleInfo(ec_model_Tree.getSelectedNodes()[0]);
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

	
	
	
	/**
	 * 字段信息提交返回信息及回列表
	 * @method ec.property.Callback
	 * @public
	 */
	ec.property.Callback=function(msg){
		var modelCode = "";
		if(ec_model_Tree.getSelectedNodes()[0]!=null) {
			modelCode = ec_model_Tree.getSelectedNodes()[0].code;
		}
		if(msg && msg.success){
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.property.editDlg.close();}catch(e){}
				if(ec.property.renew) {
					ec.property.add();
					ec.property.renew = false;
				}
				ec.model.refresh();
				},1500);
		}else{
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.submitfailure')}");
			setTimeout(function(){
				try{ec.property.editDlg.close();}catch(e){}
				ec.model.refresh();
				},1500);
         }
	}
	ec.model.refresh=function(){
		if (ec_model_Tree.getSelectedNodes()[0] != null && ec_model_Tree.getSelectedNodes()[0].level > 0) {
						ec.model.showModuleInfo(ec_model_Tree.getSelectedNodes()[0]);
		}else{
			$(".menu_item_content.datamodel_item").parent().click();
		}
	}
	ec.property.addInherented = function(){
		if(ec_model_Tree.getSelectedNodes()[0]!=null) {
			modelCode = ec_model_Tree.getSelectedNodes()[0].code;
		}else{
			CUI.Dialog.alert("${getHtmlText('ec.model.choiceModel')}");
			return false;
		}
		CUI.ajax({
			url: '/msService/ec/property/addInherent',
			type: 'post',
			async: false,
			data: "model.code=" + modelCode,
			success: function(response){
				if (response == false) {
					alert("<s:text name='foundation.company.cannotdelete'/>");
					checkFlag = false;
				}
			}
		});
	}
	
	/**
	* 生成自定义字段
	*/
	ec.model.editCustomProps = function() {
		if ( ec_model_Tree.getSelectedNodes()[0] == null || ec_model_Tree.getSelectedNodes()[0].level==0 ) {
			CUI.Dialog.alert("${getHtmlText('ec.model.choiceModel')}");
			return false;
		}
		if ( ec_model_Tree.getSelectedNodes()[0].type==3 ) {
			CUI.Dialog.alert("${getHtmlText('ec.model.sqlModelNoSupport')}");
			return false;
		}
		ec.model.editCustomPropsDialog = new CUI.Dialog({
			title    :  '配置自定义字段',
			formId   :  'ec_edit_customProps_form',
			width    :  330,
			height   :  285,
			url      :  "/msService/ec/property/editCustomProps?model.code=" + ec_model_Tree.getSelectedNodes()[0].code,
			modal    :  true,
			dragable :  true,
			buttons  :  [{ name    : "${getHtmlText('common.button.save')}",
						   handler : function(){
						   		ec.property.saveCustomProps();
						   }
						},
						{ name    : "${getHtmlText('common.button.cancel')}",
						  handler : function(){this.close();}
						}]
		
		}) ;
		ec.model.editCustomPropsDialog.show();
	}
	
	/**
	* 显示/隐藏自定义字段
	*/
	ec.model.showCustomProps = function() {
		var code = ec_model_Tree.getSelectedNodes()[0].code;
		var hide_value = $("#showCustom_hide").val();
		if (String(hide_value) == 'true') {
			$("#showCustom_hide").val(false);
			$('#ec_property_btn_showCustom').text("${getText('ec.model.showCustom')}");
		} else {
			$("#showCustom_hide").val(true);
			$('#ec_property_btn_showCustom').text("${getText('ec.model.hideCustom')}");
		}
		datatable_ec_model_property_table.setRequestDataUrl('/msService/ec/property/list?model.code=' + code + '&showInherent=' + $("#showInherent_hide").val() + '&showCustom=' + $("#showCustom_hide").val());
	}
	
})();
</script>
