<@errorbar id="EditDialogErrorBar" />
<@datatable hidekey="['code']" dtPage="lookjoblogPage" dataUrl="/msService/ec/scheduler/lookjoblog?code=${code}&isProj=${isProj?string('true', 'false')}"  id="lookjoblogList" hidekey="['code','version']" style="margin:4px 10px;" height=290 transMethod="post" >
	<@operatebar operates="code:ec_print_template_btn_add||name:${getText('ec.scheduler.job.scheduler.lookdetail')}||iconcls:add||onclick:ec.scheduler.job.lookdetailmessage()"
				  operateType="noPower" resultType="json">
			</@operatebar>
	<@datacolumn showFormat="YMD_HMS" label="${getHtmlText('ec.scheduler.job.jobFireTime')}" key="jobFireTime" type="datetime" width=120 />
	<@datacolumn showFormat="YMD_HMS" label="${getHtmlText('ec.scheduler.job.jobScheduledFireTime')}" key="jobScheduledFireTime" type="datetime" width=120 />
	<@datacolumn label="${getHtmlText('ec.scheduler.job.status')}" key="status" type="select" options="{'1':'${getText('ec.scheduler.job.status1')}','2':'${getText('ec.scheduler.job.status2')}'}" width=60 />
	<@datacolumn showFormat="YMD_HMS" label="${getHtmlText('ec.scheduler.job.nextFireTime')}" key="nextFireTime" type="datetime" width=120 />
</@datatable>
<script type="text/javascript">
/**
 * 查看详细日志
 * @method ec.scheduler.job.lookdetailmessage
 * @public
 */
ec.scheduler.job.lookdetailmessage=function(){
	if(datatable_lookjoblogList.selectedRows.length==0){
			CUI.Dialog.alert("${getText('ec.scheduler.job.choicelog')}");
		return;
	}
	var url='/msService/ec/scheduler/remindLookDetailLog?isProj=${isProj?string('true', 'false')}&schedulerJobLog.code=' + datatable_lookjoblogList.selectedRows[0].code;
	CUI(function(){
		ec.scheduler.job.lookdetailmessage.editDlg = new CUI.Dialog({
			title: "${getText('ec.scheduler.job.lookjoblog')}",
			url:url, 
			modal:true,
			height:390,
			width: 630,
			dragable:true,
			buttons:[{name:"${getText('ec.view.close')}",id:"close",handler:function(){this.close();}}]
		})
		ec.scheduler.job.lookdetailmessage.editDlg.show();
	});
}
</script>