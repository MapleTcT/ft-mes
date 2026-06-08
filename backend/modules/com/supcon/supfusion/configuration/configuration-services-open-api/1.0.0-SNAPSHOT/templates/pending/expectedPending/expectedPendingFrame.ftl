<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.expectedConsign.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />
<@errorbar id="expectedPendingFrameErrorbar" />
<@frameset id="workbenchOperateBar" >
     <@frame id="infoContent" region="center" offsetH=4>
     	<div id="pendingQuickQueryDiv">
			<form id="quickQueryForm" onsubmit="return false;">
				<@quickquery formId="quickQueryForm" isExpandAll=true datatableId="expectedConsignTable" fieldcodes="base_staff_name:ec.expectedConsign.consinger||staffName:ec.expectedConsign.consingerUser||flowName:ec.pending.flowName" unique="LAST_QUERY_ec_pending_expectedPending_expectedList" >
				     <@queryfield formId="quickQueryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="ec.expectedPending.queryList()"></@queryfield>
				     <@queryfield formId="quickQueryForm" code="staffName" isCustomize=true>
				     	<@mneclient iframe=true name="is.staff.name" formId="quickQueryForm" classStyle="cui-noborder-input" id="is_staff_name" url="other" cssStyle="padding-left: 0px; width: 95%;" type="Staff"  mnewidth=260 multiple=false clicked=true isPrecise=true searchClick="ec.expectedPending.queryList()"/>
				     </@queryfield>
				     <@queryfield formId="quickQueryForm" code="flowName" isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" id="flowName"  class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
				     	<@querybutton formId="quickQueryForm" type="adv" onclick="ec.expectedPending.queryList()" onadvancedclick="ec.expectedPending.advQuery()" />
				     	<@querybutton formId="quickQueryForm" type="clear" />
				     </div>
				 </@quickquery>
			</form>
		</div>
		<div id="pendingAdvQueryDiv" style="display:none">
		   <form id="advQueryForm" onsubmit="return false;">
		   		<table cellpadding="0"  cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
					<tr>
						<td width="10%" class="lab">${getHtmlText('ec.expectedConsign.consinger')}</td>
						<td width="20%"><@mneclient name="staff.name" formId="advQueryForm" classStyle="cui-noborder-input" id="advConsinor" url="other" type="Staff"  mnewidth=260 multiple=false clicked=true isPrecise=true/></td>
						<td width="10%" class="lab">${getHtmlText('ec.expectedConsign.consingerUser')}</td>
						<td width="20%"><@mneclient name="is.staff.name"  formId="advQueryForm" classStyle="cui-noborder-input" id="advStaffName" url="other" cssStyle="width:95%;" type="Staff"  mnewidth=260 multiple=false clicked=true isPrecise=true/></td>
					</tr>
					<tr height="8px"></tr>
					<tr>
						<td width="10%" class="lab">${getHtmlText('ec.pending.flowName')}</td>
						<td width="20%"><div class="fix-input"><input type="text" id="flowName"  class="cui-noborder-input"></input></div></td>
					</tr>
				</table>
		   </form>
		</div>
		<@datatable dtPage="records" dblclick="ec.expectedPending.expectedListClick" firstLoad=false id="expectedConsignTable" hidekey="['ID', 'TYPE']" transMethod="post" dataUrl="/msService/ec/pending/expectedPending/expectedList" style="margin:6px 4px 2px 13px;">
				<@operatebar operates="code:base_expectedPending_add||iconcls:add||name:${getHtmlText('ec.expectedConsign.newExpectedPending')}||onclick:ec.expectedPending.expectedPendingManage('add');
					code:base_expectedPending_mod||iconcls:edit||name:${getHtmlText('ec.expectedConsign.modify')}||onclick:ec.expectedPending.expectedPendingManage('modify');
					code:base_expectedPending_del||iconcls:del||name:${getHtmlText('ec.expectedConsign.del')}||onclick:ec.expectedPending.expectedPendingManage('cancal')" resultType="JSON" operateType="noPower">
				</@operatebar> 
				<@datacolumn textalign="center" label="" checkall="true"  key="checkbox" type="checkbox" width=60  />
				<@datacolumn key="STAFFNAME2" label="${getHtmlText('ec.expectedConsign.consingerUser')}" />
				<@datacolumn key="STAFFNAME" label="${getHtmlText('ec.proxyPending.proxySources')}" />
				<@datacolumn textalign="center" key="CREATEDATE" type="date" label="${getHtmlText('ec.expectedConsign.createDate')}" />
				<@datacolumn textalign="center" key="STARTDATE" type="date" label="${getHtmlText('ec.expectedConsign.startDate')}" />
				<@datacolumn textalign="center" key="ENDDATE" type="date" label="${getHtmlText('ec.expectedConsign.endDate')}" />
				<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.flowActive.flowName')}" />
				<@datacolumn key="ACTIVENAME" label="${getHtmlText('ec.flowActive.activeName')}" />
				<@datacolumn key="MEMO" label="${getHtmlText('ec.expectedConsign.memo')}" />
		</@datatable>
		
	</@frame>
</@frameset>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.expectedPending");
	ec.expectedPending.operateType="";
	ec.expectedPending.queryType='quickQuery';
	
	ec.expectedPending.expectedListClick=function(){
		ec.expectedPending.expectedPendingManage('modify');
	}
	ec.expectedPending.expectedPendingManage=function(strType){
		
		ec.expectedPending.operateType = strType;
		var height=500;
		var width=650;
		var id = "";
		var staffName="";
		var flowName="";
		var activeName="";
		var consignorStaffName="";
		var url="";
		url="/msService/ec/pending/expectedConsignEdit?actionMode=manage&openType=frame";
		if(strType=='cancal'){
			var ids="";
			var so=expectedConsignTableWidget.selectedRows;
			
			if(so.length==0){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
				return false;
			}
			for(var i=0;i<so.length;i++){
				var obj=so[i];
				ids+=","+obj['ID'];
			}
			
			url="/msService/ec/pending/expectedConsignCancal";
			url+="?ids="+ids.substr(1);
			CUI.Dialog.confirm("${getHtmlText('ec.expectedConsign.checkForm')}",function(){
				CUI.post(url, null, ec.expectedPending.deleteCallBack, "json");
			})
			return false;
		}
		
		if(strType=='add'){
			if( !ec.expectedPending.expCondialog_add ){
				ec.expectedPending.expCondialog_add = new CUI.Dialog({
					title: "${getHtmlText('ec.expectedConsign.manage')}",
					url:url,
					modal:true,
					type:4,
					dragable:true,
					
					iframe:'ec_expectedPending_expCondialog_add_iframe',
					formId:'expecteSubmitForm',
					
					buttons:[
							{	name:"${getHtmlText('common.button.check')}",
								handler:function(){
									ec_expectedPending_expCondialog_add_iframe.foundation.expectedConsign.saveExpCon()		
								}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}]
				})
			}
			ec.expectedPending.expCondialog_add.show();
			
		}
		
		if(strType=='modify'){
			var selectedRowsObj=expectedConsignTableWidget.selectedRows;
			if(selectedRowsObj.length==0){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
				return false;
			}
			if(selectedRowsObj.length>1){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pending.selecteOnlyOne')}","f");
				return false;
			}
			if(selectedRowsObj[0].TYPE == "ALL"){
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pending.changeSelectAll')}","f");
				return false;
			}
			//alert("selectedRowsObj: " + selectedRowsObj);
			url="/msService/ec/pending/expectedConsignModify";
			for(var i=0;i<selectedRowsObj.length;i++){
				var obj=selectedRowsObj[i];
				id=obj['ID'];
				activeName=obj['ACTIVENAME'];
				flowName=obj['FLOWNAME'];
				staffName=obj['STAFFNAME'];
				//consignorStaffName=obj['consignorStaffName'];
			}
			url=url+"?actionMode=manage&openType=frame&id="+id+"&staffName="+encodeURIComponent(staffName)+"&flowName="+encodeURIComponent(flowName)+"&activeName="+encodeURIComponent(activeName);
			
			if( !ec.expectedPending.expCondialog ){
				ec.expectedPending.expCondialog = new CUI.Dialog({
					title: "${getHtmlText('ec.expectedConsign.manage')}",
					url:url,
					modal:true,
					type:3,
					dragable:true,
					
					iframe:'ec_expectedPending_expCondialog_iframe',
					
					buttons:[
							{	name:"${getHtmlText('common.button.check')}",
								handler:function(){
									ec_expectedPending_expCondialog_iframe.foundation.expectedConsign.saveSignleExpCon()
								}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}]
				})
			}else{
				ec.expectedPending.expCondialog._config.url = url;
			}
			ec.expectedPending.expCondialog.show();
		
		}
	
	}
	ec.expectedPending.deleteCallBack=function(res){
		
		if(res.dealSuccessFlag == true){
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}",'s');
			if(ec.expectedPending.queryType=='quickQuery'){
				ec.expectedPending.queryList();
			}
			else{
			   ec.expectedPending.advQueryList();
			}
		}else{
			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.unsuccessfully')}",'f');
		}
	}
	//预期委托保存后的回调函数(修改)
	expectedConsign_expConcallBackInfo1=function(res){
		if(res.dealSuccessFlag == true){
			ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{ec.expectedPending.expCondialog.close();}catch(e){}
				try{ec.expectedPending.expCondialog_add.close();}catch(e){}
				if(ec.expectedPending.queryType=='quickQuery'){
					ec.expectedPending.queryList();
				}
				else{
				   ec.expectedPending.advQueryList();
				}
			},1500);
		}else{
			ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	}
	//预期委托保存后的回调函数(新增)
	expectedConsign_expConcallBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			if(ec_expectedPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget){
				ec_expectedPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			}
			else if(ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget){
				ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			}
			setTimeout(function(){
				try{ec.expectedPending.expCondialog.close();}catch(e){}
				try{ec.expectedPending.expCondialog_add.close();}catch(e){}
				if(ec.expectedPending.queryType=='quickQuery'){
					ec.expectedPending.queryList();
				}
				else{
				   ec.expectedPending.advQueryList();
				}
			},1500);
		}else{
			if(ec_expectedPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget){
				ec_expectedPending_expCondialog_add_iframe.expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
			}
			else if(ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget){
				ec_expectedPending_expCondialog_iframe.expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
			}
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	}
	ec.expectedPending.advQuery=function(){
	     ec.expectedPending.queryType='advQuery';
	     CUI(function(){
         ec.expectedPending.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"pendingAdvQueryDiv",
            formId:"advQueryForm",
            modal:true,
	        type:3,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:'ec.expectedPending.advQueryList()'},
	                {name:"<@s.text name='common.button.clear'/>",
	                 handler:function(){CUI.resetForm('advQueryForm')}
	                },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
           ec.expectedPending.advQueryDialog.show();
      })
	}

   ec.expectedPending.advQueryList=function(type,pageNo){
       
   	   expectedConsignTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.expectedPending.advQueryList(1)"
       });
		var url="/msService/ec/pending/expectedPending/expectedList";
		var dataPost=ec.expectedPending.getFormData('advQueryForm');
		if(pageNo!=undefined){
			dataPost+="&records.pageNo="+pageNo;
		}
		expectedConsignTableWidget.setRequestDataUrl(url,dataPost);
		if(ec.expectedPending.advQueryDialog.isShow !=-1){
		   ec.expectedPending.advQueryDialog.close();
		}
   }
   ec.expectedPending.queryList=function(type,pageNo){
       ec.expectedPending.queryType='quickQuery';
	   expectedConsignTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.expectedPending.queryList(1)"
       });
		var url="/msService/ec/pending/expectedPending/expectedList";
		var dataPost=ec.expectedPending.getFormData('quickQueryForm');
		if(pageNo!=undefined){
			dataPost+="&records.pageNo="+pageNo;
		}
		dataPost+="&"+quickQueryForm_getCookieParam();
		expectedConsignTableWidget.setRequestDataUrl(url,dataPost);		
	};
	ec.expectedPending.getFormData=function(formId){
		var obj=CUI("#"+formId);
		var dataPost= "&staffName=" +encodeURIComponent(CUI.trim(CUI("input[name='is.staff.name']",obj).val()));
		dataPost += "&consinor=" +encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",obj).val()));
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName",obj).val()));
		//dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName",obj).val()));
		var pageSize=CUI('input[name="expectedConsignTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
	ec.expectedPending.setEditFlag=function(obj){
		var td=obj.cellHtmlObj;
		if(!obj['CHECKBOX']||obj['CHECKBOX']=='false'){
			td.parentNode.setAttribute('isEdited', 'false');
		}
	};
	
}) ();
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>