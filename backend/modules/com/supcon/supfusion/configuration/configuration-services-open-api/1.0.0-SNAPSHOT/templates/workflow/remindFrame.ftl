<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />
<@errorbar id="remidnSubmitFormDialogErrorBar" />
<@s.form  id="remidnSubmitForm" action="newRemind" namespace="/msService/ec/workflow" validate="true" callback="waitWork_remindCallBack">
	<@s.hidden name="tableInfoId" id="remindTableInfoId" />
	<@s.hidden name="userIds" id="remindUserIds" />
	<@s.hidden name="remindType" id="remindTypeId" />
	<@s.hidden name="id" id="id" />
	<div class="edit-title fix" style="margin-top:10px;position:relative;">
		<span style="margin :10px 1px 1px 1px;font-weight:bold;font-size:14">${getHtmlText('ec.remind.pending.selectStaff')}</span><span style="padding:0 5px">${getHtmlText('foundation.common.allSelect')}</span><span style="padding:0 5px ;position: relative;top: 2px;"><input type="checkbox" id="checkAll" name="remindCheckAll" onclick="ec.remind.checkAll(this)"  /></span>
		<span style="margin:0px 20px 1px 1px;font-size:12;position:absolute;right:25ppx;">${getHtmlText('ec.workflow.remindCountContent')}${remindCount?default(0)}${getHtmlText('ec.workflow.remindCount')}</span>
	</div>
	<div class="div-pending">
	<table style="float:left;width:100%" id="editInfo" cellpadding="0" cellspacing="0" border="0" align="center"  style="height:100%;width:100%">
		<#if pendingUser??&&pendingUser["one"]??>
		<#assign userList=pendingUser["one"]>
		<tr>
			<td align="right" width="15%" style="vertical-align:top;padding-left:5px;"><input type="checkbox" id="oneDayAll" name="oneDayAll" onclick="ec.remind.oneAll(this)"/><span style="font-weight:bold;font-size:12px;white-space:nowrap; position:relative;top:-2px;padding:0 4px">${getHtmlText('ec.remind.pending.oneday')}：</span></td>
			<td colspan="3" style="vertical-align:top">	
				<#list userList?keys as recordKey>
					<#if recordKey_index == 0 >
						<div class="clearfix" style="height:30px; overflow:hidden;">
					</#if>
					<input type="checkbox" style="${((recordKey_index+1) %7==0)?string('margin-left:-1px','')}" id="user_one_${userList[recordKey]}" name="user_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />
					<span class="div-name" onclick="ec.remind.viewStaff(${userList[recordKey]})">${recordKey}</span>
					<#if recordKey_index &gt; 0 && (recordKey_index+1) %7==0>
						</div>
						<#if recordKey_index+1 !=userList.size()>
							<div class="clearfix" style="height:30px; overflow:hidden;">
						</#if>
					</#if>
				</#list>
			</td>
		</tr>
		</#if>
		<#if pendingUser??&&pendingUser["three"]??>
		<#assign userList=pendingUser["three"]>
		<tr>
			<td align="right" width="15%" style="vertical-align:top;padding-left:5px;"><input type="checkbox" id="threeDayAll" name="threeDayAlll" onclick="ec.remind.threeAll(this)"/><span style="font-weight:bold;font-size:12px;white-space:nowrap; position:relative;top:-2px;padding:0 4px">${getHtmlText('ec.remind.pending.morethanoneday')}：</span></td>
			<td colspan="3" style="vertical-align:top">	
				<#list userList?keys as recordKey>
					<#if recordKey_index == 0 >
						<div class="clearfix" style="height:30px; overflow:hidden;">
					</#if>
					<input type="checkbox" style="${((recordKey_index+1) %7==0)?string('margin-left:-1px','')}" id="user_three_${userList[recordKey]}" name="user_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />
					<span class="div-name">${recordKey}</span>
					<#if recordKey_index &gt; 0 && (recordKey_index+1) %7==0>
						</div>
						<#if recordKey_index+1 !=userList.size()>
							<div class="clearfix" style="height:30px; overflow:hidden;">
						</#if>
					</#if>
				</#list>
			</td>
		</tr>
		</#if>
		<#if pendingUser??&&pendingUser["week"]??>
		<#assign userList=pendingUser["week"]>
		<tr>
			<td align="right" width="15%" style="vertical-align:top;padding-left:5px;"><input type="checkbox" id="weekAll" name="weekAll" onclick="ec.remind.weekAll(this)"/><span style="font-weight:bold;font-size:12px;white-space:nowrap; position:relative;top:-2px;padding:0 4px">${getHtmlText('ec.remind.pending.morethanthreeday')}：</span></td>
			<td colspan="3" style="vertical-align:top">				
				<#list userList?keys as recordKey>
					<#if recordKey_index == 0 >
						<div class="clearfix" style="height:30px; overflow:hidden;">
					</#if>
					<input type="checkbox" style="${((recordKey_index+1) %7==0)?string('margin-left:-1px','')}" id="user_week_${userList[recordKey]}" name="user_week_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />
					<span class="div-name">${recordKey}</span>
					<#if recordKey_index &gt; 0 && (recordKey_index+1) %7==0>
						</div>
						<#if recordKey_index+1 !=userList.size()>
							<div class="clearfix" style="height:30px; overflow:hidden;">
						</#if>
					</#if>
				</#list>
			</td>
		</tr>
		</#if>
		<#if pendingUser??&&pendingUser["month"]??>
		<#assign userList=pendingUser["month"]>
		<tr>
			<td align="right" width="15%" style="vertical-align:top;padding-left:5px;"><input type="checkbox" id="monthAll" name="monthAll"  onclick="ec.remind.monthAll(this)" /><span style="font-weight:bold;font-size:12px;white-space:nowrap; position:relative;top:-2px; padding:0 4px">${getHtmlText('ec.remind.pending.morethanweek')}：</span></td>
			<td colspan="3" style="vertical-align:top">				
				<#list userList?keys as recordKey>
					<#if recordKey_index == 0 >
						<div class="clearfix" style="height:30px; overflow:hidden;">
					</#if>
					<input type="checkbox" style="${((recordKey_index+1) %7==0)?string('margin-left:-1px','')}" id="user_month_${userList[recordKey]}" name="user_month_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />
					<span class="div-name">${recordKey}</span>
					
					<#if recordKey_index &gt; 0 && (recordKey_index+1) %7==0>
						</div>
						<#if recordKey_index+1 !=userList.size()>
							<div class="clearfix" style="height:30px; overflow:hidden;">
						</#if>
					</#if>
					
				</#list>
			</td>
		</tr>
		</#if>
		<#if pendingUser??&&pendingUser["other"]??>
		<#assign userList=pendingUser["other"]>
		<tr>
			<td align="right" width="15%" style="vertical-align:top;padding-left:5px;"><input type="checkbox" id="otherAll" name="otherAll" onclick="ec.remind.otherAll(this)"/><span style="font-weight:bold;font-size:12px;white-space:nowrap; position:relative;top:-2px; padding:0 4px">${getHtmlText('ec.remind.pending.morethanmonth')}：</span></td>
			<td colspan="3" style="vertical-align:top">				
				<#list userList?keys as recordKey>
					<#if recordKey_index == 0 >
						<div class="clearfix" style="height:30px; overflow:hidden;">
					</#if>
					<input type="checkbox" style="${((recordKey_index+1) %7==0)?string('margin-left:-1px','')}" id="user_other_${userList[recordKey]}" name="user_other_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />
					<span class="div-name">${recordKey}</span>
					<#if recordKey_index &gt; 0 && (recordKey_index+1) %7==0>
						</div>
						<#if recordKey_index+1 !=userList.size()>
							<div class="clearfix" style="height:30px; overflow:hidden;">
						</#if>
					</#if>
				</#list>
			</td>
		</tr>
		</#if>
	</table>
	</div>
	<div class="edit-title fix">
	<h3 style="font-size:14px;">${getHtmlText('ec.remind.pending.ps')}</h3>
	</div >
	<div style="margin:10px 20px;">
		<textarea class="div-remindContent" id="remindContent" name="remindContent"></textarea>
	
	</div >
	<div class="div-remindType">
		<h3>${getHtmlText('ec.pending.remindType')}</h3> <div style="float:left;margin-top:10px"><input type="checkbox" id="pandion" name="pandion" value="pandion" checked="true" /><span style="padding:0 10px">${getHtmlText('ec.remind.pending.chatTool')}</span><input type="checkbox" id="email" name="email" value="true" checked="true" /><span style="padding:0 10px">  ${getHtmlText('ec.common.email')}</span> <input type="checkbox" id="sms" name="sms" value="sms"  />  <span style="padding:0 10px">${getHtmlText('ec.common.sms')}</span></div>
		
	</div>
	
</@s.form>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.remind");
	CUI(function(){
		CUI('input[id*="user_"]').bind('click',function() {
			ec.remind.checkAllFlag();
		});
	})
	ec.remind.checkAll=function(obj){
	
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_"]').prop("checked","true");
			CUI("#oneDayAll").prop("checked","true");
			CUI("#threeDayAll").prop("checked","true");
			CUI("#weekAll").prop("checked","true");
			CUI("#monthAll").prop("checked","true");
			CUI("#otherAll").prop("checked","true");
		}else{
			CUI('input[id*="user_"]').prop("checked","");
			CUI("#oneDayAll").prop("checked","");
			CUI("#threeDayAll").prop("checked","");
			CUI("#weekAll").prop("checked","");
			CUI("#monthAll").prop("checked","");
			CUI("#otherAll").prop("checked","");
			
		}
	}
	ec.remind.checkAllFlag=function(){
		var allChecked=CUI('input[id*="user_"]:checked').length;
		var all=CUI('input[id*="user_"]').length;
		if(allChecked==all){
			CUI("#checkAll").prop("checked","true");
		}else{
			CUI("#checkAll").prop("checked","");
		}
		var allCheckedOne=CUI('input[id*="user_one_"]:checked').length;
		var allCheckedThree=CUI('input[id*="user_three_"]:checked').length;
		var allCheckedWeek=CUI('input[id*="user_week"]:checked').length;
		var allCheckedMonth=CUI('input[id*="user_month_"]:checked').length;
		var allCheckedOther=CUI('input[id*="user_other_"]:checked').length;
		
		var allOne=CUI('input[id*="user_one_"]').length;
		var allThree=CUI('input[id*="user_three_"]').length;
		var allWeek=CUI('input[id*="user_week"]').length;
		var allMonth=CUI('input[id*="user_month_"]').length;
		var allOther=CUI('input[id*="user_other_"]').length;
		
		if(allOne==allCheckedOne){
			CUI("#oneDayAll").prop("checked","true");
		}else{
			CUI("#oneDayAll").prop("checked","");
		}
		
		if(allThree==allCheckedThree){
			CUI("#threeDayAll").prop("checked","true");
		}else{
			CUI("#threeDayAll").prop("checked","");
		}
    	if(allWeek==allCheckedWeek){
			CUI("#weekAll").prop("checked","true");
		}else{
			CUI("#weekAll").prop("checked","");
		}
		if(allMonth==allCheckedMonth){
			CUI("#monthAll").prop("checked","true");
		}else{
			CUI("#monthAll").prop("checked","");
		}
		if(allOther==allCheckedOther){
			CUI("#otherAll").prop("checked","true");
		}else{
			CUI("#otherAll").prop("checked","");
		}
    	
	}
	ec.remind.oneAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_one_"]').prop("checked","true");
		}else{
			CUI('input[id*="user_one_"]').prop("checked","");
		}
		ec.remind.checkAllFlag();
	}
	ec.remind.threeAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_three_"]').prop("checked","true");
		}else{
			CUI('input[id*="user_three_"]').prop("checked","");
		}
		ec.remind.checkAllFlag();
	}
	ec.remind.weekAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_week_"]').prop("checked","true");
		}else{
			CUI('input[id*="user_week_"]').prop("checked","");
		}
		ec.remind.checkAllFlag();
	}
	ec.remind.monthAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_month_"]').prop("checked","true");
		}else{
			CUI('input[id*="user_month_"]').prop("checked","");
		}
		ec.remind.checkAllFlag();
	}
	ec.remind.otherAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id*="user_other_"]').prop("checked","true");
		}else{
			CUI('input[id*="user_other_"]').prop("checked","");
		}
		ec.remind.checkAllFlag();
	}
	ec.remind.viewStaff=function(id){
		var url="";
		
	}
	ec.remind.submitRemind=function(){
		var userIds='';
		$('input[name*="user_"]:checked').each(function(index, item){
				userIds+=","+CUI(this).val();
		});
		if(userIds!=""){
			userIds=userIds.substr(1);
		}else{
			remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.remind.pending.noSelected')}！","f");
			return false;
		}
		
		CUI("#remindUserIds").val(userIds);
		
		var pandion=CUI("#pandion").prop("checked");
		var email=CUI("#email").prop("checked");
		var sms=CUI("#sms").prop("checked");
		if(!pandion&&!email&&!sms){
			remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.pengding.nullType')}","f");
			return false;
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
		var remindContent=CUI("#remindContent").val();
		if(remindContent!=""){
			if(sms&&remindContent.length>60){
				remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.remind.remindContent')}","f");
				return false;
			}
		}
		CUI("#remindTypeId").val(i);
		CUI("#remidnSubmitForm").submit();
	}
}) ();	

</script>