<#if (Parameters.requstObjectType!'') != 'dialog'>

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta name="viewport" content="width=device-width" />	
	<link rel="stylesheet" type="text/css" href="/bap/static/errorimg/error.css" />
<script type="text/javascript" src="/bap/static/core/jquery.js"></script>
</head>
<body onload="page500Load()">
<div style="width:400px;margin:0 auto;margin-top:120px;font-size:12px;">

<#else>

<div style="width:400px;margin:0 auto;margin-top:30px;font-size:12px;">

</#if>
	<img src="/bap/static/errorimg/system.gif"></img>
	<span style="bottom:20px; position:relative;">${getHtmlText('foundation.error.message')}
	<a id="linkShow" href="javascript:void(0);" onclick="javascript:$('#detailMsg').show();$(this).hide();$('#linkHide').show();">${getHtmlText('foundation.error.detailmessage')}</a>
	<a id="linkHide" style="display:none;" href="javascript:void(0);" onclick="javascript:$('#detailMsg').hide();$(this).hide();$('#linkShow').show();">${getHtmlText('foundation.error.hidemessage')}</a>
	</span>
<script type="text/javascript" language="javascript" charset="utf-8">
function page500Load(){
	if(typeof jQuery == "undefined"){
		var head = document.getElementsByTagName('head')[0];
        var script = document.createElement('script');
        script.src = '/bap/static/core/jquery.js';
        script.type = 'text/javascript';
        head.appendChild(script);
	}
	var pd = window.parent;
	if(pd && pd.foundation && pd.foundation.common) {
		pd.foundation.common._errorCode = 500;
	}
}
$(document).ready(function() {
<#if isMobile?? &&isMobile>
	$(document.body).html("<div class='m_head'><div class='m_hbox'><span onclick='pageBackFunc()' class='head_arrow'><a href='javascript:;' class='m_arrow' ></a></span><h2 class='m_title'></h2></div></div><div class='m_center'><div class='m_centerBox' ><div class='m_errorpic'><img src='/bap/static/errorimg/pic_500.png' alt='500报错图片'></div><div class='m_errorinfo'>发生内部错误，请联系管理员！</div><div class='m_refresh'><img src='/bap/static/errorimg/btn_sx.png' alt='刷新' onclick='refresh()'></div></div></div>");
	</#if>
});
function refresh(){
	location.reload(window.location.href)
}

// 返回上一个页面
function pageBackFunc() {
	try {
		window.mobilejs.webGoBack();
	} catch (err) {
		try {
			window.parent.window.mobilejs.webGoBack();
		} catch (err) {
			try {
				window.history.back();
			} catch (err) {
				
			}
		}
	}
}
</script>
<div id='detailMsg' style="display:none;">
<#assign fieldErrors = errMsg.items>
<#assign actionErrors = errMsgErrors>

<#-- 字段验证错误 -->
<#if fieldErrors?size gt 0>
<#assign keys = fieldErrors?keys>
<details>
	<summary>${getText('foundation.error.fieldmessage')}</summary>
	<dl>
		<#list keys as key>
		<li>${key}:${fieldErrors[key]}</li>
		</#list>
	</dl>
</details>
</#if>

<#-- Action错误 -->
<#if actionErrors?size gt 0>
<details>
	<summary>${getText('foundation.errormessage')}</summary>
	<dl>
		<#list actionErrors as error>
		<li>${error}</li>
		</#list>
	</dl>
</details>
</#if>


<#-- 系统异常 -->
<#if errMsg.exceptionMsg??>
<details>
	<summary>${getText('foundation.error.systemmessage')}</summary>
	${errMsg.exceptionMsg}
</details>
</#if>
</div>
</div>

<#if (Parameters.requstObjectType!'') != 'dialog'>

</body>
</html>

</#if>