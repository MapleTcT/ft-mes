<@datatable hidekey="['FLOWKEY','ACTIVECODE']" pageInitMethod="ec.pending.intitPt"  postData="${ptDataPost!}" dtPage="records" formId="queryForm" transMethod="post" id="proxyPendingListTable" dblclick="ec.pending.viewPendingsByFlow"  dataUrl="/msService/ec/pending/proxyPending/listByGroup" style="margin-left:10px;margin-right:4px;">
	<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.pending.flowName')}" width=150 />
	<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.pending.activeName')}" width=150 />
	<@datacolumn key="PENDINGCOUNT" textalign="right" label="${getHtmlText('ec.pending.pendingCount')}" width=100 />
</@datatable>
