
<style>
.dropdown {
outline: none;
display: inline-block;
width: 100%;
float: left;
height: 20px;
}
.cui-edit-clear {
background: url("/bap/static/images/clear.gif") no-repeat scroll transparent;
border: medium none;
cursor: pointer;
display: block;
height: 16px;
position: absolute;
right: 20px;
top: 5px;
width: 16px;
}
</style>


<@s.hidden name="closePage"/>
<@errorbar id="assignSpecialPermissionListFrameErrorBar" />
<div  style="width:100%;height:100%"  >
	<input type="hidden"  id="changed"  value="0"  />
	<input type="hidden"  id="type"  value="${type!}"  />
	<form id="specialPermissionSelectForm"  >
	<table  style="margin:auto;margin-top:10px;width:70%">
	<tr>
			<td class="edit-table-symbol"   style="text-align: left;vertical-align:center" >
				<label width:100%;;" value="${getText('Test.propertydisplayName.randon1445824498795')}" >${getText('Test.propertydisplayName.randon1445824498795')}</label>
			</td>
			<td  nullable=true class="edit-table-content"   style="text-align: left;" width="150px">																																							
				<@systemcode property_type="SYSTEMCODE" showType="SELECTCOMP" onchange=""  viewType="${viewType!}" deValue="${rabbit_codeTest2_defaultValue!}" formId="specialPermissionSelectForm" classStyle="cui-noborder-input" ecFlag=true multable=false  name="rabbit.codeTest2.id" code="treeTest2" value="${(rabbit.codeTest2.id)!}"  />
			</td>
	</tr>
	</table>
	</form>
</div>

<script type="text/javascript"  language="javascript">
(function(){
	
		CUI.ns("foundation.specialPermissionSystemCodeSel");
      
       
})();

</script>
