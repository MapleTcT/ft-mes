<@s.hidden id="queryParam_deptId" name="deptId" />
<@s.hidden id="queryParam_userId" name="userId" />
<@s.hidden id="queryParam_flowKey" name="flowKey" />
<@s.hidden id="queryParam_activeCode" name="activeCode" />
<#assign url="/msService/ec/pending/proxyPending/listByGroup?a=1" >
<@datatable hidekey="['ID','TABLEID']"  postData="${ptDataPost!}" dblclick="ec.pending.proxyPending" pageInitMethod="ec.pending.intitPt" dtPage="records" exportExcel=true formId="department_queryForms" firstLoad=false transMethod="post" id="proxyPendingListTable2" dataUrl=url style="margin-left:10px;margin-right:4px;">
	<@operatebar operates="code:base_proxyPending||iconcls:publish||name:${getHtmlText('ec.proxyPending.entrust')}||onclick:ec.pending.proxyPending()" resultType="json" operateType="noPower">
	</@operatebar>
	<#if notifyEnabled>
		<@operatebar operates="code:base_pendingRemind||iconcls:setting||name:${getHtmlText('ec.proxyPending.remind')}||onclick:ec.pending.remind()" resultType="json" operateType="noPower">
		</@operatebar>
	</#if>
	<@operatebar operates="code:base_pendingDelete||iconcls:del||name:${getHtmlText('ec.proxyPending.delete')}||onclick:ec.pending.deleteMethod()" resultType="json" operateType="noPower">
	</@operatebar>
	<@datacolumn textalign="center" key="checkbox" type="checkbox" checkall="true" label="" width=60 />
	<@datacolumn key="TABLENO" label="${getHtmlText('ec.pending.tableNo')}" width=180 />
	<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width=180 />
	<@datacolumn key="ACTIVETYPE" label="${getHtmlText('ec.pending.pendingType')}" type="select" options="{'0':'${getText('ec.pending.normalPending')}','1':'${getText('ec.pending.normalPending')}','2':'${getText('ec.pending.proxyPending')}'}"  width=100 />
	<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.pending.flowName')}" width=120 />
	<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.pending.activeName')}" width=120 />
	<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.pending.owner')}" width=70 />
	<@datacolumn key="DEPTNAME" label="${getHtmlText('ec.pending.ownerDept')}" width=110 />
	<@datacolumn key="CREATETIME" textalign="center" type="date" label="${getHtmlText('ec.pending.createTime')}" width=100 />
	<@datacolumn key="CREATOR" label="${getHtmlText('ec.pending.inputor')}" width=70 />
	<@datacolumn key="MENUNAME" label="${getHtmlText('ec.pending.menuName')}" width=120 />
</@datatable>
<@exportexcel action="/msService/ec/pending/proxyPending/listByGroup?records.exportFlag=true&records.paging=true" settingtext="" settingUrl=""  formId="department_queryForms" look="text" width=250 height=170 text="" dtPage="records" />