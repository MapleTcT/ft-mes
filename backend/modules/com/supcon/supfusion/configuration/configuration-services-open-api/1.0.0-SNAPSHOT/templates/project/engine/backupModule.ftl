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

<div class="root" id="module_backup">
	<#list modules as module>
	<#if (module_index==0)>
		<div class="list-in">
			<input type="checkbox" name="ec.module.entity.selectAll"><span>${getText('foundation.common.allSelect')}</span></input>
		</div>
	</#if>
	<div class="list-in">
		<input type="checkbox" name="ec.module" value="${module.code}"><span>${getText('${module.name}')}</span></input>
	</div>
	</#list>
</div>

<script type="text/javascript" charset="utf-8" language="javascript">
$(function(){
	$('input[name="ec.module.entity.selectAll"]').unbind().click(function(ev){
	var checkb = ev ? ev.target ? ev.target : ev.srcElement : window.event.srcElement;
	if (!checkb || !checkb.name) {
		return;
	}
	$(this).parent().siblings().each(function(){
	    var checkbox=$(this).find('input').get(0);
	    if(checkb.checked!=checkbox.checked){
			checkbox.checked=checkb.checked
	    }
	})
})
})
</script>