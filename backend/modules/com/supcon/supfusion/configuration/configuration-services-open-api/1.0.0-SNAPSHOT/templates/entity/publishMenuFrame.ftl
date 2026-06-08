<style>
.padding20 {
	padding-left: 20px;
}
table.itemList{
	background-color: #BFC7CA;
	font-size: 10pt; 
	margin-top:1; margin-bottom:0; margin-left:0 ;
	border-collapse: collapse;
	border:1px solid #79B1D0;
}
table.itemList thead tr {
   background-color: #C6E0EE ;
}
table.itemList td.title{						
	font-size: 12px;
	height:26px;
	color:#000;
	border:1px solid #5C8CB5;
	padding-left:5px;
    cursor: pointer;
    text-align: center;
    border-collapse:collapse;
}
table.itemList tbody td {
	color:#000;
	height:24px;
	padding-left:5px;
	border:1px solid #E2E2E2;
	padding:0;
	background-color: #ffffff;
}
</style>
<#macro menuTree children parent>
	<#if children?? && children?size gt 0>
		<#list children as m>
			<#assign n=(m.layRec?split("-"))?size - 1>
			<option value="${(m.id)!}"><#if n gt 0><#list 1..n as n>&nbsp;&nbsp;&nbsp;&nbsp;</#list></#if>${getText("${(m.name)!}")}</option>
			<@menuTree children=m.children parent=m />
		</#list>
	</#if>
</#macro>

<@frameset id="ec_project_publish_container">
	<@frame id="ec_project_publish_container_main" region="center" offsetH=4 style="overflow-y:auto;overflow-x:hidden;">
		<form id="ec_project_publish_menu">
			<#--<input type="hidden" name="entityId" id="entityId" value="${entityId}" />-->
			<input type="hidden" name="entityCode" id="entityCode" value="${entity.code}" />
			<div style="width:95%;margin:0 auto;">
				<table align="center" id="ec_project_publish_menu_select_list">
					<tr>
						<td>
							1.${getHtmlText('ec.MenuInfo.ChoiceView')}
						</td>
						<td class="padding20">
							<select id="viewCode" name="viewCode">
								<option value="">${getText('ec.MenuInfo.Choice')}</option>
								<#if views?exists>
									<#list views as v>
										<option value="${v.code}">${v.name}</option>
									</#list>
								</#if>
							</select>
						</td>
					</tr>
					<tr>
						<td>
							2.${getHtmlText('ec.MenuInfo.ChoiceDirectory')}
						</td>
						<td class="padding20">
							<select name="parentMenuId" id="parentMenuId">
								<option value="">${getText('ec.MenuInfo.Choice')}</option>
								<@menuTree children=menusTree.children parent=menusTree />
							</select>
						</td>
					</tr>
					<tr>
						<td>
							3.${getHtmlText('ec.MenuInfo.MenuName')}
						</td>
						<td class="padding20">
							<@international name="menuName" key="" moduleCode=module.artifact isNew=true maxLength=80></@international>
							<#--
							<@international name="menuName" cssClass="cui-edit-field" key="" moduleCode="${(module.artifact)!}"></@international>
							-->
						</td>
					</tr>
					<tr>
						<td>
							&nbsp;
						</td>
						<td class="padding20">
							<button class="btn-primary" type="button" onclick="doPublishEcProject();">${getHtmlText('common.button.Confirm')}</button>
						</td>
					</tr>
				</table>
			</div>
			<div style="width:95%;margin:0 auto;margin-top:20px;">
				<div id="ec_project_publish_menu_nav" style="width:100%;clear: both;float: none;">
					<div class="left_action" style="float: left;">${getHtmlText('ec.MenuInfo.published')}：</div>
					<div class="right_action" style="float: right;cursor: pointer;" onclick="openMenuManage()"><a href="#">${getHtmlText('ec.MenuInfo.MenuManage')}</a></div>
				</div>
				<table cellspacing="0" align="center" width="100%" class="itemList">
					<thead>
						<tr>
							<td style="width: 30%;" class="title">${getHtmlText('ec.MenuInfo.Name')}</td>
							<td style="width: 70%;" class="title">${getHtmlText('ec.MenuInfo.Operate')}</td>
						</tr>
					</thead>
					<tbody>
						<#list publishMenus as menu>
							<#assign mop =menu.menuOperates />
							<#if mop?size!=0>
							<tr>
								<td align="center" valign="middle" rowspan="${mop?size}">
									<label>${getText('${menu.name}')}</label>
								</td>
								<#list mop as op>
									<#if op_index!=0>
									<tr>
									</#if>
										<td align="center">${getText('${op.name!}')}</td>
									</tr>
								</#list>
							<#else>
								<tr>
									<td align="center" valign="middle">
										<label>${getText('${menu.name}')}</label>
									</td>
									<td align="center">${getText('ec.entity.wf.unpublished')}</td>
								</tr>
							</#if>
						</#list>
					</tbody>
				</table>

			</div>
		</form>
	</@frame>
</@frameset>
<script type="text/javascript">
$('#ec_project_publish_menu').on('keypress', 'input', function(e){
	if (e.keyCode == 13) {
		e.preventDefault();
		this.blur();
	} 
})
function doPublishEcProject(){
	CUI('#international_menuName_showName').val($.trim(CUI('#international_menuName_showName').val()));
	$("#international_menuName_showName").trigger("change");
	var data = $('#ec_project_publish_menu').serialize();
	if(CUI('#viewCode').val()=='') {
		CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.alert.ChoiceView')}");
		CUI('#viewCode').focus();
		return;
	}
	if(CUI('#parentMenuId').val()=='') {
		CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.ChoiceDirectory')}");
		CUI('#parentMenuId').focus();
		return;
	}
	if(CUI('#international_menuName_showName').val()=='') {
		CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.alert.MenuName')}");
		CUI('#international_menuName_showName').focus();
		return;
	}
	
	$.ajax({
		type : "POST",
		url : "/msService/ec/entity/public-menu",
		data : data,
		success : function(msg){
			if(msg && msg.success == true){
				
				CUI('#ecContentDiv').load('/msService/ec/entity/publishMenuFrame?entityCode=${entityCode}',null,function(){
					try {
						closeLoadPanel();
					}catch(e){}
				});
				CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.success')}");
			} else {
				CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.failure')}");
			}
		}
	});
	
}

//打开主窗口
function openMenuManage(){

	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	foundation.common.callOpenWeb("menuInfo",windowStyle,null,true,null,"&moduleArtifact=${(entity.module.artifact)!}","");
}
</script>