<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<@head/>

<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<title i18n="foundation.company.title">${getText('foundation.company.title')}</title>
</head>
<body id="dialog_page">
</#if>
<@errorbar id="companyListFramePageErrorBar" />
<@frameset id="chooseDepartmentFrameset" border=0 style="height:100%;width:100%">
	  <@frame id="company_select_center" region="center" offsetH=5 resize=true>
		  	<div id="companyQueryDiv" >
				<form id="company_query_form" name="company_query_form" onsubmit="return false;" >
					<@quickquery formId="company_query_form"  fieldcodes="base_company_shortName:foundation.company.shortname||base_company_code:foundation.company.code" unique="LAST_QUERY_foundation_company_list_frame">
				        <@queryfield formId="company_query_form" code="base_company_shortName" key="company.shortName"  searchClick="foundation.company.companyQueryList()"></@queryfield>
				        <@queryfield formId="company_query_form" key="company_code" code="base_company_code"></@queryfield>
				        <@querybutton formId="company_query_form" type="search" onclick="foundation.company.companyQueryList()"/>
			 			<@querybutton formId="company_query_form" type="clear"  />
				     </@quickquery>
					
				</form>
			</div>
			<#assign superChecked = multiSelect?default('false') == 'true'>
		  	<@datatable transMethod="post" superChecked=superChecked superCheckedName="shortName" firstLoad=true dtPage="page" hidekey="['id','name']" formId="company_query_form" id="companyListCommon" style="margin:6px 12px;" dblclick="foundation.company.sendBackCompanyInfo" dataUrl="/msService/ec/foundation/company/queryList?a=-1" postData="&companyCode=${companyCode?default('')}&companyName=${companyName?default('')}&${condition?default('')?replace('@@equal_quote@@','=')}">
				<#if multiSelect?default("false") == "true">
					<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
				</#if>
				<@datacolumn key="code" label="${getHtmlText('foundation.company.code')}" width="200" />
				<@datacolumn key="shortName" label="${getHtmlText('foundation.company.shortname')}" width="200" />
			</@datatable>
			<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
			  <@frame id="company_Button" region="south" height=28>
			    <div align="right" style="margin-right:20px;position:absolute;bottom:5px;right:0;z-index:100;">
			     	<#if closePage?exists&&closePage==false>
			     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
			     	<#else>
			     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.chooseandclose')}</span></a>
					</#if>
						<a id="bottom-reset" onclick="CUI.resetForm('company_query_form')" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.cancel')}</span></a>
				</div>
		     </@frame>
	     </#if>
	  </@frame>
	  
</@frameset>

<input type="hidden" id="multiSelect" name="multiSelect" value="${multiSelect!}" />
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" id="callBackFuncName"/>
<input type="hidden" id="rowIndex" name="rowIndex" />
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="UTF-8" language="javascript">
changeBtnStyle();//按钮交互效果
(function(){
	//注册命名空间
	CUI.ns("foundation.company");
	
	
	<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
		$(function(){
			$("#bottom-submit").click( function(){
				foundation.company.sendBackCompanyInfo();
			});
			$("#bottom-reset").click( function(){
				window.close();
			});
		});
	</#if>
	/**
	 * 列表查询
	 * @method foundation.company.queryList
	 * @public
	 */
	foundation.company.companyQueryList = function(type,pageNo){
	   
		
		var url = "";
		var pageSize=CUI('input[name="companyListCommon_PageLink_PageCount"]').val();
        url = "/foundation/company/queryList.action?a=-1";
      	var dataPost = "&companyCode="+CUI.trim(encodeURIComponent(CUI('#company_query_form #company_code').val()));
 		dataPost += "&companyName="+CUI.trim(encodeURIComponent(CUI("input[name='company.shortName']",CUI("#company_query_form")).val()));
 		dataPost+="&companyId="+CUI.trim(encodeURIComponent(CUI("input[name='company.id']",CUI("#company_query_form")).val()));
 		<#if condition?? && condition != "">
 		dataPost+="&${condition?replace('@@equal_quote@@','=')}";
 		</#if>
        if(pageSize!=null&&pageSize!="undefined") {
 			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
        }
        if(pageNo!=undefined){
        	dataPost+="&page.pageNo="+pageNo;
        }
        dataPost+="&"+company_query_form_getCookieParam();
 		datatable_companyListCommon.setRequestDataUrl(url,dataPost);
	}
	<#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
	<#assign requestUri = requestUri?replace('/', '_', 'r')>
	// 供外部调用
	foundation.common.${requestUri}__callbackFunction = function(){
		if(datatable_companyListCommon.getSelectedRow().length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.company.checkselected')}");
			return false;
		}
		foundation.company.sendBackCompanyInfo(null,datatable_companyListCommon.getSelectedRow());
	}
	/**
	 * 双击事件
	 * @method foundation.company.sendBackCompanyInfo
	 * @public
	 */
	foundation.company.sendBackCompanyInfo=function(event,oRow){
		var arrObj = new Array();
	
		var oRows = new Array();
		if(event == undefined){
			oRows = datatable_companyListCommon.getSelectedRow();
		}else{
			oRows.push(oRow);
		}	
		if(oRows.length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.company.checkselected')}");
			return false;
		}
		
		for(var i=0; i<oRows.length; i++){
			
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
				getCompanyInfo(arrObj);
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
			companyListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.company.add.success')}","s");
			</#if>
		}catch(e){
			companyListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.company.add.failure')}","f");
			//alert("注意：父窗口回调出错！");
		}
		
	}
	
})();
</script>
