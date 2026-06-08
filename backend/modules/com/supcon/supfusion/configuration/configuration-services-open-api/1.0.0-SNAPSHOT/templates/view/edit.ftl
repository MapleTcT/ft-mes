<style type="text/css">
.t{background:blue;cursor:pointer;}
.t tr{background:#FFF;}
.selected{background:#f2f200;}
.layoutselect{display:none;}
.cui-search-click {right:5px!important}
</style>
<#macro menuTree children parent defaultvalue>
<#assign def=defaultvalue>
	<#if children?? && children?size gt 0>
		<#list children as m>
			<#assign n=(m.layRec?split("-"))?size - 1>
			<#if m.id != -1>
			<option value="${(m.id)!}" <#if m.id==def>selected="true"</#if>><#if n gt 0><#list 1..n as n>&nbsp;&nbsp;&nbsp;&nbsp;</#list></#if>${getText("${(m.name)!}")}</option>
			</#if>
			<@menuTree children=m.children parent=m defaultvalue=def/>
		</#list>
	</#if>
</#macro>
<@errorbar id="ec_view_edit_formDialogErrorBar" />
<form id="ec_view_edit_form" action="/msService/ec/view/save" method="post"namespace="/msService/ec/view" validate="true" callback="ec.view.callBack">
	<input type="hidden"  name="view.version" <#if view?? && view.version??>value="${view.version}"</#if> id="ec_view_edit_form_view_version" />
	<input type="hidden"  name="view.code" <#if view?? && view.code??>value="${view.code}"</#if>  id="ec_view_edit_form_view_code"/>
	<input type="hidden"  name="view.entity.code" <#if view?? && view.entity?? && view.entity.code??>value="${view.entity.code}"</#if> id="ec_view_edit_form_view_entity_code"/>
	<input type="hidden"  name="view.moduleCode" <#if view?? && view.moduleCode?? >value="${view.moduleCode}"</#if> id="ec_view_edit_form_view_moduleCode"/>
	<input type="hidden"  name="view.editViewType" <#if view?? && view.editViewType?? >value="${view.editViewType}"</#if> id="ec_view_edit_form_view_editViewType"/>
	<#if (isProj)?exists && (isProj)>
		<#if view?exists && (view.isControl)== true>
			<input type="hidden"  name="view.urlCustom" value="" />
			<input type="hidden"  name="view.mainView"  <#if view?? && view.mainView??>value="${view.mainView}"</#if> />
			<input type="hidden"  name="view.openType" <#if view?? && view.openType??>value="${view.openType}"</#if>  />
			<input type="hidden"  name="view.showType" <#if view?? && view.showType??>value="${view.showType}"</#if>  />
			<input type="hidden"  name="view.layoutId" value="" />
			<input type="hidden"  name="view.assModel.code" <#if view?? && view.assModel??>value="${view.assModel.code}"</#if> />
		</#if>
	</#if>
	<input type="hidden"  name="view.inheritType"  <#if view?? && view.inheritType??>value="${view.inheritType}"</#if> />
	<input type="hidden" name="view.usedForWorkFlow" id="ec_view_edit_form_view_usedForWorkFlow"  <#if view?exists&&view.usedForWorkFlow?exists&&view.usedForWorkFlow== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.onlyForQuery" id="ec_view_edit_form_view_onlyForQuery" <#if view?exists&&view.onlyForQuery?exists&&view.onlyForQuery== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.customFlag" id="ec_view_edit_form_view_customFlag" <#if view?exists&&view.customFlag?exists&&view.customFlag== true>value="true"<#else>value="false"</#if>  />
	<input type="hidden" name="view.mainView" id="ec_view_edit_form_view_mainView" <#if view?exists&&view.mainView?exists&&view.mainView== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.attachmentFlag" id="ec_view_edit_form_view_attachmentFlag" <#if view?exists&&view.attachmentFlag?exists&&view.attachmentFlag== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.mainRef" id="ec_view_edit_form_view_mainRef" <#if view?exists&&view.mainRef?exists&&view.mainRef== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.hasAttachment" id="ec_view_edit_form_view_hasAttachment" <#if view?exists&&view.hasAttachment?exists&&view.hasAttachment== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.dealInfoShow" id="ec_view_edit_form_view_dealInfoShow" <#if view?exists&&view.dealInfoShow?exists&&view.dealInfoShow== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.isControl" id="ec_view_edit_form_view_isControl" <#if view?exists&&view.isControl?exists&&view.isControl== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.importFlag" id="ec_view_edit_form_view_importFlag"  <#if view?exists&&view.importFlag?exists&&view.importFlag== true>value="true"<#else>value="false"</#if> />
	<input type="hidden" name="view.retrialFlag" id="ec_view_edit_form_view_retrialFlag"  <#if view?exists&&view.retrialFlag?exists&&view.retrialFlag== true>value="true"<#else>value="false"</#if>  />
	<input type="hidden"  name="view.isShadow" id="ec_view_edit_form_view_isShadow" <#if view?exists&&view.isShadow?exists&&view.isShadow== true>value="true"<#else>value="false"</#if>  />
	<input type="hidden"  name="view.includeChildren" id="ec_view_edit_form_view_includeChildren" <#if view?exists&&view.includeChildren?exists&&view.includeChildren== true>value="true"<#else>value="false"</#if>  />
	<input type="hidden"  name="view.usedForTree" id="ec_view_edit_form_view_usedForTree" <#if view?exists&&view.usedForTree?exists&&view.usedForTree== true>value="true"<#else>value="false"</#if> />
	<input type="hidden"  name="view.isPrint" id="ec_view_edit_form_view_isPrint" <#if view?exists&&view.isPrint?exists&&view.isPrint== true>value="true"<#else>value="false"</#if> />
	<input type="hidden"  name="view.isAudit" id="ec_view_edit_form_view_isAudit" <#if view?exists&&view.isAudit?exists&&view.isAudit== true>value="true"<#else>value="false"</#if>  />
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la supplant-required-field" style="width:20%;">${getHtmlText('ec.view.name')}</td>
			<td class="co" style="width:30%">
			<#if view?? && (view.name)?? && (view.name)!=''>
			<input type="text" style="width: 100%" name="view.name" readonly="true" cssClass="cui-noborder-input" <#if view?? && view.name??>value="${view.name}" readonly="true"</#if>/>
			<#else>
			<input type="text" style="width: 100%" name="view.name" cssClass="cui-noborder-input" value=""/>
			</#if>
			</td>
			<td class="la supplant-required-field" style="width:20%;">${getHtmlText('ec.view.title')} </td>
			<td class="co" style="width:30%">
				<@international name="view.title" key="${(view.title)!}" moduleCode=(entity.module.artifact)! isNew=true maxLength=80></@international>
			</td>
		</tr>
		<tr>
			<td class="la">${getHtmlText('ec.view.type')}</td>
			<td class="co">
			<#if (view.code)??>
				<#list viewTypes?keys as k>
					<#if view?? &&  view.type = k>
						<input type="hidden" name="view.type" value="${k}">
						<input type="text" value="${getText('${(viewTypes[k])!}')}" class="cui-edit-field" readonly=true>
					</#if>
				</#list>
			<#else>
				<select id="view_type" name="view.type" onchange="ec.view.isShadow()" style="width:100%">
				<#list viewTypes?keys as k>
					<#if  !isProj||(k!='DIGEST'&&k!='MNECODE')>
					<option value="${k}"
					<#if  view?? &&  view.type = k>
							selected
						</#if>
					>${getText('${(viewTypes[k])!}')}</option>
					</#if>
				</#list>
				</select>
			</#if>
			</td>
			<td class="la">${getHtmlText('ec.view.displayName')} </td>
			<td class="co">
				<@international name="view.displayName" key="${(view.displayName)!}" moduleCode=(entity.module.artifact)! isNew=true maxLength=80></@international>
			</td>
			<#--
			<td class="la" name="view_isControl_td">${getHtmlText('ec.entity.getIsControl')}</td>
			<td class="co" name="view_isControl_td">
				<input onclick="javascript:ec.view.isControl(this);" type="checkbox" <#if view?exists&&view.isControl?exists&&view.isControl == true>checked="checked"</#if> />
			<td>
			-->
		</tr>
		<tr id="view_workflow_tr">
			<#if !isProj?? || (isProj?? && !isProj)>
			<td class="la" name="view_usedForWorkFlow">${getHtmlText('ec.view.usedForWorkFlow')}</td>
			<td class="co" name="view_usedForWorkFlow">
				<input id="ck_workflow" onclick="javascript:ec.view.usedForWorkFlow(this);" type="checkbox" <#if isProj || (view?exists&&view.inheritType??)>disabled="true"</#if> <#if view?exists&&view.usedForWorkFlow== true>checked="checked" </#if> value="true" />
			</td>
			</#if>
			<#if entity?? && entity.workflowEnabled>
			<td class="la" name="view_onlyForQuery">${getHtmlText('ec.view.onlyForQuery')}</td>
			<td class="co" name="view_onlyForQuery">
				<input id="ck_onlyForQuery" onclick="javascript:ec.view.onlyForQuery(this);" type="checkbox" <#if view?? && view.onlyForQuery?? && view.onlyForQuery== true>checked="checked"</#if> value="true" />
			</td>
			</#if>
		</tr>
		<#if !isProj?? || (isProj?? && !isProj)>
		<tr id="workflowTd_view_mainview">
			<td class="la" name="view_mainView">${getHtmlText('ec.view.mainview')}</td>
			<td class="co" name="view_mainView">
			<input onclick="javascript:ec.view.mainView(this);" type="checkbox" <#if isProj || (view?exists&&view.inheritType??)>disabled="true"</#if> <#if view?exists&&view.mainView?exists&&view.mainView == true>checked="checked" disabled="disabled"</#if> value="true" />
			</td>
			<td class="la" name="attachment_flag">${getHtmlText('ec.view.attachmentFlag')}</td>
			<td class="co" name="attachment_flag">
			<input name="attachmentFlag" onclick="javascript:ec.view.attachmentFlag(this);" type="checkbox" <#if view?exists&&view.attachmentFlag?exists&&view.attachmentFlag == true>checked="checked" </#if> value="true" />
			<span id="attachmentFlagHelpinfo" class="baphelp-icon"></span>
			<div id="attachmentFlagHelpinforef" style="display:none">
				<p class="baphelp-info">仅当视图展现模式为扩展模式为有效。</p>
			</div>					
			<script type="text/javascript">
				$('#attachmentFlagHelpinfo').helptip({refElm:"#attachmentFlagHelpinforef", html: true , isCustom :false, width: 330 , title :"说明"});
			</script>
			</td>
		</tr>
		</#if>
		<tr id="workflowTd_view_mainref">
			<td class="la" name="view_mainRef">${getHtmlText('ec.view.mainRef')}</td>
			<td class="co" name="view_mainRef">
				<input onclick="javascript:ec.view.mainRef(this);" type="checkbox" <#if isProj || (view?exists&&view.inheritType??)>disabled="true"</#if>  <#if view?exists&&view.mainRef?exists&&view.mainRef == true>checked="checked" disabled="disabled"</#if> value="true" />
			</td>
		</tr>
		<tr id="workflowTd_view_dealInfoShow">
			<td class="la" name="view_dealInfoShow">${getHtmlText('ec.view.dealinfo')}</td>
			<td class="co" name="view_dealInfoShow">
				<input onclick="javascript:ec.view.dealInfoShow(this);" type="checkbox" <#if view?exists&&view.dealInfoShow?exists&&view.dealInfoShow == true>checked="checked"</#if> value="true" />
				<span id="dealInfoHelpinfo" class="baphelp-icon"></span>
				<div id="dealInfoHelpinforef" style="display:none">
					<p class="baphelp-info">仅当视图展现模式为扩展模式为有效。</p>
				</div>					
				<script type="text/javascript">
					$('#dealInfoHelpinfo').helptip({refElm:"#dealInfoHelpinforef", html: true , isCustom :false, width: 330 , title :"说明"});
				</script>
			</td>
			<td class="la" name="view_enableSimpleDealInfo">${getHtmlText('ec.view.enableSimpleDealInfo')}</td>
			<td class="co" name="view_enableSimpleDealInfo">
				<input type="hidden" name="view.enableSimpleDealInfo" value="<#if (view.enableSimpleDealInfo)?? && view.enableSimpleDealInfo>true<#else>false</#if>"/>
				<input type="checkbox" id="enableSimpleDealInfo_cbx" onclick="ec.view.enableSimpleDealInfo()" <#if (view.enableSimpleDealInfo)?? && view.enableSimpleDealInfo>checked="checked"</#if> />
			</td>
		</tr>
		<tr id="workflowTd_view_dealInfoGroup">
			<td class="la" name="view_dealInfoGroup">${getHtmlText('ec.view.dealInfoGroup')}</td>
			<td class="co" name="view_dealInfoGroup">
				<select name="view.dealInfoGroup" style="width:100%">
					<option value="byTime" <#if view?? && view.dealInfoGroup?? && view.dealInfoGroup = 'byTime'>selected</#if>>${getText('ec.view.dealInfoByTime')}</option>
					<option value="byTask" <#if (view?? && view.dealInfoGroup?? && view.dealInfoGroup = 'byTask') || ((view.enableSimpleDealInfo)?? && view.enableSimpleDealInfo)>selected</#if>>${getText('ec.view.dealInfoByTask')}</option>
				</select>
			</td>
		</tr>
		<tr id="view_customView_tr">
			<#if !isProj?? || (isProj?? && !isProj)>
			<td class="la" name="view_customView">${getHtmlText('ec.view.customView')}</td>
			<td class="co" name="view_customView">
			<input id="ck_customView" onclick="javascript:ec.view.urlCustom(this);" type="checkbox"   <#if isProj> disabled="true"</#if> <#if view?exists&&view.customFlag?exists&&view.customFlag == true>checked="checked"</#if> value="true" />
			</td>
			</#if>
			<td class="la" name="view_hasAttachment"><label id="att">${getHtmlText('ec.view.attachment')}</label><label id="attShow" style="display:none">${getHtmlText('ec.view.showAttachment')}</label></td>
			<td class="co" name="view_hasAttachment">
				<input onclick="javascript:ec.view.hasAttachment(this);" id="view_attachment" type="checkbox" <#if view?exists&&view.hasAttachment?exists&&view.hasAttachment == true>checked="checked"</#if> value="true" />
			</td>
		</tr>
		<tr id="customUrlTr" style="display:none;">
			<td class="la">${getHtmlText('ec.view.existurl')}</td>
			<td class="co" colspan="3">
				<input type="text" <#if view?? && view.url??>value="${view.url}" </#if> name="view.url" id="custom_view_url" cssClass="cui-edit-field" cssStyle="width:98%;"/>
			</td>
		</tr>
		<tr id="retrial" style="display:none;">
			<td class="la">${getHtmlText('ec.view.retrial')}</td>
			<td class="co" >
				<input id="view_retrial" type="checkbox" onclick="ec.view.retrialFlag(this);" <#if view?exists&&view.retrialFlag?exists&&view.retrialFlag == true>checked="checked"</#if> value="true"/></td>
			<td class="la" name="view_scriptCode">${getHtmlText('js.ec.script.manager.code')}</td>
			<td class="co" name="view_scriptCode">
			<@selector name="view.scriptCode" id="scriptCode" cssClass="cui-edit-field" value="${(view.scriptCode)!}"
				openType="page" closePage="true" pageType="script" onclick="ec.view.scriptCode" clearFunc="ec.view.clearScriptCode()" />
			</td>
		</tr>
		<#if !isProj?? || (isProj?? && !isProj)>
		<tr id="view_shadow_tr">
			<td class="la">${getHtmlText('ec.view.isshadow')}</td>
			<td class="co">
			<input onclick="javascript:ec.view.isShadow(this);" type="checkbox" id="view_isShadow" <#if view?exists&&view.inheritType??>disabled="true"</#if> <#if view?? && view.isShadow?? && view.isShadow>checked="checked"</#if>/>
			</td>
			<td class="la shadow-td" style="display:none;color:rgb(179, 3, 3);">${getHtmlText('ec.view.shadow')}</td>
			<td class="co shadow-td" style="display:none;">
				<select id="shadowViews" name="view.shadowView.code" style="width:100%"></select>
			</td>
		</tr>
		</#if>
		<tr id="view_openType_tr">
			<td class="la" name="view_openType_td">${getHtmlText('ec.view.openType')}</td>
			<td class="co" name="view_openType_td">
				<select name="view.openType" style="width:100%">
					<option value="frame"<#if  view ?? &&  view.openType = 'frame'>selected</#if>>${getText('ec.view.openpage')}</option>
					<option value="dialog"<#if  view ?? &&  view.openType = 'dialog'>selected</#if>>${getText('ec.view.dialog')}</option>
				</select>
			</td>
			<td class="la" name="view_dialogType_td">${getHtmlText('ec.view.dialogType')}</td>
			<td class="co" name="view_dialogType_td">
				<select name="view.dialogType" style="width:100%">
				</select>
			</td>
		</tr>
		<tr id="view_viewType_tr"<#if entity?? && (entity.module)?? && (entity.module.type)?? && (entity.module.type) == 'Mis'> style="display:none" </#if>>
			<td class="la" name="view_editViewType_td">${getHtmlText('ec.view.isExtra')}</td>
			<td class="ca" name="view_editViewType_td">
				<input type="checkbox" id="view_editViewType" valid=1 <#if view?? && view.type?? && (view.type == 'EDIT' || view.type == 'VIEW') >disabled=disabled <#if view.editViewType?? && view.editViewType==1 >checked="checked"<#else></#if><#else> checked="checked" </#if>  onclick="ec.view.editViewType();" />
			</td>
			<td></td>
			<td></td>
		</tr>
		<tr id="view_width_height">
			<td class="la">${getHtmlText('ec.view.width')}</td>
			<td class="co"><input type="text" name="view.width" <#if view?? && view.width??>value="${view.width}" </#if> cssClass="cui-edit-field" style="width:98%;"/></td>
			<td class="la">${getHtmlText('ec.view.height')}</td>
			<td class="co"><input type="text" name="view.height" <#if view?? && view.height??>value="${view.height}" </#if> cssClass="cui-edit-field" style="width:98%;"/></td>
		</tr>
		<tr id="view_pagetype">
			<td class="la">${getHtmlText('ec.view.pagetype')}</td>
			<td class="co">
				<select name="view.showType" onChange="ec.view.viewShowTypeChanged();" style="width:50%" <#if view?? && view.code??>disabled=true</#if>>
					<option value="SINGLE" <#if view?? && view.showType! == 'SINGLE'>selected</#if>>${getText('ec.view.single')}</option>
					<option value="PART" <#if view?? && view.showType! == 'PART'>selected</#if>>${getText('ec.view.part')}</option>
					<#if view?? && view.code??>
					<option value="LAYOUT" <#if view?? && view.showType! == 'LAYOUT'>selected</#if>>${getText('ec.view.layout')}</option>
					</#if>
					<option value="LAYOUT2" <#if view?? && view.showType! == 'LAYOUT2'>selected</#if>>${getText('ec.view.layout2')}</option>
				</select>
				<#if view?? && view.code??><input type="hidden" name="view.showType" value="${(view.showType)!}"></#if>
			</td>
			<td id="view_layout_id_1" class="la layoutselect">${getHtmlText('ec.view.choicelayout')}</td>
			<td id="view_layout_id_2" class="co layoutselect">
				<select name="view.layoutCode">
					<option value="2" selected=selected>2</option><#--新布局只能选左右两列-->
				</select>
			</td>
		</tr>
		<tr id="treeSelectTr" style="display:none;">
			<td class="la">${getHtmlText('ec.view.istreeassociate')}</td>
			<td class="co">
				<input onclick="javascript:ec.view.usedForTree(this);" type="checkbox" id="view_usedForTree" <#if view??&&view.usedForTree??&&view.usedForTree>checked="checked"</#if>/>
			</td>
			<td class="la includeChildrenTr">${getHtmlText('ec.view.includeChildren')}</td>
			<td class="co includeChildrenTr">
				<input onclick="javascript:ec.view.includeChildren(this);" type="checkbox" id="view_includeChildren" <#if view??&&view.includeChildren??&&view.includeChildren>checked="checked"</#if>/>
			</td>
		</tr>
		<tr id="treeassociateTr" style="display:none;">
			<td class="la">${getHtmlText('ec.view.treeassociate')}</td>
			<td class="co" colspan="3">
			<div class="fix-search-click">
				<input type="hidden" name="view.assTreeModelCode" value="${(view.assTreeModelCode)!}">
				<input type="hidden" name="view.assTreeLayRec" value="${(view.assTreeLayRec)!}">
				<input type="text" class="cui-readonly-field" style="width:99%;" readonly=true name="view.assTreePath" value="${(view.assTreePath)!}"  />
				<input type="button" onclick="ec.view.showAssTreeModel();" title="${getText('ec.view.choiceTreeModel')}" value="" class="cui-search-click">
			</div>
			</td>
		</tr>
		<#if entity?exists&&entity.workflowEnabled?exists&&entity.workflowEnabled == true>
		<tr id="view_assview">
			<td class="la" name="view_isassview_td">${getHtmlText('ec.view.isAssView')}</td>
			<td class="co" name="view_isassview_td">
				<input onclick="javascript:ec.view.isAssView(this);" type="checkbox" name=assViewFlag id="view_isAssView" <#if  view ?? && view.assView ??>checked="checked"</#if>/>
			</td>
			<td class="la" style="display:none;" name="view_assview_td">${getHtmlText('ec.view.assView')}</td>
			<td class="co" style="display:none;" name="view_assview_td">
				<select name="view.assView.code"  style="width:100%">
					<#list assViews as av>
						<option value="${av.code}" 
						<#if  view ?? && view.assView ??&& view.assView.code = av.code>
							selected
						</#if>
						>
						${getText('${av.name}')}
						</option>
					</#list>
				</select>
			</td>
		</tr>
		</#if>
		<tr id="view_datagrid_type">
			<td class="la supplant-required-field" name="view_addModel_td">${getHtmlText('ec.view.assModel')}</td>
			<td class="co" name="view_addModel_td">
			    <#if view?exists&&view.inheritType??><input type="hidden"  name="view.assModel.code" value="${view.assModel.code}" /></#if>
				<select name="view.assModel.code" onchange="ec.view.assModelChange()" style="width:100%" <#if view?exists&&view.inheritType??>disabled="true"</#if>>
					<#list assModels as am>
						<option value="${am.code}" entityUseForFlow="${((am.entity.workflowEnabled)!false)?string}" isMainModel="${(am.isMain!false)?string}" dataType="${(am.dataType)?string}" <#if am.type??>modelType="${(am.type)?string}"<#else>modelType="1"</#if>
						<#if  view ?? &&  view.assModel.code = am.code>
							selected
						</#if>
						<#if  !(view ??) &&  am.isMain>
							selected
						</#if>
						>
						${getText('${am.name}')}
						</option>
					</#list>
				</select>
			</td>
			<!--<td class="la" name="view_datagrid">${getHtmlText('ec.view.datagridType')}</td>
			<td class="co" name="view_datagrid">
				<select name="view.dataGridType" style="width:100%">
					<option value="0" <#if view?? && view.dataGridType?? && view.dataGridType==0>selected="selected"</#if>>${getText('ec.model.dataType.1')}</option>
					<option value="1" <#if view?? && view.dataGridType?? && view.dataGridType==1>selected="selected"</#if>>${getText('ec.view.support')}</option>
				</select>
			</td>-->
		</tr>
		<!-- 默认提供
		<tr id="view_audit" style="display:none;">
			<td class="la">${getText('ec.view.audit')}</td>
			<td class="co"><input type="checkbox" id="view_isAudit" <#if (view??&&view.isAudit??&&view.isAudit) && (entity?exists&&entity.enableAudit == true)>checked="checked"</#if> <#if entity?exists&&entity.enableAudit == false>disabled=true</#if> onclick="ec.view.allowAudit(this)"></td>
		</tr> -->
		<tr id="canzaotr" style="display:none;">
			<td class="la">${getHtmlText('ec.view.isreference')}</td>
			<td class="co"><input type="hidden" name="view.isReference" <#if view?? && view.isReference??>value="${view.isReference?c}" </#if> /><input type="checkbox" id="view_isReference" onclick="ec.view.allowRef(this)" <#if view??&&view.isReference??&&view.isReference>checked="checked"</#if>></td>
			
			<td id="view_layout_id_12" class="la layoutselect">${getHtmlText('ec.view.property.refview')}</td>
			<td id="view_layout_id_22" class="co layoutselect">
				<select id="refView" name="view.reference.code" style="width:100%">
					<#list refviews as refview>
						<option value="${refview.code}" <#if view?? && view.reference ?? && view.reference.code?? && view.reference.code == (refview.code)>selected=selected</#if>>${refview.name}</option>
					</#list>
				</select>
			</td>
		</tr>
		<tr id="view_print" style="display:none;">
			<#if entity?exists&&entity.isBase?exists&&entity.isBase == true>
				<td class="la">${getText('ec.view.closePageAfterSave')}</td>
				<td class="co">
					<input type="hidden" id="view_closePageAfterSave" name="view.closePageAfterSave" value="<#if view??&&view.closePageAfterSave??&&view.closePageAfterSave>${(view.closePageAfterSave!)?string}</#if>">
					<input type="checkbox" <#if view??&&view.closePageAfterSave??&&view.closePageAfterSave>checked="checked"</#if> onclick="javascript:$('#view_closePageAfterSave').val($(this).prop('checked'));">
				</td>
			<#else>
				<td class="la" style="display:none">${getText('ec.view.closePageAfterSave')}</td>
				<td class="co" style="display:none">
					<input type="hidden" id="view_closePageAfterSave" name="view.closePageAfterSave" value="<#if view??&&view.closePageAfterSave??&&view.closePageAfterSave>${(view.closePageAfterSave!)?string}</#if>">
					<input type="checkbox" <#if view??&&view.closePageAfterSave??&&view.closePageAfterSave>checked="checked"</#if> onclick="javascript:$('#view_closePageAfterSave').val($(this).prop('checked'));">
				</td>
			</#if>
			<td class="la">${getText('ec.view.print')}</td>
			<td class="co"><input type="checkbox" id="view_isPrint" <#if view??&&view.isPrint??&&view.isPrint>checked="checked"</#if> onclick="ec.view.allowPrint(this)"></td>
		</tr>
		<tr id="view_control_print" style="display:none;">
			<td class="la">${getText('ec.view.controlPrint')}</td>
			<td class="co">
				<input type="hidden" id="view_controlPrint" name="view.controlPrint" value="<#if view??&&view.controlPrint??&&view.controlPrint>true<#else>false</#if>">
				<input type="checkbox" id="view_controlPrint_cbx" <#if view??&&view.controlPrint??&&view.controlPrint>checked="checked"</#if> onclick="ec.view.controlPrint();">
			</td>
			<td class="la controlName" style="<#if view??&&view.controlPrint??&&view.controlPrint><#else>display:none;</#if>">${getText('ec.print.control.name')}</td>
			<td class="co controlName" style="<#if view??&&view.controlPrint??&&view.controlPrint><#else>display:none;</#if>">
				<@international name="view.controlName" key="${(view.controlName)!}" moduleCode=(entity.module.artifact)! isNew=true maxLength=80></@international>
			</td>
		</tr>
		<tr id="view_control_print_set_name" style="display:none;">
			<td class="la">${getText('ec.print.controlset.name')}</td>
			<td class="co">
				<@international name="view.controlSetingName" key=(view.controlSetingName)! moduleCode=(entity.module.artifact)! isNew=true maxLength=80></@international>
				<#--
				<@international name="view.controlSetingName" moduleCode="${(entity.module.artifact)!}" key="${(view.controlSetingName)!}" cssClass="cui-edit-field"></@international>
				-->
			</td>
		</tr>
		<tr id="view_batch_control_print" style="display:none;">
			<td class="la">${getText('ec.view.batchControlPrint')}</td>
			<td class="co">
				<input type="hidden" id="view_batchControlPrint" name="view.isBatchControlPrint" value="<#if (view.isBatchControlPrint)?? && view.isBatchControlPrint>true<#else>false</#if>"/>
				<input type="checkbox" id="batchControlPrint_cbx" onclick="ec.view.batchControlPrint();" <#if (view.isBatchControlPrint)?? && view.isBatchControlPrint>checked="checked"</#if>/>
			</td>
			<td class="la batchControlPrintViews" style="display:none;">${getText('ec.view.viewSelect')}</td>
			<td class="co batchControlPrintViews" style="display:none;">
				<select id="batchControlPrint_selectViews" name="view.batchControlPrintSelectView.code" style="width:100%;"></select>
 			</td>
		</tr>
		<tr id="view_permission_tr" style="display:none;">
			<td class="la" style="width:20%">${getText('ec.view.ref.permission')}</td>
			<td class="co" style="width:30%">
				<input type="hidden" id="view_isNew" name="view.isNew" value="<#if view??>false<#else>true</#if>"/>
				<input type="hidden" id="view_isPermission" name="view.isPermission" value="<#if view??&&view.isPermission??&&view.isPermission>true<#else>false</#if>"/>
				<input type="checkbox" id="isPermission_cbx" onclick="ec.view.isPermission();" <#if view??&&view.isPermission??&&view.isPermission>checked="checked"</#if>/>
			</td>
		</tr>
		<tr id="view_permission_tr2" style="display:none;">
			<td class="la permissionCode" style="width:20%;">${getText('ec.view.ref.permission.code')}</td>
			<td class="co permissionCode" style="width:30%;">
				<input type="text" <#if view?? && view.permissionCode??>value="${view.permissionCode}" </#if>  name="view.permissionCode" cssClass="cui-edit-field" style="width:100%;" onblur="ec.view.getOperateInfo();"/>
 			</td>
			<td class="la">${getText('ec.view.ref.operate.name')}</td>
			<td class="co">
				<@international name="view.refOperateName" moduleCode="${(entity.module.artifact)!}" key="${(view.refOperateName)!}" isNew=true cssClass="cui-edit-field"></@international>
			</td>
		</tr>
		<tr id="view_permission_tr1" style="display:none;">
			<td class="la">${getHtmlText('ec.view.ref.operate.url')}</td>
			<td class="co" colspan="3">
				<input type="text" name="view.operateUrl" <#if view?? && view.operateUrl??>value="${view.operateUrl}" </#if> cssClass="cui-edit-field"/>
			</td>
		</tr>
		<#if isProj&&(!view??||!(view.inheritType)??)>
		<tr id="view_menuinfo_tr" style="display:none;">
			<td class="la" style="width:20%">${getText('ec.MenuInfo.ChoiceDirectory')}</td>
			<td class="co" style="width:30%">
				<select name="view.parentMenuId" id="parentMenuId" style="width:178px;">
					<option value="-1">${getText('ec.MenuInfo.Choice')}</option>
					<#if view??>
						<@menuTree children=menusTree.children parent=menusTree defaultvalue=(view.parentMenuId!-1)/>
					<#else>
						<@menuTree children=menusTree.children parent=menusTree defaultvalue=-1/>
					</#if>
				</select>
			</td>
			<td style="width:20%;" class="la">${getText('ec.MenuInfo.Name')}</td>
			<td style="width:30%;" class="co">
				<@international name="view.menuName" key="${(view.menuName)!}" moduleCode="${(entity.module.artifact)!}" isNew=true maxLength=80></@international>
 			</td>
		</tr>
		</#if>
		<#if (view.code)??>
		<tr id="view_url">
			<td class="la">URL</td>
			<td class="co" colspan="3"><input type="text"  id="readonly_view_url" value="${view.url!}" readonly="true" cssClass="cui-edit-field" cssStyle="width:98%;"/></td>
		</tr>
		</#if>
		<#--
		<#if (view.type)??&&view.type=="LIST">
		<tr>
			<td class="la">${getHtmlText('ec.view.forImplort')}</td>
			<td class="co" colspan="3">	<input type="checkbox" id="importFlag"   onclick="ec.view.importFlag(this)"   <#if view?exists&&view.importFlag?exists&&view.importFlag == true>checked="checked"</#if> /></td>
		</tr>
		</#if>
		-->
		<tr>
			<td class="la">${getHtmlText('ec.view.description')}</td>
			<td class="co" colspan="3">
			<textarea name="view.description" cols="" rows=""  id="ec_view_edit_form_view_description" class="cui-edit-textarea" style="width:100%;height:60px"><#if view?? && view.description??>${view.description} </#if></textarea>
			</td>
		</tr>
		
	</table>
</form>
<script type="text/javascript">
	//注册命名空间
	CUI.ns("ec.view");
	
	// zentao118091, 单独实现清空脚本
	ec.view.clearScriptCode = function() {
		$('[name="view.scriptCode"]').val('');
	}

	ec.view.controlPrint = function(){
		$('#view_controlPrint').val($("#view_controlPrint_cbx").prop('checked'));
		if($("#view_controlPrint_cbx").prop('checked')){
			$(".controlName").show();
			$("#view_control_print_set_name").hide();
		} else {
			$(".controlName").hide();
			$("#view_control_print_set_name").hide();
		}
	}
	
	ec.view.editViewType = function(){
		if($("#view_editViewType").prop('checked')){
			//$('#view_pagetype').hide();
			$("[name='view.showType']").val("SINGLE");
			$("[name='view.showType']").trigger("change");
			$("[name='view.editViewType']").val(1);
			$('#view_print td:eq(2)').hide(); //增强型视图不支持页面打印
			$('#view_print td:eq(3)').hide();
		} else {
			//$('#view_pagetype').show();
			$("[name='view.editViewType']").val(0);
			$('#view_print td:eq(2)').show();
			$('#view_print td:eq(3)').show();
		}
	}
	
	ec.view.getOperateInfo = function(){
		CUI.ajax({
			url: "/msService/ec/view/getOperateInfo?isProj=${isProj?string}",
			type: 'post',
			async: false,
			data: {"view.entity.code":$("*[name='view.entity.code']").val(),
					"view.permissionCode":$("*[name='view.permissionCode']").val()
				  },
			success: function(res){
				if(undefined != res && null != res && res != ""){
					if(res.dealSuccess){
						var opDisplayName = res.displayName;
						var opUrl = res.url;
						var opName = res.name;
						$("input[name='view.operateUrl']").val(opUrl);
						$("#international_viewrefOperateName").val(opDisplayName);
						$("#international_viewrefOperateName_showName").val(opName);
					}
				}
		    }
		});
	}
	
	 /* 编码规则验证
	 * @method ec.view.nameValidate
	 * @public
	 */
	ec.view.nameValidate=function(){
	 	var validate=/^[a-z]{1}[a-zA-Z0-9_]{2,19}$/;
	 	var obj = $("#ec_view_edit_form input[name='view.name']").val();
	 	if(null != obj && obj.endsWith("__mobile__")){
	 		ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('视图编码不能以__mobile__结尾')}");
	 		return false;
	 	}
		var assModelcode = CUI('*[name="view.assModel.code"]').val();
		if(assModelcode ==''){
 			ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('关联模型不允许为空,请先建立模型。')}");
			return false;
		}
	 	$("#ec_view_edit_form input[name='view.name']").val(obj.trim());
		if (validate.test($("#ec_view_edit_form input[name='view.name']").val())){
			if($('[name="view.title"]').val() == null || $('[name="view.title"]').val() == '') {
                ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.title.required')}");
                return false
            } else {
                return true;
            }
		}else{
		    ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.formatmessage')}");
			return false;
		}
		
	}
	
	ec.view.requiredVerification=function(){
		if($('#view_isReference').attr("checked")){
			if($('#refView').val()==""){
				ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.referenceViewIsRequired')}");
				return false;
			}else{
				return true;
			}
		}else{
			return true;
		}
	}
	
	ec.view.requiredShadowView = function(){
		if($('#view_isShadow').attr("checked")){
			if($('#shadowViews').val() == "" || $('#shadowViews').val() == null){
				ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.ShadowViewIsRequired')}");
				return false;
			}else{
				return true;
			}
		}else{
			return true;
		}
	}
	
	/* 
	 * @method ec.view.urlCustom
	 * @public
	 */
	ec.view.urlCustom=function(obj){
		var type = $("*[name='view.type']").val();
		if(CUI(obj).prop('checked')) {
			$('td[name^="view_"]').not("td[name='view_customView']").hide();
			$('tr[id^="view_"]').not('#view_customView_tr').hide();
			CUI('#customUrlTr').show();
			
			CUI('input[name="view.isShadow"]').val('false');
			CUI('*[name="view.shadowView.code"]').val('');
			CUI('input[name="view.customFlag"]').val('true')
			if(type!='TREE' && type!="REFTREE"){
				$('#view_openType_tr').show();
				$('td[name="view_openType_td"]').show();
				$('td[name="view_openType_td"]').show(); 
			}
			CUI('*[name="view.openType"]').trigger('change');
			if(type!="REFERENCE"){
				$('#workflowTd_view_mainref').show();
			}
			if(type==='VIEW'){
				$('#workflowTd_view_mainview').show();
				$('#workflowTd_view_dealInfoShow').show();
				$('#retrial').hide();
			}else if(type==='LIST'){
				$('#view_workflow_tr').show();
				$('td[name="view_usedForWorkFlow"]').show();
			}
			if(type == 'EDIT'||type == 'VIEW'||type == 'LIST'||type == 'REFERENCE'||type == 'EXTRA'||type == 'TREE'||type == 'REFTREE'){
				$('tr[id="view_datagrid_type"]').show();
				$('td[name="view_addModel_td"]').show();
			}
		} else {
			CUI('#customUrlTr').hide();
			CUI('input[name="view.customFlag"]').val('false');
			//CUI('input[name="view.isShadow"]').val('false');
			//CUI('*[name="view.shadowView.code"]').val('${(view.shadowView.code)!}');
			viewTypeChanged();
			ec.view.checkOpenType();
		}
		
	}
	
	ec.view.usedForTree = function(obj) {
		if(CUI(obj).prop('checked')) {
			CUI('#treeassociateTr').show();
			$('.includeChildrenTr').show();
			CUI('input[name="view.usedForTree"]').val('true');
		} else {
			CUI('#treeassociateTr').hide();
			$('.includeChildrenTr').hide();
			CUI('input[name="view.usedForTree"]').val('false');
		}
	}
	
	ec.view.assModelChange = function(){
		ec.view.isShadow();
		ec.view.allowRef();
		ec.view.batchControlPrint();		
		ec.view.checkOpenType();
	}

	ec.view.checkOpenType = function(){
		/*var openType = $('select[name="view.assModel.code"] option:selected');
		if(openType.attr('entityUseForFlow') == 'true' && openType.attr('isMainModel') == 'true') {
			$('td[name="view_openType_td"]').hide();
			$('select[name="view.openType"]').val('frame');
			$('td[name="view_dialogType_td"]').hide();
			$('#view_width_height').hide();
		} else {
			$('td[name="view_openType_td"]').show();
		}*/
		$('td[name="view_openType_td"]').show();
	}
	ec.view.allowAudit = function(obj){
		var flag = false;
		if(obj){
			flag = CUI(obj).prop('checked');
		}else{
			flag = CUI('#view_isAudit').prop('checked');
		}
		if(flag){
			CUI('input[name="view.isAudit"]').val('true');
		}else{
			CUI('input[name="view.isAudit"]').val('false');
		}
	}

	ec.view.allowPrint = function(obj){
		var flag = false;
		if(obj){
			flag = CUI(obj).prop('checked');
		}else{
			flag = CUI('#view_isPrint').prop('checked');
		}
		if(flag){
			CUI('input[name="view.isPrint"]').val('true');
		}else{
			CUI('input[name="view.isPrint"]').val('false');
		}
	}
	
	ec.view.allowRef = function(obj) {
		var flag = false;
		var assmodelFlag =false;
		if($("*[name='view.assModel.code']").val()==""){
		    assmodelFlag =false;
		}else{
		    assmodelFlag =true;
		}
		if(obj){
			flag = CUI(obj).prop('checked');
		}else{
			flag = CUI('#view_isReference').prop('checked');
		}
		if(flag){
			$('#view_layout_id_12').show();
			$('#view_layout_id_22').show();
			CUI('input[name="view.isReference"]').val('true');
			if(assmodelFlag){
			    CUI.ajax({
                				url: "/msService/ec/view/modelRefViews?isProj=${isProj?string}",
                				type: 'post',
                				async: false,
                				data: {"modelCode":$("*[name='view.assModel.code']").val(),
                						"viewType":$("*[name='view.type']").val()
                					  },
                				success: function(res){
                					CUI('#refView').empty();
                					CUI('#refView').append('<option value=""></option>');
                					$.each(res, function(index, item){
                						CUI('#refView').append('<option value="' + item.code + '">'+ item.displayNameInternational +'</option>');
                					});
                			    }
                			});
                			$('#refView').val('${(view.reference.code)!}');
			}else{
			    $('#refView').empty();
			}

		}else{
			$('#view_layout_id_12').hide();
			$('#view_layout_id_22').hide();
			CUI('input[name="view.isReference"]').val('false');
			$('#refView').val('');
		}
	}
	ec.view.allowRef();
	
	ec.view.isAssView = function(obj) {
		if(CUI(obj).prop('checked')) {
				CUI(obj).val(true);
				$('td[name="view_assview_td"]').show();
			} else {
				CUI(obj).val(false);
				$('td[name="view_assview_td"]').hide();
			}
	}
	
	ec.view.isShadow = function(obj) {
		var flag = false;
		if(obj) {
			flag = CUI(obj).prop('checked');
		} else {
			flag = CUI('#view_isShadow').prop('checked');
		}
		if(flag) {
			$('.shadow-td').show();
			CUI('input[name="view.isShadow"]').val('true');
			if(CUI('*[name="view.type"]').val() == "LIST" || CUI('*[name="view.type"]').val() == "REFERENCE") {
				$('td[name="view_hasAttachment"]').hide();
				$('#view_attachment').prop("checked",false);
				$('input[name="view.hasAttachment"]').val("false");
			}
			CUI.ajax({
				url: "/msService/ec/view/shadowViews?isProj=${isProj?string}",
				type: 'post',
				async: false,
				data: {"modelCode":$("*[name='view.assModel.code']").val(),
						"viewType":$("*[name='view.type']").val(),
						"showType":$("*[name='view.showType']").val(),
						"editViewType":$("*[name='view.editViewType']").val()
					  },
				success: function(res) {
					CUI('#shadowViews').empty();
					//CUI('#shadowViews').append('<option value=""></option>');
					$.each(res,function(index,item){
						if(item.code != "${(view.code)!}") {
							CUI('#shadowViews').append('<option value="' + item.code + '">'+ item.displayNameInternational +'</option>');
						}
					});
				}
			});
			CUI('*[name="view.shadowView.code"]').val('${(view.shadowView.code)!}');
			<#if entity?? && (entity.module)?? && (entity.module.type)?? && (entity.module.type) == 'Mis'>
			<#else>
			$("[name='view_editViewType_td']").hide();
			$("#view_viewType_tr").hide();
			</#if>
			$("[id='view_editViewType']").hide();
			if(CUI('*[name="view.type"]').val() == "EXTRA"){
				CUI('#canzaotr').hide();
				$("#view_isAssView").removeAttr("checked");			
				CUI('#view_assview').hide();
				$('#view_print td:eq(2)').hide(); //增强型视图不支持页面打印
				$('#view_print td:eq(3)').hide(); //增强型视图不支持页面打印						
			}
		} else {
			$('.shadow-td').hide();
			CUI('input[name="view.isShadow"]').val('false');
			if(CUI('*[name="view.type"]').val() == "LIST" || CUI('*[name="view.type"]').val() == "REFERENCE") {
				$('td[name="view_hasAttachment"]').show();
			}
			CUI('*[name="view.shadowView.code"]').val('');
			if(CUI('*[name="view.type"]').val()=="TREE" || CUI('*[name="view.type"]').val()=="REFTREE"){
				$("#view_shadow_tr").hide();
			}
			if(CUI('*[name="view.type"]').val() == "EDIT" || CUI('*[name="view.type"]').val() == "VIEW"){
				$('#view_audit').show();
			}else{
				$('#view_audit').hide();
			}
			if(CUI('*[name="view.type"]').val() == "EXTRA"){
				CUI('#view_audit').hide();
				CUI('#view_pagetype').hide();
				if($("#view_isReference").prop("checked")){
					ec.view.allowRef($("#view_isReference"));
					$("#view_isReference").removeAttr("checked");					
				}				
				CUI('#canzaotr').hide();
				$("#view_isAssView").removeAttr("checked");				
				CUI('#view_assview').hide();
				$('#view_print td:eq(2)').hide(); //增强型视图不支持页面打印
				$('#view_print td:eq(3)').hide(); //增强型视图不支持页面打印							
			}
			//树视图，列表，参照，列表布局，参照布局都需要显示是否启用权限
			if(CUI('*[name="view.showType"]').val()!="PART" && (CUI('*[name="view.type"]').val()=="REFERENCE" 
				|| CUI('*[name="view.type"]').val()=="REFTREE"  || CUI('*[name="view.type"]').val()=="VIEW" )){
				if(CUI('*[name="view.isNew"]').val() == "true"){
					if(CUI('*[name="view.type"]').val() == "VIEW"){
						$("#isPermission_cbx").prop('checked', true);
					}else if(CUI('*[name="view.type"]').val() == "REFERENCE" || CUI('*[name="view.type"]').val()=="REFTREE"){
						$("#isPermission_cbx").prop('checked', false);
					}
				}
				$("#view_permission_tr").show();
				if($("#isPermission_cbx").prop('checked') && CUI('*[name="view.type"]').val()=="REFERENCE"){
					$('#view_permission_tr1').hide();
					$('#view_permission_tr2').show();
				} else {
					$('#view_permission_tr1').hide();
					$('#view_permission_tr2').hide();
				}
			} else {
				$("#view_permission_tr").hide();
				$('#view_permission_tr1').hide();
				$('#view_permission_tr2').hide();
			}
			if(CUI('*[name="view.type"]').val() == "EDIT" || CUI('*[name="view.type"]').val() == "VIEW") {
				<#if entity?? && (entity.module)?? && (entity.module.type)?? && (entity.module.type) == 'Mis'>
				<#else>
				$("[name='view_editViewType_td']").show();
				$("#view_viewType_tr").show();
				</#if>
				$("[id='view_editViewType']").show();
				if($("#view_editViewType").prop('checked')){	//增强型视图不支持页面打印
					$('#view_print td:eq(2)').hide();
					$('#view_print td:eq(3)').hide();
				}else{
					$('#view_print td:eq(2)').show();
					$('#view_print td:eq(3)').show();
				}
			}
		}
	}
	
	
	
	ec.view.includeChildren = function(obj) {
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.includeChildren"]').val('true');
		} else {
			CUI('input[name="view.includeChildren"]').val('false');
		}
	}
	
	ec.view.showAssTreeModel = function() {
		var modelCode = CUI('*[name="view.assModel.code"]').val();
		var open_url= "/msService/ec/view/showTreeModelsMapByModel?isProj=${isProj?string}&modelCode=" + modelCode + "&closePage=true&callBackFuncName=ec.view.assTreeResult";
		var handle = null;
		var window_height = 600;
		var window_width  = 280;
		ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		handle = window.open(open_url,"${getText('ec.view.assTreeModel')}",ShowStyle);
		handle = null;
		return false;
	
	}
	
	ec.view.assTreeResult = function(obj) {
		CUI('input[name="view.assTreePath"]').val(obj.assTreePath);
		CUI('input[name="view.assTreeModelCode"]').val(obj.relationCode);
		CUI('input[name="view.assTreeLayRec"]').val(obj.layRec);
	}
	
	/* 
	 * @method ec.view.mainView
	 * @public
	 */
	ec.view.mainView=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.mainView"]').val('true')
		} else {
			CUI('input[name="view.mainView"]').val('false')
		}
	}
	/* 
	 * @method ec.view.attachmentFlag
	 * @public
	 */
	ec.view.attachmentFlag=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.attachmentFlag"]').val('true')
		} else {
			CUI('input[name="view.attachmentFlag"]').val('false')
		}
	}
	/* 
	 * @method ec.view.isControl
	 * @public
	 */
	ec.view.isControl=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.isControl"]').val('true')
		} else {
			CUI('input[name="view.isControl"]').val('false')
		}
	}
	
	ec.view.importFlag=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.importFlag"]').val('true')
		} else {
			CUI('input[name="view.importFlag"]').val('false')
		}
	}
	
	/* 
	 * @method ec.view.hasAttachment
	 * @public
	 */
	ec.view.hasAttachment=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.hasAttachment"]').val('true')
		} else {
			CUI('input[name="view.hasAttachment"]').val('false')
		}
	}
	
	/* 
	 * @method ec.view.dealInfoShow
	 * @public
	 */
	ec.view.dealInfoShow=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.dealInfoShow"]').val('true');
			$('td[name="view_enableSimpleDealInfo"]').show();
			$('#enableSimpleDealInfo_cbx').prop('checked', true);
			ec.view.enableSimpleDealInfo();
		} else {
			CUI('input[name="view.dealInfoShow"]').val('false');
			$('td[name="view_enableSimpleDealInfo"]').hide();
			$('select[name="view.dealInfoGroup"]').prop('disabled', false);
		}
	}
	
	/* 
	 * @method ec.view.usedForWorkFlow
	 * @public
	 */
	ec.view.usedForWorkFlow=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI.Dialog.confirm("${getText('ec.list.change.mainList')}",function(){
				CUI('input[name="view.usedForWorkFlow"]').val('true');
			},function(){
				CUI(obj).prop('checked',false);
			});
		} else {
			CUI('input[name="view.usedForWorkFlow"]').val('false');
		}
		viewTypeChanged();
	}
	
	/* 
	 * @method ec.view.mainRef
	 * @public
	 */
	ec.view.mainRef=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.mainRef"]').val('true');
		} else {
			CUI('input[name="view.mainRef"]').val('false');
		}
		viewTypeChanged();
	}
	/* 
	 * @method ec.view.onlyForQuery
	 * @public
	 */
	ec.view.onlyForQuery = function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.onlyForQuery"]').val('true');
		} else {
			CUI('input[name="view.onlyForQuery"]').val('false');
		}
		viewTypeChanged();
	}
	
	<#if view?exists&&view.customFlag?exists&&view.customFlag == true>
	CUI('#customUrlTr').show();
	</#if>
	ec.view.retrialFlag=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI('input[name="view.retrialFlag"]').val('true');
			$('td[name="view_scriptCode"]').show();
		} else {
			CUI('input[name="view.retrialFlag"]').val('false');
			$('td[name="view_scriptCode"]').hide();
		}
	}
	ec.view.scriptCode=function(){
		var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		<#if view?exists&&view.entity?exists>
			var param="entityCode=${view.entity.code}";
		<#else>
			var param="entityCode=${entity.code}";
		</#if>
		
		foundation.common.callOpenWeb("script",windowStyle,null,true,null,"setScript",param);
		
	}

	ec.view.viewShowTypeChanged = function(){
		var obj = $('select[name="view.showType"]');
		var viewType = $('*[name="view.type"]').val();
		if(obj.val() == 'LAYOUT' || obj.val() == 'LAYOUT2') {
			//$('#view_layout_id_1').show();
			//$('#view_layout_id_2').show();
			$('#view_shadow_tr').hide();
			$('#view_isShadow').prop("checked",false);
			$('#shadowViews').val('');
			$('#view_batch_control_print').hide();
		} else {
			$('#view_layout_id_1').hide();
			$('#view_layout_id_2').hide();
			if(viewType == "EDIT" || viewType == "VIEW"){
				$('#view_audit').show();
			}else{
				$('#view_audit').hide();
			}
			if(viewType == "EDIT" || viewType == "VIEW" || viewType == "EXTRA" || viewType == "LIST" || viewType == "REFERENCE"){
				$('#view_shadow_tr').show();
			}else{
				$('#view_shadow_tr').hide();
			}
			if(viewType == "VIEW"){
				$("[name='attachment_flag']").show();
			}else{
				$("#attachment_flag").attr("checked", false);
				$("[name='attachment_flag']").hide();
				CUI('input[name="view.attachmentFlag"]').val('false');
				$("[name='attachmentFlag']").attr("checked", false)
			}
			
			if(viewType == 'LIST'){
				if(obj.val() == 'PART'){
					$('#view_workflow_tr').hide();
					$('#view_workflow_tr').prop('checked',false);
				} else {
					$('#view_workflow_tr').show();
				}
			} else {
				$('#view_workflow_tr').hide();
			}
			if (viewType == 'LIST') {
				$('#view_batch_control_print').show();	
			}
			if (viewType == 'REFERENCE') {
				if(obj.val() == 'PART'){
					$('#workflowTd_view_mainref').hide();
					$('#view_assview').hide();
				} else {
					$('#workflowTd_view_mainref').show();
					$('#view_assview').show();
				}	
			}
			ec.view.isShadow();
		}
		<#if isProj&&(!view??||!(view.inheritType)??)>
		if((CUI('*[name="view.type"]').val() == 'LIST'||CUI('*[name="view.type"]').val() == 'EXTRA')&&obj.val()!="PART") {
			$('#view_menuinfo_tr').show();
		}else{
			$('#view_menuinfo_tr').hide();
		}
		</#if>	
	}

	CUI(function(){
		ec.view.viewShowTypeChanged();
		<#if (isProj)?exists && (isProj)>
			$(".infoTable input[name='view.isControl']").attr('disabled', true);
			<#if view?exists && (view.isControl)== true>
				$("#ec_view_edit_form input[name='view.name']").attr('readonly', true);
				$("#ec_view_edit_form input[name='view.name']").addClass('cui-readonly-field');
				$(".infoTable input[name='view.urlCustom']").attr('disabled', true);
				$(".infoTable input[name='view.mainView']").attr('disabled', true);
				$(".infoTable input[select='view.openType']").attr('disabled', true);
				$(".infoTable input[select='view.showType']").attr('disabled', true);
				$(".infoTable input[select='view.layoutId']").attr('disabled', true);
				$(".infoTable input[select='view.assModel.code']").attr('disabled', true);
			</#if>
		</#if>
		if($('input[name="view.permissionCode"]').val()!=""){
			$('input[name="view.permissionCode"]').attr('readonly', true);
		}
		if($('input[name="view.operateUrl"]').val()!=""){
			$('input[name="view.operateUrl"]').attr('readonly', true);
		}
		function submitBapForm(){//电子签名成功之后出现进度条并提交表单
			var ecFormFlag = false;
			var retrialFormFlag = false;
			if(ecFormFlag && ( $('#ec_view_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
				// dialog不出进度条
				ecFormFlag = false;
			}
			ecFormFlag = (ecFormFlag || retrialFormFlag);
			
			//前台验证通过之后出进度条
			CUI.Dialog.toggleAllButton('ec_view_edit_form',true,ecFormFlag, true);
				// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
			setTimeout(function(){
			
				// 延迟保存数据, 解决onchange事件无法触发问题
				var formId = 'ec_view_edit_form';
				var ecformflag = false;
				
				
				$('input[type="text"]','#'+formId).each(function(){
					var v=$.trim($(this).val());
					$(this).val(v);
				});
				var files = $('input[type="file"]', '#' + formId);
				if(ecformflag || (files!=null&&files.length>0)) {
					ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
				} else {
				
				var postData = CUI('#'+formId).serialize();
				CUI.ajax({
					url : CUI('#'+formId).attr('action'),
					type : 'POST',
					dataType : 'json',
					data : postData,
					error : function(XMLHttpRequest, textStatus, errorThrown){
						//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
						if (XMLHttpRequest.status==401) {
							//showLoginDialog();
							return ;
						}
						var msg = CUI.parseJSON(XMLHttpRequest.responseText);
						var errorMsgs = "";
						CUI.each(msg.items,function(index,item){
							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
								$("#ec_view_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else {
								var field = CUI("#ec_view_edit_form *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
									showErrorField(field.next());
								} else {
									showErrorField(field);
								}
							}
							CUI("#ec_view_edit_form *[name='"+index+"']").first().focus();
							for(var i = 0 ; i < item.length ; i++){
								errorMsgs += item[i] + '<br/>';
							}
						});
						CUI.each(msg.actionErrors,function(index,item){
							errorMsgs += item + '<br/>';
						});
						if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
							errorMsgs += msg.exceptionMsg + '<br/>';
						}
						var oErrorWidget = null;
						oErrorWidget = ec_view_edit_formDialogErrorBarWidget;
						if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
							oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
						}	else {
							oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
						}					
						if(CUI.Dialog){
							CUI.Dialog.toggleAllButton('ec_view_edit_form', true);
						}
					},
					success : function(msg){
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							setTimeout(function(){closeLoadPanel();}, 500);
						}
						ec.view.callBack(msg,postData);
					}
				});
			}
		}, 600);
			return false;
		}

		CUI('#ec_view_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
			//每次提交时先隐藏报错信息
			try{
			}catch(e){
			
			}
			// 清除错误标红
			try{clearErrorLabels();}catch(e){}
			var ecFormFlag = false;
			var retrialFormFlag = false;
			if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
				// dialog不出进度条
				ecFormFlag = false;
			}
			ecFormFlag = (ecFormFlag || retrialFormFlag);
			
			
			//禁用所有按钮
			//CUI("body").one("click", function(event){
			//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_view_edit_form',true,ecFormFlag);
			//});
				//if(!validateForm_ec_view_edit_form())	return false;
			$('#ec_view_edit_form').trigger('beforeSubmit');
			if($('#ec_view_edit_form input[name="operateType"]').val() == "submit"){
				var deploymentId=$('#ec_view_edit_form input[name="deploymentId"]');
				var buttonCode=$('#ec_view_edit_form input[name="buttonCode"]');
				var namespace=$('#ec_view_edit_form input[name="namespace"]');
				if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
					var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
					if(signatureInfo[0] != '') {
						var cancelItem = $('input[name="workFlowVarStatus"]');
						if(cancelItem.val() != "cancel") {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_view_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_view_edit_form','ec.flow.submit',false)});
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_view_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_view_edit_form','ec.edit.remove',false)});
						}
						
					}
					else {
						submitBapForm();
					}
				}
				else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
					var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
					if(signatureInfo[0] != '') {
						if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
							parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_view_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_view_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_view_edit_form',buttonCode.val(),false)});},2000);
							}	
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_view_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_view_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_view_edit_form',buttonCode.val(),false)});},2000);
							}
						}
					}
					else {
						submitBapForm();	
					}
				}
				else {
					submitBapForm();
				}
			}
			else {
				submitBapForm();
			}
			return false;
		});
	});

	function viewTypeChanged(){
		$('#workflowTd_view_dealInfoGroup').hide();
		var obj = CUI('*[name="view.type"]');
		var openTypeTd = CUI('select[name="view.openType"]').parent();
		$($("option[value='']"), $("select[name='view.assModel.code']")).remove();
		$($("option[dataType='1']"), $("select[name='view.assModel.code']")).show();
		// 视图类型右侧checkbox显示控制
		if(obj.val() == 'MNECODE') {
			$('tr[id^="view_"]').hide();
			$('td[name^="view_"]').hide();
			$('#view_isShadow').prop("checked",false);
			$('#shadowViews').val('');
			$('tr[id="view_datagrid_type"]').show();
			$('td[name="view_addModel_td"]').show();
			$('#view_print').hide();
			$('#view_control_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
		} else if(obj.val() == 'DIGEST'){
			$('tr[id^="view_"]').hide();
			$('td[name^="view_"]').hide();
			$('#view_isShadow').prop("checked",false);
			$('#shadowViews').val('');
			$('tr[id="view_datagrid_type"]').show();
			$('td[name="view_addModel_td"]').show();
			$('tr[id="canzaotr"]').hide();
			$('#view_print').hide();
			$('#view_control_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
			var assModelCode = "${(view.assModel.code)!''}";
			if($($("option[dataType='3']"), $("select[name='view.assModel.code']")).size()<1){
				$("select[name='view.assModel.code']").prepend("<option value=''></option>");
			}
			if(assModelCode!=""){
				$("select[name='view.assModel.code']").val(assModelCode);
			}else{
				var option = $("select[name='view.assModel.code'] option[datatype = '3']").val();
				if(option == undefined){
					option = "";
				}
				$("select[name='view.assModel.code']").val(option);
			}
			$($("option[modelType='3']"), $("select[name='view.assModel.code']")).hide();
		}else {
			$('tr[id^="view_"]').show();
			$('td[name^="view_"]').show();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
		}
		$('#retrial').hide();
		$('#att').show();
		$('#attShow').hide();
		$("[name='view_editViewType_td']").hide();
		$("#view_viewType_tr").hide();
		$("[name='view_editViewType']").hide();
		
		var openType = $('select[name="view.openType"]').val();
		if(openType=='dialog'){
			$('td[name="view_dialogType_td"]').show();
			$('tr[id="view_width_height"]').show();
		}else{
			$('td[name="view_dialogType_td"]').hide();
			$('tr[id="view_width_height"]').hide();
		}
		<#if entity?? && (entity.workflowEnabled!false)?string('true','false')=='true'>
			$('td[name="view_dealInfoShow"]').show();
			$('td[name="view_dealInfoGroup"]').show();
		<#else>
			$('td[name="view_dealInfoShow"]').hide();
			$('td[name="view_dealInfoGroup"]').hide();
		</#if>
		if(obj.val() == 'EDIT') {
			$('#canzaotr').show();
			$('#view_workflow_tr').hide();
			$('td[name="view_hasAttachment"]').show();
			$('#workflowTd_view_mainview').show();
			$('#workflowTd_view_dealInfoShow').show();
			$('#workflowTd_view_mainref').hide();
			$('#view_assview').hide();
			<#if !((view.dealInfoShow)?? && view.dealInfoShow)>
				$('td[name="view_enableSimpleDealInfo"]').hide();
			</#if>
			if($('td[name="view_dealInfoShow"]').css('display') == 'none'){
				$('td[name="view_enableSimpleDealInfo"]').hide();
			}
			if($('td[name="view_enableSimpleDealInfo"]').css('display') != 'none' && $('#enableSimpleDealInfo_cbx').prop('checked')){
				$('select[name="view.dealInfoGroup"]').prop('disabled', true);
			}
			$('#workflowTd_view_dealInfoGroup').show();
			$('td[name="view_mainView"]').hide();
			if($("#view_editViewType").prop('checked')){	//增强型视图不支持页面打印
				$('#view_print td:eq(2)').hide();
				$('#view_print td:eq(3)').hide();	
			}else{
				$('#view_print td:eq(2)').show();
				$('#view_print td:eq(3)').show();	
			}
			$('#view_control_print').show();
			if ($("#view_controlPrint_cbx").prop('checked')) { 
				$('#view_control_print_set_name').hide(); 
			} else {
				$('#view_control_print_set_name').hide(); 
			}
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
			<#if entity?? && (entity.module)?? && (entity.module.type)?? && (entity.module.type) == 'Mis'>
			<#else>
			$("[name='view_editViewType_td']").show();
			$("#view_viewType_tr").show();
			</#if>
			$("[name='view_editViewType']").show();			
			
			$('#view_pagetype').hide();
			$("[name='view.showType']").val("SINGLE");
			$("[name='view.showType']").trigger("change");
			
			var assModelCode = "${(view.assModel.code)!''}";
			if($($("option[dataType='3']"), $("select[name='view.assModel.code']")).size()<1){
				$("select[name='view.assModel.code']").prepend("<option value=''></option>");
			}
			if(assModelCode!=""){
				$("select[name='view.assModel.code']").val(assModelCode);
			}else{
				var option = $("select[name='view.assModel.code'] option[datatype = '3']").val();
				if(option == undefined){
					option = "";
				}
				$("select[name='view.assModel.code']").val(option);
			}
			$($("option[modelType='3']"), $("select[name='view.assModel.code']")).hide();
		}else if(obj.val() == 'EXTRA') {
			$('#canzaotr').show();
			$('#view_workflow_tr').hide();
			$('td[name="view_hasAttachment"]').show();
			$('#workflowTd_view_mainview').hide();
			$('#workflowTd_view_dealInfoShow').hide();
			$('#workflowTd_view_mainref').hide();
			<#if !((view.dealInfoShow)?? && view.dealInfoShow)>
				$('td[name="view_enableSimpleDealInfo"]').hide();
			</#if>
			if($('td[name="view_dealInfoShow"]').css('display') == 'none'){
				$('td[name="view_enableSimpleDealInfo"]').hide();
			}
			if($('td[name="view_enableSimpleDealInfo"]').css('display') != 'none' && $('#enableSimpleDealInfo_cbx').prop('checked')){
				$('select[name="view.dealInfoGroup"]').prop('disabled', true);
			}
			$('#workflowTd_view_dealInfoGroup').hide();
			$('td[name="view_mainView"]').hide();
			$('#view_print').show();
			$('#view_control_print').show();
			if ($("#view_controlPrint_cbx").prop('checked')) { 
				//$('#view_control_print_set_name').show();
				$('#view_control_print_set_name').hide(); //关联禅道bug3777隐藏控件设置名称
			} else {
				$('#view_control_print_set_name').hide(); 
			}
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
			$('tr[id="view_openType_tr"]').hide();
			$('#view_pagetype').hide();
			$("[name='view.showType']").val("SINGLE");
			$("[name='view.showType']").trigger("change");
		} else if(obj.val() == 'VIEW') {
			$("#view_assview").hide();
			$('#canzaotr').hide();
			$('#view_workflow_tr').hide();
			$('td[name="view_hasAttachment"]').show();
			$('#workflowTd_view_mainview').show();
			$('#workflowTd_view_dealInfoShow').show();
			$('#workflowTd_view_mainref').hide();
			<#if !((view.dealInfoShow)?? && view.dealInfoShow)>
				$('td[name="view_enableSimpleDealInfo"]').hide();
			</#if>
			if($('td[name="view_dealInfoShow"]').css('display') == 'none'){
				$('td[name="view_enableSimpleDealInfo"]').hide();
			}
			if($('td[name="view_enableSimpleDealInfo"]').css('display') != 'none' && $('#enableSimpleDealInfo_cbx').prop('checked')){
				$('select[name="view.dealInfoGroup"]').prop('disabled', true);
			}
			$('#workflowTd_view_dealInfoGroup').show();
			$('td[name="view_mainView"]').show();
			<#if entity?? && (entity.workflowEnabled!false)?string('true','false')=='true'>
			$('#retrial').show();
			<#else>
			$('#retrial').hide();
			</#if>
			<#if entity?? && (entity.workflowEnabled!false)?string('true','false')=='true' && ((view?? && (!view.code?? || (view.retrialFlag?? && view.retrialFlag))))>
				$('#view_retrial').prop('checked',true);
				CUI('input[name="view.retrialFlag"]').val('true');
				$('td[name="view_scriptCode"]').show();
			<#else>
				$('#view_retrial').prop('checked',false);
				CUI('input[name="view.retrialFlag"]').val('false');
				$('td[name="view_scriptCode"]').hide();
			</#if>
			$('#att').hide();
			$('#attShow').show();
			$('#view_print').show();
			if($("#view_editViewType").prop('checked')){	//增强型视图不支持页面打印
				$('#view_print td:eq(2)').hide();
				$('#view_print td:eq(3)').hide();
			}else{
				$('#view_print td:eq(2)').show();
				$('#view_print td:eq(3)').show();
			}
			$('#view_control_print').show();
			if ($("#view_controlPrint_cbx").prop('checked')) { 
				$('#view_control_print_set_name').hide(); 
			} else {
				$('#view_control_print_set_name').hide(); 
			}
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
			<#if entity?? && (entity.module)?? && (entity.module.type)?? && (entity.module.type) == 'Mis'>
			<#else>
			$("[name='view_editViewType_td']").show();
			$("#view_viewType_tr").show();
			</#if>
			$("[name='view_editViewType']").show();
			$('#view_pagetype').hide();
			$("[name='view.showType']").val("SINGLE");
			$("[name='view.showType']").trigger("change");
		} else if(obj.val() == 'REFERENCE') {
			$('#canzaotr').hide();
			$('#workflowTd_view_mainref').show();
			$('#view_assview').show();
			if(CUI('#view_isAssView').prop('checked')) {
				$('#view_isAssView').val(true);
				$('td[name="view_assview_td"]').show();
			} else {
				$('#view_isAssView').val(false);
				$('td[name="view_assview_td"]').hide();
			}
			$('#view_workflow_tr').hide();
			$('#workflowTd_view_mainview').hide();
			$('#workflowTd_view_dealInfoShow').hide();
			if(CUI('#view_isShadow').prop('checked')) {
				$('td[name="view_hasAttachment"]').hide();
			} else {
				$('td[name="view_hasAttachment"]').show();
			}
			$('tr[id="view_openType_tr"]').hide();
			$("tr[id='view_width_height']").hide();
			if(obj.val() == 'LIST'){
				$('#view_control_print').hide();
			}else{
				$('#view_control_print').hide();
			}
			$('#view_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').hide();
			if(CUI('*[name="view.showType"]').val()!="PART"){
				$('#view_permission_tr').show();
				if($("#isPermission_cbx").prop('checked') && obj.val() == 'REFERENCE'){
					$('#view_permission_tr1').hide();
					$('#view_permission_tr2').show();
				} else {
					$('#view_permission_tr1').hide();
					$('#view_permission_tr2').hide();
				}
			} else {
				$('#view_permission_tr').hide();
				$('#view_permission_tr1').hide();
				$('#view_permission_tr2').hide();
			}
			var assModelCode = "${(view.assModel.code)!''}";
			if($($("option[dataType='3']"), $("select[name='view.assModel.code']")).size()<1){
				$("select[name='view.assModel.code']").prepend("<option value=''></option>");
			}
			if(assModelCode!=""){
				$("select[name='view.assModel.code']").val(assModelCode);
			}else{
				var option = $("select[name='view.assModel.code'] option[datatype = '3']").val();
				if(option == undefined){
					option = "";
				}
				$("select[name='view.assModel.code']").val(option);
			}
			$($("option[modelType='3']"), $("select[name='view.assModel.code']")).hide();		
		} else if(obj.val() == 'LIST') {
			$('#canzaotr').hide();
			$('#view_workflow_tr').show();
			$('#workflowTd_view_mainview').hide();
			$('#workflowTd_view_mainref').hide();
			$('#view_assview').hide();
			$('#workflowTd_view_dealInfoShow').hide();
			if(CUI('#view_isShadow').prop('checked')) {
				$('td[name="view_hasAttachment"]').hide();
			} else {
				$('td[name="view_hasAttachment"]').show();
			}
			$('tr[id="view_openType_tr"]').hide();
			$("tr[id='view_width_height']").hide();
			$('#view_print').hide();
			$('#view_control_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').show();
			if(CUI('*[name="view.showType"]').val()!="PART"){
				//$('#view_permission_tr').show();
			}
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
		} else if(obj.val() == 'MNECODE') {
			$('#canzaotr').hide();
			$('tr[id^="view_"]').hide();
			$('td[name^="view_"]').hide();
			$('tr[id="view_datagrid_type"]').show();
			$('td[name="view_addModel_td"]').show();
			$('#view_print').hide();
			$('#view_control_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').hide();
			$('#view_permission_tr').hide();
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
			var assModelCode = "${(view.assModel.code)!''}";
			if($($("option[dataType='3']"), $("select[name='view.assModel.code']")).size()<1){
				$("select[name='view.assModel.code']").prepend("<option value=''></option>");
			}
			if(assModelCode!=""){
				$("select[name='view.assModel.code']").val(assModelCode);
			}else{
				var option = $("select[name='view.assModel.code'] option[datatype = '3']").val();
				if(option == undefined){
					option = "";
				}
				$("select[name='view.assModel.code']").val(option);
			}
			$($("option[modelType='3']"), $("select[name='view.assModel.code']")).hide();		
		}else if(obj.val()=="TREE" || obj.val()=="REFTREE"){
			$("#view_workflow_tr").hide();
			$("#workflowTd_view_mainview").hide();
			$('#workflowTd_view_mainref').hide();
			$('#view_assview').hide();
			$("#workflowTd_view_dealInfoShow").hide();
			//$("#view_customView_tr").hide();
			$('td[name="view_hasAttachment"]').hide();
			$("#view_shadow_tr").hide();
			$("#view_openType_tr").hide();
			$("#view_pagetype").hide();
			$("#canzaotr").hide();
			$("td[name='view_datagrid']").each(function(){
				$(this).hide();
			});
			$("td[name='view_isControl_td']").each(function(){
				$(this).hide();
			});
			var assModelCode = "${(view.assModel.code)!''}";
			if($($("option[dataType='2']"), $("select[name='view.assModel.code']")).size()<1){
				$("select[name='view.assModel.code']").prepend("<option value=''></option>");
			}
			if(assModelCode!=""){
				$("select[name='view.assModel.code']").val(assModelCode);
			}else{
				var option = $("select[name='view.assModel.code'] option[datatype = '2']").val();
				if(option == undefined){
					option = "";
				}
				$("select[name='view.assModel.code']").val(option);
			}
			
			$($("option[dataType='1']"), $("select[name='view.assModel.code']")).hide();
			$('#view_print').hide();
			$('#view_control_print').hide();
			$('#view_control_print_set_name').hide();
			$('#view_batch_control_print').hide();
			if(obj.val()=="REFTREE" && CUI('*[name="view.showType"]').val()!="PART"){
				$('#view_permission_tr').show();
				if($("#isPermission_cbx").prop('checked')){
					$('#view_permission_tr1').show();
					$('#view_permission_tr2').show();
				} else {
					$('#view_permission_tr1').hide();
					$('#view_permission_tr2').hide();
				}
			} else {
				$('#view_permission_tr').hide();
				$('#view_permission_tr1').hide();
				$('#view_permission_tr2').hide();
			}
		}
		
		if($('#ck_customView').prop('checked')){
			ec.view.urlCustom($('#ck_customView').get(0));
		}
		
		var showType = $('select[name="view.showType"]').val();
		if(showType == 'LAYOUT' || showType == 'LAYOUT2'){
			$("#view_shadow_tr").hide();
		}
		<#if isProj&&(!view??||!(view.inheritType)??)>
		if((obj.val() == 'LIST'||obj.val() == 'EXTRA')&&CUI('*[name="view.showType"]').val()!="PART") {
			$('#view_menuinfo_tr').show();
		}else{
			$('#view_menuinfo_tr').hide();
		}
		</#if>	
	}
	function assTreeModelsChanged(){
		/*var modelCode = CUI('*[name="view.assModel.code"]').val();
		var layoutid = CUI('*[name="view.showType"]').val();
		if(layoutid == 'PART') {
			<#if view?? && view.code??>
			$('#treeSelectTr').show();
			if(CUI('#view_usedForTree').prop('checked')) {
				$('#treeassociateTr').show();
				$('.includeChildrenTr').show();
			}
    		CUI('*[name="view.assTreeModelCode"]').val('${(view.assTreeModelCode)!}');
    		</#if>
		}else {
			$('#treeSelectTr').hide();
			$('#treeassociateTr').hide();
			$('.includeChildrenTr').hide();
		}*/
	}
	
	function hasAttachmentInit(){
		if(CUI('*[name="view.type"]').val() == 'EDIT' || CUI('*[name="view.type"]').val() == 'VIEW') {
			CUI('#view_hasAttachment').show();
			if(CUI('*[name="view.openType"]').val() == 'dialog') {
				CUI('#view_hasAttachment input').prop('checked', false);
				CUI('*[name="view.hasAttachment"]').val('false');
			} else {
				CUI('#view_hasAttachment input').prop('checked', true);
				CUI('*[name="view.hasAttachment"]').val('true');
			}
		} else {
			CUI('#view_hasAttachment').hide();
			CUI('*[name="view.hasAttachment"]').val('false');
		}
	}
	function dealInfoShowInit(){
		if(CUI('*[name="view.type"]').val() == 'EDIT' || CUI('*[name="view.type"]').val() == 'VIEW' || CUI('*[name="view.usedForWorkFlow"]').val() == 'true') {
			CUI('#view_dealInfoShow').show();
			CUI('#view_dealInfoGroup').show();
		} else {
			CUI('#view_hasAttachment').hide();
			CUI('*[name="view.dealInfoShow"]').val('false');
		}
	}

	CUI('*[name="view.type"]').change(function(){
		viewTypeChanged();
		ec.view.checkOpenType();
	});
	
	CUI('*[name="view.showType"]').change(function(){
		assTreeModelsChanged();
	});
	
	viewTypeChanged();
	assTreeModelsChanged();
	ec.view.isShadow();
	ec.view.viewShowTypeChanged();
	
	if(CUI('*[name="view.hasAttachment"]').val() == '' || CUI('*[name="view.hasAttachment"]').val() == null) {
		hasAttachmentInit();
	}
	CUI('*[name="view.type"]').change(function(){
		hasAttachmentInit();
		dealInfoShowInit();
	});
	CUI('*[name="view.openType"]').change(function(){
		hasAttachmentInit();
		if($('*[name="view.openType"]').val() == 'dialog') {
			$('*[name="view.dialogType"]').parents('td:first').show();
			$('*[name="view.dialogType"]').parents('td:first').prev().show();
			$('#view_width_height').show();
		} else {
			$('*[name="view.dialogType"]').parents('td:first').hide();
			$('*[name="view.dialogType"]').parents('td:first').prev().hide();
			$('#view_width_height').hide();
		}
	});
	$('*[name="view.dialogType"]').change(function(){
		var dialogType = $('*[name="view.dialogType"]').val();
		if(dialogType && dialogType.length > 0) {
			dialogType = dialogType.substring(dialogType.lastIndexOf('_') + 1);
			var dialogSize = foundation.common.DIALOG_TYPE[dialogType];
			if(dialogSize != null && dialogType != 'OTHER') {
				$('*[name="view.width"]').prop('disabled', true);
				$('*[name="view.height"]').prop('disabled', true);
				if(dialogSize.width) {
					$('*[name="view.width"]').val(dialogSize.width);
				}
				if(dialogSize.height) {
					$('*[name="view.height"]').val(dialogSize.height);
				}
			} else {
				$('*[name="view.width"]').prop('disabled', false);
				$('*[name="view.height"]').prop('disabled', false);
			}
		}
	});
	$(function(){
		var openSelect = $('select[name="view.openType"]');
		var viewType = $('input[name="view.type"]');
		var viewCode = $('input[name="view.code"]').val();
		if(viewType.val() === 'EDIT' || viewType.val()==='VIEW' || viewCode===''){
			if(openSelect.val()==='frame'){
				$('td[name="view_dialogType_td"]').hide();
				$('tr[id="view_width_height"]').hide();
			}else if(openSelect.val()==='dialog'){
				$('td[name="view_dialogType_td"]').show();
				$('tr[id="view_width_height"]').show();
			}
		}
		var objSelect = $('*[name="view.dialogType"]');
		$.each(foundation.common.DIALOG_TYPE, function(key, value){
			var defaultValue = '<#if view??>${(view.dialogType)!"DIALOG_OTHER"}</#if>';
			var optionHTML = '<option value="DIALOG_' + key + '"';
			if(defaultValue.endsWith('_' + key)) {
				optionHTML += ' selected'
			}
			optionHTML += '>' + ((key == 'OTHER') ? '${getText("ec.view.else")}' : key);
			if(value && value.width && value.height) {
				optionHTML += ' : ' + value.width + '*' + value.height + '</option>';
			}
			objSelect.append(optionHTML);
		});
		objSelect.trigger('change');
		ec.view.checkOpenType();
	});
	
	//批量控件打印
	ec.view.batchControlPrint = function(){
	    var bcp_cbx = $('#batchControlPrint_cbx');
		if(bcp_cbx != undefined && bcp_cbx != null){
			if(bcp_cbx.prop('checked')){
				$("input[name='view.isBatchControlPrint']").val(true);
				//$('.batchControlPrintViews').show();
				//$.ajax({
				//	type : 'POST',
				//	async : false,
				//	url : '/msService/ec/view/batchControlPrintViews?isProj=${isProj?string}',
				//	data : {"modelCode" : $("*[name='view.assModel.code']").val()},
				//	success : function(rs){
				//		$('#batchControlPrint_selectViews').empty();
				//		$('#batchControlPrint_selectViews').append("<option value=''></option>");
				//		$.each(rs, function(intex, item){
				//			$('#batchControlPrint_selectViews').append("<option value='" + item.code + "'>" + item.displayNameInternational + "</option>");
				//		});
				//	}
				//});
			}else{
				$("input[name='view.isBatchControlPrint']").val(false);
				$('.batchControlPrintViews').hide();
				$('#batchControlPrint_selectViews').val('');
			}
		}
	}
	ec.view.isPermission = function(){
		var p_cbx = $("#isPermission_cbx");
		$("#view_isPermission").val(p_cbx.prop('checked'));
		if(p_cbx.prop('checked') && CUI('*[name="view.type"]').val() == "REFERENCE"){
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').show();
		} else {
			$('#view_permission_tr1').hide();
			$('#view_permission_tr2').hide();
		}
	}
	
	ec.view.checkBatchControlPrint = function(){
	/*
		if($('#batchControlPrint_cbx').prop('checked')){
			if($('#batchControlPrint_selectViews').val() != null && $('#batchControlPrint_selectViews').val() != ''){
				return true;
			}else{
				ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.batchControlPrintSelectViewIsRequired')}");
				return false;
			}
		}
	*/	
		return true;
	}
	
	$(function(){
		if($("input[name='view.type']").val() == 'LIST'){
			ec.view.batchControlPrint();
			<#if (view.batchControlPrintSelectView)??>
				$('#batchControlPrint_selectViews').val('${view.batchControlPrintSelectView.code}');
			</#if>
		}
	});
	
	ec.view.enableSimpleDealInfo = function(){
		if($('#enableSimpleDealInfo_cbx').prop('checked')){
			$('input[name="view.enableSimpleDealInfo"]').val('true');
			$('select[name="view.dealInfoGroup"] option[value="byTask"]').attr('selected', true);
			$('select[name="view.dealInfoGroup"]').prop('disabled', true);
		}else{
			$('input[name="view.enableSimpleDealInfo"]').val('false');
			$('select[name="view.dealInfoGroup"]').prop('disabled', false);
		}
	}
	
	ec.view.requiredViewmenu=function(){
		if(($("input[name='view.type']").val() == 'LIST' || $("input[name='view.type']").val() == 'EXTRA') && $('#international_viewmenuName_showName').val()!=""){
			if($('#parentMenuId').val()==-1){
				ec_view_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.view.requiredViewParentMenu')}");
				return false;
			}else{
				return true;
			}
		}else{
			return true;
		}
	}
	$(function(){
	<#if !(view?? && view.type??)>
		ec.view.editViewType();
	</#if>
	});
</script>