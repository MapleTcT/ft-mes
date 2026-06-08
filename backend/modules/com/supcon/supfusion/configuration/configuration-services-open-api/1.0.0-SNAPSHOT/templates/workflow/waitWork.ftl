
<script type="text/javascript" charset="utf-8" language="javascript">
CUI.ns("ec.waitWork");
ec.waitWork.remind=function(node){
	var id=node['ID'];
	var url="/msService/ec/workflow/remindSet.action?id="+id+"&tableInfoId="+CUI('#pendingTableinfoId').val();
	ec.waitWork.remindDialog = new CUI.Dialog({
		title: "${getHtmlText('ec.pending.remind')}",
		url:url,
		modal:true,
		type:3,
		dragable:true,
		buttons:[
				{	name:"${getHtmlText('common.button.check')}",
					handler:function(){
						ec.waitWork.remindResult();
					}
				},
				{	name:"${getHtmlText('common.button.cancel')}",
					handler:function(){this.close()}
	
				}]
		});
	ec.waitWork.remindDialog.show();

	
}
ec.waitWork.remindResult=function(){
	var pandion=CUI("#pandion").prop("checked");
	var email=CUI("#email").prop("checked");
	var sms=CUI("#sms").prop("checked");
	if(!pandion&&!email&&!sms){
		remindTypeDialogErrorBarWidget.show("${getHtmlText('ec.pengding.nullType')}","f");
	}
	var i=0;
	if(pandion){
		i+=1;
	}
	if(email){
		i+=2;
	}
	if(sms){
		i+=4;
	}
	CUI("#remindTypeId").val(i);
	CUI('#remidnSubmitForm').submit();
}
//回调
waitWork_remindCallBack=function(res){
	if(res.dealSuccessFlag == true){
		remidnSubmitFormDialogErrorBarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
		setTimeout(function(){
			try{ec.waitWork.remindDialog.close();}catch(e){}
		},1500);
	}else{
		remidnSubmitFormDialogErrorBarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","s");
		if(CUI.Dialog) CUI.Dialog.toggleAllButton();
	}
}

</script>
<@s.hidden name="pdTableInfoId" id="pendingTableinfoId" />
<@datatable transMethod="post" dtPage="records"  hidekey="['ID']" id="pendingId" dataUrl="/msService/ec/workflow/pending-data.action" style="margin:10px 10px 10px 10px;" postData="tableInfoId=${pdTableInfoId?default('')}">
	<@datacolumn key="DEPTNAME" label="${getHtmlText('ec.common.deptName')}" width=120/>
	<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.common.pendingOwn')}" width=80/>
	<@datacolumn key="TASKDEC" label="${getHtmlText('ec.pending.activeName')}" width=120/>
	<@datacolumn key="REMIND" label="${getHtmlText('ec.pending.remind')}"  click="ec.waitWork.remind" width=120/>
</@datatable>