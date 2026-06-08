<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />
<@errorbar id="remidnSubmitFormDialogErrorBar" offsetY=50 />
<form  id="remidnSubmitForm" onsubmit="return false;" style="padding-top:15px;">
<@s.hidden name="tableInfoId" id="remindTableInfoId" />
<@s.hidden name="userIds" id="remindUserIds" />
<@s.hidden name="remindType" id="remindTypeId" />
<div class="col-xs-12 col-sm-6 col-md-6 col-lg-4">
	<div class="row">
		<div class="text-right div-label" style="width:25%;">
			<label class="label-wd">
				${getText('ec.remind.pending.selectStaff')}
			</label>
		</div>

		<div class="div-input form-control no-border">
			<span class="checkbox-radio-span">
				<label>
					${getText('foundation.common.allSelect')}
					<input type="checkbox" id="checkAll" name="remindCheckAll" onclick="ec.remind.checkAll(this)"/>
				</label>
			</span>
			<span class="checkbox-radio-last-span">
				${getText('ec.workflow.remindCountContent')}${remindCount?default(0)}${getText('ec.workflow.remindCount')}
			</span>
		</div>
	</div>
</div>
<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">
	<#if pendingUser??&&pendingUser["one"]??>
	<#assign userList=pendingUser["one"]>
	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				<span class="checkbox-radio-span"><input type="checkbox" id="oneDayAll" name="oneDayAll" onclick="ec.remind.all(this, 'user_one_')"/>${getText('ec.remind.pending.oneday')}</span>
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<#list userList?keys as recordKey>
				<span class="checkbox-radio-span"><label><input type="checkbox" id="user_one_${userList[recordKey]}" name="user_one_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />${recordKey}</label></span>
			</#list>
		</div>
	</div>
	</#if>

	<#if pendingUser??&&pendingUser["three"]??>
	<#assign userList=pendingUser["three"]>
	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				<span class="checkbox-radio-span"><input type="checkbox" id="threeDayAll" name="threeDayAlll" onclick="ec.remind.all(this, 'user_three_')"/>${getText('ec.remind.pending.morethanoneday')}</span>
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<#list userList?keys as recordKey>
				<span class="checkbox-radio-span"><label><input type="checkbox" id="user_three_${userList[recordKey]}" name="user_three_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />${recordKey}</label></span>
			</#list>
		</div>
	</div>
	</#if>

	<#if pendingUser??&&pendingUser["week"]??>
	<#assign userList=pendingUser["week"]>
	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				<span class="checkbox-radio-span"><input type="checkbox" id="weekAll" name="weekAll" onclick="ec.remind.all(this, 'user_week_')"/>${getText('ec.remind.pending.morethanthreeday')}</span>
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<#list userList?keys as recordKey>
				<span class="checkbox-radio-span"><label><input type="checkbox" id="user_week_${userList[recordKey]}" name="user_week_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />${recordKey}</label></span>
			</#list>
		</div>
	</div>
	</#if>

	<#if pendingUser??&&pendingUser["month"]??>
	<#assign userList=pendingUser["month"]>
	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				<span class="checkbox-radio-span"><input type="checkbox" id="monthAll" name="monthAll"  onclick="ec.remind.all(this, 'user_month_')" />${getText('ec.remind.pending.morethanweek')}</span>
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<#list userList?keys as recordKey>
				<span class="checkbox-radio-span"><label><input type="checkbox" id="user_month_${userList[recordKey]}" name="user_month_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />${recordKey}</label></span>
			</#list>
		</div>
	</div>
	</#if>

	<#if pendingUser??&&pendingUser["other"]??>
	<#assign userList=pendingUser["other"]>
	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				<span class="checkbox-radio-span"><input type="checkbox" id="monthAll" name="monthAll"  onclick="ec.remind.all(this, 'user_other_')" />${getText('ec.remind.pending.morethanweek')}</span>
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<#list userList?keys as recordKey>
				<span class="checkbox-radio-span"><label><input type="checkbox" id="user_other_${userList[recordKey]}" name="user_other_${userList[recordKey]}" value="${userList[recordKey]}" class="input-line" />${recordKey}</label></span>
			</#list>
		</div>
	</div>
	</#if>

	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				${getText('ec.remind.pending.ps')}
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<textarea class="form-control" id="remindContent" name="remindContent"></textarea>
		</div>
	</div>

	<div class="row">
		<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
			<label class="label-wd">
				${getText('ec.pending.remindType')}
			</label>
		</div>

		<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
			<input type="checkbox" id="pandion" name="pandion" value="pandion" checked="true" /><span style="padding:0 10px">${getText('ec.remind.pending.chatTool')}</span><input type="checkbox" id="email" name="email" value="true" checked="true" /><span style="padding:0 10px">  ${getText('ec.common.email')}</span> <input type="checkbox" id="sms" name="sms" value="sms"  />  <span style="padding:0 10px">${getText('ec.common.sms')}</span>
		</div>
	</div>
</div>
</form>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.remind");
	CUI(function(){
		CUI('input[id^="user_"]').bind('click',function() {
			ec.remind.checkAllFlag();
		});
	})
	ec.remind.checkAll=function(obj){
		if(CUI(obj).prop("checked")){
			CUI('input[id^="user_"]').prop("checked","true");
			CUI("#oneDayAll").prop("checked","true");
			CUI("#threeDayAll").prop("checked","true");
			CUI("#weekAll").prop("checked","true");
			CUI("#monthAll").prop("checked","true");
			CUI("#otherAll").prop("checked","true");
		}else{
			CUI('input[id^="user_"]').prop("checked","");
			CUI("#oneDayAll").prop("checked","");
			CUI("#threeDayAll").prop("checked","");
			CUI("#weekAll").prop("checked","");
			CUI("#monthAll").prop("checked","");
			CUI("#otherAll").prop("checked","");
			
		}
	}
	ec.remind.checkAllFlag=function(){
		var allChecked=CUI('input[id^="user_"]:checked').length;
		var all=CUI('input[id^="user_"]').length;
		if(allChecked==all){
			CUI("#checkAll").prop("checked","true");
		}else{
			CUI("#checkAll").prop("checked","");
		}
		var allCheckedOne=CUI('input[id^="user_one_"]:checked').length;
		var allCheckedThree=CUI('input[id^="user_three_"]:checked').length;
		var allCheckedWeek=CUI('input[id^="user_week"]:checked').length;
		var allCheckedMonth=CUI('input[id^="user_month_"]:checked').length;
		var allCheckedOther=CUI('input[id^="user_other_"]:checked').length;
		
		var allOne=CUI('input[id^="user_one_"]').length;
		var allThree=CUI('input[id^="user_three_"]').length;
		var allWeek=CUI('input[id^="user_week"]').length;
		var allMonth=CUI('input[id^="user_month_"]').length;
		var allOther=CUI('input[id^="user_other_"]').length;
		
		CUI("#oneDayAll").prop("checked", allOne==allCheckedOne);
		CUI("#threeDayAll").prop("checked", allThree==allCheckedThree);
		CUI("#weekAll").prop("checked",allWeek==allCheckedWeek);
		CUI("#monthAll").prop("checked", allMonth==allCheckedMonth);
		CUI("#otherAll").prop("checked", allOther==allCheckedOther);
    	
	}
	ec.remind.all=function(obj, type){
		$('input[id^="' + type + '"]').prop("checked", $(obj).prop("checked"));
		ec.remind.checkAllFlag();
	}
}) ();	

</script>