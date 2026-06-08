<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<style type="text/css">#flowInfo_center{position:relative;padding-top:3px\9;}</style>
	<@head/>
	<title>${getText('ec.flowActive.title')}</title>
	<body id="dialog_page">
</#if>
<@errorbar id="departmentListFramePageErrorBar" />
<@loadpanel />
<@s.hidden name="closePage" />
<@s.hidden name="callBackFuncName" />
<@s.hidden name="multiSelect" />
<@s.hidden name="userId" />
<@s.hidden name="companyId" id="currentCompanyId" />
<@errorbar id="flowActiveListFrameErrorbar" />
<@frameset id="flowInfoFrameset" border=0>
    <@frame id="flowInfo_left"  region="west" width=200 offsetH=6 resize=true style="overflow:auto">
   		<@tree id="flowTree" nameCol="nameInternational" rootName="${getText('ec.flowActive.flowInfo')}" dataUrl="/msService/ec/workflow/flowListJSON.action"
   			callback="{onClick:function(event,treeId,node){foundation.flowActive.showflowInfo(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
    </@frame>
    <@frame id="flowInfo_center" region="center" offsetH=4 >
		<@datatable hidekey="['id','flowKey','code']" dtPage="menuOperatePage" dblclick="foundation.flowActive.sendBackFlowInfo" multiselect=true editable=false transMethod="post" id="flow_ListTable" dataUrl="/msService/ec/workflow/flowActiveList.action" style="margin:4px 8px 40px;" paginator=true>
			<#if (Parameters.multiSelect)?default('false') == 'true'>
				<@datacolumn key="checkbox" textalign="center" type="checkbox" checkall="true" label="" width=60 />
			</#if>
			<@datacolumn key="flowName" label="${getHtmlText('ec.flowActive.flowName')}"  width=200 />
			<@datacolumn key="name" label="${getHtmlText('ec.flowActive.activeName')}"  width=200 />
		</@datatable>
		<@frame id="flowInfo_Button" region="south" height=26>
		    <div align="right" style="margin-right:20px;position:absolute;bottom:15px;right:0;z-index:100;">
		        <@querybutton onclick="foundation.flowActive.sendBackFlowInfo()" isCustomize=true customizeName="common.button.choose"/>
		        <@querybutton onclick="CUI.closeWindow()" isCustomize=true customizeName="common.button.close"/>
			</div>
		</@frame>
    </@frame>
	
</@frameset>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
	//注册命名空间
	CUI.ns("foundation.flowActive");
	foundation.flowActive.showflowInfo=function(oNode){
		if(flowTree.getSelectedNodes()[0].level!=0){
			var url = "/msService/ec/workflow/flowActiveList.action?id=" +oNode.id+"&userId="+CUI("#userId").val();
			flow_ListTableWidget.setRequestDataUrl(encodeURI(url));
		}
	}
	//双击或点击选择按钮进行选取
	foundation.flowActive.sendBackFlowInfo=function(event,oRow){
		var arrObj = new Array();
		var oRows = new Array();
		
		if(event == undefined){
			oRows = flow_ListTableWidget.selectedRows;
		}else{
			oRows.push(oRow);
		}
	
		if(oRows.length == 0){
			flowActiveListFrameErrorbarWidget.show("${getHtmlText('ec.flowActive.pleaseRecorde')}","f");
			return false;
		}
		
		for(var i=0; i<oRows.length; i++){
			var oDepartment = new Object();
			oDepartment.id = oRows[i].id;
			oDepartment.active_code = oRows[i].code;
			oDepartment.active_name = oRows[i].name;
			oDepartment.flowKey = oRows[i].flowKey;
			oDepartment.flowName = oRows[i].flowName;
			//oDepartment.flowVersion = oRows[i].flowVersion;
			arrObj.push(oDepartment);
		}
	
		try{
			if(CUI("#closePage").val() != "false") top.opener.focus();
			if(CUI("#callBackFuncName").val() != ""){
				eval("top.opener." + CUI("#callBackFuncName").val() + "(arrObj)");
			}else{
				top.opener.expectedConsign_getFlowActive(arrObj);
			}
			flowActiveListFrameErrorbarWidget.show("${getHtmlText('foundation.common.add.success')}","s");
			if(CUI("#closePage").val() != "false") CUI.closeWindow();
		}catch(e){
			flowActiveListFrameErrorbarWidget.show("${getHtmlText('ec.flowActive.pleaseCallback')}","f");
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	}
}) ();
</script>