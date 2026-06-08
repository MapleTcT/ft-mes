<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<@head/>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<title>${getText('foundation.position.title')}</title>
</head>
<body>
</#if>
<@errorbar id="positionListFrameErrorbar" />
<@frameset id="positionFrameset" border=0 style="height:100%;width:100%">
   <@frame id="positionFrameset_left_in" region="west" width=200 resize=true style="overflow-y:auto;overflow-x:hidden;">
   		<#if crossCompanyFlag??&&crossCompanyFlag || isSingleMode??&&isSingleMode?string("true","false")=='false'>
   			<style type="text/css">
				#positionListFrameTreePage{position:relative;top:23px;}
				#positionFrameset_left_in{position:relative;}
			</style>
			<div class="tree-companylist">
        		<div class="tree-companylist-son">
					<#assign l = companyList>
					<@listmenu list=l id="DepartCompanyList" listName="name" listKey="id"   value="${getCurrent('companyName')}" onclick="foundation.position.changeCompany"  cssStyle="top:4px;left:50px;" ></@listmenu>
				</div>
			</div>
	   </#if>
		<@tree id="positionListFrameTreePage" dataUrl="/msService/ec/foundation/position/listChildren" rootName="${getCurrent('companyName')}" isCopy=false isMove=false
			callback="{onClick:function(event,treeId,node){CUI.resetForm('position_query_form');foundation.position.showPositionWorks(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
			<input type="hidden" id="selectid" name="selectid" />
	</@frame>
    <@frame id="positionFrameset_center" region="center" offsetH=4 >
    	<div id="positionQueryDiv" >
			<form id="position_query_form" name="position_query_form" onsubmit="return false;" >
			<input type="hidden" id="formPositionCompanyId" name="companyId" value="${getCurrent('companyId')}" reset="false"  />
				<@quickquery formId="position_query_form"  fieldcodes="base_position_code||base_position_name||base_department_name" unique="LAST_QUERY_foundation_position_common_list_frame">
			       	<#if (crossCompanyFlag??&&crossCompanyFlag)>
			       		<@queryfield formId="position_query_form" code="base_position_name" key="position.name" isCrossCompany=true mneurl="other" mneclick=false type="Position" searchClick="foundation.position.queryListCommon()"></@queryfield>
			       		<@queryfield formId="position_query_form" code="base_position_code" key="position.code" isCrossCompany=true></@queryfield>
			       		<@queryfield formId="position_query_form" code="base_department_name" key="department.name" isCrossCompany=true mneurl="other" mneclick=false type="Department" searchClick="foundation.position.queryListCommon()"></@queryfield>
			    	<#else>
			    		<@queryfield formId="position_query_form" code="base_position_name" key="position.name" mneurl="other" mneclick=false type="Position" searchClick="foundation.position.queryListCommon()"></@queryfield>
			       		<@queryfield formId="position_query_form" code="base_position_code" key="position.code"></@queryfield>
			       		<@queryfield formId="position_query_form" code="base_department_name" key="department.name" mneurl="other" mneclick=false type="Department" searchClick="foundation.position.queryListCommon()"></@queryfield>
			    	</#if>
			    	<@querybutton formId="position_query_form" type="search" onclick="foundation.position.queryListCommon()"/>
			 		<@querybutton formId="position_query_form" type="clear"  />
			     </@quickquery>
			 
				<!--<table cellpadding="0" cellspacing="0" border="0" align="center" width="100%" style="margin-top: 8px">
					<tr style="height: 22px">
						<td width="12%" align="right" style="padding-right: 5px;">${getText('foundation.position.code')}</td>
						<td width="18%"><div class="fix-input"><input type="text" id="positionCode" class="cui-noborder-input" value="${positionCode!}"></input></div></td>
						<td width="12%" align="right" style="padding-right: 5px;">${getText('foundation.position.name')}</td>
						<td width="18%">
						<input id="positionId" name="position.id" type="hidden"/>
						<@mneclient name="position.name" id="positionName" onkeyupfuncname="foundation.position.positionInfoCallback(obj)" url="other" classStyle="cui-noborder-input"  type="Position" multiple=false mnewidth=260 isPrecise=true/>
						</td>
						<td width="12%" align="right" style="padding-right: 5px;">${getText('foundation.department.name')}</td>
						<td width="18%">  
						<input id="departmentId" name="department.id" type="hidden"/>
						<@mneclient name="department.name" onkeyupfuncname="foundation.position.departmentInfoCallback(obj)" id="deptName" url="other" classStyle="cui-noborder-input" type="Department" multiple=false mnewidth=260 isPrecise=true/>
						</td>
						<td width="10%"></td>
					</tr>
					
					<tr style="height: 30px">
						<td width="18%"></td>
						<td width="20%" colspan="5" style="text-align:right;">
							<div class="edit-btn btn-act" id="reset" style="float:right;" onclick="CUI.resetForm('position_query_form');">
								<a class="cui-btn-l">&nbsp;</a>
								<a class="cui-btn-c">${getText('common.button.reset')}</a>
								<a class="cui-btn-r">&nbsp;</a>
							</div>
							<div class="edit-btn btn-act" id="queryButton" style="float:right;" onclick="foundation.position.queryListCommon()">
								<a class="cui-btn-l">&nbsp;</a>
								<a class="cui-btn-c">${getText('common.button.query')}</a>
								<a class="cui-btn-r">&nbsp;</a>
							</div>
							
						</td>
					</tr>
				</table>-->
			</form>
		</div>
		<#assign superChecked = (Parameters.multiSelect)?string('true','false') == 'true'>
		<#assign customPropList = getShowCustomSecret("sysbase_1.0_position_base_position", "sysbase_1.0_position_edit", 'LIST')>
		<div id="positionList_datatable_container" style='clear:both;'>
		<@datatable transMethod="post" superChecked=superChecked superCheckedName="name" dtPage="page" firstLoad=false hidekey="['id','cid','company.name','department.id','fullPathName','layRec']" formId="position_query_form" id="positionListCommon" style="margin:4px 10px;" dblclick="foundation.position.sendBackPositionInfo" dataUrl="/msService/ec/foundation/position/queryList?a=-1" postData="&positionCode=${positionCode?default('')}&positionName=${positionName?default('')}&deptName=${deptName?default('')}">
			<#if (Parameters.multiSelect)?string('true','false') == "true">
				<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
			</#if>
			<@datacolumn key="code" label="${getHtmlText('foundation.position.code')}" width=150 />
			<@datacolumn key="name" label="${getHtmlText('foundation.position.name')}" width=150 />
			<@datacolumn key="department.name" label="${getHtmlText('foundation.position.department')}" width=150 />
			
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
		</div>
    </@frame>
	<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
	    <@frame id="position_Button" region="south" height=28>
		    <div align="right" style="margin-right:20px;position:absolute;bottom:5px;right:0;z-index:100;">
		     	<#if closePage?exists&&closePage==false>
		     		<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
			    <#else>
			    	<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.chooseandclose')}</span></a>
				</#if>
					<a id="bottom-reset" onclick="YUD.get('department_query_form').reset(); return false;" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.cancel')}</span></a>
			</div>
	     </@frame>
     </#if>
</@frameset>
<input type="hidden" id="multiSelect" name="multiSelect" value="${(multiSelect)?string('true','false')}" />
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="crossCompanyFlag" name="crossCompanyFlag" value="${(crossCompanyFlag)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="rowIndex" name="rowIndex" />
<input type="hidden" id="companyName" name="companyName" value="${getCurrent('companyName')}" />
<input type="hidden" id="positionCompanyId" name="companyId" value="${getCurrent('companyId')}" />
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="utf-8" language="javascript">
changeBtnStyle();//按钮交互效果
(function(){
	//注册命名空间
	CUI.ns("foundation.position");
	
	<#if (Parameters.openType)?default('page') != 'dialog' && (Parameters.openType)?default('page') != 'frame'>
		$(function(){
			<#if (Parameters.multiSelect)?string('true','false') == "true">
				//alert("multiSelect");
			</#if>
			var preHeight = $("#positionListCommon").css("height");
			//$("positionList").css("height", parseInt( (parseInt(preHeight.slice(0,-2)) * 0.90)) + 'px' );
			$("#bottom-submit").click( function(){
				foundation.position.sendBackPositionInfo();
			});
			$("#bottom-reset").click( function(){
				window.close();
			});
		});
	</#if>
	
	/**
	 * 点击岗位树节点，该岗位下级岗位列表
	 * @method foundation.position.showPositionWorks
	 * @public
	 */
	 foundation.position.showPositionWorks=function(oNode){
	 	CUI("#position_query_form #inputValue input").val("");
	 	CUI('#selectid').val(oNode.id);
		url = "/msService/ec/foundation/position/common/showPositionList?id=" + oNode.id ;
		var dataPost="&companyId="+ CUI("#positionCompanyId").val();
		datatable_positionListCommon.setRequestDataUrl(encodeURI(url),dataPost);
		CUI("#formPositionCompanyId").val(CUI("#positionCompanyId").val());
		positionListCommonWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"foundation.position.showPositionWorksPage(1)"
	    });
	}
	foundation.position.showPositionWorksPage = function(type,pageNo){
		var id=CUI('#selectid').val();
		var pageSize=CUI('input[name="positionListCommon_PageLink_PageCount"]').val();
		var	url = "/msService/ec/foundation/position/common/showPositionList?a=-1";
		var dataPost = "&id="+encodeURIComponent(id);
		if(pageSize!=null&&pageSize!="undefined") {
 			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
        }
        if(pageNo!=undefined){
        	dataPost+="&page.pageNo="+encodeURIComponent(pageNo);
        }
		datatable_positionListCommon.setRequestDataUrl(url,dataPost);
		datatable_positionListCommon._initDomElements();
	}
	<#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
	<#assign requestUri = requestUri?replace('/', '_', 'r')>
	// 供外部调用
	foundation.common.${requestUri}__callbackFunction = function(){
		if(datatable_positionListCommon.getSelectedRow().length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.position.checkselected')}");
			return false;
		}
		foundation.position.sendBackPositionInfo(null,datatable_positionListCommon.getSelectedRow());
	}
	specialPermission__callbackFunction = function(){
		if(datatable_positionListCommon.getSelectedRow().length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.position.checkselected')}");
			return false;
		}
		foundation.position.sendBackPositionInfo(null,datatable_positionListCommon.getSelectedRow());
	}
	/**
	 * 列表查询
	 * @method foundation.position.queryList
	 * @public
	 */
	 foundation.position.queryListCommon=function(type,pageNo){
	 		positionListCommonWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"foundation.position.queryListCommon(1)"
	    	});
			positionListFrameTreePage.cancelSelectedNode();//查询后解除树的选中状态
			var url = "";
			var pageSize=CUI('input[name="positionListCommon_PageLink_PageCount"]').val();
			url = "/msService/ec/foundation/position/queryList?a=-1";
		      	var dataPost = "&position.code="+CUI.trim(encodeURIComponent(CUI('#position_query_form input[name="position.code"]').val()));
	     		dataPost += "&position.name="+CUI.trim(encodeURIComponent(CUI("input[name='position.name']",CUI("#position_query_form")).val()));
	     		dataPost += "&position.id="+CUI.trim(encodeURIComponent(CUI("#position_query_form input[name='position.id']").val()));
	     		dataPost += "&department.name="+CUI.trim(encodeURIComponent(CUI("input[name='department.name']",CUI("#position_query_form")).val()));
	     	    dataPost += "&department.id="+CUI.trim(encodeURIComponent(CUI("#position_query_form input[name='department.id']").val()));
				dataPost += "&companyId="+encodeURIComponent(CUI('#positionCompanyId').val());
	            if(pageSize!=null&&pageSize!="undefined") {
	     			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
	            }
	            if(pageNo!=undefined){
	            	dataPost += "&page.pageNo="+pageNo;
	            }
	            dataPost+="&"+position_query_form_getCookieParam();
	     		datatable_positionListCommon.setRequestDataUrl(url,dataPost);
	}	
	
	/**
	 * 公司选择
	 * @method foundation.position.changeCompany
	 * @public
	 */
 	foundation.position.changeCompany=function(oSelect){
		setTimeout(function(){
			// 修改root节点
			CUI("#positionCompanyId").val(oSelect.getAttribute("key"));
			CUI("#formPositionCompanyId").val(oSelect.getAttribute("key"));
			positionListFrameTreePage.getNodes()[0].name = oSelect.innerHTML;
			positionListFrameTreePage.updateNode(positionListFrameTreePage.getNodes()[0], true);
			var url = "/msService/ec/foundation/position/listChildren?companyId="+ oSelect.getAttribute("key");
			positionListFrameTreePage.setting.async.url=url;
			positionListFrameTreePage.reAsyncChildNodes(positionListFrameTreePage.getNodes()[0], "refresh");
			//if(closeLoadPanel){closeLoadPanel(false);}
		},0);
	}
	/**
	*岗位助记码回调函数
	*/
	foundation.position.positionInfoCallback=function(obj){
	   if(obj!=null){
	      CUI("#position_query_form #Position_id").val(obj[0].id);
	   }
	}
	/**
	*部门助记码回调函数
	*/
	foundation.position.departmentInfoCallback=function(obj){
	   if(obj!=null){
	      CUI("#position_query_form #Department_id").val(obj[0].id);
	   }
	}
	
	/**
	 * 双击事件
	 * @method foundation.position.sendBackPositionInfo
	 * @public
	 */
	foundation.position.sendBackPositionInfo=function(event,oRow){
		var arrObj = new Array();
	
		var oRows = new Array();
		if(event == undefined){
			oRows = datatable_positionListCommon.getSelectedRow();
		}else{
			oRows.push(oRow);
		}	
		if(oRows.length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.position.checkselected')}");
			return false;
		}
		
		for(var i=0; i<oRows.length; i++){
			//var oPosition = new Object();
			//oPosition.id = oRows[i].id;
			//oPosition.baseposition_code = oRows[i].code;
			//oPosition.baseposition_name = oRows[i].name;
			//oPosition.companyID = oRows[i].company.id;
			//oPosition.companyName = oRows[i].company.name;
			//oPosition.rowIndex = CUI("#rowIndex").val();
			//arrObj.push(oPosition);
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
			positionListFrameErrorbarWidget.showMessage("${getHtmlText('foundation.add.success')}","s");
			</#if>
		}catch(e){
			positionListFrameErrorbarWidget.showMessage("${getHtmlText('foundation.add.failure')}","f");
			//alert("注意：父窗口回调出错！");
		}
	}
})();
</script>