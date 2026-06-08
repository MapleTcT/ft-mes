<#if (Parameters.openType)?default('page') != 'dialog'>
	<!DOCTYPE html>
	<html  xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<meta http-equiv="X-UA-Compatible" content="IE=8,9,10" />
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText('foundation.role.select')}</title>
	<@head/>
	</head>
	<body>
</#if>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<@errorbar id="roleListFrameErrorBar" />
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="currentCompanyId" name="currentCompanyId" value="${getCurrent('companyId')}" />

<@frameset id="roleFrameset" border=0 style="height:100%;width:100%">
     <@frame id="role_left" region="west" width=200 offsetH=0 resize=true style="overflow-y:auto;overflow-x:hidden;">
		<#if (crossCompanyFlag??&&crossCompanyFlag)&&isUserRef?string('true','false')=='false'>
		<style type="text/css">
			#roleTree1{position:relative;top:23px;}
			#role_left{position:relative;}
		</style>
	 	<div class="tree-companylist">
        	<div class="tree-companylist-son">
				<#assign l = companyList>
				<@listmenu list=l id="role_companyList" listName="name" listKey="id"   value="${getCurrent('companyName')}" onclick="role_changeCompany"  cssStyle="top:4px;left:10px;" ></@listmenu>
			</div>
		</div>
		</#if>
		<@tree id="roleTree1" dataUrl="/msService/ec/foundation/role/listChildren" rootName="${getCurrent('companyName')}"
			 callback="{onClick:function(event,treeId,node){CUI.resetForm('role_queryForm');showRoleWorks(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
     </@frame>
     <@frame id="role_center" region="center" offsetH=4>
     	<div>
     		<form id="role_queryForm" onsubmit="return queryRoleWorks()">
				 <@quickquery formId="role_queryForm" fieldcodes="base_role_name:foundation.role.name||base_role_code:foundation.role.code" unique="LAST_QUERY_foundation_role_common_roleListFrame">
				 <#if (crossCompanyFlag??&&crossCompanyFlag)>
				 	<@queryfield formId="role_queryForm" code="base_role_name" mneclick=false isCrossCompany=true key="role.name" showRange="${Parameters.showRange!}" mneurl="/msService/ec/foundation/role/common/roleListFrameset" type="Role" searchClick="queryRoleWorks()"/>
				 <#else>
				 	<@queryfield formId="role_queryForm" code="base_role_name" mneclick=false key="role.name" showRange="${Parameters.showRange!}"  mneurl="/msService/ec/foundation/role/common/roleListFrameset" type="Role" searchClick="queryRoleWorks()"/>
				 </#if>
				      
				      <@queryfield formId="role_queryForm" code="base_role_code" key="role.code"/>
				      <@querybutton formId="role_queryForm" type="search" onclick="queryRoleWorks()"/>
	                  <@querybutton formId="role_queryForm" type="clear" />
		   		</@quickquery>

			</form>
     	</div>
     	<#assign superChecked = (Parameters.multiSelect)?string('true','false') == 'true'>
     	<@datatable formId="role_queryForm" superChecked=superChecked superCheckedName="name" id="role_ListTable" hidekey="['id','version']"  dtPage="roleUserPage" dblclick="sendBackRoleInfo" transMethod="post"  dataUrl="/msService/ec/foundation/role/common/showRoleList?id=-1" style="margin:4px 10px;" >
			<#if (Parameters.multiSelect)?string('true','false') == "true">
				<@datacolumn key="checkbox" type="checkbox" textalign="center" checkall="true" label="" width=60 />
			</#if>
			<@datacolumn key="code" label="${getHtmlText('foundation.role.code')}" width="200"/>
			<@datacolumn key="name" label="${getHtmlText('foundation.role.name')}" width="200"/>
		</@datatable>
		
     </@frame>
     <#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
	    <@frame id="group_Button" region="south" height=28>
		    <div align="right" style="margin-right:20px;position:absolute;bottom:0px;right:0;z-index:100;">
		     	<#if closePage?exists&&closePage==false>
		     	    <a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;position: relative;top:-10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
		     	<#else>
		     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;position: relative;top:-10px;"><span class="btn_r">${getHtmlText('common.button.chooseandclose')}</span></a>
				</#if>
					<a id="bottom-reset" class="cui-btn-blue" style="margin-right:10px;position: relative;top:-10px;"><span class="btn_r">${getHtmlText('common.button.cancel')}</span></a>
			</div>
	     </@frame>
     </#if>
</@frameset>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="utf-8" language="javascript">

<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
	$(function(){
	
		//var preHeight = $("#departmentList").css("height");
		//$("departmentList").css("height", parseInt( (parseInt(preHeight.slice(0,-2)) * 0.90)) + 'px' );
		$("#bottom-submit").click( function(){
			sendBackRoleInfo();
		});
		$("#bottom-reset").click( function(){
			window.close();
		});
	});
</#if>
//点击角色树节点，该角色下级角色列表
function showRoleWorks(oNode){
	var url = "/msService/ec/foundation/role/common/showRoleList";
	if(oNode!=null){
		url+="?id="+oNode.id;
	}else{
		url+="?id=-2";
		}
	url+="&companyId="+CUI("#currentCompanyId").val();
	role_ListTableWidget.delAllRows();
	role_ListTableWidget.setRequestDataUrl(encodeURI(url));
}

//查询
function queryRoleWorks(type,pageNo){
		role_ListTableWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"queryRoleWorks(1)"
	    });
		var url = "/msService/ec/foundation/role/common/showRoleList?a=1";
		var dataPost="";
	    var companyId=CUI("#currentCompanyId").val();
		var roleCode =encodeURIComponent(CUI.trim(CUI("input[name='role.code']",CUI("#role_queryForm")).val())) ;
		var roleId=encodeURIComponent(CUI.trim(CUI("input[name='role.id']",CUI("#role_queryForm")).val())) ;
		var roleName =encodeURIComponent(CUI.trim(CUI("input[name='role.name']",CUI("#role_queryForm")).val())) ;
		if(companyId !=''){
			dataPost +="&companyId="+companyId;
		}
		if(roleCode != ''){
			dataPost += "&roleCode=" + roleCode;
		}
		if(roleId!=''){
		    dataPost +="&id="+roleId;
		}
		else{
			if(roleName != ''){ 
				dataPost += "&roleName=" + roleName;
	        }
        }	
        if(pageNo!=undefined){
        	dataPost+="&roleUserPage.pageNo="+pageNo;
        }	
		var pageSize=CUI('input[name="role_ListTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		dataPost+="&"+role_queryForm_getCookieParam();
		<#if Parameters.showRange??>
			url += "&showRange=${Parameters.showRange!}";
		</#if>
		role_ListTableWidget.setRequestDataUrl(url,dataPost);
}


function role_changeCompany(oSelect){

	setTimeout(function(){
		var url = "/msService/ec/foundation/role/listChildren?companyId="+ oSelect.getAttribute("key")+"&resultType=json";
		//roleTreeWidget.refresh(null,null,url);
		CUI("#currentCompanyId").val(oSelect.getAttribute("key"));
		roleTree1.getNodes()[0].name = oSelect.innerHTML;
		roleTree1.updateNode(roleTree1.getNodes()[0], true);
		roleTree1.setting.async.url=url;
		roleTree1.reAsyncChildNodes(roleTree1.getNodes()[0], "refresh");
	
		
		//if(closeLoadPanel){closeLoadPanel(false);}
	},0);
}
<#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
<#assign requestUri = requestUri?replace('/', '_', 'r')>
// 供外部调用
foundation.common.${requestUri}__callbackFunction = function(){
	if(role_ListTableWidget.getSelectedRow().length == 0){
		CUI.Dialog.alert("${getHtmlText('foundation.role.checkselected')}","f");
		return false;
	}
	sendBackRoleInfo(null,role_ListTableWidget.getSelectedRow());
}
//双击或点击选择按钮进行选取
sendBackRoleInfo = function(event,oRow){
	var arrObj = new Array();
	var oRows = new Array();
	if(event == undefined){
		oRows = role_ListTableWidget.getSelectedRow();
	}else{
		oRows.push(oRow);
	}
	if(oRows.length == 0){
		roleListFrameErrorbarWidget("${getHtmlText('foundation.role.checkselected')}");
		return false;
	}

	for(var i=0; i<oRows.length; i++){
		//var oRole = new Object();
		//oRole.roleID = oRow.id;
		//oRole.roleCode = oRow.code;
		//oRole.roleName = oRow.name;
		//arrObj.push(oRole);
		oRows[i].rowIndex = CUI("#rowIndex").val();
		arrObj.push(oRows[i]);
	}
	try{
		if(CUI("#callBackFuncName").val() != ""){
			<#if (Parameters.openType)?default('page') != 'dialog'>
				<#if (Parameters.openType)?default('page') != 'frame'>
					eval("opener." + CUI("#callBackFuncName").val() + "(arrObj)");
				<#else>
					eval("parent." + CUI("#callBackFuncName").val() + "(arrObj)");
				</#if>
			<#else>
				eval(CUI("#callBackFuncName").val() + "(arrObj)");
			</#if>
		}else{
			getStaffInfo(arrObj);
		}
		<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
			setTimeout(function(){
				try {
					if(CUI("#closePage").val() != "false") {
						top.opener.focus();
						CUI.closeWindow();
					}
				}catch(e){}
			},1000);
		</#if>
		<#if (Parameters.openFrom)?default('bap') != 'supplant'>
		roleListFrameErrorBarWidget.showMessage("${getHtmlText('foundation.add.success')}","s");
		</#if>
	}catch(e){
		roleListFrameErrorBarWidget.showMessage("${getHtmlText('foundation.add.failure')}","f");
		//alert("注意：父窗口回调出错！");
	}
}
</script>