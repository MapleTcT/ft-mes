<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<style type="text/css">#flowInfo_center{position:relative;padding-top:3px\9;}</style>
	<@head/>
	<title></title>
	<script type="text/javascript" charset="utf-8" src="/bap/static/js/FusionCharts.js"></script>
	<body id="dialog_page">
	</head>
</#if>
<@errorbar id="referenceListFrameErrorBar" />
<@frameset id="referenceFrameset" border=0>
    <@frame id="flowInfo_left"  region="west" width=200 offsetH=6 resize=true style="overflow:auto">
   		<@tree id="referenceflowTree" rootName="${getText('ec.entity.entityName')}" dataUrl="/msService/ec/workflow/foundationListJSON"
   			callback="{onClick:function(event,treeId,node){foundation.workflow.showEntity(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
    </@frame>
    <@frame id="flowInfo_center" region="center" offsetH=4 >
    		<div id="flowQueryDiv">
				<form id="queryForm" onsubmit="return false;">
					<table cellpadding="0" cellspacing="0" border="0" align="center" width="100%" style="margin-top: 10px">
						<tr style="height: 22px">
							<td width="8%" align="right" style="padding-right: 10px;">${getHtmlText('ec.workflow.ecname')}</td>
							<td width="15%">
							<input type="text" name="entityName" value="" id="entityName" class="cui-edit-field" url="other"/>
							</td>
							<td width="8%" align="right" style="padding-right: 10px;">${getHtmlText('ec.workflow.flowname')}</td>
							<td width="15%">
							<input type="text" name="flowName" value="" id="flowName" class="cui-edit-field" url="other"/>
							</td>
							<td width="8%" align="right" style="padding-right: 10px;"></td>
							<td align="right" style="padding-right: 30px;">
								<button type="submit" id="queryButton" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);" 
									onclick="foundation.workflow.queryList()">${getHtmlText('common.button.query')}</button>
								<button type="reset" id="reset" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);">${getHtmlText('common.button.reset')}</button>
							</td>
						</tr>
					</table>
				</form>
			</div>
		
		<@datatable formId="queryForm" hidekey="['ID','DEPLOYMENT_ID','ENTITY_CODE']" dtPage="records"  transMethod="post" id="flow_ListTable"	dataUrl="/msService/ec/workflow/queryflowList" firstLoad=false style="margin:4px 8px 40px;" multiselect=false>
			<@datacolumn key="NAME" label="${getHtmlText('ec.workflow.flowname')}"  width=200 />
			<@datacolumn key="PROCESS_VERSION" label="${getHtmlText('ec.workflow.version')}"  width=200 />
			<@datacolumn key="IS_CURRENT_VERSION" textalign="center" type="boolean" label="${getHtmlText('ec.workflow.currentversion')}"  width=200 />
		</@datatable>
		<@frame id="flowInfo_Button" region="south" height=26>
		    <div align="right" style="margin-right:20px;position:absolute;bottom:15px;right:0;z-index:100;">
		    	包括权限
		    	<input type="checkbox" id="hasPower" name="hasPower" value="pandion" checked="true" />&#160;&#160;
		    	包含自定义分组
		    	<input type="checkbox" id="hasSelectStaff" name="hasSelectStaff" value="pandion" checked="true" />&#160;&#160;
		    
				<button id="reset" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);" onclick="foundation.workflow.sendBackFlowInfo(null,null,null)" >${getHtmlText('foundation.common.checked')}</button>&#160;&#160;
				<button id="queryButton" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);" onclick="CUI.closeWindow()">${getHtmlText('common.button.cancel')}</button>
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
	CUI.ns("foundation.workflow");
	foundation.workflow.showEntity=function(oNode){
		var url;
		var dataPost;
		if(referenceflowTree.getSelectedNodes()[0].level==1){
			dataPost="moduleCode="+oNode.code;
		}else{
			dataPost="entityCode="+oNode.code;
		}
		if(oNode.children){
			url = "/msService/ec/workflow/queryflowList?a=1";
		}else{
			url = "/msService/ec/workflow/queryflowList?a=1";
		}
		flow_ListTableWidget.setRequestDataUrl(encodeURI(url),dataPost);
	}
	foundation.workflow.queryList=function(){
		var dataPost="";
		var url = "/msService/ec/workflow/queryflowList?a=1";
		var flowName =encodeURIComponent(CUI.trim(CUI("#flowName").val())) ;
		var ecName =encodeURIComponent(CUI.trim(CUI("#entityName").val())) ;
		if(flowName != ''){
			 dataPost += "&flowName=" + flowName;
		}
		if(ecName != ''){
			 dataPost += "&entityName=" + ecName;
		}
		flow_ListTableWidget.setRequestDataUrl(encodeURI(url),dataPost);
	}
	//双击或点击选择按钮进行选取
	foundation.workflow.sendBackFlowInfo=function(event,oRow,permission){
		var arrObj = new Array();
		var oRows = new Array();
		
		if(event == undefined){
			oRows = flow_ListTableWidget.selectedRows;
		}else{
			oRows.push(oRow);
		}
	
		if(oRows.length == 0){
			referenceListFrameErrorBarWidget.show("${getHtmlText('ec.flowActive.pleaseRecorde')}","f");
			return false;
		}
		var permissionFlag=0;
		if(CUI("#hasPower").prop('checked')){
			permissionFlag=1;
		}
		var customStaff=0;
		if(CUI("#hasSelectStaff").prop('checked')){
			customStaff=1;
		}
		CUI.Dialog.confirm("${getHtmlText('ec.flowActive.referenceSrue')}", function(){
			eval("opener.getReferenceDeploymentId(oRows[0].ID,oRows[0].ENTITY_CODE,permissionFlag,customStaff)");
			CUI.closeWindow();
		});
		
	}
}) ();
</script>