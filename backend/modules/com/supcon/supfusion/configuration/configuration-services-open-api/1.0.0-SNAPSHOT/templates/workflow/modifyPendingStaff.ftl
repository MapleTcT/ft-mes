<@errorbar id="modifyPendingFrameErrorbar" />
<@s.form id="createPendingsStaffForm">
<@s.hidden name="addPendingUserIds" id="addPendingUserIds" />
<@s.hidden name="deletePendingIds" id="deletePendingIds" />
<@s.hidden name="tableInfoId" id="tableInfoId" />
<@datatable transMethod="post" formId="createPendingsStaffForm"  dtPage="records"  hidekey="['ID','USERID','TASKNAME']" id="modifyPendingList" dataUrl="/msService/ec/workflow/pending-data.action" style="margin:10px 10px 5px 20px;" postData="tableInfoId=${tableInfoId?default('-1')}">
	<@operatebar operates="code:pending_staff_add||iconcls:add||name:${getHtmlText('foundation.buttonPropertyshowName.radion1318218547065')}||onclick:ec.waitWork.add();code:pending_staff_delete||iconcls:del||name:${getHtmlText('foundation.buttonPropertyshowName.radion1318218860647')}||onclick:ec.waitWork.del()"  operateType="noPower"  resultType="json">
	</@operatebar>
	<@datacolumn label="" checkall="true" textalign="center"  key="checkbox" type="checkbox" width=60  />
	<@datacolumn key="DEPTNAME" label="${getHtmlText('ec.common.deptName')}" width=120/>
	<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.common.pendingOwn')}" width=80/>
	<@datacolumn key="TASKDEC" label="${getHtmlText('ec.pending.activeName')}" width=120/>
</@datatable>
</@s.form>
<script type="text/javascript" charset="utf-8" language="javascript">

CUI.ns("ec.waitWork");

ec.waitWork.add=function(){
				ec.waitWork.createPendingDialog = new CUI.Dialog({
				title: '${getText("ec.pending.add")}',
				url: "/msService/ec/workflow/addPendingFrame.action?tableInfoId=${tableInfoId?default('')}&pendingId=${pendingId!?default('')}",
				modal:true,
				type:3,
				buttons:[
				{	name:"${getText('foundation.common.save')}",
							handler:function(){ec.waitWork.submitForm();}
						},
						{	name:"${getText('foundation.common.closed')}",
							handler:function(){this.close()}
						}]
			});
			ec.waitWork.createPendingDialog.show();
}
ec.waitWork.submitForm=function(){ 
	var userIds=CUI("#createPendingUserIdsMultiIDs").val();
	if(userIds!=""){
		userIds=userIds.substr(0,userIds.length-1);
	}
	var tableInfoId=CUI("#tableInfoId").val();
	var activityName=CUI("#activeName").val();
	if(userIds==""){
		createPendingsFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.createPending.userStaffsnull')}","f");
		return ;
	}
	var url="/msService/ec/workflow/addPendingResult.action?tableInfoId="+tableInfoId+"&userIds="+userIds+"&activityName="+activityName;
	CUI.ajax({
		      type:"POST",
		      url:url,
		      success:function(res){
		       	if(res.dealSuccessFlag==true){
						createPendingsFormDialogErrorBarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
						ec.waitWork.createPendingDialog.close();
						ec.waitWork.refreshList();
				}
		      },
		      error:function(XMLHttpRequest, textStatus, errorThrown){
		      	var msg = CUI.parseJSON(XMLHttpRequest.responseText);
				CUI.showErrorInfos(msg,createPendingsFormDialogErrorBarWidget);
		      }
		  });
	
}
ec.waitWork.del=function(){
		var row=modifyPendingListWidget.selectedRows;
		if(row.length==0){
			modifyPendingFrameErrorbarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
			return false;
		}
		var ids="";
		for(var i=0;i<row.length;i++){
			ids+=","+row[i].ID;
		}
		if(ids!=""){
			ids=ids.substr(1);
		}
		CUI.Dialog.confirm("${getText('ec.pending.delete.confirm')}",function(){
		var checkPowerUrl="/msService/ec/workflow/checkDeletePending.action?pendingIds="+ids;
		CUI.ajax({
		      type:"POST",
		      url:checkPowerUrl,
		      data:"",
		      success:function(res){
		       	if(res.dealSuccessFlag==false){
						modifyPendingFrameErrorbarWidget.showMessage("${getHtmlText('com.supcon.orchid.container.exceptions.NO_PENDING')}","f");
				}else{
					var url="/msService/ec/workflow/deletePending.action?pendingIds="+ids;
					CUI.post(url, null, ec.waitWork.deleteCallBack, "json");
				}
		      }
		  });
		})
		
}
ec.waitWork.deleteCallBack=function(res){
	if(res.dealSuccessFlag==true){
		modifyPendingFrameErrorbarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
		ec.waitWork.refreshList();
	}
}
ec.waitWork.refreshList=function(){
	var dataPost="tableInfoId=${tableInfoId?default('-1')}";
	modifyPendingListWidget.setRequestDataUrl("/msService/ec/workflow/pending-data.action",dataPost);

}


</script>