<#if (Parameters.openType)?default('page') == 'frame'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.staff.selectStaff')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">

<style type="text/css">
	.elm-layout-doc-wrap .elm-layout-wrap-west{
		background-color: #f3f3f3;
	}
	.elm-layout-doc-wrap{
		background-color: #f3f3f3;
	}
</style>

</#if>

<@errorbar id="positionStaffListErrorBar" />
<input type="hidden" name="tabCallBackFuncName" value="foundation.common.__callbackFunctionPosStaff">
<@frameset id="positionFrameset" border=0>
     <@frame id="position_left" region="west" width=200 offsetH=0 resize=true style="overflow-y:auto;overflow-x:hidden;">	
      		<#if crossCompanyFlag??&&crossCompanyFlag>
      		<style type="text/css">
				#positionStaffListTree{position:relative;top:23px;}
				#position_left{position:relative;}
			</style>
      		<div class="tree-companylist">
        	<div class="tree-companylist-son">
				<#assign l = companyList>
				<@listmenu list=l id="positionCompanyList" listName="name" listKey="id"   value="${getCurrent('companyName')}" onclick="foundation.staff.positionChangeCompany"  cssStyle="top:4px;left:50px;" ></@listmenu>
			</div>
			</div>
	   </#if>
        <@tree id="positionStaffListTree" dataUrl="/msService/ec/foundation/position/listChildren?companyId=${companyId}" rootName="${getCurrent('companyName')}"
				 callback="{onClick:function(event,treeId,node){CUI.resetForm('position_queryForm');foundation.staff.showPositionWorks(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
     </@frame>
     <@frame id="position_center" region="center" offsetH=5 >
     		<form id="position_queryForm" name="position_queryForm" onsubmit="return false;">
     			<div id="primaryPosition" style="float: left; margin-top: 8px; _margin-top: 5px;">
     			    <input type="checkbox" name="filterNoUserStaff" value="${defaultFilterNoUserStaff?string('true','false')}" checked="checked" id="filterNoUserStaff_dept" style="border:none; vertical-align: middle; margin: 0 3px 0 12px;" reset="false"/>
					<span class="headsplit" style="position: absolute; z-index: 5; top: 5px;">&nbsp;</span>
				</div>
				<@quickquery formId="position_queryForm"  fieldcodes="base_staff_name:foundation.staff.name||base_staff_code:foundation.staff.code" unique="LAST_QUERY_foundation_staff_common_positionStaffList">
			       	<@queryfield formId="position_queryForm" code="base_staff_code" key="staff.code"></@queryfield>
			       	<#if (crossCompanyFlag??&&crossCompanyFlag)>
			       		<@queryfield formId="position_queryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" isCrossCompany=true mneclick=false searchClick="foundation.staff.queryPositionWorks()"></@queryfield>
					<#else>
						<@queryfield formId="position_queryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" mneclick=false searchClick="foundation.staff.queryPositionWorks()"></@queryfield>
					</#if>
					 <@querybutton formId="position_queryForm" type="search" onclick="foundation.staff.queryPositionWorks()" />
			 	 <@querybutton formId="position_queryForm" type="clear"  />
				</@quickquery>
			</form>
		<#assign superChecked = (Parameters.multiSelect)?string('false','true') == 'true'>
		<@datatable style="margin-top:6px;margin-left:12px;" superChecked=superChecked superCheckedName="staff.name" superCheckedId="staff.id" formId="position_queryForm" firstLoad=false dtPage="positionWorkPage" hidekey="['id','staff.id','company.id','staff.sex','staff.user.username','staff.user.id','position.id','position.name','position.department.name','position.department.id']"  dblclick="foundation.staff.sendBackStaffInfo"  transMethod="post" id="position_staffListTable" dataUrl="/msService/ec/foundation/staff/common/getPositionWorkList?companyId=-1" pageInitMethod="hideAddToGroupPos" moreButtonResizeFlag=false>
			<@operatebar operates="code:base_addToUserDefinedGroup_pos||name:${getHtmlText('foundation.customGroup.userDefined.addToGroup')}||iconcls:add||onclick:foundation.staff.addToUserDefinedGroup_pos()" operateType="noPower" resultType="json"/>
			<#if (Parameters.multiSelect)?string('false','true') == "true">
				<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
			</#if>
			<@datacolumn key="staff.code" label="${getHtmlText('foundation.staff.code')}" width=70></@datacolumn>
			<@datacolumn key="staff.name" label="${getHtmlText('foundation.staff.name')}" width=90></@datacolumn>
			<@datacolumn key="position.department.name" label="${getHtmlText('foundation.department.name')}" width=150></@datacolumn>
			<@datacolumn key="position.name" label="${getHtmlText('foundation.position.name')}" width=150></@datacolumn>
		</@datatable>
		
     </@frame>
</@frameset>
<script type="text/javascript" charset="UTF-8" language="javascript">
changeBtnStyle();//按钮交互效果
(function(){
	//注册命名空间
	CUI.ns("foundation.staff");

	/**
	 * 点击部门树节点，该部门下的人员信息列表
	 * @method foundation.staff.showDepartmentWorks
	 * @public
	 */
	foundation.staff.showPositionWorks=function(oNode){
		
		url = "/msService/ec/foundation/staff/common/getPositionWorkList?positionId=" + oNode.id + "&companyId=" + <#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>CUI("#companyId").val();
		var pageSize=CUI('input[name="position_staffListTable_PageLink_PageCount"]').val();
		var	dataPost = "&pageSize="+encodeURIComponent(pageSize);	
		if($('#filterNoUserStaff_pos') && $('#filterNoUserStaff_pos').prop('checked')){
			dataPost += '&filterNoUserStaff=true';
		}
		datatable_position_staffListTable.setRequestDataUrl(encodeURI(url),dataPost);
	}
	//查询
	 foundation.staff.queryPositionWorks=function(){
			positionStaffListTree.cancelSelectedNode();
			var url = "/msService/ec/foundation/staff/common/getPositionWorkList?companyId=" + <#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>CUI("#companyId").val();
			var staffCode = CUI.trim(CUI("input[name='staff.code']",CUI("#position_queryForm")).val());
			var staffName = CUI.trim(CUI("input[name='staff.name']",CUI("#position_queryForm")).val());
			var staffId=CUI.trim(CUI("#position_queryForm input[name='staff.id']").val());
			var dataPost="";
			if(staffCode != ''){
				dataPost += "&staff.code=" + staffCode;
			}	
			if(staffId!=''){
			   dataPost+="&staff.id="+staffId;
			}
			else{
				if(staffName != ''){
					dataPost += "&staff.name=" + staffName;
				}
			}
			if($('#filterNoUserStaff_pos') && $('#filterNoUserStaff_pos').prop('checked')){
				dataPost += '&filterNoUserStaff=true';
			}
			var pageSize=CUI('input[name="position_staffListTable_PageLink_PageCount"]').val();
			dataPost += "&pageSize="+encodeURIComponent(pageSize);	
			//console.log(pageSize);
			dataPost+="&"+position_queryForm_getCookieParam();
			position_staffListTableWidget.setRequestDataUrl(url,dataPost);
			
			
		    return false;
	}
    /**
	*人员助记码回调函数
	*/
	foundation.staff.staffInfoCallback=function(obj){
	    if(obj!=null){
	       CUI("#position_queryForm #staffId").val(obj[0].id);
	    }
	}
	// 供外部调用
	foundation.common.__callbackFunctionPosStaff = function(){
		if(position_staffListTableWidget.getSelectedRow().length == 0){
			<#if (Parameters.openType)?default('page') == 'frame'>
			parent.CUI.Dialog.alert("${getText('foundation.staff.checkselected')}");
			<#else>
			CUI.Dialog.alert("${getText('foundation.staff.checkselected')}");
			</#if>
			return false;
		}
		return foundation.staff.sendBackStaffInfo(null,position_staffListTableWidget.getSelectedRow());
	}
	//双击或点击选择按钮进行选取
	foundation.staff.sendBackStaffInfo = function(event,oRow){
		var arrObj = new Array();
	
		var oRows = new Array();
		if(event == undefined || event == null){
			oRows = position_staffListTableWidget.getSelectedRow();
		}else{
			oRows.push(oRow);
		}
	
		if(oRows.length == 0){
			positionStaffListErrorBarWidget.showMessage("${getHtmlText('foundation.staff.checkselected')}","f");
			return false;
		}
	
		for(var i=0; i<oRows.length; i++){
			var oStaff = new Object();
			if( oRows[i].staff ){
				oStaff.id = oRows[i].staff.id;
				oStaff.code = oRows[i].staff.code;
				oStaff.name = oRows[i].staff.name;
				if( oRows[i].staff.user ){
					oStaff.user = {}
					oStaff.user.username = oRows[i].staff.user.username;
					oStaff.user_id = oRows[i].staff.user.id;
				}
			}
			if( oRows[i].position ){
				oStaff.position = {}
				oStaff.position.name = oRows[i].position.name;
				oStaff.position.id = oRows[i].position.id;
				if( oRows[i].position.department ){
					oStaff.position.department = {}
					oStaff.position.department.name = oRows[i].position.department.name;
					oStaff.position.department.id = oRows[i].position.department.id;
				}
			}
			if( oRows[i].company ){
				oStaff.company = {}
				oStaff.company.id = oRows[i].company.id;
			}
			
			oStaff.rowIndex = CUI("#rowIndex").val();
			//alert("oStaff [" + i + "].user_id: " + oStaff.user_id + "oStaff [" + i + "].name: " + oStaff.name);
			arrObj.push(oStaff);
		}
		//alert("arrObj length: " + arrObj.length);
		try{
			if( <#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>CUI("#callBackFuncName").val() != ""){
				var flag;
				<#if (Parameters.openType)?default('page') == 'page'>
					flag = eval("opener." + CUI("#callBackFuncName").val() + "(arrObj)");
				<#elseif (Parameters.openType)?default('page') == 'frame'>
					flag = eval("parent.parent." + parent.CUI("#callBackFuncName").val() + "(arrObj)");
				<#else>
					if(CUI("#callBackFuncName").val() == "specialPermission"){
					     flag = eval("parent." + CUI("#callBackFuncName").val() + "(arrObj)");
					 }else {
						flag = eval(CUI("#callBackFuncName").val() + "(arrObj)");
					}
				</#if>
				if(flag === false){
					return flag;
				}
			}else{
				getStaffInfo(arrObj);
			}
			<#if (Parameters.openType)?default('page') == 'page'>
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
			positionStaffListErrorBarWidget.showMessage("${getHtmlText('foundation.add.success')}","s");
			</#if>
		}catch(e){
			positionStaffListErrorBarWidget.showMessage("${getHtmlText('foundation.add.failure')}","f");
		}
	}

	/**
	 * 公司选择
	 * @method foundation.department.sendBackDepartmentInfo
	 * @public
	 */
 	foundation.staff.positionChangeCompany=function(oSelect){
		setTimeout(function(){
			// 修改root节点
			
			<#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>CUI("#companyId").val(oSelect.getAttribute("key"));
			positionStaffListTree.getNodes()[0].name = oSelect.innerHTML;
			positionStaffListTree.updateNode(positionStaffListTree.getNodes()[0], true);
			var url = "/msService/ec/foundation/position/listChildren.action?companyId="+ oSelect.getAttribute("key");
			positionStaffListTree.setting.async.url=url;
			positionStaffListTree.reAsyncChildNodes(positionStaffListTree.getNodes()[0], "refresh");
			if(<#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>closeLoadPanel){<#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>closeLoadPanel(false);}
		},0);
	}
	
	foundation.staff.addToUserDefinedGroup_saveHandler = function(){
		parent.parent.foundation_staff_addToGroupFrame.CUI("#foundation_addToGroup_form").submit();
	}
	foundation.staff.addToUserDefinedGroup_cancelHandler = function(){
		parent.parent[ 'foundation_staff_addToGroupDialog' ].close();
	}
	
	foundation.staff.addToUserDefinedGroup_pos = function(){
		var rows = datatable_position_staffListTable.getSelectedRow();
		if(rows.length == 0){
			positionStaffListErrorBarWidget.showMessage("${getHtmlText('foundation.staff.checkselected')}","f");
			return false;
		}
		var selectedStaffIds = '';
		var selectedStaffNames = '';
		for(var i=0; i<rows.length; i++){
			selectedStaffIds += (selectedStaffIds == '') ? rows[i].staff.id : (',' + rows[i].staff.id);
			selectedStaffNames += (selectedStaffNames == '') ? rows[i].staff.name : (',' + rows[i].staff.name);
		}
		//alert(selectedStaffIds);
		var url = "/foundation/staff/userDefinedGroup/listGroups.action?groupType=CUSTOM&groupMemberIds=" + selectedStaffIds + '&addToGroupNs=staff';
		var params = {};
		params['selectedStaffNames'] = selectedStaffNames;
		
		
		<#if (Parameters.openType)?default('page') == 'frame'>
			url += "&crossCompanyFlag=${(crossCompanyFlag!false)?string('true', 'false')}&openType=frame";
			if( parent.parent[ 'foundation_staff_addToGroupDialog' ] ){
				parent.parent[ 'foundation_staff_addToGroupDialog' ]._config.url = url;
				parent.parent[ 'foundation_staff_addToGroupDialog' ].argument = params;
				parent.parent[ 'foundation_staff_addToGroupDialog' ].show();
			}else{
				foundation.staff.addToGroupDialog = parent.parent[ 'foundation_staff_addToGroupDialog' ] = parent.parent.CUI.createDialog({
					title    :  "${getHtmlText('foundation.customGroup.userDefined.select')}",
					formId   :  'foundation_addToGroup_form',
					url      :  url,
					modal    :  true,
					
					iframe: 'foundation_staff_addToGroupFrame',
					
					type   	 :  4,
					dragable :  true,
					argument :  params,
					buttons  :  [{
									name:"${getHtmlText('common.button.save')}",
								  	handler: parent.frameElement.getAttribute( 'name' ) + '.' + window.frameElement.getAttribute( 'name' ) + '.foundation.staff.addToUserDefinedGroup_saveHandler()',
								 },
								 {
								 	name:"${getHtmlText('common.button.cancel')}",
								  	handler: parent.frameElement.getAttribute( 'name' ) + '.' + window.frameElement.getAttribute( 'name' ) + '.foundation.staff.addToUserDefinedGroup_cancelHandler()',
								 }]
				});
				foundation.staff.addToGroupDialog.show();
			}
		<#else>
			<#if (Parameters.openType)?default('page') == 'dialog'>
				url += "&openType=dialog";
			</#if>
			foundation.staff.addToGroupDialog = new CUI.Dialog({
				title    :  "${getHtmlText('foundation.customGroup.userDefined.select')}",
				formId   :  'foundation_addToGroup_form',
				url      :  url,
				modal    :  true,
				type   	 :  4,
				dragable :  true,
				argument :  params,
				buttons  :  [{
								name:"${getHtmlText('common.button.save')}",
							  	handler:function(){CUI("#foundation_addToGroup_form").submit();}
							 },
							 {
							 	name:"${getHtmlText('common.button.cancel')}",
							  	handler:function(){this.close();}
							 }]
			});
			foundation.staff.addToGroupDialog.show();
		</#if>		
	}
	
	foundation.staff.callBackUserDefinedGroup = function(res){
		if(res.dealSuccessFlag == true){	
			foundation_addToGroup_formDialogErrorBarWidget.show("${getHtmlText('foundation.common.saveandclosesuccessful')}","s");
			<#if (Parameters.openType)?default('page') == 'frame'>
				setTimeout(function(){
					parent.parent[ 'foundation_staff_addToGroupDialog' ].close();
				},1000);
			<#else>
				setTimeout(function(){
					foundation.staff.addToGroupDialog.close();
				},1000);
			</#if>
		}else{
			CUI.showErrorInfos(res, foundation_addToGroup_formDialogErrorBarWidget);
		}
	}
	
	foundation.staff.showGroupMemberList = function(select){
		var sid = select.options[select.selectedIndex].value;
		if(sid > -1){
			datatable_userDefinedGroupMembers.setRequestDataUrl("/foundation/customGroup/listGroupMember.action?group.groupType=CUSTOM&id=" + sid + "&crossCompanyFlag=${(crossCompanyFlag!false)?string('true', 'false')}");
		}	
	}
	hideAddToGroupPos = function(){
		<#if !(enableAddToGroup?? && enableAddToGroup)>
			$('#base_addToUserDefinedGroup_pos').parent('a').hide();
		</#if>
	}
})();
</script>

<#if (Parameters.openType)?default('page') == 'frame'>
</body>
</html>
</#if>