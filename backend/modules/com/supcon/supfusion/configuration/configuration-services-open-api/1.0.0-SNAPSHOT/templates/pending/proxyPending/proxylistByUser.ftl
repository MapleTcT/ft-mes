<@datatable hidekey="['STAFFID','USERID','DEPTID']" pageInitMethod="ec.pending.intitPt" postData="${ptDataPost!}"  dtPage="records" formId="queryForm" dblclick="ec.pending.viewPendings" transMethod="post"  id="proxyPendingListTable"  dataUrl="/msService/ec/pending/proxyPending/listByGroup"  firstLoad=false style="margin:6px 4px 2px 13px;">
	<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.pending.owner')}" width=150 />
	<@datacolumn key="DEPTNAME" label="${getHtmlText('ec.pending.ownerDept')}" width=150 />
	<@datacolumn key="PENDINGCOUNT" textalign="right" label="${getHtmlText('ec.pending.pendingCount')}" width=100 />
</@datatable>
