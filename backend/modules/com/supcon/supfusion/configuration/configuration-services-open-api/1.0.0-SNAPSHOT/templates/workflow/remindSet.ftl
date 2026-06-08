<#assign s=JspTaglibs["/META-INF/struts-tags.tld"]> 
<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />
<@errorbar id="remidnSubmitFormDialogErrorBar" />
<@s.form id="remidnSubmitForm" action="remind" namespace="/msService/ec/workflow" validate="true" callback="waitWork_remindCallBack">

	<@s.hidden name="id" id="pendingId" />
	<@s.hidden name="pendingIds" id="pendingIds" />
	<@s.hidden name="remindType" id="remindTypeId" />
	<style>
	  td.lab_contant span{position:relative;top:-1px;padding-right:5px;#top:-3px;}
	  td.lab_contant input{position:relative;top: 2px;#top:-2px}
	</style>
	<table class="cui-fd-infotable" id="editInfo" width="90%" cellpadding="0" cellspacing="0">
		<tr>
			<td style="width:15%;"  class="lab">${getHtmlText('ec.pending.remindType')}</td>

			<td style="width:35%" class='lab_contant'>				
					${getHtmlText('ec.remind.pending.chatTool')}<input  type="checkbox" id="pandion" name="pandion" value="pandion" checked="true" />&#160;&#160;&#160;${getHtmlText('ec.common.email')}<input  type="checkbox" id="email" name="email" value="true" checked="true" />&#160;&#160;&#160;${getHtmlText('ec.common.sms')}<input type="checkbox" id="sms" name="waitWork_remindCallBack" value="sms"  />
				
			</td>
		</tr>
		<tr>
			<td  class="lab v-align">${getHtmlText('ec.remind.pending.ps')}</td>
			<td  class="cui-vte">
			 <div class="fix-input">		
				<@s.textarea   cssClass="cui-noborder-textarea" style="width:100%" id="remindContent" name="remindContent"/>
			 </div>
			</td>
		</tr>
	</table>
</@s.form>
