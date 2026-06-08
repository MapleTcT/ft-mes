	<#--
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />	
<@errorbar id="myPendingManageErrorBar" />
	-->
<div id="infoContent1" style="padding-top:8px;width:100%;height:95%;" region="center">
		<div id="proxySourceQuickQueryDiv">
			<form id="proxySourceQuickQueryForm" onsubmit="return false;">
			<@quickquery formId="proxySourceQuickQueryForm" isExpandAll=true datatableId="myProxyPendingTable3" fieldcodes="tableNo:ec.pending.tableNo||base_staff_name:ec.expectedConsign.consinger||activeName:ec.pending.activeName||summary:ec.property.summary" unique="LAST_QUERY_ec_pending_myPending_myProxySourcePendingList">
			     <@queryfield formId="proxySourceQuickQueryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="ec.myPending3.queryList('user')"></@queryfield>
			     <@queryfield formId="proxySourceQuickQueryForm" code="activeName" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="activeName3" name="activeName" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxySourceQuickQueryForm" code="tableNo" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="tableNo3" name="tableNo" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxySourceQuickQueryForm" code="summary" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="summary3" name="summary3" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
				     <@querybutton formId="proxySourceQuickQueryForm" type="adv" onclick="ec.myPending3.queryList('user')" onadvancedclick="ec.myPending3.advQuery()" />
					 <@querybutton formId="proxySourceQuickQueryForm" type="clear" />
				</div>
			</@quickquery>
			</form>
		</div>
		<div id="proxySourceAdvQueryDiv" style="display:none;">
			<form id="proxySourceAdvQueryForm" onsubmit="return false;">
				<table cellpadding="0"  cellpadding="0"  cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.tableNo')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="tableNo3" name="tableNo" class="cui-noborder-input" />
					     	</div>
						</td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.expectedConsign.consinger')}</td>
						<td width="20%"><@mneclient id="advConsinor" formId="proxySourceAdvQueryForm" name="staff.name" url="other" classStyle="cui-noborder-input" type="Staff" mnewidth=260 multiple=false clicked=true isPrecise=true/></td>
					</tr>
					<tr height="8px"></tr>
					<tr>
					    <td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.activeName')}</td>
						<td width="20%">	
							<div class="fix-input">
					     		<input type="text" id="activeName3" name="activeName" class="cui-noborder-input" />
					     	</div>
						</td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.property.summary')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="summary3" name="summary" class="cui-noborder-input" />
					     	</div>
						</td>
					</tr>
				</table>
			</form>
		</div>
		<#assign url="/msService/ec/pending/myPending/myProxySourcePendingList?a=1" >
		<@datatable hidekey="['ID','TABLEID']" id="myProxyPendingTable3" dtPage="myProxySourcePendingPage" transMethod="post"  dataUrl=url style="margin:6px 4px 2px 13px;">
			<@operatebar operates="code:cancalExpectedConsign3||iconcls:del||name:${getHtmlText('ec.proxyPending.cancel')}||onclick:ec.myPending3.ExpectedConsignManage('cancal')" operateType="noPower"  resultType="json">
			</@operatebar>
			<@datacolumn key="checkbox" textalign="center" type="checkbox" checkall="true" label="" width=60 />
			<@datacolumn key="TABLENO" label="${getHtmlText('ec.pending.tableNo')}" width=180 />
			<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width=180 />
			<@datacolumn key="ACTIVETYPE" label="${getHtmlText('ec.pending.pendingType')}" type="select" options="{'0':'${getText('ec.pending.normalPending')}','1':'${getText('ec.pending.normalPending')}','2':'${getText('ec.pending.proxyPending')}'}"  width=100 />
			<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.pending.flowName')}" width=120 />
			<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.pending.activeName')}" width=120 />
			<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.pending.owner')}" width=70 />
			<@datacolumn key="DEPTNAME"  label="${getHtmlText('ec.pending.ownerDept')}" width=110 />
			<@datacolumn key="CREATETIME" textalign="center" type="date" label="${getHtmlText('ec.pending.createTime')}" width=100 />
			<@datacolumn key="CREATOR" label="${getHtmlText('ec.pending.inputor')}" width=70 />
			<@datacolumn key="MENUNAME" label="${getHtmlText('ec.pending.menuName')}" width=120 />
		</@datatable>
	
</div>
<script type="text/javascript" charset="utf-8" language="javascript">
(function() {
	//注册命名空间
	CUI.ns("ec.myPending3");
		
	ec.myPending3.refleshDataTable=function(){
		myProxyPendingTable3Widget.setRequestDataUrl("/msService/ec/pending/myPending/myProxySourcePendingList");
	}
	ec.myPending3.ExpectedConsignManage=function(strType){
		var selectedRowsObj=myProxyPendingTable3Widget.selectedRows;
		
		if(selectedRowsObj.length==0){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.myPending.noSelected')}","f");
			return false;
		}
		//if(selectedRowsObj.length>1){
			//workbenchErrorBarWidget.showMessage("${getText('ec.pending.selecteOnlyOne')}","f");
			//return false;
		//}
		//var id = selectedRowsObj[0].ID;
		//url="/msService/ec/pending/myPending/myProxyPendingCancal?id=" + id;
		//确认删除
		var ids = "";
		for(var k = 0; k < selectedRowsObj.length; ++ k) {
			ids += "," + selectedRowsObj[k].ID;
		}
		url="/msService/ec/pending/myPending/myProxyPendingCancal?ids=" + ids.substr(1);
		CUI.Dialog.confirm("${getHtmlText('ec.expectedConsign.checkForm')}",function(){
			CUI.post(url, null, ec.myPending3.deleteCallBack, "json");
		})
		
		
	}
	ec.myPending3.deleteCallBack=function(res){
		if(res.dealSuccessFlag == true){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			ec.myPending3.refleshDataTable();
		}else{
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.unsuccessfully')}","f");
		}
	}
	ec.myPending3.advQuery=function(){
	     CUI(function(){
         ec.myPending3.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"proxySourceAdvQueryDiv",
            formId:"proxySourceAdvQueryForm",
            modal:true,
	        type:3,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:"ec.myPending3.advQueryList('user')"},
	                {name:"<@s.text name='common.button.clear'/>",
	                 handler:function(){CUI.resetForm('proxySourceAdvQueryForm')}
	                },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
           ec.myPending3.advQueryDialog.show();
      })
	}
	ec.myPending3.advQueryList=function(groupType,pageNo){
	    myProxyPendingTable3Widget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending3.advQueryList('user')"
        });
		var url="/msService/ec/pending/myPending/myProxySourcePendingList";
		var dataPost=ec.myPending3.getFormData('proxySourceAdvQueryForm');
		//alert("url: " + url);
		if(pageNo!=undefined){
		   dataPost+="&myProxySourcePendingPage.pageNo="+pageNo;
		}
		myProxyPendingTable3Widget.setRequestDataUrl(url,dataPost);	
		if(ec.myPending3.advQueryDialog.isShow !=-1){
		   ec.myPending3.advQueryDialog.close();
		}	
	};
	ec.myPending3.queryList=function(groupType,pageNo){
	   myProxyPendingTable3Widget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending3.queryList('user')"
       });
		var url="/msService/ec/pending/myPending/myProxySourcePendingList";
		var dataPost=ec.myPending3.getFormData('proxySourceQuickQueryForm');
		//alert("url: " + url);
		if(pageNo!=undefined){
		   dataPost+="&myProxySourcePendingPage.pageNo="+pageNo;
		}
		dataPost+="&"+proxySourceQuickQueryForm_getCookieParam();
		myProxyPendingTable3Widget.setRequestDataUrl(url,dataPost);		
	};
	ec.myPending3.getFormData=function(formId){
	    var obj=CUI("#"+formId);
		var dataPost= "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName3",obj).val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName3",obj).val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#tableNo3",obj).val()));
		dataPost += "&summary=" +encodeURIComponent(CUI.trim(CUI("#summary3",obj).val()));
		dataPost += "&consinor=" +encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",obj).val()));
		var pageSize=CUI('input[name="myProxyPendingTable3_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
}) ();
</script>

<#--
-->