<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<%@taglib prefix="cui" uri="/META-INF/orchid-cui-tags.tld"%>
<%@taglib prefix="s" uri="/META-INF/struts-tags.tld"%>
<%@taglib prefix="o" uri="/META-INF/orchid-foundation-tags.tld"%>
<style>
.padding20 {
	padding-left: 20px;
}
</style>
<cui:frameset id="ec_project_publish_container" border="0">
	<cui:frame id="ec_project_publish_container_main" region="center" offsetH="4">
		<form id="ec_project_publish_menu">
			<#--<input type="hidden" name="entityId" id="entityId" value="${entityId}" />-->
			<input type="hidden" name="entityCode" id="entityCode" value="${entity.code}" />
			<div align="center" style="width: 100%;">
				<table>
					<tr height="40px">
						<td>
							1.${getHtmlText('ec.MenuInfo.ChoiceView')}
						</td>
						<td class="padding20">
							<select id="viewCode" name="viewCode" style="width:178px;">
								<option value="">${getText('ec.MenuInfo.Choice')}</option>
								<#list views as view>
								<option value="${view.code!}">${view.name!}</option>
								</#list>
							</select>
						</td>
					</tr>
					<tr height="40px">
						<td>
						2.${getHtmlText('ec.MenuInfo.ChoiceDirectory')}
						</td>
						<td class="padding20">
							<select name="parentMenuId" id="parentMenuId" style="width:178px;">
								<option value="">${getText('ec.MenuInfo.Choice')}</option>
								<s:iterator value="menuList">
									<s:if test="!leaf">
									<option value="${id}">
									<s:if test="layNo > 1">
										<s:iterator begin="2" end="layNo" step="1">--</s:iterator>
									</s:if>
									${getText('${name}')}
									</option>
									</s:if>
								</s:iterator>
							</select>
						</td>
					</tr>
					<tr height="40px">
						<td>
							3.${getHtmlText('ec.MenuInfo.MenuName')}
						</td>
						<td class="padding20">
							<input type="text" name="menuName" class="cui-edit-field" />
						</td>
					</tr>
					<tr height="40px">
						<td>
							&nbsp;
						</td>
						<td class="padding20">
							<button type="button" class="Dialog_button" onclick="doPublishEcProject();">${getHtmlText('common.button.Confirm')}</button>
						</td>
					</tr>
				</table>
			</div>
			<div style="width:95%;padding: 90px 10px 0 10px;clear: both;float: none;">
				<div style="color: rgb(0, 106, 163); font-weight: bold; font-size: 13px;float: left;">${getHtmlText('ec.MenuInfo.published')}：</div>
				<div style="color: rgb(0, 106, 163); font-weight: bold; font-size: 13px;float: right;padding-right: 20px;cursor: pointer;" onclick="openMenuManage()"><a href="#">${getHtmlText('ec.MenuInfo.MenuManage')}</a></div>
			</div>
			<div style="width: 100%;padding: 10px 10px 0 10px;">
				<table cellspacing="0" align="center" width="98%" class="itemList">
					<thead>
						<tr>
							<td style="width: 30%;" class="title">${getHtmlText('ec.MenuInfo.Name')}</td>
							<td style="width: 70%;" class="title">${getHtmlText('ec.MenuInfo.Operate')}</td>
						</tr>
					</thead>
					<tbody>
						<s:iterator id="item" value="publishMenus" status="statusMe">
							<s:if test="menuOperates.size()!=0">
							<tr>
								<td align="center" valign="middle" rowspan="<s:property value="#item.menuOperates.size()" />">
									<s:if test="#item.id!=null">
										<label>${item.name }</label>
									</s:if>
								</td>
								<s:iterator id="operate" value="#item.menuOperates" status="statusOp">
									<s:if test="#statusOp.index!=0">
									<tr>
									</s:if>
										<td align="center">${operate.name }</td>
									</tr>
								</s:iterator>
							</s:if>
						</s:iterator>
					</tbody>
				</table>

			</div>
		</form>
	</cui:frame>
</cui:frameset>
<script type="text/javascript">
function doPublishEcProject(){
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
	if(checkMenuUnique()) {
		$.ajax({
			type : "POST",
			url : "/msService/ec/entity/public-menu",
			data : data,
			success : function(msg){
				if(msg && msg.success == true){
					createLoadPanel(false);
					CUI('#ecContentDiv').load('/msService/ec/entity/publish?entityCode=${entityCode}',null,function(){
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
}
// 一个列表只能发布一次
function checkMenuUnique(){
	var checkCodeUniqueFlag = true;
	CUI.ajax({
		url: '/foundation/menuInfo/checkCodeUnique',
		type: 'post',
		async: false,
		data: "menuInfoCode=" + (CUI("#entityCode").val()+"_"+CUI("#viewCode").val()),
		success: function(response) {
		 	if(response == false) {
		 		CUI.Dialog.alert("${getHtmlText('ec.MenuInfo.alert.view')}");
		 		checkCodeUniqueFlag = false;
			}
		}
	});
	return checkCodeUniqueFlag;
}
//打开主窗口
function openMenuManage(){
	var handle = null;

	try{
		var open_url= "/portal/portalredirect.jsp?serviceUrl=backslashfoundationbackslashmenuInfobackslashframe";

		var window_height = window.screen.availHeight;
		var window_width  = window.screen.availWidth;
		if(window.navigator.userAgent.indexOf('MSIE 8.0') > -1){
			window_height = window_height - 60;
			window_width = window_width - 10;
		}
		if(window.navigator.userAgent.indexOf('MSIE 7.0') > -1){
			window_height = window_height - 50;
			window_width = window_width - 12;
		}
		if(window.navigator.userAgent.indexOf('MSIE 6.0') > -1){
		      window_height = window_height-50;
		      window_width = window_width-12;
		    }
		ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,top=0,left=0,toolbar=no,menubar=no,location=no,status=yes";
		handle = window.open(open_url,"",ShowStyle);
	}catch(e){}

	if(handle != null){
		window.opener = null;
		window.open("","_blank");
	}else{
		window.location.href = "/loginredirect.jsp";//若弹出窗口被阻止了，则跳转到提醒页面
	}
	handle = null;
}
</script>