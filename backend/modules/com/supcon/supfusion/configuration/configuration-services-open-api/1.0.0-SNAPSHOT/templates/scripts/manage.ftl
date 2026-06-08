<@frameset id="ec_script_publish_container">
	<@frame id="ec_script_publish_container_main" offsetH=4>
		<@datatable id="ec_script_publish_datatable" hidekey="['id']" dblclick="script_dblclick" editable=false transMethod="post" dataUrl="/msService/ec/scripts/list?entityCode="+entityCode>
		<@operatebar operates="code:script_btn_add||name:${getHtmlText('js.ec.script.manager.new')}||iconcls:add||onclick:script_form_manager('add');
						code:script_btn_mod||name:${getHtmlText('js.ec.script.manager.modify')}||iconcls:edit||onclick:script_form_manager('modify');
						code:script_btn_del||name:${getHtmlText('删除')}||iconcls:del||onclick:script_form_manager('delete');"
						 operateType="noPower" resultType="json">
				</@operatebar>
			<@datacolumn key="name" label="${getHtmlText('js.ec.script.manager.name')}" width=150/>
			<@datacolumn key="scriptCode" label="${getHtmlText('js.ec.script.manager.code')}" width=150/>
			<@datacolumn key="description" label="${getHtmlText('js.ec.script.manager.descript')}" width=250/>
		</@datatable>
	</@frame>
</@frameset>
<@errorbar id="workbenchErrorBar" offsetY=10 />
<div id="ec_scr_script_del_div" style="display:none;">
<div style="padding:8px 8px 8px 8px"">
	<div>
	${getHtmlText('是否确认删除该脚本，删除后无法恢复！')}
	</div>
</div>
<script type="text/javascript">
function script_dblclick(){
	script_form_manager('modify');
}
function script_form_manager(action){
	switch(action){
	case 'add' : 
		window.open("/msService/ec/scripts/edit?entityCode=${entityCode}");
		break;
	case 'modify' : 
		if(ec_script_publish_datatableWidget.selectedRows[0])
			window.open("/msService/ec/scripts/edit?entityCode=${entityCode}&id=" + ec_script_publish_datatableWidget.selectedRows[0].id);
		else
			CUI.Dialog.alert("${getHtmlText('js.ec.script.manager.selectRow')}");return;
		break;
	case 'delete' : 
		if(ec_script_publish_datatableWidget.selectedRows.length==0){
			CUI.Dialog.alert("<span i18n='ec.model.choicedelpro'>未选择要删除的脚本</span>");
			return;
		}
		ec_property_dialog = new CUI.Dialog({
				title: "<span i18n='ec.common.delChoise'>删除选项</span>",
				elementId: "ec_scr_script_del_div",
				modal:true,
				width:376,
				height:95,
				buttons:[
						{	name:"<span i18n='ec.view.delete'>删除</span>",
							handler:function(){
								this.close();
								deleteMethod("/msService/ec/scripts/delete");
							}
						},
						{	name:"<span i18n='common.button.cancel'>取 消</span>",
							handler:function(){this.close()}
						}]
			});
		ec_property_dialog.show();
		break;
	}
}
function refreshScriptList(){
	datatable_ec_script_publish_datatable.setRequestDataUrl("/msService/ec/scripts/list?entityCode=${entityCode}");
}

deleteMethod = function(url) {
		closeLoadPanel();
		createLoadPanel(false, null, {head: '正在处理，请稍等', show: true, opacity:(50), bgColor:("#666666")});
		setTimeout(function(){
			$.ajax({
				data : {id:ec_script_publish_datatableWidget.selectedRows[0].id},
				url : url , 
				success : function(msg){
					closeLoadPanel();
					if(msg && msg.isSuccess){
						CUI.Dialog.alert("<span i18n='common.delete.success'>"+msg.message+"</span>");
						refreshScriptList();
					}else{
						workbenchErrorBarWidget.showMessage(msg.message, "f");
					}
				}	
			});
		},1500);
	}
</script>