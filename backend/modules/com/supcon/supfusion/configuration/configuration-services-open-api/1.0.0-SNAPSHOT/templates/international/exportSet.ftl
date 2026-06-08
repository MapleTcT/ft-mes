<@errorbar id="foundation_international_export_formDialogErrorBar" />
<form id="foundation_international_export_form" name="exportForm" action="/foundation/international/downloads.action" method="post" target="exportFrame">
	<table class="infoTable" id="editInfo"  cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<tr>
			<td style="width: 30%;padding-right: 10px;" align="right">${getHtmlText('foundation.inter.exportMo')}</td>
			<td style="width: 50%">
				<select id="exportinternational" name="moduleCode" style="width: 50%">
						
						<option value="all">${getText('foundation.inter.all')}</option>
						<option value="foundation">${getText('foundation.inter.base')}</option>
						<option value="ec">${getText('foundation.inter.cfg')}</option>
					<#list internationalMap?keys as key>
						<option value="${key}">${internationalMap[key]}</option>
					</#list>
				</select>
			</td>
		</tr>
		
		
	</table>
	<iframe style="display:none" name="exportFrame" id="exportFrame"></iframe>
</form>
<script type="text/javascript" charset="UTF-8" language="javascript">
$("#exportinternational").mSelect();
</script>