
	<style type="text/css">
	.cui-label-font{bottom:0px;}
	</style>
<script type="text/javascript" src="/bap/static/frameset/${getCurrent('lang')}/layout.js"></script>
<link rel="stylesheet" type="text/css" href="/bap/static/frameset/assets/layout.css" />
<#--code:base_systemCode_modityValue||onclick:foundation.systemCode.ValueCodeManage('modify'); -->
<@errorbar id="SystemCodeFormDialogErrorBar"></@errorbar>
<input type="hidden" id="systemEntityCode" name="systemEntityCode" value="${systemEntityCode}" ></input>
<@frameset id="workbenchOperateBar2" >
	<@frame id="CodetreeContent" region="west" width=170 resize=true style="overflow-y:auto;overflow-x:hidden;">
		<@tree id="codeTreeWidget" nameCol="value" dataUrl="/msService/ec/systemCode/codeValueManager/valueTreeList?systemEntityCode=${systemEntityCode!}" rootName="${getText(systemEntity.name)}"
			 callback="{onClick:function(event,treeId,node){foundation.systemCode.showTreeSystemCode(node);}}" />
	</@frame>
<#--	<div style="position:absolute;bottom:0px;z-index:55;">
		<div id="opratebar" class="opratebar" style="width:154px;">
			<a id="base_systemCode_addValue" class="cui-btn mr10 cui-btn-add" href="#" onclick="foundation.systemCode.ValueCodeManage('add')">${getHtmlText('foundation.basic.systemCode.addCodeValue')}</a>
			<a id="base_systemCode_modifyValue" class="cui-btn mr10 cui-btn-modify" href="#" onclick="foundation.systemCode.ValueCodeManage('modify')">${getHtmlText('foundation.basic.systemCode.modifyCodeValue')}</a>
			<a id="base_systemCode_deleteValue" class="cui-btn mr10 cui-btn-edit" href="#" onclick="foundation.systemCode.ValueCodeDel()">${getHtmlText('foundation.basic.systemCode.deleteCode')}</a>
			<a id="base_systemCode_move" class="cui-btn mr10 cui-btn-move" href="#" onclick="foundation.systemCode.move()">${getHtmlText('foundation.role.move')}</a>
			<a id="base_systemCode_sortitem" class="cui-btn mr10 cui-btn-sort" href="#" onclick="foundation.systemCode.TreesortItem()">${getHtmlText('foundation.basic.systemCode.sortCodeValue')}</a>
		</div>
	</div> -->
	<@frame id="Codecenter" region="center"> 	
			<div id="SytemCodecontent" style="margin-top: 10px;">
			<table cellpadding="0"  cellspacing="0" border="0" align="center" width="98%" class="infoTable"  style="margin-top: 8px;padding-right:65px;">
				<tr>
					<td style="width: 10%;" class="lab">${getHtmlText('foundation.systemCode.code')}</td>
					<td style="width: 15%" align="left">
					    <input type="text" name="systemCodes.code" value="${(systemCode.code)!}" id="systemcodeCode" class="cui-edit-field" readOnly="true"/>
					</td>
					<td style="width: 10%" class="lab">${getHtmlText('foundation.systemCode.value')}</td>
					<td style="width: 15%" align="left">
					    <input type="text" name="systemCodes.value" value="${(systemCode.value)!}" id="systemCodeValue" class="cui-edit-field" readOnly="true"/>
					</td>
				</tr>
				<tr>
					<td style="width: 10%" class="lab">${getText('foundation.systemCode.treeValueadd.PNode')}</td>
					<td style="width: 15%" >
					    <input type="text" name="systemCodes.parent.value" value="${(systemCode.parent.value)!}" id="parentCodeValue" class="cui-edit-field" readOnly="true"/>
					</td>
					<td style="width: 10%" class="lab">${getHtmlText('foundation.systemCode.creatorcom')}</td>
					<td style="width: 15%" align="left">
					    <input type="text" name="systemCodes.company.name" value="${(systemCode.company.name)!}" id="systemCodes_company_name" class="cui-edit-field" readOnly="true"/>
					</td>
				</tr>
				<tr>
					<td align="right" class="lab">${getHtmlText('foundation.systemCode.memo')}</td>
					<td colspan="3">
					    <textarea name="systemCodes.memo" value="${(systemCode.memo)!}" cols="" rows="3" id="memo" class="cui-edit-textarea" style="width:98%" readOnly="true"></textarea>
					</td>
				</tr>
			  </table>
	       </div>
			
	</@frame> 
</@frameset>
<script type="text/javascript" charset="UTF-8" language="javascript">
//注册命名空间
CUI.ns("foundation.systemCode");
//排序
foundation.systemCode.TreesortItem=function(){
	var node=codeTreeWidget.getSelectedNodes()[0];
	var id;
	if(node==null){
		id=-1;
	}else{
		id=node.id;
	}
	if(id==-1){
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCodevalue.treeSort')}");
		return false;
	}
	var url='/msService/ec/foundation/systemCode/codeValueSort/default?systemEntityCode=${systemEntityCode}'+"&systemCodeId="+id;
	foundation.systemCode.valueCodeSortDialog = new CUI.Dialog({
			title: "${getHtmlText('foundation.systemcode.codeValueOrder')}",
			url:url,
			modal:true,
			type:3,
			dragable:true,
			buttons:[{	name:"${getHtmlText('common.button.save')}",
						handler:function(){
						foundation.systemCode.TreeorderSave();
						}
					}, {name:"${getHtmlText('common.button.close')}",
						handler:function(){this.close()}
					}]
		});
		foundation.systemCode.valueCodeSortDialog.show();
}
//编码值排序
	foundation.systemCode.TreeorderSave = function(){
	var SystemValueorder='';
	var i=0;
	CUI('tr[id^="SystemCodeOrder_"]').each(function(){
		var id=CUI(this).attr("colid");
		SystemValueorder+=";"+id+","+i;
		i++;
	});
	SystemValueorder=SystemValueorder.substr(1);
	CUI.post("/msService/ec/systemCode/codeValueSort/save",{"systemCodeCode":SystemValueorder}, function(res){
		if(res.dealSuccessFlag == true){
			sortValueDialogErrorBarWidget.show("${getHtmlText('foundation.systemClass.saveSucceed')}");
			setTimeout(function(){foundation.systemCode.valueCodeSortDialog.close();foundation.systemCode.refreshTree();}, 1000);
		}		
	});
}

foundation.systemCode.ValueCodesave=function(){
	var id = codeTreeWidget.getSelectedNodes()[0].id;
	if(codeTreeWidget.getSelectedNodes()[0]==null||codeTreeWidget.getSelectedNodes()[0].id==-1){
			SystemCodeFormDialogErrorBarWidget.show("${getText('foundation.systemCode.valueTreeManager.select_update_node')}","f");
			return ;
	}
	var url="/msService/ec/foundation/systemCode/modifyValue/systemCodeSave";
	var data="systemCode.id="+id+"&";
	data +="systemCode.entityCode="+codeTreeWidget.getSelectedNodes()[0].entityCode+"&";
	data +="systemCode.code="+$("#systemcodeCode").val()+"&";
	data +="systemCode.value="+$("#systemCodeValue").val()+"&";
	data +="systemCode.memo="+$("#memo").val()+"&";
	data +="strType=modify";
	
	CUI.post(url,data,foundation.systemCode.systemCodesaveCallBackInfo, "json");

	
}
foundation.systemCode.ValueCodeDel=function(){
	if(codeTreeWidget.getSelectedNodes()[0] == null || codeTreeWidget.getSelectedNodes()[0].id == -1){
			SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.select_code_node')}");
			return false;
	}
	var id = codeTreeWidget.getSelectedNodes()[0].id;
	var version=codeTreeWidget.getSelectedNodes()[0].version;
	if(codeTreeWidget.getSelectedNodes()[0].isParent==false){
		CUI.Dialog.confirm("${getHtmlText('foundation.systemCode.valueTreeManager.sure_del_node')}", function(){
		CUI.post("/msService/ec/foundation/systemCode/deleteValue/default?systemCodeId="+id+"&systemCodeVersion="+version,foundation.systemCode.systemCodeCallBackInfo, "json");
		});
	}else{
		CUI.Dialog.confirm("${getHtmlText('foundation.systemCode.valueTreeManager.sure_del_node_cascade')}", function(){
		CUI.post("/msService/ec/foundation/systemCode/deleteValue/default?systemCodeId="+id+"&systemCodeVersion="+version,foundation.systemCode.systemCodeCallBackInfo, "json");
		});
	}
}
foundation.systemCode.ValueCodeManage=function(strType){
	var systemCode_company_type='';
	var id = '-1';//初始化为-1，防止id为空的情况下Action报错
	var parentId='';
	
	if(strType=='add'&&codeTreeWidget&&codeTreeWidget.selected&&codeTreeWidget.selected.id!='codeTree'){
		parentId=codeTreeWidget.selected.nodeAttrs.id;
	}
	var url='';
	if(strType == 'add'){
		if(codeTreeWidget&&codeTreeWidget.selected&&codeTreeWidget.selected.id!='codeTree'&&codeTreeWidget.selected.nodeAttrs.attribute=='true'){
			SystemCodeFormDialogErrorBarWidget.show("<s:text name='foundation.systemCode.notCreateTree'/>");
			return false;
		}
		url="/msService/ec/foundation/systemCode/addValue/treeValueadd?systemEntityCode=${systemEntity.code}";//parentId="+parentId;
		if(codeTreeWidget.getSelectedNodes()[0]!=null&&codeTreeWidget.getSelectedNodes()[0].id!=-1){
			url+='&systemCode.id='+codeTreeWidget.getSelectedNodes()[0].id;
		}
		
	}else if(strType=='modify'){
		if(codeTreeWidget.getSelectedNodes()[0]==null||codeTreeWidget.getSelectedNodes()[0].id==-1){
			SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.select_update_node')}");
			return ;
		}
		id=codeTreeWidget.getSelectedNodes()[0].id;
		url='/msService/ec/foundation/systemCode/addValue/treeValuemodify?systemCode.id='+id;
	}
	
	CUI(function(){
		foundation.systemCode.valueTreeCodeManageDialog = new CUI.Dialog({
			title: "${getText('foundation.systemcode.codeValueManager')}",
			url:url,
			formId:'SubmitSystemCodeEdit',
			modal:true,
			type:3,
			dragable:true,
			buttons:[{name:"${getText('common.button.save')}",handler:function(){foundation.systemCode.saveSysCodeTreeInfo();}},{name:"${getText('common.button.cancel')}",id:"close",handler:function(){this.close()}}]
		})
		foundation.systemCode.valueTreeCodeManageDialog.show();
	});
}

foundation.systemCode.saveSysCodeTreeInfo=function(){
	var internationValue=$("[name='systemCode.value']").val();
	var  temp="zh_CN=";
	if(internationValue.indexOf("zh_CN=")!=-1)  {
		internationValue=internationValue.substring(internationValue.indexOf("zh_CN=")+temp.length);
	}
    $('input[name="systemCode.zhCnValue"]').attr("value",internationValue);
	if(CUI("#systemCode_id").val()==""){
		if ($("#SubmitSystemCodeEdit input[name='systemCode.code']").val() != "") {
			CUI.ajax({
				url: '/msService/ec/foundation/systemCode/checkSysCodeUnique',
				type: 'post',
				async: false,
				data: "systemCodeCode=" + CUI("#SubmitSystemCodeEdit_systemCode_code").val()
			});
		}
	}
	CUI('#SubmitSystemCodeEdit').submit();
}
//添加回调
foundation.systemCode.systemCodeAddCallBackInfo=function(res){
 if(res.dealSuccessFlag == true){
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.addSuccessed')}","s");
		setTimeout(function(){
			try{foundation.systemCode.valueTreeCodeManageDialog.close();}catch(e){}
			foundation.systemCode.refreshTree();
		},400);	
			
}else{
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.addFailed')}","f");
	}
}
//修改回调
foundation.systemCode.systemCodesaveCallBackInfo=function(res){
 if(res.dealSuccessFlag == true){
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.updateSaveSuccessed')}","s");
		setTimeout(function(){
			try{foundation.systemCode.valueTreeCodeManageDialog.close();}catch(e){}
			foundation.systemCode.refreshTree();
		},400);	
}else{
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.SaveFailed')}","f");
	}
}
//删除回调
foundation.systemCode.systemCodeCallBackInfo=function(res){
 if(res.dealSuccessFlag == true){
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
			foundation.systemCode.refreshTree();
}else{
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemClass.dealfailure')}","f");
	}
}
//refresh
foundation.systemCode.refreshTree = function(){
	var rootNode = codeTreeWidget.getNodeByParam("id", -1);
	codeTreeWidget.reAsyncChildNodes(rootNode, "refresh");
	codeTreeWidget.cancelSelectedNode();
	codeTreeWidget.selectNode(codeTreeWidget.getNodes('-1')[0]);
	foundation.systemCode.showTreeSystemCode(codeTreeWidget.getNodes('-1')[0]);
}
//刷新树，并展开
foundation.systemCode.selectSystemCodeTree=function(parentRes){
 //   var systemEntityCode=${systemEntityCode};//CUI("#systemEntityCode").val();
	var url = '/msService/ec/systemCode/codeValueManager/valueTreeList?systemEntityCode=${systemEntityCode}';
	CUI.ajax({
	   type: "POST",
	   url: url,
	  // data:"systemEntityCode="+systemEntityCode,
	   success: function(res){
		   codeTreeWidget.refresh(res);
			if(parentRes!=null){
				codeTreeWidget.autoExpand(parentRes.id);
				//setTimeout(function(){codeTreeWidget.imitateClick();},0);
			}
	   }
	});
}
//显示systemCode 信息
foundation.systemCode.showTreeSystemCode=function(node){
	CUI.post("/msService/ec/systemCode/codeValueManager/SystemCodeinfo?systemCode.id=" + node.id, function(res){
			CUI.fillValues('SytemCodecontent','systemCodes',res);
		}, "json");
	/*CUI("#systemcodeCode").val(node.code);
	CUI("#systemCodeValue").val(node.value);
	CUI("#memo").val(node.memo);
	if(node.parent!=null){
	
		CUI("#parentCodeValue").val(node.parent.value);
	}
	url = "/msService/ec/systemCode/codeValueManager/valueTreeList?systemEntityCode=${systemEntityCode!}";*/
	
}
//判断是否选择
	foundation.systemCode.checkSelectedAny = function(){
		if(codeTreeWidget.getSelectedNodes()[0] == null || codeTreeWidget.getSelectedNodes()[0].level == 0){
			SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.select_code_node')}");
			return false;
		}
		return true;
	};

	/**
	 * 编码移动

	 * @public
	 */
	foundation.systemCode.move = function(){
		if(codeTreeWidget.getSelectedNodes()[0]==null||codeTreeWidget.getSelectedNodes()[0].id==-1){
			SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.selectMoveNode')}","f");
			return ;
		}
		var open_url= "/msService/ec/foundation/systemCode/move/TreeFrame?id=" + codeTreeWidget.getSelectedNodes()[0].id+"&systemEntityCode=${systemEntityCode!}";
		var windowStyle = "width = 280,height=550,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("other",windowStyle,null,null,true,"foundation.systemCode.systemCodeCallBack",null,open_url);
	}
	//回调
foundation.systemCode.systemCodeCallBack=function(res){
 if(res.dealSuccessFlag == true){
		setTimeout(function(){
			//try{foundation.systemCode.valueTreeCodeManageDialog.close();}catch(e){}
			foundation.systemCode.refreshTree();
		},400);	
			
}else{
		SystemCodeFormDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valueTreeManager.moveFailed')}");
	}
}
</script>
