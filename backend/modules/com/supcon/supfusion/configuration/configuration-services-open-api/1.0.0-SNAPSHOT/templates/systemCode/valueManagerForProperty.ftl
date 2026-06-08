<@errorbar id="SystemCodeValueManageDialogErrorBar"></@errorbar>
<input type="hidden" id="systemEntityCode" name="systemEntityCode" value="${systemEntityCode!}" ></input>
<@frameset id="workbenchOperateBar" border="0">
	<@frame id="infoContent" offsetH=4>
		<div id="systemCodeDiv" style="height:100%">
		<div id="systemCodeQueryDiv1">
			<form id="QueryFormvalue"  class="clearfix" onsubmit="return false;" enctype="multipart/form-data" >
				<@quickquery formId="QueryFormvalue"  fieldcodes="systemCodeCode:foundation.systemCode.code||systemCodeValue:foundation.systemCode.value" unique="LAST_QUERY_foundation_systemCode_valueManager">
			       	<@queryfield isCustomize=true formId="QueryFormvalue" code="systemCodeCode">
			       		<input type="text" id="systemCodeCode" class="cui-edit-field"></input>
			       	</@queryfield>
			       	<@queryfield isCustomize=true formId="QueryFormvalue" code="systemCodeValue">
			       		<input type="text" id="systemCodeValue" class="cui-edit-field"></input>
			  	 	</@queryfield>
			  	 	<@querybutton formId="QueryFormvalue" type="search" onclick="foundation.systemCode.queryValueList()" />
			 		<@querybutton formId="QueryFormvalue" type="clear"  />
			     </@quickquery>
			</form>
		</div>
		<@datatable dtPage="systemCodePage" editable=false  formId="QueryForm" height=430 dblclick="foundation.systemCode.ValueCodeManage('modify');" hidekey="['id','company.type','version']" transMethod="post" id="systemCodeList" dataUrl="/msService/ec/systemCode/codeValueManager/valueList?systemEntityCode=${(systemEntityCode)!}" style="margin:2px 10px 3px 11px;">
			<@operatebar operates="code:base_systemCode_addValue||onclick:foundation.systemCode.ValueCodeManage('add');
				code:base_systemCode_modityValue||onclick:foundation.systemCode.ValueCodeManage('modify');
				code:base_systemCode_deleteValue||onclick:foundation.systemCode.ValueCodeManage('delete');
				code:base_systemCode_sortitem||onclick:foundation.systemCode.sortitem();"
				 resultType="JSON" >
			</@operatebar>
			<@datacolumn key="company.shortName" label="${getHtmlText('foundation.systemCode.creatorcom')}" width=150/>
			<@datacolumn key="code" label="${getHtmlText('foundation.systemCode.code')}" width=150/>
			<@datacolumn key="value" label="${getHtmlText('foundation.systemCode.value')}" width=150/>
			<@datacolumn key="defaultFlag" width=20  textalign="center" type="boolean" sortable=false label="${getHtmlText('ec.property.dafult')}" width=50 />
		</@datatable>
		</div>
	</@frame>
</@frameset>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
//注册命名空间
CUI.ns("foundation.systemCode");
//默认值设置
foundation.systemCode.valueChange = function(obj){
	var objTr=obj.rowHtmlObj;
	var checked=$('input[type=checkbox]',objTr).prop('checked');
	if(checked===false){
		$('input[type=checkbox]',objTr).prop('checked',true);
		return ;
	}
	var systemEntityCode=CUI("input[name=systemEntityCode]").val();
	var systemCodeId=obj.id;
	var index = obj.rowHtmlObj.rowIndex;
	var allRows=systemCodeListWidget.getAllRows();
	for(var i=0;i<allRows.length;i++){
		var tr = allRows[i].rowHtmlObj;
		if(index !== i){
			systemCodeListWidget._setUnSelected(tr);
			$('input[type=checkbox]',tr).prop('checked',false);
		}	
	}
	CUI.Dialog.confirm("${getHtmlText('foundation.systemCode.setDefault')}?", function(){
		CUI.post("/foundation/systemCode/setdefaultValue.action", {"systemCodeId":systemCodeId,"systemEntityCode":systemEntityCode}, function(res){
				if(res.dealSuccessFlag == true){
					SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.setDefualtValue')}","s");
				}
		});
		});

};
foundation.systemCode.ValueCodeManage=function(strType){
	var systemCode_company_type='';
	var id = '-1';//初始化为-1，防止id为空的情况下Action报错
	var version;
	if(strType != 'add'){
		if(systemCodeListWidget.selectedRows.length == 0){
			SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.valuecheckselected')}");
			return false;
		}
		id=systemCodeListWidget.selectedRows[0].id;
		version=systemCodeListWidget.selectedRows[0].version;
		systemCode_company_type=systemCodeListWidget.selectedRows[0].company.type;
	}
	if(strType == 'delete'){
		//只能编辑本公司的系统编码
		if(systemCode_company_type!=CUI("#currentCompany_type").val()){
			SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.cantEdit')}");
			return false;
		}
		CUI.Dialog.confirm("${getHtmlText('foundation.systemClass.checkdelete')}", function(){
			CUI.post("/foundation/systemCode/deleteValue/default.action", {"systemCodeId":id,"systemCodeVersion":version}, function(res){
				if(res.dealSuccessFlag == true){
					SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}");
					foundation.systemCode.queryValueList();
				}else{
					SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemClass.dealfailure')}");
				}
			});
			});
		return false;
	}
	var url='';
	if(strType == 'add'){
		url="/foundation/systemCode/addValue/default.action?systemEntityCode="+'${(systemEntityCode)!}'+'&strType='+strType;
	} else if(strType=='modify'){
		//只能编辑本公司的系统编码，除非登陆者是集团
	   var  systemCodeId=systemCodeListWidget.selectedRows[0].id;
		if(systemCode_company_type!=CUI("#currentCompany_type").val()){
			SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemClass.cantEdit')}");
			return false;
		}
		url='/foundation/systemCode/modifyValue/default.action?systemCodeId='+systemCodeId+'&strType='+strType;
		url+='&systemEntityCode='+'${(systemEntityCode)!}';
	}
	
	
	CUI(function(){
		foundation.systemCode.valueCodeManageDialog = new CUI.Dialog({
			title: "${getHtmlText('foundation.systemcode.codeValueManager')}",
			url:url,
			formId:'SubmitSystemCodeEdit',
			modal:true,
			type:3,
			dragable:true,
			buttons:[{name:"${getHtmlText('common.button.save')}",handler:function(){foundation.systemCode.saveSysCodeInfo();}},{name:"${getHtmlText('common.button.cancel')}",id:"close",handler:function(){this.close()}}]
		})
		foundation.systemCode.valueCodeManageDialog.show();
	});
}
// 系统编码值保存

foundation.systemCode.saveSysCodeInfo=function(){
	var internationValue=$("[name='systemCode.value']").eq(1).val();
	var  temp="zh_CN=";
	if(internationValue.indexOf("zh_CN=")!=-1)  {
		internationValue=internationValue.substring(internationValue.indexOf("zh_CN=")+temp.length);
	}
    $('input[name="systemCode.zhCnValue"]').attr("value",internationValue);
	if(CUI("#SubmitSystemCodeEdit_systemCode_id").val()==""){
		if ($("input[name='systemCode.code']","#SubmitSystemCodeEdit").val() != "") {
			CUI.ajax({
				url: '/foundation/systemCode/checkSysCodeUnique.action',
				type: 'post',
				async: false,
				data: "systemCodeCode=" + $("input[name='systemCode.code']","#SubmitSystemCodeEdit").val()
			});
		}
	}
	CUI('#SubmitSystemCodeEdit').submit();
}
// 系统编码值保存完毕回调
foundation.systemCode.systemCodeCallBackInfo=function(res){
	if(res.dealSuccessFlag == true){
	   SubmitSystemCodeEditErrorBarWidget.show("${getHtmlText('foundation.systemClass.saveSucceed')}","s");
		setTimeout(function(){
			try{foundation.systemCode.valueCodeManageDialog.close();}catch(e){}
			foundation.systemCode.queryValueList();
			_fillDefaultContent();
		},1500);	
	}else{
		SubmitSystemCodeEditErrorBarWidget.show("${getHtmlText('foundation.systemClass.dealfailure')}");
	}
}
//查询
foundation.systemCode.queryValueList=function(){
	var dataPost="";
	var url="/msService/ec/systemCode/codeValueManager/valueList?systemEntityCode=${systemEntityCode!}";
	if(CUI("#systemCodeCode").val()!=""){
		dataPost +='&systemCodeCode='+CUI("#systemCodeCode").val();
	}
	if(CUI("#systemCodeValue").val()!=""){
		dataPost +='&systemValue='+CUI("#systemCodeValue").val();
	}
	var pageSize=CUI('input[name="systemCodeList_PageLink_PageCount"]').val();
	dataPost += "&pageSize="+encodeURIComponent(pageSize);
	dataPost+="&"+QueryFormvalue_getCookieParam();
	systemCodeListWidget.setRequestDataUrl(url,dataPost);
}
foundation.systemCode.sortitem = function(){
		if(systemCodeListWidget.selectedRows.length == 0){
			SystemCodeValueManageDialogErrorBarWidget.show("${getHtmlText('foundation.systemCode.checkselected')}");
			return false;
		}
		var treeType=systemCodeListWidget.selectedRows[0].listType;
		var dialogWidth=500;
		var dialogHeight=400;
		var url='/foundation/systemCode/codeValueSort/default.action?systemEntityCode='+systemCodeListWidget.selectedRows[0].id.split("/")[0];
		foundation.systemCode.valueCodeSortDialog = new CUI.Dialog({
			title: "${getHtmlText('foundation.systemcode.codeValueOrder')}",
			url:url,
			modal:true,
			//type:3,
			width:460,
			height:330,
			dragable:true,
			buttons:[{	name:"${getHtmlText('common.button.save')}",
						handler:function(){
						foundation.systemCode.orderSave();
						}
					}, {name:"${getHtmlText('common.button.close')}",
						handler:function(){this.close()}
					}]
		});
		foundation.systemCode.valueCodeSortDialog.show();
	};
	//编码值排序
	foundation.systemCode.orderSave = function(){
	var SystemValueorder='';
	var i=0;
	CUI('tr[id^="SystemCodeOrder_"]').each(function(){
		var id=CUI(this).attr("colid");
		SystemValueorder+=";"+id+","+i;
		i++;
	});
	SystemValueorder=SystemValueorder.substr(1);
	CUI.post("/foundation/systemCode/codeValueSort/save.action",{"systemCodeCode":SystemValueorder}, function(res){
		if(res.dealSuccessFlag == true){
			sortValueDialogErrorBarWidget.show("${getHtmlText('foundation.systemClass.saveSucceed')}");
			setTimeout(function(){foundation.systemCode.valueCodeSortDialog.close();foundation.systemCode.queryValueList();}, 1000);
		}		
	});
}
})();
</script>