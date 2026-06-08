<@errorbar id="printTemplateSelect"/>
<@frameset id="ec_knowledge_documentManage_docManage_historyRecord_container" border=0>
	<@frame id="ec_knowledge_documentManage_docManage_historyRecord_container_main"  offsetH=4 region="center" class="center_in">
		<#if false && isIE>
			<#assign routeFlag = "1">
		<#else>
			<#assign routeFlag = "0">
		</#if>
		<#if id??>
		<#assign dUrl="/msService/ec/printManage/customViewSelect?viewCode=${viewCode}&id=${id}">
		<#else>
		<#assign dUrl="/msService/ec/printManage/customViewSelect?viewCode=${viewCode}">
		</#if>
		<input type="hidden" id="sqlKeyWordCheck" value="${sqlKeyWordCheckParam}" />	
		<input type="hidden" id="dgViewRecord_id" value="print_template_select_dgViewRecord" />					
		<input type="hidden" id="dgViewRecord_url" value="${dUrl}" />
		<@datatable id="print_template_select_dgViewRecord" formId="print_template_model_sel_listviewLog_form" noPermissionKeys="" 
			dataUrl="${dUrl}" postData="&dgHistoryRecordPage.pageSize=65536" dtPage="templatesListPage" 
			renderOverEvent="" hidekeyPrefix="dgHistoryRecord" hidekey="['code','viewCode','templateCode']" transMethod="post" paginator=false editable=false  
			noPadding=true exportExcel=false isEdit=false tabViewIndex=0 style="margin-left:13px;margin-right:4px;" width=500 >
			<@datacolumn key="templateName"    showFormat="TEXT" defaultValue="" defaultDisplay=""  rowMerge=false decimal="" editable=false type="textfield" 
			showType="textfield" notnull=false     textalign="left"  hiddenCol=false viewUrl="" mneenable=false viewTitle="${getText('')}" label="${getText('ec.print.template.templateName')}" width=210/>
			<@datacolumn key="templateRemark"    showFormat="TEXT" defaultValue="" defaultDisplay="" rowMerge=false decimal="" editable=false type="textfield" showType="textfield" notnull=false  
			textalign="left"  hiddenCol=false viewUrl="" mneenable=false viewTitle="${getText('')}"  label="${getText('ec.print.template.templateRemark')}" width=220/>
		</@datatable>
	
	</@frame>
</@frameset>