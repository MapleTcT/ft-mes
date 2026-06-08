<script type="text/javascript">
	function beforesubmit(){
		$('#generateForm').hide();
		$('#generateWaitForm').show();
		return true;
	}
</script>
<div style="padding:2px 20px 0 20px;">
<form id="generateForm" target="generateWaitForm" onSubmit="javascript:return beforesubmit();" action="<#if isDeploy?? && isDeploy>/ec/module/deploy<#else>/ec/module/generate</#if>" method="post">
<@s.hidden name="module.code" />
<@s.hidden name="isDeploy" />
<input type="hidden" name="generatorV3" value="false" ></input><!--使用BAPGenerateServiceImpl3-->
<#if (module.code)?? && (module.code) != "undefined" && (module.code) != "-1">
</br>
<span id="generate_full_span"><label for="generate_full" style="font-size:12px;padding-right:15px;">${getHtmlText('全部生成:')}</label>
<#--
<input type="checkbox" value="true" name="generate_full" id="generate_full" checked/>
-->
</span>
<select name="generate_full" id="generate_full">
	<option value="false">增量</option>
	<option value="true">全部</option>
</select>
<#if isDeploy?? && isDeploy>
<label for="init_type" style="font-size:12px;padding-right:15px;">${getHtmlText('部署方式')}</label>
<select name="init_type" id="init_type">
	<option value="0">一般</option>
	<option value="1">升级</option>
	<option value="2">初始化</option>
</select>
</#if>
</#if>
</br>
<input type="submit" value="${getText('确定')}" />【&nbsp;<span>${getText('ec.module.createAlert')}</span>】
</br>
</br>
<span>注：选中全部生成时将生成所有实体，否则只生成最近修改的实体</span>

</form>
</div>
<iframe style="display:none;" id="generateWaitForm" name="generateWaitForm" frameborder="0" scrolling="auto" width="100%" height="315" src="/msService/ec/module/generatewait"></iframe>