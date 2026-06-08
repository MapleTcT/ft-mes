<#-- 自定义Select Control by fangzhibin -->
<#macro customselect id,dataSource="",cssStyle="",showValue="",title="",value="",type="",name="",href="",readOnly=false,view=false>
<script type="text/javascript">
	$(document).ready(function(){
		$("select").sSelect();
	})
</script>

<#-- 编辑 -->
<#if !view>
	<#-- 单选-->
	<select id="${id}" name="${name}"<#rt/>
		<#if cssStyle??>
			 style="${cssStyle?html}"<#rt/>
		<#else>
			 style="width:99%;border: 1px solid #A5C7EF"<#rt/>
		</#if>
		<#if readOnly>
		 	disabled="true"<#rt/>
		</#if>
	>
	<#if dataSource != "">
	<#list dataSource?keys as key> 
		<option value="${key}"<#rt/>
		<#if value?? && value == key>
			selected="true"<#rt/>
		</#if>
		>${dataSource[key]}</option>
	</#list>
	</#if>
	</select>
<#-- 查看 -->
<#else>
<input type="text" class="cui-readonly-field" name="systemCode" readonly="true" id="systemValue"  style="padding-left:0px;width:98%;"
		<#if value??>
		 value="${showValue?html}"<#rt/>
		</#if>
		<#if cssStyle??>
		 style="${cssStyle?html}"<#rt/>
		</#if>
		<#if title??>
		 title="${title?html}"<#rt/>
		</#if>
		/>
</#if>
</#macro>