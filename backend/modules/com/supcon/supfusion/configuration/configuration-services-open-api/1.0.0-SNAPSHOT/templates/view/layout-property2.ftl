<@errorbar id="layoutpartpropErrorBar" />
<div id="layoutpartprop">
<form>
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
		<tr>
			<td class="la" align="right" style="padding-right:5px">ID </td>
			<td class="co"> <input id="lp_id" name="lp_id" type="text" class="cui-edit-field" readonly="readonly" /></td>
			<td class="la block_view" align="right" style="padding-right:5px">${getText('ec.MenuInfo.ChoiceViews')} </td>
			<td class="co block_view" colspan="3"><select  style="max-width: 300px" id="lp_vcode" name="lp_vcode">
				<option value=""></option>
				<#list viewList as v>
				<#if v.showType != 'LAYOUT' && v.showType == 'PART' && v.type == view.type && v.entity.module.code == view.entity.module.code && (!(v.usedForTree??) || !v.usedForTree)>
				<option value="${v.code}" url="${v.url}">${getText(v.displayName)} (${v.name}) [${getText(v.entity.name)}]</option>
				</#if>
				</#list>
			</select></td>
		</tr>
		<tr class="block_view">
			<td class="la" align="right" style="padding-right:5px">URL </td>
			<td class="co" colspan="3"> <input type="text" class="cui-edit-field" id="lp_url" name="lp_url" /></td>
		</tr>
		<tr>
			<td class="la" width="20%" align="right" style="padding-right:5px">${getText('ec.view.width')} </td>
			<td class="co" width="30%"> <input type="text" class="cui-edit-field" id="lp_width" name="lp_width" /></td>
			<td class="la" width="20%" align="right" style="padding-right:5px">${getText('ec.view.high')} </td>
			<td class="co" width="30%"> <input type="text" class="cui-edit-field" id="lp_height" name="lp_height" /></td>
		</tr>
		<tr>
			<td class="la" align="right" style="padding-right:5px">${getText('ec.view.showtype')}</td>
			<td class="co">
				<select id="lp_ctype" name="lp_ctype" onchange="select_ctype(this.value)">
					<option value=""></option>
					<option value="tree">${getText('ec.model.dataType.2')}</option>
				</select>
			</td>
			<#--
			<td class="la permissionTd block_tree" align="right" style="padding-right:5px;display:none;">${getText('ec.view.isPermission')}</td>
			<td class="co permissionTd block_tree" style="display:none;">
				<select id="lp_tree_isPermission" name="lp_tree_isPermission">
					<option value="none">${getText('ec.select.no')}</option>
					<option value="yes">${getText('ec.select.yes')}</option>
				</select>
			</td>
			-->
		</tr>
		<#if (view.entity.crossCompanyFlag)?? && view.entity.crossCompanyFlag>
		<tr style="display:none;" class="block block_tree">
			<td class="la crossCompanyFlagTd block_tree" align="right" style="padding-right:5px;display:none;">${getText('ec.entity.crossCompanyFlag')}</td>
			<td class="co crossCompanyFlagTd block_tree" style="display:none;">
				<select id="lp_tree_crossCompanyFlag" name="lp_tree_crossCompanyFlag">
					<option value="none">${getText('ec.select.no')}</option>
					<option value="yes">${getText('ec.select.yes')}</option>
				</select>
			</td>
		</tr>
		</#if>
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">${getText('树片段')} </td>
			<td class="co" colspan="3">
				<select id="treeView" name="treeView">
					<#list viewList as v>
					<#if (v.type == "TREE" && view.type=="LIST") || (v.type == "REFTREE" && view.type=="REFERENCE")>
					<option value="${v.code}">${getText('${v.displayName!""}')}</option>
					</#if>
					</#list>
				</select>
			</td>
		</tr>	
		<#-- 
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">${getText('ec.view.treemodel')} </td>
			<td class="co">
				<select id="lp_tree_model" name="lp_tree_model" onchange="select_model(this.value)">
					<option value=""></option>
					<#list models as model>
					<#if model.dataType == 2>
					<option value="${model.code}">${getText(model.name)}</option>
					</#if>
					</#list>
				</select>
			</td>
			<td class="la" align="right" style="padding-right:5px">${getText('ec.view.showfield')}</td>
			<td class="co">
				<select id="lp_tree_property" name="lp_tree_property">
					<option value=""></option>
				</select>
			</td>
		</tr>
		
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">${getText('ec.view.querycondition')} </td>
			<td class="co" colspan="3">
				<input type="text" class="cui-edit-field" id="lp_tree_cond" name="lp_tree_cond" />
			</td>
		</tr>
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">${getText('ec.view.rootname')}</td>
			<td class="co" colspan="3">
				<input type="text" class="cui-edit-field" id="lp_tree_root" name="lp_tree_root" />
			</td>
		</tr>
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">JSON${getText('ec.view.datasource')} </td>
			<td class="co" colspan="3">
				<input type="text" class="cui-edit-field" id="lp_tree_source" name="lp_tree_source" />
			</td>
		</tr>
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">onclick </td>
			<td class="co" colspan="3">
				<textarea class="cui-edit-textarea" style="width:100%;height:60px;" id="lp_tree_click" name="lp_tree_click"></textarea>
			</td>
		</tr>
		<tr style="display:none;" class="block block_tree">
			<td class="la" align="right" style="padding-right:5px">ondbclick </td>
			<td class="co" colspan="3">
				<textarea class="cui-edit-textarea" style="margin-top:5px;width:100%;height:60px;" id="lp_tree_dblclick" name="lp_tree_dblclick"></textarea>
			</td>
		</tr>
		-->
	</table>
</form>
</div>
<script type="text/javascript">
	$(function(){
		fillData();
		var part = "${part!}";
		if("west1" == part){
			$("#lp_ctype").val("tree");
    		select_ctype("tree");
    		$("#lp_ctype").attr("disabled",true);                		
    	} else if("center1" == part){
    		select_ctype("");
    		$("#lp_ctype").attr("disabled",true);
    	}
	});
	function fillData(){
		var o = $('#${part}');
		$('input[id^="lp_"],textarea[id^="lp_"],select[id^="lp_"]', 'form').each(function(){
			
			var id = this.id;
			id = id.substr(3);
			$(this).val(o.attr(id));
		});
		
		if($('#west1').attr('treeView')){
		  $("#treeView").val( $('#west1').attr('treeView') );
		}
		select_ctype(o.attr('ctype'));
		select_model(o.attr('tree_model'),o.attr('tree_property'));
		
	}

	function select_model(val,p){
		if(!val)return;
		$.get("/msService/ec/view/findPropertiesByModel",{"model.code" : val},function(res){
			$("#lp_tree_property").empty();
			$.each(res,function(index,item){
				$("#lp_tree_property").append('<option value="' + item.name + '">' + item.name + '</option>');
			});
			if(p){
				$("#lp_tree_property").val(p);
			}
		},"json");
		if(val == "sysbase_1.0_department_base_department"){
			$(".permissionTd").show();
		} else if(val == "sysbase_1.0_position_base_position") {
			$(".permissionTd").show();
		} else {
			$(".permissionTd").hide();
		}
	}
	function select_ctype(val){
		if(!val){$(".block_tree").hide();$(".block_view").show();return;}
		$(".block").hide();
		$(".block_" + val).show();
		switch(val){
			case "tree":
				$('#lp_vcode').val('');
				$(".block_view").hide();
				$(".block_tree").show();
				break;
			case "" :
				$(".block_tree").hide();
				$(".block_view").show();
				break;
		}
	}
</script>