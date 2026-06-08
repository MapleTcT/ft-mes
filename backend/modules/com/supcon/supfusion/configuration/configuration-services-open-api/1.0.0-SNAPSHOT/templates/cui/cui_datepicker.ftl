<#-- datepicker -->
<#macro datepicker id,yearChangeMonthLink="",value="",nullable=1, type="date",deValue="",cssStyle="",view=false,formid="",funcname="",exp="",onclick="",onchange="",cssClass="cui-noborder-input",tabindex="",title="",name="",property_type="",readonly=false,disabled=false,datepickerClass="cui-calpick", yearRangeStart = 3, yearRangeEnd = 5,align="">
<#if type="yearMonth" || type="year" || type="month">
	<#if value?? && value?has_content>
		<#assign dateValue = value?date>
		<#assign dv = (dateValue?string)?split('-')>
		<#list 0..2 as i>
			<#if i==0>
				<#assign yearValue = dv[i]>
			<#elseif i==1>
				<#assign monthValue = dv[i]>
			<#elseif i==2>
				<#assign dayValue = dv[i]>
			</#if>
		</#list>
	</#if>
	<#if type="yearMonth">
		<input type="hidden" dateType="${type}" name="${name}" id="${id}-select-time" value="<#if dateValue?? && dateValue?has_content>${dateValue?string('yyyy-MM')}</#if>"/>
		<div class="fix-search-click" style="width:49%; float:left;">
			<select id="${id}-select-year" onchange="CUI.setTimeValue('${id}-select-time', '${id}-select-year', '${id}-select-month');" originalValue="" value="" deValue="" class="cui-noborder-input" style="display:none;">
				<option value=""></option>
			</select>
			<script type="text/javascript">
				
				YAHOO.util.Event.onDOMReady(function() {
					var n = new Date().getFullYear();
					var slecetYear = YUD.get('${id}-select-year');
					$(slecetYear).html('<option value=""></option>');//清空操作
					for(var i = n - ${yearRangeStart} ; i <=  n + ${yearRangeEnd}; i++ ){
						var op = document.createElement('option');
						op.text = i;
						op.value = i;
						try{
							slecetYear.add(op,null); // standards compliant
						}catch(ex){
							slecetYear.add(op); // IE only
						}
					}
					$(slecetYear).mSelect();
					<#if yearValue?? && yearValue?has_content>
						$(slecetYear).setValue('${yearValue!}');
					</#if>
					<#if (readonly?? && readonly)||view==true>
						$(slecetYear).disabledSelect();
					</#if>
				})
			
			</script>
		</div>
		
		<div class="fix-search-click" style="width:49%; float:left; margin-left: 2%;">
			<select id="${id}-select-month" onchange="CUI.setTimeValue('${id}-select-time', '${id}-select-year', '${id}-select-month');"   originalValue="" value="" deValue="" class="edit-select cui-noborder-input" style="display:none;">
				<option value=""></option>
				<option value="01">1</option>
				<option value="02">2</option>
				<option value="03">3</option>
				<option value="04">4</option>
				<option value="05">5</option>
				<option value="06">6</option>
				<option value="07">7</option>
				<option value="08">8</option>
				<option value="09">9</option>
				<option value="10">10</option>
				<option value="11">11</option>
				<option value="12">12</option>
			</select>
			<script type="text/javascript">
				
				YAHOO.util.Event.onDOMReady(function() {
					var slecetMonth = YUD.get('${id}-select-month');
					$(slecetMonth).mSelect();
					<#if monthValue?? && monthValue?has_content>
						$(slecetMonth).setValue('${monthValue!}');
					</#if>
					<#if (readonly?? && readonly)||view==true>
						$(slecetMonth).disabledSelect();
					</#if>
				})
			
			</script>
		</div>
		<#if (yearChangeMonthLink?? && yearChangeMonthLink?has_content)>
		<script>
		// 用于快速查询年月联动
		$('#${id}-select-year').on('change', function(){
			var val = this.value;
			var month = $('#${id}-select-month');
			var monthVal = month.val();
			if (val && !monthVal){
				month.setValue("${yearChangeMonthLink}");
			}
		});
		</script>			
		</#if>
	<#elseif type="year">
		<input type="hidden" dateType="${type}" name="${name}" id="${name}-select-time" value="<#if dateValue?? && dateValue?has_content>${dateValue?string('yyyy')}</#if>"/>
		<div class="fix-search-click" style="width:100%; float:left;">
			<select id="${name}-select-year" onchange="CUI.setTimeValue('${name}-select-time', '${name}-select-year');" originalValue="" value="" deValue="" class="cui-noborder-input"  style="display:none;">
				<option value=""></option>
			</select>
	
			<script type="text/javascript">
				
				YAHOO.util.Event.onDOMReady(function() {
					var n = new Date().getFullYear();
					var slecetYear = YUD.get('${name}-select-year');
					$(slecetYear).html('<option value=""></option>');//清空操作
					for(var i = n - ${yearRangeStart} ; i <=  n + ${yearRangeEnd}; i++ ){
						var op = document.createElement('option');
						op.text = i;
						op.value = i;
						try{
							slecetYear.add(op,null); // standards compliant
						}catch(ex){
							slecetYear.add(op); // IE only
						}
					}
					$(slecetYear).mSelect();
					<#if yearValue?? && yearValue?has_content>
						$(slecetYear).setValue('${yearValue!}');
					</#if>
					<#if (readonly?? && readonly)||view==true>
						$(slecetYear).disabledSelect();
					</#if>
				})
			
			</script>
		</div>
	<#elseif type="month">
		<input type="hidden" name="${name}" id="${name}-select-time" value="<#if dateValue?? && dateValue?has_content>${dateValue?string('MM')}</#if>"/>
		<div class="fix-search-click" style="width:100%; float:left;">
			<select id="${name}-select-month" onchange="CUI.setTimeValue('${name}-select-time', null, '${name}-select-month');"   originalValue="" value="" deValue="" class="edit-select cui-noborder-input"  style="display:none;">
				<option value=""></option>
				<option value="01" <#if monthValue?? && monthValue?has_content && monthValue=='01'>selected="selected"</#if>>1</option>
				<option value="02" <#if monthValue?? && monthValue?has_content && monthValue=='02'>selected="selected"</#if>>2</option>
				<option value="03" <#if monthValue?? && monthValue?has_content && monthValue=='03'>selected="selected"</#if>>3</option>
				<option value="04" <#if monthValue?? && monthValue?has_content && monthValue=='04'>selected="selected"</#if>>4</option>
				<option value="05" <#if monthValue?? && monthValue?has_content && monthValue=='05'>selected="selected"</#if>>5</option>
				<option value="06" <#if monthValue?? && monthValue?has_content && monthValue=='06'>selected="selected"</#if>>6</option>
				<option value="07" <#if monthValue?? && monthValue?has_content && monthValue=='07'>selected="selected"</#if>>7</option>
				<option value="08" <#if monthValue?? && monthValue?has_content && monthValue=='08'>selected="selected"</#if>>8</option>
				<option value="09" <#if monthValue?? && monthValue?has_content && monthValue=='09'>selected="selected"</#if>>9</option>
				<option value="10" <#if monthValue?? && monthValue?has_content && monthValue=='10'>selected="selected"</#if>>10</option>
				<option value="11" <#if monthValue?? && monthValue?has_content && monthValue=='11'>selected="selected"</#if>>11</option>
				<option value="12" <#if monthValue?? && monthValue?has_content && monthValue=='12'>selected="selected"</#if>>12</option>
			</select>
		</div>
	</#if>
<#else>
	<div class="fix-input<#if (readonly?? && readonly)||view==true>-readonly</#if>"><div class="fix-search-click clearfix">
	<input type="text" dateType="${type}" validateRule="${type}"<#rt/>
	 checkDate="true"<#rt/>
	 name="${name?html}"<#rt/>
	 property_type="${property_type}"<#rt/>
	  nullable="${nullable?html}"<#rt/>
	<#if disabled?? && disabled>
	 disabled="true"<#rt/>
	</#if>
	<#if (readonly?? && readonly)||view==true>
	 readonly="true"<#rt/>
	</#if>
	<#if deValue!=''>
	deValue="${deValue!}"<#rt/> 
	</#if>
	<#if tabindex??>
	 tabindex="${tabindex?html}"<#rt/>
	</#if>
	<#if id??>
	 id="${id?html}"<#rt/>
	</#if>
	<#if value??&&value!="">
		<#if type=='dateTimeHour'>
			value="${formatDate(value,type)?substring(0,13)}" originalValue="${formatDate(value,type)?substring(0,13)}"<#rt/>
		<#elseif type=='dateTimeMin'>
			value="${formatDate(value,type)?substring(0,16)}" originalValue="${formatDate(value,type)?substring(0,16)}"<#rt/>
		<#else>
			value="${formatDate(value,type)}" originalValue="${formatDate(value,type)}"<#rt/>
		</#if>
	<#else>
		originalValue=""
	</#if>
	<#if title??>
	 title="${title?html}"<#rt/>
	</#if>
	<#if exp??>
	 exp="${exp?html}"<#rt/>
	</#if>
	<#if funcname??>
	 funcname="${funcname?html}"<#rt/>
	</#if>
	<#if cssStyle??>
	 style="${cssStyle?html}<#if align??>;text-align:${align?html};</#if>"<#rt/>
	 <#else>
	 style="padding-left:2px;width:90px<#if align??>;text-align:${align?html};</#if>"
	</#if>
	<#if cssClass??>
	 class="${cssClass?html}"<#rt/>
	 <#else>
	 class="cui-noborder-input"<#rt/>
	</#if>
	/>
	</div></div>
	<script type="text/javascript">
		$(document).ready(function() {
			<#if view!=true>
			var dateInput = <#if formid?? && formid != "">$('input[name="${name}"]','#${formid}')<#else>CUI('input[id="${id}"]')</#if>
			dateInput.datepicker({id:"${id}", picker: "<input type='button' class='${datepickerClass}'></input>"<#if type!='date'>,type:"${type}"</#if>});
			dateInput.parents('.fix-search-click').addClass('cui-dateinput')
			</#if>   
			<#if onchange?? &&  onchange!="">
			CUI('input[id="${id}"]').unbind("change").bind("change", function() {
				eval("${onchange}");
			});
			</#if>
	    });
	</script>
</#if>
</#macro>
