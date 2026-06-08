<@errorbar id="international_edit_formDialogErrorBar"/>

<div>
	<input type="hidden" name="currentLanguage" value="${currentLanguage}">
	<@s.form id="editTreeRootNameForm" errorBarId="international_edit_formDialogErrorBar"  method="post" validate="true" 
		action="editInternationalValue" namespace="/foundation/international" callback="foundation.international.addCallback">
		<table cellspacing="0" cellpadding="0" width="90%" class="cui-fd-infotable">
			<tr>
				<td>${getHtmlText('ec.view.tree.treeRootName')}</td>
				<td>
					<@international name="textValue" key="${(key)!}" isNew=true maxLength=80></@international>
				</td>
			</tr>
			
		</table>
	</@s.form>
</div>

<script type="text/javascript" charset="UTF-8" language="javascript">
(function(){
	//注册命名空间
	CUI.ns("foundation.international");
})();	
</script>