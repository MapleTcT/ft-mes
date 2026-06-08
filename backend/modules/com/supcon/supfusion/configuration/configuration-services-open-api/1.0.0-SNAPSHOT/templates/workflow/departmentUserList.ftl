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

<#assign dblcallbackName="foundation.user.sendBackUserInfo">
<#if (Parameters.openType)?? && (Parameters.openType) == 'frame'>
<#assign dblcallbackName="parent.foundation.user.sendBackUserInfo">
</#if>
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="multiSelect" name="multiSelect" value="${(multiSelect)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="currentDepartCompanyId" name="currentDepartCompanyId" value="${getCurrent('companyId')}" />

<@errorbar id="departmentUserListErrorbar" />
<@frameset id="departmentFrameset" border=0>
     <@frame id="department_left" region="west" width=200 offsetH=0 resize=true style="overflow-y:auto;overflow-x:hidden;">
		<#if crossCompanyFlag??&&crossCompanyFlag && (isSingleMode!false)?string == "false" >
		<style type="text/css">
			#chooseDepartmentUserListTree{position:relative;top:23px;}
			#department_left{position:relative;}
		</style>
		<div class="tree-companylist">
        	<div class="tree-companylist-son">
				<#assign l = companyList>
				<@listmenu list=l id="departUserCompanyList" listName="name" listKey="id"   value="${getCurrent('companyName')}" onclick="foundation.user.changeCompanyForDepart"  cssStyle="left:50px;" />
			</div>
		</div>
	   </#if>
		<@tree id="chooseDepartmentUserListTree" dataUrl="/msService/ec/foundation/department/listChildren?companyId=${getCurrent('companyId')}" rootName="${getCurrent('companyName')}" isCopy=false isMove=false
				 callback="{beforeDrag:function(treeId, treeNodes){return false;},onClick:function(event,treeId,node){CUI.resetForm('department_queryForm');foundation.user.showDepartmentUsers(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />	
    </@frame>
     <@frame id="department_center" region="center" offsetH=5 >
     	<div>
     		<form id="department_queryForm" onsubmit="return false;">
			 <@quickquery formId="department_queryForm" fieldcodes="base_staff_name:foundation.staff.name||base_user_name" unique="LAST_QUERY_foundation_user_common_departmentUserList">
			 			<#if (crossCompanyFlag??&&crossCompanyFlag)>
			       	<@queryfield formId="department_queryForm" code="base_staff_name" showRange="${Parameters.showRange!}" key="staff.name" mneclick=false isCrossCompany=true  mneurl="other" type="Staff" searchClick="foundation.user.queryDepartmentUsers()"></@queryfield>
			       	<@queryfield formId="department_queryForm" code="base_user_name" showRange="${Parameters.showRange!}" key="user.name" mneclick=false isCrossCompany=true mneurl="other" type="User" searchClick="foundation.user.queryDepartmentUsers()"></@queryfield>
					<#else>
					<@queryfield formId="department_queryForm" showRange="${showRange!}" code="base_staff_name" key="staff.name" mneclick=false mneurl="other" type="Staff" searchClick="foundation.user.queryDepartmentUsers()"></@queryfield>
			       	<@queryfield formId="department_queryForm" showRange="${showRange!}" code="base_user_name" key="user.name" mneclick=false mneurl="other" type="User" searchClick="foundation.user.queryDepartmentUsers()"></@queryfield>
					</#if>
					<@querybutton formId="department_queryForm" type="search" onclick="foundation.user.queryDepartmentUsers()" />
			 		<@querybutton formId="department_queryForm" type="clear"  />
			</@quickquery>
			</form>
     	</div>
     	<#assign superChecked = multiSelect>
     	<@datatable formId="department_queryForm" superChecked=superChecked firstLoad=false superCheckedName="staff.name" dtPage="page" hidekey="['id','staff.id','staff.code','staff.name']" dblclick=dblcallbackName  transMethod="post" id="department_userListTable" dataUrl="/msService/ec/foundation/user/common/getDepartmentUserList?a=-1" moreButtonResizeFlag=false pageInitMethod="hideAddToGroupDept">

			<@operatebar operates="code:base_addToUserDefinedGroup_dept||name:${getHtmlText('foundation.customGroup.userDefined.addToGroup')}||iconcls:add||onclick:foundation.user.addToUserDefinedGroup_dept()" operateType="noPower" resultType="json"/>
			<#if (Parameters.multiSelect)?string('true','false') == "true">
				<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
			</#if>
			<@datacolumn key="name" label="${getHtmlText('foundation.user.name')}" width=200 />
			<@datacolumn key="staff.name" label="${getHtmlText('foundation.staff.name')}" width=200 />
			<@datacolumn key="staff.mainPosition.department.name" label="${getHtmlText('foundation.department.name')}" width=150></@datacolumn>
			<@datacolumn key="staff.mainPosition.name" label="${getHtmlText('foundation.position.name')}" width=150></@datacolumn>
		</@datatable>
     </@frame>
</@frameset>

</body>
</html>
<script type="text/javascript" charset="utf-8" language="javascript">
$(function(){
	//注册命名空间
	CUI.ns("foundation.user");

	//点击部门树节点，该部门下的人员信息列表
	foundation.user.showDepartmentUsers = function(oNode){
		CUI("#inputValue input").val("");
		url = "/msService/ec/foundation/user/common/getDepartmentUserList?departmentId=" + oNode.id+"&companyId="+CUI("#currentDepartCompanyId").val();
		var pageSize=CUI('input[name="department_userListTable_PageLink_PageCount"]').val();
		var dataPost = "&pageSize="+encodeURIComponent(pageSize);
		datatable_department_userListTable.setRequestDataUrl(encodeURI(url), dataPost);
	};
	
	//切换公司
	foundation.user.changeCompanyForDepart=function(oSelect){
		setTimeout(function(){
			// 修改root节点
			
			CUI("#currentDepartCompanyId").val(oSelect.getAttribute("key"));
			chooseDepartmentUserListTree.getNodes()[0].name = oSelect.innerHTML;
			chooseDepartmentUserListTree.updateNode(chooseDepartmentUserListTree.getNodes()[0], true);
			var url = "/msService/ec/foundation/department/listChildren?companyId="+ oSelect.getAttribute("key");
			chooseDepartmentUserListTree.setting.async.url=url;
			chooseDepartmentUserListTree.reAsyncChildNodes(chooseDepartmentUserListTree.getNodes()[0], "refresh");
			if(<#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>closeLoadPanel){<#if (Parameters.openType)?default('page') == 'frame'>parent.</#if>closeLoadPanel(false);}
		},0);
	}
	/**
	 *用户助记码回调函数
	*/
	foundation.user.userInfoCallback=function(obj){
	    if(obj!=null){
	       CUI("#department_queryForm #userId").val(obj[0].id);
	    }
	}
	/**
	 *人员记码回调函数
	*/
	foundation.user.staffInfoCallback=function(obj){
	    if(obj!=null){
	       CUI("#department_queryForm #staffId").val(obj[0].id);
	    }
	}
	//查询
	foundation.user.queryDepartmentUsers = function(){
		chooseDepartmentUserListTree.cancelSelectedNode();
		var dataPost="";
			var url = "/msService/ec/foundation/user/common/getDepartmentUserList?companyId="+CUI("#currentDepartCompanyId").val();
	
			var userName =encodeURIComponent(CUI.trim(CUI("input[name='user.name']",CUI("#department_queryForm")).val())) ;
			var userId=encodeURIComponent(CUI.trim(CUI("input[id='user.id']",CUI("#department_queryForm")).val())) ;
			var staffName =encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",CUI("#department_queryForm")).val())) ;
			var staffId=encodeURIComponent(CUI.trim(CUI("input[id='staff.id']",CUI("#department_queryForm")).val())) ;
			if(userId!=''){
			     dataPost+="&user.id="+userId;
			}
			else{
				if(userName != ''){
					 dataPost += "&user.name=" + userName;
				}
			}
			if(staffId!=''){
			   dataPost+="&staff.id="+staffId;
			}
			else{
				if(staffName != ''){
					 dataPost += "&staff.name=" + staffName;
				}
			}
			var pageSize=CUI('input[name="department_userListTable_PageLink_PageCount"]').val();
			dataPost += "&pageSize="+encodeURIComponent(pageSize);	
			dataPost+="&"+department_queryForm_getCookieParam();
			<#if Parameters.showRange??>
				url += "&showRange=${Parameters.showRange!}";
			</#if>
			department_userListTableWidget.setRequestDataUrl(url,dataPost);
	
		return false;
	};
	
	foundation.user.addToUserDefinedGroup_saveHandler = function(){
		parent.parent.foundation_user_addToGroupFrame.CUI("#foundation_addToGroup_form").submit();
	}
	foundation.user.addToUserDefinedGroup_cancelHandler = function(){
		parent.parent[ 'foundation_user_addToGroupDialog' ].close();
	}
	
	foundation.user.addToUserDefinedGroup_dept = function(){
		var rows = datatable_department_userListTable.getSelectedRow();
		if(rows.length == 0){
			departmentUserListErrorbarWidget.showMessage("${getHtmlText('foundation.user.checkselected')}","f");
			return false;
		}
		var selectedStaffIds = '';
		var selectedStaffNames = '';
		for(var i=0; i<rows.length; i++){
			selectedStaffIds += (selectedStaffIds == '') ? rows[i].staff.id : (',' + rows[i].staff.id);
			selectedStaffNames += (selectedStaffNames == '') ? rows[i].staff.name : (',' + rows[i].staff.name);
		}
		//alert(selectedStaffIds);
		var url = "/foundation/user/userDefinedGroup/listGroups.action?groupType=CUSTOM&groupMemberIds=" + selectedStaffIds + '&addToGroupNs=user';
		var params = {};
		params['selectedStaffNames'] = selectedStaffNames;
		
		<#if (Parameters.openType)?default('page') == 'frame'>
			url += "&crossCompanyFlag=${(crossCompanyFlag!false)?string('true', 'false')}&openType=frame";
			if( parent.parent[ 'foundation_user_addToGroupDialog' ] ){
				parent.parent[ 'foundation_user_addToGroupDialog' ]._config.url = url;
				parent.parent[ 'foundation_user_addToGroupDialog' ].argument = params;
				parent.parent[ 'foundation_user_addToGroupDialog' ].show();
			}else{
			
				foundation.user.addToGroupDialog = parent.parent[ 'foundation_user_addToGroupDialog' ] = parent.parent.CUI.createDialog({
					title    :  "${getHtmlText('foundation.customGroup.userDefined.select')}",
					formId   :  'foundation_addToGroup_form',
					url      :  url,
					modal    :  true,
					
					iframe: 'foundation_user_addToGroupFrame',
					
					type   	 :  4,
					dragable :  true,
					argument :  params,
					buttons  :  [{
									name:"${getHtmlText('common.button.save')}",
								  	handler:parent.frameElement.getAttribute( 'name' ) + '.' + window.frameElement.getAttribute( 'name' ) + '.foundation.user.addToUserDefinedGroup_saveHandler()',
								 },
								 {
								 	name:"${getHtmlText('common.button.cancel')}",
								  	handler:parent.frameElement.getAttribute( 'name' ) + '.' + window.frameElement.getAttribute( 'name' ) + '.foundation.user.addToUserDefinedGroup_cancelHandler()',
								 }]
				});
				foundation.user.addToGroupDialog.show();
			}
		<#else>
			foundation.user.addToGroupDialog = new CUI.Dialog({
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
			foundation.user.addToGroupDialog.show();
		</#if>	
	}
	
	foundation.user.callBackUserDefinedGroup = function(res){
		if(res.dealSuccessFlag == true){	
			foundation_addToGroup_formDialogErrorBarWidget.show("${getHtmlText('foundation.common.saveandclosesuccessful')}","s");
			<#if (Parameters.openType)?default('page') == 'frame'>
				setTimeout(function(){
					parent.parent[ 'foundation_user_addToGroupDialog' ].close();
				},1000);
			<#else>
				setTimeout(function(){
					foundation.user.addToGroupDialog.close();
				},1000);
			</#if>
		}else{
			CUI.showErrorInfos(res, foundation_addToGroup_formDialogErrorBarWidget);
		}
	}
	
	foundation.user.showGroupMemberList = function(select){
		var sid = select.options[select.selectedIndex].value;
		if(sid > -1){
			datatable_userDefinedGroupMembers.setRequestDataUrl("/foundation/customGroup/listGroupMember.action?group.groupType=CUSTOM&id=" + sid + "&crossCompanyFlag=${(crossCompanyFlag!false)?string('true', 'false')}");
		}
	}
	
	hideAddToGroupDept = function(){
		<#if !(enableAddToGroup?? && enableAddToGroup)>
			$('#base_addToUserDefinedGroup_dept').parent('a').hide();
		</#if>
	}
});
</script>

<#if (Parameters.openType)?default('page') == 'frame'>
</body>
</html>
</#if>