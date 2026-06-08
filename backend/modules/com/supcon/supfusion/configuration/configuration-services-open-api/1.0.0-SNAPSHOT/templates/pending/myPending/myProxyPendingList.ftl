	<#--	
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />	
<@errorbar id="myPendingManageErrorBar" />
	-->
<div id="infoContent1" style="padding-top:8px;width:100%;height:95%;" region="center">
		<div id="proxyPendingQuickQueryDiv">
			<form id="proxyQuickQueryForm" onsubmit="return false;">
			<@quickquery formId="proxyQuickQueryForm" isExpandAll=true datatableId="myProxyPendingTable2" fieldcodes="tableNo:ec.pending.tableNo||flowName:ec.pending.flowName||activeName:ec.pending.activeName||summary:ec.property.summary" unique="LAST_QUERY_ec_pending_myPending_myProxyPendingList">
			     <@queryfield formId="proxyQuickQueryForm" code="flowName"  isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="flowName2"  class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxyQuickQueryForm" code="activeName" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="activeName2"  class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxyQuickQueryForm" code="tableNo" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="tableNo2"  class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxyQuickQueryForm" code="summary" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="summary2"  class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
				     <@querybutton formId="proxyQuickQueryForm" type="adv" onclick="ec.myPending2.queryList('user')" onadvancedclick="ec.myPending2.advQuery()" />
					 <@querybutton formId="proxyQuickQueryForm" type="clear" />
				</div>
			</@quickquery>
			</form>
		</div>
		<div id="proxyPendingAdvQueryDiv" style="display:none">
			<form id="proxyAdvQuickForm" onsubmit="return false;">
				<table cellpadding="0"  cellpadding="0"  cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.tableNo')}</td>
						<td width="20%">	
							<div class="fix-input">
					     		<input type="text" id="tableNo2" name="tableNo" class="cui-noborder-input" />
					     	</div>
						</td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.flowName')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="flowName2" name="flowName" class="cui-noborder-input" />
					     	</div>
						</td>
					</tr>
					<tr height="8px"></tr>
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.activeName')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="activeName2" name="activeName" class="cui-noborder-input" />
					     	</div>
						</td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.property.summary')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="summary2" name="summary" class="cui-noborder-input" />
					     	</div>
						</td>
					</tr>
				</table>
			</form>
		</div>
		<#assign url="/msService/ec/pending/myPending/myProxyPendingList?a=1" >
		<@datatable hidekey="['ID','TABLEID']" id="myProxyPendingTable2" dblclick="ec.myPending2.proxyPending" dtPage="myProxyPendingPage" transMethod="post" dataUrl=url style="margin:6px 4px 2px 13px;">
			<@operatebar operates="code:base_proxyPending||iconcls:publish||name:${getHtmlText('ec.proxyPending.proxyPending')}||onclick:ec.myPending2.proxyPending()" operateType="noPower"  resultType="json">
			</@operatebar>
			<@datacolumn label="" checkall="true" textalign="center"  key="checkbox" type="checkbox" width=60  />
			<@datacolumn key="TABLENO" label="${getHtmlText('ec.pending.tableNo')}" width=180 />
			<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width=180 />
			<@datacolumn key="ACTIVETYPE" label="${getHtmlText('ec.pending.pendingType')}" type="select" options="{'0':'${getText('ec.pending.normalPending')}','1':'${getText('ec.pending.normalPending')}','2':'${getText('ec.pending.proxyPending')}'}"  width=100 />
			<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.pending.flowName')}" width=120 />
			<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.pending.activeName')}" width=120 />
			<@datacolumn key="CREATETIME" textalign="center" type="date" label="${getHtmlText('ec.pending.createTime')}" width=100 />
			<@datacolumn key="CREATOR" label="${getHtmlText('ec.pending.inputor')}" width=70 />
			<@datacolumn key="MENUNAME" label="${getHtmlText('ec.pending.menuName')}" width=120 />
		</@datatable>
	

</div>
<script type="text/javascript" charset="utf-8" language="javascript">
(function() {
	//注册命名空间
	CUI.ns("ec.myPending2");
	ec.myPending2.operateType="quickQuery";
	ec.myPending2.proxyPending=function(){
	
		var pendingIds='';
		//var selectedRowsObj=myProxyPendingTable2Widget.getEditData();
		var selectedRowsObj=myProxyPendingTable2Widget.selectedRows;
		if(selectedRowsObj.length==0){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
			return false;
		}
		for(var i=0;i<selectedRowsObj.length;i++){
			var obj=selectedRowsObj[i];
			pendingIds+=","+obj['ID'];
		}
		
		var url="/msService/ec/workflow/proxyPending?openType=frame&pendingIds="+pendingIds.substr(1);
		if( ec.myPending2.proxyPendingDialog ){
			ec.myPending2.proxyPendingDialog._config.url = url;
		}else{
			ec.myPending2.proxyPendingDialog = new CUI.Dialog({
				title: "${getHtmlText('ec.proxyPending.manage')}",
				url:url,
				modal:true,
				type:3,
				
				iframe:'ec_myPending2_proxyPendingDialog_iframe',
				
				dragable:true,
				buttons:[
						{	name:"${getHtmlText('common.button.check')}",
							handler:function(){ec.myPending2.saveProxyPending()}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			})
		}
		ec.myPending2.proxyPendingDialog.show();
	};
	ec.myPending2.saveProxyPending=function(){
		if(ec_myPending2_proxyPendingDialog_iframe.CUI("#proxyUsers_MultiIDs").val()==""){
			ec_myPending2_proxyPendingDialog_iframe.proxyPendIngDialogErrorBarWidget.show("${getHtmlText('ec.proxyPending.proxyor')}","f");
			return ;
		}else{
			ec_myPending2_proxyPendingDialog_iframe.CUI("#proxyUsersInput").val(ec_myPending2_proxyPendingDialog_iframe.CUI("#proxyUsers_MultiIDs").val());
		}
		
		ec_myPending2_proxyPendingDialog_iframe.CUI('#SubmitForm').submit();
	};
	proxyPendingCallBackInfo=function(res){
	
		if(res.dealSuccessFlag == true){
			ec_myPending2_proxyPendingDialog_iframe.proxyPendIngDialogErrorBarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{
					ec.myPending2.proxyPendingDialog.close();	
					if(ec.myPending2.operateType=='quickQuery'){
						ec.myPending2.queryList();
					}
					else{
						ec.myPending2.advQueryList();
					}
				}catch(e){}
			},1500);
		}else{
			ec_myPending2_proxyPendingDialog_iframe.proxyPendIngDialogErrorBarWidget.show("${getHtmlText('ec.common.unsuccessfully')}",'f');
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	};
	ec.myPending2.advQuery=function(){
	     ec.myPending2.operateType="advQuery";
	     CUI(function(){
         ec.myPending2.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"proxyPendingAdvQueryDiv",
            formId:"proxyAdvQuickForm",
            modal:true,
	        type:3,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:"ec.myPending2.advQueryList('user')"},
	                 {name:"<@s.text name='common.button.clear'/>",
	                 handler:function(){CUI.resetForm('proxyAdvQuickForm')}
	                },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
           ec.myPending2.advQueryDialog.show();
      })
	}
	ec.myPending2.advQueryList=function(groupType,pageNo){
	   myProxyPendingTable2Widget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending2.advQueryList('user')"
       });
		var url="/msService/ec/pending/myPending/myProxyPendingList";
		var dataPost=ec.myPending2.getFormData('proxyAdvQuickForm');
		//alert("url: " + url);
		if(pageNo!=undefined){
		   dataPost+="&myProxyPendingPage.pageNo="+pageNo;
		}
		//console.log(dataPost);
		myProxyPendingTable2Widget.setRequestDataUrl(url,dataPost);	
		if(ec.myPending2.advQueryDialog.isShow!=-1){
		   ec.myPending2.advQueryDialog.close();
		}	
	};
	ec.myPending2.queryList=function(groupType,pageNo){
	   ec.myPending2.operateType="quickQuery";
	   myProxyPendingTable2Widget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending2.queryList('user')"
       });
		var url="/msService/ec/pending/myPending/myProxyPendingList";
		var dataPost=ec.myPending2.getFormData('proxyQuickQueryForm');
		//alert("url: " + url);
		if(pageNo!=undefined){
		     dataPost+="&myProxyPendingPage.pageNo="+pageNo;
		}
		dataPost+="&"+proxyQuickQueryForm_getCookieParam();
		myProxyPendingTable2Widget.setRequestDataUrl(url,dataPost);		
	};
	ec.myPending2.getFormData=function(formId){
	    var obj=CUI("#"+formId);
		var dataPost= "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName2",obj).val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName2",obj).val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#tableNo2",obj).val()));
		dataPost += "&summary=" +encodeURIComponent(CUI.trim(CUI("#summary2",obj).val()));
		var pageSize=CUI('input[name="myProxyPendingTable2_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
}) ();
</script>
<#--
-->