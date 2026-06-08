<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=8"/>
<#assign fileName = (Parameters['fileName'])!'' >
<style>
.root {
	padding: 6px 0 0 15px;
	margin-left: 6px;
	overflow: auto;
	height:97%;
}
.list-in {
	white-space: nowrap;
	margin-left: 3px;
	margin-top:2px;
	height: 17px;
}
.list-in span{
	margin-left: 6px;
	font-size: 12px;
	font-family: Verdana;
}
</style>

<@s.hidden name="viewCodeCode"/>
<div class="root" id="printTemplate_viewSelectListId">
	<#list views as view>
	<div class="list-in">
		<input type="checkbox" name="${view}" value="${view.code}" class="choosedView"><span>${getText('${view.displayName}')}[${view.name}]</span></input>
	</div>
	</#list>
	
</div>

<script type="text/javascript" charset="utf-8" language="javascript">
CUI.ns("project.print.template");
project.print.template.getSystemCodeValueprintTemplate_viewSelectListFormId = function(){
	var checkedValue='';
	var checkedCode='';
	CUI('#printTemplate_viewSelectListId input:checked').each(function(index){
		checkedValue+=','+CUI(this).attr("value");
		checkedCode+=','+CUI(this).attr("name");
	});
	checkedValue=checkedValue.substring(1);
	checkedCode=checkedCode.substring(1);
	
	return checkedValue+"@|@"+checkedCode;
}
</script>