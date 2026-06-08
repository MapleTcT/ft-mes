<#if (Parameters.openType)?default('page') == 'frame'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.proxyPending.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
<#assign callbackName="parent.proxyPendingCallBackInfo">
<#else>
<#assign callbackName="proxyPendingCallBackInfo">
</#if>

<@errorbar id="proxyPendIngDialogErrorBar" />
<@s.form errorBarId="proxyPendIngDialogErrorBar" id="SubmitForm" action="proxyPendingResult" namespace="/msService/ec/workflow" validate="true" callback=callbackName>
	<@s.hidden name="pendingId" id="pendingId" />
	<@s.hidden name="pendingIds" id="pendingIds" />
	<@s.hidden name="proxyUsers" id="proxyUsersInput" />
	<table class="cui-fd-infotable" id="editInfo" cellpadding="0" cellspacing="0">
		<tr>
			<td style="width:20%;" class="lab cui-lmust">${getHtmlText('ec.proxyPending.proxyType')}</td>
			<td style="width:80%">
				<#if isCountersign?? && isCountersign><input type="radio" name="proxyType" id="proxyType1" class="cui-radio" onkeydown="if(event.keyCode==13) return false" value="3" checked="true"></input>${getHtmlText('ec.proxyPending.allType')}<#else><input type="radio" id="proxyType" class="cui-radio" name="proxyType" value="2" checked="true"></input>${getHtmlText('ec.proxyPending.copyType')}<input type="radio" name="proxyType" id="proxyType1" class="cui-radio" onkeydown="if(event.keyCode==13) return false" value="3" ></input>${getHtmlText('ec.proxyPending.allType')}</#if>
			</td>
		</tr>
		<tr>
			<td class="lab cui-lmust">${getHtmlText('ec.proxyPending.proxySources')}</td>
			<td>
				<@mneclient iframe=true reftitle="${getText('ec.edit.refStaff')}" mneTip="${getText('ec.expectedConsign.consinger')}" isCrossCompany=true isWrap=true multiDivStyle="height:20px;overflow:hidden;" conditionfunc="proxyUsers_querycustomFunc()"  name="proxyUsers_" id="proxyUsers_" type="User" url="/msService/ec/foundation/user/common/userListFrameset.action" displayFieldName="staffname"  ids="" names=""  onkeyupfuncname="getproxyUsers_MultiInfo()" funcparam="crossCompanyFlag=true&multiSelect=true" clicked=true multiple=true mnewidth=260 isEdit=true />
			</td>
		</tr>
		<tr>
			<td class="lab v-align">${getHtmlText('ec.proxyPending.description')}</td>
			<td class="cui-vte">
				<div class="fix-input"><@s.textarea name="proxDesc" cssClass="cui-noborder-textarea" style="width:100%"  rows="5" cols=""/></div>
			</td>
		</tr>
	</table>
</@s.form>


<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
