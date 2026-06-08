	<#--
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />	
<@errorbar id="expectedConsignEditErrorbar" />
	-->
	
	<div id="infoContent1" style="padding-top:8px;width:100%;height:95%;" region="center">
		<div id="expectedPendingQuikcQueryDiv">
			<form id="expectedQuickQueryForm" onsubmit="return false;">
			<@quickquery formId="expectedQuickQueryForm" isExpandAll=true datatableId="myExpectedConsignTable"  fieldcodes="base_staff_name:ec.expectedConsign.consinger||activeName:ec.pending.activeName1||flowName:ec.pending.flowName" unique="LAST_QUERY_ec_pending_myPending_myExpectedConsignList">
				     <@queryfield formId="expectedQuickQueryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="ec.myPending.queryList()"></@queryfield>
				     <@queryfield formId="expectedQuickQueryForm" code="activeName" isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" name="activeName" id="activeName1" class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <@queryfield formId="expectedQuickQueryForm" code="flowName" isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" name="flowName" id="flowName1"  class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
					    <@querybutton formId="expectedQuickQueryForm" type="adv" onclick="ec.myPending.queryList()" onadvancedclick="ec.myPending.advQuery()" />
					 	<@querybutton formId="expectedQuickQueryForm" type="clear" />
				 	</div>
			</@quickquery>
				
			</form>
		</div>
		<div id="expectedPendingAdvQueryDiv" style="display:none">
		   <form id="expectedAdvQueryForm" onsubmit="return false;">
				<table cellpadding="0"  cellpadding="0"  cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.expectedConsign.consinger')}</td>
						<td width="20%"><@mneclient name="staff.name" formId="expectedAdvQueryForm" id="consinor1" url="other" classStyle="cui-noborder-input" type="Staff" mnewidth=260 multiple=false clicked=true isPrecise=true/></td>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.flowName')}</td>
						<td width="20%">
							<div class="fix-input">
					     		<input type="text" name="flowName" id="flowName1"  class="cui-noborder-input" />
					     	</div>
						</td>
					</tr>
					<tr height="8px"></tr>
					<tr>
						<td width="10%" align="right" style="padding-right: 3px;">${getHtmlText('ec.pending.activeName1')}</td>
						<td width="20%">		
							<div class="fix-input">
					     		<input type="text" name="activeName" id="activeName1" class="cui-noborder-input" />
					     	</div>	
						</td>
					</tr>
				</table>
			</form>
		</div>
		<#assign url="/msService/ec/pending/myPending/myExpectedConsignList?a=1" >
		<@datatable dtPage="myExpectedConsignPage" dblclick="ec.myPending.ExpectedConsignClick" id="myExpectedConsignTable" hidekey="['ID', 'TYPE']"   transMethod="post"  dataUrl=url style="margin:6px 4px 2px 13px;">
		 	<@operatebar operates="code:addExpectedConsign1||iconcls:add||name:${getHtmlText('ec.pending.add')}||onclick:ec.myPending.ExpectedConsignManage('add');	code:addExpectedConsign1||iconcls:edit||name:${getHtmlText('ec.pending.edit')}||onclick:ec.myPending.ExpectedConsignManage('modify');code:addExpectedConsign1||iconcls:del||name:${getHtmlText('ec.pending.cancel')}||onclick:ec.myPending.ExpectedConsignManage('cancal')" operateType="noPower"  resultType="json">
			</@operatebar>
		 	<@datacolumn label="" checkall="true" textalign="center"  key="checkbox" type="checkbox" width=60  />
			<@datacolumn key="STAFFNAME"  label="${getHtmlText('ec.proxyPending.proxySources')}" width=100 />
			<@datacolumn key="CREATEDATE" textalign="center" type="date" label="${getHtmlText('ec.expectedConsign.createDate')}" width=100 />
			<@datacolumn key="STARTDATE" textalign="center"  type="date" label="${getHtmlText('ec.expectedConsign.startDate')}" width=100 />
			<@datacolumn key="ENDDATE" textalign="center" type="date" label="${getHtmlText('ec.expectedConsign.endDate')}" width=100 />
			<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.flowActive.flowName')}" width=100/>
			<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.flowActive.activeName')}" width=100 />
			<@datacolumn key="MEMO" label="${getHtmlText('ec.expectedConsign.memo')}" width=100 />
		</@datatable>
	</div>


<script type="text/javascript" charset="utf-8" language="javascript">
(function() {
	//注册命名空间
	CUI.ns("ec.myPending");
	ec.myPending.strType="";
	ec.myPending.operateType='quickQuery';
	ec.myPending.refleshDataTable=function(){
		myExpectedConsignTableWidget.setRequestDataUrl("/msService/ec/pending/myPending/myExpectedConsignList?a=1");
	}
	ec.myPending.ExpectedConsignClick=function(){
		ec.myPending.ExpectedConsignManage('modify');
	}
	ec.myPending.ExpectedConsignManage=function(strType){
		ec.myPending.strType = strType;
		var height=500;
		var width=650;
		var id = "";
		var staffName="";
		var flowName="";
		var activeName="";
		var url="";
		url="/msService/ec/pending/expectedConsignEdit?actionMode=myself&openType=frame";
		if(strType != 'add'){
			//var selectedRowsObj=myExpectedConsignTableWidget.getEditData();
			var selectedRowsObj=myExpectedConsignTableWidget.selectedRows;
			if(selectedRowsObj.length==0){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.myPending.noSelected')}","f");
				return false;
			}
			if(selectedRowsObj.length>1 && strType!='cancal' ){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pending.selecteOnlyOne')}","f");
				return false;
			}
			id=selectedRowsObj[0].ID;
			activeName=selectedRowsObj[0].ACTIVENAME;
			flowName=selectedRowsObj[0].FLOWNAME;
			staffName=selectedRowsObj[0].STAFFNAME;
			url="/msService/ec/pending/expectedConsignModify";
		}
		if(strType=='modify'){
			var selectedRowsObj=myExpectedConsignTableWidget.selectedRows;
			if(selectedRowsObj[0].TYPE == "ALL"){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pending.changeSelectAll')}","f");
				return false;
			}
			url=url+"?actionMode=myself&openType=frame&id="+id+"&staffName="+encodeURIComponent(staffName)+"&flowName="+encodeURIComponent(flowName)+"&activeName="+encodeURIComponent(activeName);
			if( !ec.myPending.expCondialog ){
				ec.myPending.expCondialog = new CUI.Dialog({
					title: "${getHtmlText('ec.myExConsign.manage')}",
					url:url,
					modal:true,
					type:3,
					
					form:'expecteSubmitForm',
					iframe: 'ec_myPending_expCondialog_iframe',
					
					dragable:true,
					buttons:[
							{	name:"${getHtmlText('common.button.check')}",
								handler:function(){
									ec_myPending_expCondialog_iframe.ec_myPending_saveSignleExpCon()
								}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}]
				})
			}else{
				ec.myPending.expCondialog._config.url = url;
			}
			ec.myPending.expCondialog.show();
		}
		if(strType=='add'){
			if( !ec.myPending.expCondialog_add ){
				ec.myPending.expCondialog_add = new CUI.Dialog({
					title: "${getHtmlText('ec.myExConsign.manage')}",
					url:url,
					modal:true,
					type:4,
					
					form:'expecteSubmitForm',
					iframe: 'ec_myPending_expCondialog_add_iframe',
					
					
					dragable:true,
					buttons:[
							{	name:"${getHtmlText('common.button.check')}",
								handler:function(){
									ec_myPending_expCondialog_add_iframe.ec_myPending_saveExpCon()
								}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}]
				})
			}
			ec.myPending.expCondialog_add.show();
		}	
		if(strType=='cancal'){
			url="/msService/ec/pending/expectedConsignCancal";
			var ids="";
			var selectedRowsObj=myExpectedConsignTableWidget.selectedRows;
			if(selectedRowsObj.length>1){
				ids=id;
				for(var i=1; i<selectedRowsObj.length; i++){
					ids=ids+","+selectedRowsObj[i].ID;
				}
				url=url+"?ids="+ids;
			}else{
				url=url+"?id="+id;
			}
			//确认删除
			if(!confirm("${getText('ec.expectedConsign.checkForm')}")) return false;
			CUI.post(url, null, ec.myPending.deleteCallBack, "json");
			
			return false;
		}
	}
	ec.myPending.deleteCallBack=function(res){
		if(res.dealSuccessFlag == true){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			ec.myPending.refleshDataTable();
		}else{
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.unsuccessfully')}","f");
		}
	}
	ec.myPending.saveSignleExpCon=function(){
		if(CUI('#userStaffName').val()==""){
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
			return ;
		}
		if(CUI('#startTime').val()==""){
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimenull')}","f");
			return ;
		}
		if(CUI('#endTime').val()==""){
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.endTimenull')}","f");
			return ;
		}
		if(CUI('#m_FlowActiveCode').val()==""){
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
			return ;
		}
		CUI('#expecteSubmitForm').submit();
	}
	
	ec.myPending.saveExpCon=function(){
		if(CUI('#expectedUserId')&&CUI('#expectedUserId').val()==""){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.expectedConsign.consingorStaffNull')}","f");
			return ;
		}
		if(CUI('#flowInfos').val()=="" && $('#selectAll').prop("checked")==false){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
			return ;
		}
		if(CUI('#consigners').val()==""){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
			return ;
		}
		if(CUI('#startTime').val()==""){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimenull')}","f");
			return ;
		}
		if(CUI('#endTime').val()==""){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.endTimenull')}","f");
			return ;
		}
		if(CUI('#startTime').val()>CUI('#endTime').val()){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimegtendTimenull')}","f");
			return ;
		}
		CUI('#expecteSubmitForm').submit();
	}
	//预期委托保存后的回调函数(修改)
	expectedConsign_expConcallBackInfo1=function(res){
		if(res.dealSuccessFlag == true){
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{ec.myPending.expCondialog.close();}catch(e){}
				try{ec.myPending.expCondialog_add.close();}catch(e){}
				if(ec.myPending.queryType=='quickQuery'){
					ec.myPending.queryList();
				}
				else{
				   ec.myPending.advQueryList();
				}
			},1500);
		}else{
			ec_myPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	}
	//新增预期委托保存后的回调函数
	expectedConsign_expConcallBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{ec.myPending.expCondialog.close();}catch(e){}
				try{ec.myPending.expCondialog_add.close();}catch(e){}
				ec.myPending.refleshDataTable();
			},1500);
		}else{
			ec_myPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
			if(CUI.Dialog) 
				CUI.Dialog.toggleAllButton();
		}
	}
	ec.myPending.advQuery=function(){
	    ec.myPending.operateType='advQuery';
	     CUI(function(){
         ec.myPending.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"expectedPendingAdvQueryDiv",
            formId:"expectedAdvQueryForm",
            modal:true,
	        type:3,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:'ec.myPending.advQueryList()'},
	                {name:"<@s.text name='common.button.clear'/>",
	                 handler:function(){CUI.resetForm('expectedAdvQueryForm')}
	                },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
          ec.myPending.advQueryDialog.show();
      })
	}
	// 点击查询按钮，查到数据,显示在下面的datatable中
	ec.myPending.queryList=function(type,pageNo){
	   ec.myPending.operateType='quickQuery';
	   myExpectedConsignTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending.queryList(1)"
       });
		var url="/msService/ec/pending/myPending/myExpectedConsignList";
		var dataPost=ec.myPending.getFormData('expectedQuickQueryForm');
		if(pageNo!=undefined){
		   dataPost+="&myExpectedConsignPage.pageNo="+pageNo;
		}
		  dataPost+="&"+expectedQuickQueryForm_getCookieParam();
		myExpectedConsignTableWidget.setRequestDataUrl(url,dataPost);		
	};
	ec.myPending.advQueryList=function(type,pageNo){
	   myExpectedConsignTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.myPending.advQueryList(1)"
       });
		var url="/msService/ec/pending/myPending/myExpectedConsignList";
		var dataPost=ec.myPending.getFormData('expectedAdvQueryForm');
		if(pageNo!=undefined){
		   dataPost+="&myExpectedConsignPage.pageNo="+pageNo;
		}
		myExpectedConsignTableWidget.setRequestDataUrl(url,dataPost);
		try{
		if(ec.myPending.advQueryDialog.isShow!=-1){
		    ec.myPending.advQueryDialog.close();
		}		
		}catch(e){}
	};
	ec.myPending.getFormData=function(formId){
	    var obj=CUI("#"+formId);
		var dataPost = "";
		dataPost += "&consinor=" +encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",obj).val()));
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName1",obj).val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName1",obj).val()));
		var pageSize=CUI('input[name="myExpectedConsignTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
}) ();
</script>

<#--
-->