<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<@head/>

<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<title>${getText('foundation.department.title')}</title>
</head>
<body id="dialog_page">
</#if>
<@errorbar id="departmentListFramePageErrorBar" />

<@frameset id="chooseDepartmentFrameset" border=0 style="height:100%;width:100%">
   	  <@frame id="department_left"  region="west" width=200 offsetH=0 resize=true style="overflow-y:auto;overflow-x:hidden;">
	  	<#if crossCompanyFlag??&&crossCompanyFlag||isSingleMode??&&isSingleMode?string("true","false")=='false'>
	  		<style type="text/css">
				#chooseDepartmentTreePage{position:relative;top:23px;}
				#department_left{position:relative;}
			</style>
			<div class="tree-companylist">
        		<div class="tree-companylist-son">
					<#assign l = companyList>
					<@listmenu list=l id="DepartCompanyList" listName="name" listKey="id" value="${getCurrent('companyName')}" onclick="foundation.department.department_changeCompany"  cssStyle="top:4px;left:10px;" ></@listmenu>
				</div>
			</div>
	   </#if>
	   	  <@tree id="chooseDepartmentTreePage" dataUrl="/msService/ec/foundation/department/listChildren" rootName="${getCurrent('companyName')}" isCopy=false isMove=false
				 callback="{onClick:function(event,treeId,node){foundation.department.showDepartmentWorks(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
			<input type="hidden" id="selectid" name="selectid" />
	  </@frame>
	  <@frame id="department_center" region="center" offsetH=5 resize=true>
		  	<div id="departmentQueryDiv" >
				<form id="department_query_form" name="department_query_form" onsubmit="return false;" >
				    <input type="hidden" id="formDepartmentCompanyId" name="companyId" value="${getCurrent('companyId')}" />
					<@quickquery formId="department_query_form"  fieldcodes="base_department_name:foundation.department.name||base_department_code:foundation.department.code" unique="LAST_QUERY_foundation_department_list_frame">
				       	<#if (crossCompanyFlag??&&crossCompanyFlag)>
				       	<@queryfield formId="department_query_form" code="base_department_name" key="department.name" isCrossCompany=true mneurl="other" mneclick=false type="Department" searchClick="foundation.department.CommonqueryList()"></@queryfield>
				        <@queryfield formId="department_query_form" key="department_code" code="base_department_code" isCrossCompany=true></@queryfield>
				        <#else>
				        <@queryfield formId="department_query_form" code="base_department_name" key="department.name" mneurl="other" mneclick=false type="Department" searchClick="foundation.department.CommonqueryList()"></@queryfield>
				        <@queryfield formId="department_query_form" key="department_code" code="base_department_code"></@queryfield>
			 			</#if>
				        <@querybutton formId="department_query_form" type="search" onclick="foundation.department.CommonqueryList()"/>
			 			<@querybutton formId="department_query_form" type="clear"  />
				     </@quickquery>
					<#--<table cellpadding="0" cellspacing="0" border="0" align="center" width="100%" style="margin-top:6px">
						<tr style="height: 22px">
							<td width="12%" align="right" style="padding-right:3px;">${getText('foundation.department.code')}</td>
							<td width="18%"><input type="text" id="departmentCode" class="cui-edit-field" value="${departmentCode?default('')}"></input></td>
							<td width="10%" align="right" style="padding-right:3px;">${getText('foundation.department.name')}</td>
							<td width="18%">
							<input id="departmentId" name="department.id" type="hidden"/>
							<@mneclient name="department.name" id="departmentName" url="other" onkeyupfuncname="foundation.department.departmentInfoCallback(obj)" classStyle="cui-noborder-input" type="Department" multiple=false mnewidth=260 isPrecise=true/>
							</td>
							<td width="5%"></td>
							<td>
							    <div class="edit-btn btn-act" id="queryButton" onclick="foundation.department.CommonqueryList()">
									<a class="cui-btn-l">&nbsp;</a>
									<a class="cui-btn-c">${getText('common.button.query')}</a>
									<a class="cui-btn-r">&nbsp;</a>
								</div>
								<div class="edit-btn btn-act" id="reset" onclick="CUI.resetForm('department_query_form')">
									<a class="cui-btn-l">&nbsp;</a>
									<a class="cui-btn-c">${getText('common.button.reset')}</a>
									<a class="cui-btn-r">&nbsp;</a>
								</div>
							</td>
						</tr>
					</table>-->
				</form>
			</div>
			<#assign superChecked = (Parameters.multiSelect)?string('true','false') == 'true'>
			<#assign customPropList = getShowCustomSecret("sysbase_1.0_department_base_department", "sysbase_1.0_department_edit", 'LIST')>
			<div class="etv-navset" style='clear:both;'><div id="departmentList_datatable_container" style='clear:both;'>
		  	<@datatable transMethod="post" height=362 superChecked=superChecked superCheckedName="name" firstLoad=false dtPage="page" hidekey="['id','parent.id','parent.code','parent.name','fullPathName','layRec']" formId="department_query_form" id="departmentListCommon" style="margin:6px 12px;" dblclick="foundation.department.sendBackDepartmentInfo" dataUrl="/msService/ec/foundation/department/queryList?a=-1" postData="&departmentCode=${departmentCode?default('')}&departmentName=${departmentName?default('')}">
				<#if (Parameters.multiSelect)?string('true','false') == "true">
					<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
				</#if>
				<@datacolumn key="code" label="${getHtmlText('foundation.department.code')}" width="200" />
				<@datacolumn key="name" label="${getHtmlText('foundation.department.name')}" width="200" />
				<#-- 排布自定义字段 -->
				<#if customPropList?? && customPropList?has_content>
					<#list customPropList as c>
						<#assign cpWidth = 100>
						<#if c.property.type == 'DATETIME'>
							<#assign cpWidth = 130>
						</#if>
						<#if c.fieldType == 'TEXTAREA'>
							<#assign cpWidth = 200>
						</#if>
						<#assign cpOrder = true>
						<#assign cpMultable = false>
						<#if (c.property.type == 'SYSTEMCODE' && ((c.property.multable!false) || (c.property.seniorSystemCode!false))) || c.property.type == 'OBJECT'>
							<#assign cpOrder = false>
						</#if>
						<#if c.property.type == 'SYSTEMCODE' && (c.property.multable!false)>
							<#assign cpMultable = true>
						</#if>
						<#assign cpType = ''>
						<#if c.property.type == 'SYSTEMCODE'>
							<#if (c.property.seniorSystemCode!false)>
								<#assign cpType = 'textfield'>
							<#else>
								<#assign cpType = 'systemcode'>
							</#if>
						<#elseif c.property.type == 'TEXT' || c.property.type == 'OBJECT'>
							<#assign cpType = 'textfield'>
						<#else>
							<#assign cpType = c.property.type?lower_case>
						</#if>
						<#assign cpDecimal = ''>
						<#if c.property.type == 'DECIMAL'>
							<#assign cpDecimal = (c.property.decimalNum!0)>
						</#if>

						<#assign mainDisplay = 'attrMap.' + c.propertyLayRec>
						<@datacolumn columnName=c.property.columnName width=cpWidth key="${mainDisplay}" showFormat=c.format order=cpOrder label="${getText('${c.displayName}')}" decimal="${cpDecimal}" multable=cpMultable type="${cpType}" />

					</#list>
				</#if>
				<#-- 排布自定义结束 -->


			</@datatable>
			</div></div>
			<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
			  <@frame id="department_Button" region="south" height=28>
			    <div align="right" style="margin-right:20px;position:absolute;bottom:5px;right:0;z-index:100;">
			     	<#if closePage?exists&&closePage==false>
			     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
			     	<#else>
			     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.chooseandclose')}</span></a>
					</#if>
						<a id="bottom-reset" onclick="CUI.resetForm('department_query_form')" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.cancel')}</span></a>
				</div>
		     </@frame>
		<script>
   		$(function(){
   			var windowHeight = $(window).height();
   			$("#departmentList_datatable_container").css({"height":(windowHeight - 60) + "px"});

   			$(window).on("resize",function(){
   				var windowHeight = $(window).height();
   				$("#departmentList_datatable_container").css({"height":(windowHeight - 60) + "px"});
   			})

   		});
   		</script>
	     </#if>
	  </@frame>

</@frameset>

<input type="hidden" id="multiSelect" name="multiSelect" value="${(multiSelect)?string('true','false')}" />
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="departmentCompanyId" name="departmentCompanyId" value="${getCurrent('companyId')}" />
<input type="hidden" id="rowIndex" name="rowIndex" />
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="UTF-8" language="javascript">
changeBtnStyle();//按钮交互效果
(function(){
	//注册命名空间
	CUI.ns("foundation.department");
	foundation.department.showDepartmentWorks=function(oNode){
		CUI("#department_query_form #inputValue input").val("");
		CUI('#selectid').val(oNode.id);
		url = "/msService/ec/foundation/department/common/showDepartmentList?id=" + oNode.id + "&code=" + oNode.code;
		var dataPost="&companyId="+ CUI("#departmentCompanyId").val();
		datatable_departmentListCommon.setRequestDataUrl(encodeURI(url),dataPost);
		CUI("#formDepartmentCompanyId").val(CUI("#departmentCompanyId").val());
		departmentListCommonWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"foundation.department.showDepartmentWorksPage(1)"
	    	});
	}
	foundation.department.showDepartmentWorksPage = function(type,pageNo){
		var id=CUI('#selectid').val();
		var pageSize=CUI('input[name="departmentListCommon_PageLink_PageCount"]').val();
		var	url = "/msService/ec/foundation/department/common/showDepartmentList?a=-1";
		var dataPost = "&id="+encodeURIComponent(id);
		if(pageSize!=null&&pageSize!="undefined") {
 			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
        }
        if(pageNo!=undefined){
        	dataPost+="&page.pageNo="+encodeURIComponent(pageNo);
        }
		datatable_departmentListCommon.setRequestDataUrl(url,dataPost);
		datatable_departmentListCommon._initDomElements();
	}
	<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
		$(function(){

			//var preHeight = $("#departmentList").css("height");
			//$("departmentList").css("height", parseInt( (parseInt(preHeight.slice(0,-2)) * 0.90)) + 'px' );
			$("#bottom-submit").click( function(){
				foundation.department.sendBackDepartmentInfo();
			});
			$("#bottom-reset").click( function(){
				window.close();
			});
		});
	</#if>
	/**
	 * 列表查询
	 * @method foundation.department.queryList
	 * @public
	 */
	foundation.department.CommonqueryList = function(type,pageNo){
	    departmentListCommonWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"foundation.department.CommonqueryList(1)"
	    });
		chooseDepartmentTreePage.cancelSelectedNode();//查询后解除树的选中状态
		var url = "";
		var pageSize=CUI('input[name="departmentListCommon_PageLink_PageCount"]').val();
        url = "/msService/ec/foundation/department/queryList?a=-1";
      	var dataPost = "&departmentCode="+CUI.trim(encodeURIComponent(CUI('#department_query_form #department_code').val()));
      	//console.log(CUI('#department_query_form #Department_name'));
      	//console.log(CUI.trim(encodeURIComponent(CUI('#department_query_form #departmentName').val())));
 		dataPost += "&departmentName="+CUI.trim(encodeURIComponent(CUI("input[name='department.name']",CUI("#department_query_form")).val()));
 		dataPost+="&departmentId="+CUI.trim(encodeURIComponent(CUI("input[name='department.id']",CUI("#department_query_form")).val()));
 		dataPost += "&companyId="+encodeURIComponent(CUI('#departmentCompanyId').val());
        if(pageSize!=null&&pageSize!="undefined") {
 			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
        }
        if(pageNo!=undefined){
        	dataPost+="&page.pageNo="+pageNo;
        }
        dataPost+="&"+department_query_form_getCookieParam();
 		datatable_departmentListCommon.setRequestDataUrl(url,dataPost);
	}
	<#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
	<#assign requestUri = requestUri?replace('/', '_', 'r')>
	// 供外部调用
	foundation.common.${requestUri}__callbackFunction = function(){
		if(datatable_departmentListCommon.getSelectedRow().length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.department.checkselected')}");
			return false;
		}
		foundation.department.sendBackDepartmentInfo(null,datatable_departmentListCommon.getSelectedRow());
	}
	// 供外部调用
	specialPermission__callbackFunction = function(){
		if(datatable_departmentListCommon.getSelectedRow().length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.department.checkselected')}");
			return false;
		}
		foundation.department.sendBackDepartmentInfo(null,datatable_departmentListCommon.getSelectedRow());
	}
	/**
	 * 双击事件
	 * @method foundation.department.sendBackDepartmentInfo
	 * @public
	 */
	foundation.department.sendBackDepartmentInfo=function(event,oRow){
		var arrObj = new Array();

		var oRows = new Array();
		if(event == undefined){
			oRows = datatable_departmentListCommon.getSelectedRow();
		}else{
			oRows.push(oRow);
		}
		if(oRows.length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.department.checkselected')}");
			return false;
		}

		for(var i=0; i<oRows.length; i++){
			//var oDepartment = new Object();
			//oDepartment.id = oRows[i].id;
			//oDepartment.code = oRows[i].code;
			//oDepartment.name = oRows[i].name;
			//oDepartment.parent = {};
			//oDepartment.parent.id = oRows[i].parent.id;
			//oDepartment.parent.code = oRows[i].parent.code;
			//oDepartment.parent.name = oRows[i].parent.name;
			//oDepartment.rowIndex = CUI("#rowIndex").val();
			//arrObj.push(oDepartment);
			oRows[i].rowIndex = CUI("#rowIndex").val();
			arrObj.push(oRows[i]);
		}
		try{
			if(CUI("#callBackFuncName").val() != ""){
				var flag;
				<#if (Parameters.openType)?default('page') != 'dialog'>
					<#if (Parameters.openType)?default('page') != 'frame'>
						flag = eval("opener." + CUI("#callBackFuncName").val() + "(arrObj)");
					<#else>
						flag = eval("parent." + CUI("#callBackFuncName").val() + "(arrObj)");
					</#if>
				<#else>
					flag = eval(CUI("#callBackFuncName").val() + "(arrObj)");
				</#if>
				if(flag === false){
					return flag;
				}
			}else{
				getDepartmentInfo(arrObj);
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
			departmentListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.department.add.success')}","s");
		}catch(e){
			departmentListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.department.add.failure')}","f");
			//alert("注意：父窗口回调出错！");
		}

	}
	//部门助记码回调函数
	foundation.department.departmentInfoCallback=function(obj){
	   if(obj!=null){
	      CUI("#Department_id",CUI("#department_query_form")).val(obj[0].id);
	   }
	}
	/**
	 * 公司选择
	 * @method foundation.department.sendBackDepartmentInfo
	 * @public
	 */
 	foundation.department.department_changeCompany=function(oSelect){
	setTimeout(function(){
		// 修改root节点
		CUI("#departmentCompanyId").val(oSelect.getAttribute("key"));
		CUI("#formDepartmentCompanyId").val(oSelect.getAttribute("key"));
		chooseDepartmentTreePage.getNodes()[0].name = oSelect.innerHTML;
		chooseDepartmentTreePage.updateNode(chooseDepartmentTreePage.getNodes()[0], true);
		var url = "/msService/ec/foundation/department/listChildren?companyId="+ oSelect.getAttribute("key");
		chooseDepartmentTreePage.setting.async.url=url;
		chooseDepartmentTreePage.reAsyncChildNodes(chooseDepartmentTreePage.getNodes()[0], "refresh");
		//if(closeLoadPanel){closeLoadPanel(false);}
	},0);
	}
})();
</script>
