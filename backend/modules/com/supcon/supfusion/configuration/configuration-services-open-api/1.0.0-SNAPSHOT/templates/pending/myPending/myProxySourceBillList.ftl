<#assign prefixDealTime = getDefaultDateTime('beforeYear')?date />
<#assign endDealTime = getDefaultDateTime('today')?date />

<div id="infoContent1" style="padding-top:8px;width:100%;height:95%;" region="center">
		<div id="proxySourceBillQuickQueryDiv">
			<form id="proxySourceBillQuickQueryForm" onsubmit="return false;">
			<@quickquery formId="proxySourceBillQuickQueryForm" isExpandAll=true datatableId="myProxyBillTable" fieldcodes="base_staff_dealtime:ec.workflow.pendingTransition.createdTime||tableNo:ec.pending.tableNo||base_staff_name:ec.workflow.createstaff||summary:ec.property.summary" unique="LAST_QUERY_ec_pending_myPending_myProxySourceBillList">
			     <@queryfield formId="proxySourceBillQuickQueryForm" code="base_staff_dealtime" isCustomize=true>
				  	 <div>
				  	 	<div style="float:left;width:45%">
					  	 <@datepicker name="beginTime" id="beginTime" formid="proxySourceBillQuickQueryForm" type="date" cssStyle="float:left;width:90px"  value="${prefixDealTime!}" deValue="${prefixDealTime!}" cssClass="cui-noborder-input" ></@datepicker>
						</div>
						<div style="float:left;width:10%;text-align:center;line-height:26px;">${getHtmlText('foundation.permissionQuery.to')}</div>
						<div style="float:left;width:45%">
						 <@datepicker name="endTime" id="endTime" formid="proxySourceBillQuickQueryForm" type="date" cssStyle="float:left;width:90px"  value="${endDealTime!}" deValue="${endDealTime!}" cssClass="cui-noborder-input" ></@datepicker>
				  		</div>
				  	</div>
			  	 </@queryfield>
			     <@queryfield formId="proxySourceBillQuickQueryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="ec.myPending4.queryList()"></@queryfield>
			     <@queryfield formId="proxySourceBillQuickQueryForm" code="tableNo" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="tableNo4" name="tableNo" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="proxySourceBillQuickQueryForm" code="summary" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="summary4" name="summary" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
				     <@querybutton formId="proxySourceBillQuickQueryForm" type="adv" onclick="ec.myPending4.queryList()" onadvancedclick="ec.myPending4.advQuery()" />
					 <@querybutton formId="proxySourceBillQuickQueryForm" type="clear" />
				</div>
			</@quickquery>
			</form>
		</div>
		<div id="proxySourceBillAdvQueryDiv" style="display:none;">
			<form id="proxySourceBillAdvQueryForm" onsubmit="return false;">
				<table cellpadding="0"  cellpadding="0"  cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.workflow.pendingTransition.createdTime')}${getText('foundation.calendar.from')}</td>
						<td width="20%"><@datepicker name="beginTime" id="beginTime" formid="proxySourceBillAdvQueryForm" type="date" cssStyle="float:left;width:90px"  value="${prefixDealTime!}" deValue="${prefixDealTime!}" cssClass="cui-noborder-input" ></@datepicker></td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.workflow.pendingTransition.createdTime')}${getText('foundation.calendar.to')}</td>
						<td width="20%"><@datepicker name="endTime" id="endTime" formid="proxySourceBillAdvQueryForm" type="date" cssStyle="float:left;width:90px"  value="${endDealTime!}" deValue="${endDealTime!}" cssClass="cui-noborder-input" ></@datepicker></td>
					</tr>
					<tr height="8px"></tr>
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.tableNo')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="tableNo4" name="tableNo" class="cui-noborder-input" />
					     	</div>
						</td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.workflow.createstaff')}</td>
						<td width="20%"><@mneclient id="advConsinor" formId="proxySourceBillAdvQueryForm" name="staff.name" url="other" classStyle="cui-noborder-input" type="Staff" mnewidth=260 multiple=false clicked=true isPrecise=true/></td>
					</tr>
					<tr height="8px"></tr>
					<tr>
					    <td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.property.summary')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" id="summary4" name="summary" class="cui-noborder-input" />
					     	</div>
						</td>
					</tr>
				</table>
			</form>
		</div>
		<#assign url="/msService/ec/pending/myPending/myProxySourceBillList?a=1" >
		<@datatable hidekey="['ID','STA','TARGET_TABLE_NAME','TARGET_ENTITY_CODE']" id="myProxyBillTable" dtPage="myProxySourceBillPage" transMethod="post" dataUrl=url style="margin:6px 4px 2px 13px;">
			<@datacolumn key="TABLE_NO" label="${getHtmlText('ec.workflow.tableNo')}" width=180 click="function(cell){ec.myPending4.viewMyProxySourceBill(cell);};" />
			<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width="180" textalign="left"/>
			<@datacolumn key="NAME" label="${getHtmlText('ec.workflow.name')}" />
			<@datacolumn key="STATUS" label="${getHtmlText('ec.workflow.status')}" width=100 />
			<@datacolumn key="SNAME"  label="${getHtmlText('ec.workflow.createstaff')}" width=100 />
			<@datacolumn key="DNAME"  label="${getHtmlText('ec.workflow.createdepart')}" width=100 />
			<@datacolumn key="CREATE_TIME" type="datetime" label="${getHtmlText('ec.workflow.pendingTransition.createdTime')}" width=130 textalign="center"/>
		</@datatable>
	
</div>
<script type="text/javascript" charset="utf-8" language="javascript">
(function() {
	//注册命名空间
	CUI.ns("ec.myPending4");
		
	ec.myPending4.viewMyProxySourceBill = function(cell){
		var id = cell.ID;
		var entitycode = cell.TARGET_ENTITY_CODE;
		var status = cell.STA;
		var tablename = cell.TARGET_TABLE_NAME;
		var url = "/msService/ec/myWorkflow/openURL";
		var data = "tableInfoId=" + id + "&entityCode=" + entitycode + "&status=" + status + "&targetTablename=" + tablename;
		CUI.ajax({
   		   type: "POST",
   		   url: url,
   		   data:data,
   		   success: function(res){
   			   // alert(res);
   				if(res == "noview"){
   						CUI.Dialog.alert("${getHtmlText('foundation.workflow.noview')}");
   						return false;
   					}
   					var window_height = window.screen.availHeight-63;
   			   		var window_width  = window.screen.availWidth-20;
   			   	    var showStyle = "width=" + window_width + ",height=" + window_height + ",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
   			   		window.open(res, "", showStyle);				   				
   				}
   		});
	}
	
	ec.myPending4.queryList = function(pageNo){
	   myProxyBillTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending4.queryList()"
       });
		var url = "/msService/ec/pending/myPending/myProxySourceBillList";
		var dataPost = ec.myPending4.getFormData('proxySourceBillQuickQueryForm');
		if(pageNo != undefined){
		   dataPost += "&myProxySourceBillPage.pageNo="+pageNo;
		}
		dataPost += "&" + proxySourceBillQuickQueryForm_getCookieParam();
		myProxyBillTableWidget.setRequestDataUrl(url,dataPost);		
	};
	
	ec.myPending4.getFormData = function(formId){
		var dataPost;
	    var obj = CUI("#"+formId);
		dataPost += "&tableNo=" + encodeURIComponent(CUI.trim(CUI("#tableNo4",obj).val()));
		dataPost += "&summary=" + encodeURIComponent(CUI.trim(CUI("#summary4",obj).val()));
		dataPost += "&consinor=" + encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",obj).val()));
		dataPost += "&startDate=" + encodeURIComponent(CUI.trim(CUI("input[name='beginTime']",obj).val()));
		dataPost += "&endDate=" + encodeURIComponent(CUI.trim(CUI("input[name='endTime']",obj).val()));
		var pageSize = CUI('input[name="myProxyBillTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
	
	ec.myPending4.advQuery = function(){
		CUI(function(){
			ec.myPending4.advQueryDialog = new CUI.Dialog({
	            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
	            elementId:"proxySourceBillAdvQueryDiv",
	            formId:"proxySourceBillAdvQueryForm",
	            modal:true,
		        type:3,
		        dragable:true,
				buttons:[{name:"<@s.text name='foundation.common.query'/>",
					handler:"ec.myPending4.advQueryList()"},
				{name:"<@s.text name='common.button.clear'/>",
					handler:function(){CUI.resetForm('proxySourceBillAdvQueryForm')}
		                },
				{name:"<@s.text name='common.button.cancel'/>",
					handler:function(){this.close()}
				}]
		      
			});
			ec.myPending4.advQueryDialog.show();
		})
	}
	
	ec.myPending4.advQueryList = function(pageNo){
	    myProxyBillTableWidget.setAttributeConfig('queryFunc',{
			writeOnce: true,
			value:"ec.myPending4.advQueryList()"
        });
		var url="/msService/ec/pending/myPending/myProxySourceBillList";
		var dataPost=ec.myPending4.getFormData('proxySourceBillAdvQueryForm');
		if(pageNo!=undefined){
		   dataPost+="&myProxySourceBillPage.pageNo="+pageNo;
		}
		myProxyBillTableWidget.setRequestDataUrl(url,dataPost);	
		if(ec.myPending4.advQueryDialog.isShow != -1){
		   ec.myPending4.advQueryDialog.close();
		}	
	};
}) ();
</script>
