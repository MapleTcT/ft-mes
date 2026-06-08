<div id="myflowAttention" class="rowItemDiv">
 <div class="mod modBody">
    <#if tableinfos?exists>
    	<#if tableinfos["on"]?exists>
    	 <ul id="remindList" class="port-list">
			<#list tableinfos["on"] as info>
		    <li title="${info['CREATE_TIME']?string("yyyy-MM-dd")} ${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))}) ${info['STATUS']?default('')}" onclick="foundation.attention.opentableinfoURL('${info['ID']}','${info['TARGET_ENTITY_CODE']}','${info['TARGET_TABLE_NAME']}')">
				<span style="width:20%;" >${info['CREATE_TIME']?string("yyyy-MM-dd")}</span>
				<span style="width:50%;" >${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))})</span>
				<span style="width:30%;" >${info['STATUS']?default('')}</span>
			</li>
		    </#list>
		  </ul>
		 <#else>
			<div align="" style="padding-top: 8px;padding-bottom: 4px; text-align: center;"> 
			${getHtmlText('foundation.workflow.nodata')}</div>
	    </#if>
	</#if>
 </div>   
</div>
<script type="text/javascript" language="javascript" charset="utf-8">
//注册命名空间
CUI.ns("foundation.attention");
foundation.attention.opentableinfoURL = function(ID,entityCode,targetTablename){
	var url="/msService/ec/myWorkflow/openURL.action";
	var data="tableInfoId="+ID+"&entityCode="+entityCode+"&status=99&targetTablename="+targetTablename;
	//console.log(data);
	CUI.ajax({
	   type: "POST",
	   url: url,
	   data:data,
	   success: function(res){
		   // alert(res);
			if(res=="noview"){
					CUI.Dialog.alert("${getHtmlText('foundation.workflow.noview')}");
					return false;
				}
				var window_height = window.screen.availHeight-63;
		   		var window_width  = window.screen.availWidth-20;
		   	    var ShowStyle = "width="+window_width+",height="+window_height+",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		   		//foundation.staff.currentEditStaffId = staffListWidget.selectedRows[0].STAFFID;
		   	   // var openurl = '/foundation/staff/view/default.action?staffId='+staffListWidget.selectedRows[0].STAFFID;
		   		window.open(res,"",ShowStyle);				   				
		}
	});
};
</script>