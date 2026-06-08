<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.workflow.commission.pending')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<#--
<#if (Parameters.openType)?default('page') != 'dialog'>
<@head/>
</#if>
<@loadpanel text="${getText('ec.myPending.framesetTitle')}"/>
-->
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />	
<@errorbar id="myPendingManageErrorBar" />
<@tabview id="tab" ext="" pattern="vertical" tabs=[
{"label":"${getText('ec.myExpectedConsign.manage')}","hide":false,"requestUrl":"/msService/ec/pending/myPending/myExpectedConsign"}
,{"label":"${getText('ec.myProxyPending.manage')}","hide":false,"requestUrl":"/msService/ec/pending/myPending/myProxyPending"}
,{"label":"${getText('ec.myProxySourcePending.manage')}","hide":false,"requestUrl":"/msService/ec/pending/myPending/myProxySourcePending"}
,{"label":"${getText('ec.myProxySourceBill.manage')}","hide":false,"requestUrl":"/msService/ec/pending/myPending/myProxySourceBill"}
]>
<#--
,{"label":"${getText('foundation.staff.unassignstaffselect')}","hide":false,"requestUrl":"/foundation/staff/common/unassignCompanyStaff?closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}"}
-->
<div id="expectedConsignTab"></div>
<div id="proxyPendingTab"></div>
<div id="proxySourcePendingTab"></div>
<div id="proxySourceBillTab"></div>
</@tabview>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>