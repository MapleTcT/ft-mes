<#if (Parameters.openType)?default('page') != 'dialog'>
<html xmlns="http://www.w3.org/1999/xhtml">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<head>
<@head/>
<style type="text/css">
	.ewc-dialog-el{height:95%;padding-top:8px;}
	#assignStaffList .configtable{display:none;}
	.add-person{position:absolute;_position:absolute;top:6px;right:30px;*top:6px;_right:40px;}
</style>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<title>${getText('foundation.staff.select')}</title>
</head>
<body>

<#else>
<style type="text/css">
	.ewc-dialog-el{height:95%;padding-top:8px;}
	#assignStaffList .configtable{display:none;}
	.add-person{position:absolute;top:14px;right:30px;*top:10px;_right:40px;}
</style>
</#if>
<@loadpanel></@loadpanel>
<@errorbar id="assignStaffFrameErrorBar" />
<form id="assignStaffForm" onsubmit="return false" style="height: calc(100% - 30px);">
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="assignStaff_callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="companyId" name="companyId" value="${getCurrent('companyId')}" />
<input type="hidden" id="companyName" name="companyName" value="${getCurrent('companyName')}" />


   <div id="assignstaff_datatable_container">
	   <@datatable  exportExcel=false  dtPage="records" id="assignStaffList" hidekey="['STAFFID']"  transMethod="post" style="margin:4px 10px;" paginator=false firstLoad=true  dataUrl="/msService/ec/foundation/staff/common/assignListData?assignStaffs=${assignStaffs!''}">
	        <#if !viewFlag>
	        <@operatebar operates="code:rolePermission_assignStaff_add||iconcls:add||name:${getHtmlText('foundation.assignStaff.add')}||onclick:foundation.assignStaff.openStaffDialog();code:rolePermission_assignStaff_del||iconcls:del||name:${getHtmlText('foundation.assignStaff.del')}||onclick:foundation.assignStaff.delRow()" operateType="noPower" resultType="json"/>
	        </#if>
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width="24" />
			<@datacolumn key="CODE" label="${getHtmlText('foundation.staff.code')}" width="100" textalign="left"/>
			<@datacolumn key="NAME" label="${getHtmlText('foundation.staff.name')}" width="100" textalign="left"/>
	  </@datatable>
   </div>
   <script>
   		$(function(){
			var contentHeight = $(".content").height();
   			$("#assignstaff_datatable_container").css({"height":(contentHeight) + "px"});
   			
   			$(window).on("resize",function(){
   				var contentHeight = $(".content").height();
   				$("#assignstaff_datatable_container").css({"height":(contentHeight) + "px"});
			})
   			
   		});
   </script>
	<div class="add-person">
		<div style="height:18px;width:156px;background-color:#FFF;float:right;">
		    <@mneclient view=viewFlag name="staff.name" formId="assignStaffForm" id="staff_name1" url="/foundation/staff/common/staffListFrame.action" onkeyupfuncname="foundation.assignStaff.staffCallbackInfo(obj)" classStyle="cui-noborder-input" type="Staff" multiple=false isPrecise=true />
		</div>
	</div>
<#if (Parameters.openType)?default('page') != 'dialog'>
	 <div align="right" style="margin-right:20px;bottom:10px;position:absolute;right:0;">
	 	<#if !viewFlag>
	 	<@querybutton isCustomize=true onclick="foundation.assignStaff.save()" customizeName="foundation.common.save"/>
	 	</#if>
	 	<@querybutton isCustomize=true onclick="foundation.assignStaff.closePage()" customizeName="common.button.cancel"/>
	 </div>
</#if>
 </form>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" language="javascript">
   (function(){
       CUI.ns("foundation.assignStaff");
       //判断数据在DT中是否已经存在
       foundation.assignStaff.isExist=function(obj){
          var allRows=assignStaffListWidget.getAllRows();
          for(var i=0;i<allRows.length;i++){
             if(obj.id==allRows[i].STAFFID){
                assignStaffFrameErrorBarWidget.show("${getHtmlText('foundation.assignStaff.tip')}","f");
                return true;
             }
          } 
          return false; 
       }
       //添加数据到DataTable
       foundation.assignStaff.addRow=function(obj){
            if(obj!=null&&obj.id!=undefined){
	             var records={
	                'STAFFID':obj.id,
	                'CODE':obj.code,
	                'NAME':obj.name 
	             }
             assignStaffListWidget.addNewRowWithValue(null,records);
          }
       }
       //助词码回调函数
       foundation.assignStaff.staffCallbackInfo=function(obj){
          if(obj!=null && obj != undefined && obj[0].id != undefined && obj[0].id != null){
             if(!foundation.assignStaff.isExist(obj[0])){
                 foundation.assignStaff.addRow(obj[0]);
             }
             CUI('#staff_name',CUI("#assignStaffForm")).val('');
           	 //foundation.assignStaff.restoreTips(CUI('#staff_name',CUI("#assignStaffForm")));
          }
       }
       foundation.assignStaff.openStaffDialog=function(){
           CUI(function(){
               foundation.assignStaff.assignStaffDialog=new CUI.Dialog({
	                  title:'${getText('foundation.staff.selectStaff')}',
	                  url:'/msService/ec/foundation/staff/common/staffListFrameset?openType=dialog&callBackFuncName=foundation.assignStaff.selectStaffCallback&multiSelect=true<#if getCurrent('company')??&&getCurrent('company').id==1>&crossCompanyFlag=true</#if>',
	                  type:5,
	                  modal:true,
		              dragable:true,
	                  buttons:[{name:"${getText('common.button.choose')}",id:"assignStaffSave",
						                handler:function(){
													if( $("#staffListTab .selected").html().indexOf("${getText('foundation.department.according')}") != -1) { // 按部门
														foundation.staff.sendBackDepartmentStaffInfo();
													} else if( $("#staffListTab .selected").html().indexOf("${getText('foundation.position.according')}") != -1) { //按岗位
														foundation.staff.sendBackStaffInfo();
													} else if ( $("#staffListTab .selected").html().indexOf("${getText('foundation.staff.unassignstaffselect')}") != -1) { //未分配人员
														foundation.unassignStaff.sendBackStaffInfo();
													} else if ($('#staffListTab .selected').html().indexOf("${getText('foundation.customGroup.userDefined')}") > -1){ //自定义组
														foundation.userDefinedGroupStaff.sendBackGroupStaffList();
													}	
										         }
										},
						               {name:"${getText('common.button.close')}",
						                handler:function(){this.close()}
				    }]
               });
               foundation.assignStaff.assignStaffDialog.show();
           })
       }
       //选择人员的回调函数
       foundation.assignStaff.selectStaffCallback=function(obj){
          if(obj!=null){
             for(var i=0;i<obj.length;i++){
                if(!foundation.assignStaff.isExist(obj[i])){
                   foundation.assignStaff.addRow(obj[i]);
                }
                
             }
          }
       }
       //删除DataTable中的数据
       foundation.assignStaff.delRow=function(){
          if(foundation.assignStaff.selectedAny()){
             CUI.Dialog.confirm("${getHtmlText('foundation.assignStaff.confrimDel')}",function(){
                assignStaffListWidget.delRow();
             });
          }
       }
       //判断在dataTable是否选中了记录
       foundation.assignStaff.selectedAny=function(){
          var rows=assignStaffListWidget.selectedRows;
          if(rows.length<=0){
              assignStaffFrameErrorBarWidget.show("${getHtmlText('foundation.assignStaff.checkedSelect')}","f");
              return false;
          }
          else{
              return true;
          }
       }
       
       //指定人员保存
       foundation.assignStaff.save=function(dialog){
          var rows=assignStaffListWidget.getAllRows();
          var arrObj=new Array();
          if(rows.length<=0){
             CUI.Dialog.confirm("${getHtmlText('foundation.assignStaff.nullChecked')}",function(){
                var arrObj=new Array();
                <#if (Parameters.openType)?default('page') != 'dialog'>
                	eval("opener." +CUI("#assignStaff_callBackFuncName",CUI("#assignStaffForm")).val()+ "(arrObj)"); //当指定人员为空时，取消当前行指定岗位的选择状态
                	top.opener.focus();
					CUI.closeWindow();
                <#else>
	                eval(CUI("#assignStaff_callBackFuncName",CUI("#assignStaffForm")).val()+ "(arrObj)"); //当指定人员为空时，取消当前行指定岗位的选择状态
	                dialog.close();
                </#if>
             });
             return ;
          }
          else{
	           for(var i=0;i<rows.length;i++){
		            var oStaff=new Object();
					oStaff.staffName=rows[i].NAME;
					oStaff.staffID=rows[i].STAFFID;
					oStaff.staffCode=rows[i].CODE;
					arrObj.push(oStaff);
	          }
	          //console.log(arrObj)
	         try{
	              //console.log(CUI("#assignStaff_callBackFuncName").val());
	              <#if (Parameters.openType)?default('page') != 'dialog'>
	              		eval("opener." + CUI("#assignStaff_callBackFuncName",CUI("#assignStaffForm")).val()+ "(arrObj)");
	              <#else>
			      		eval(CUI("#assignStaff_callBackFuncName",CUI("#assignStaffForm")).val()+ "(arrObj)");
	              </#if>
			      assignStaffFrameErrorBarWidget.show("${getHtmlText('foundation.add.success')}","s");
			      <#if (Parameters.openType)?default('page') != 'dialog'>
			      		setTimeout(function(){
							try {
								top.opener.focus();
								CUI.closeWindow();
							}catch(e){}
						},1000);
			      <#else>
					      setTimeout(function(){
					        if(dialog.isShow !== -1){
					           dialog.close();
					        }
					      },1000)
			      </#if>
			   }
			   catch(e){
			      assignStaffFrameErrorBarWidget.show("${getHtmlText('foundation.add.failure')}","f");
			   }	
          }
       }
       foundation.assignStaff.closePage=function(){
       		top.opener.focus();
			CUI.closeWindow();
       }
   })();
</script>