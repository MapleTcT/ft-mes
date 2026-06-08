<div id="batchGenerateSelectOutDiv" style="overflow:auto;height:100%" >
<div id="batchGenerateSelectDiv" style="overflow:auto">
<@errorbar id="batchDeployWaitErrorBar" offsetY=0 />
<@datatable  formId="batchGenerateForm" dtPage="page" hidekey="['code','version','category']"  id="ec_modules_datatable"  dataUrl="/msService/ec/module/listModules" style="margin:-35px 0px 0px 0px;" paginator=false noPadding=true moreButtonResizeFlag=false withoutConfigTable=true width=740 height=460 renderOverEvent="ec_modules_datatableRenderOverEvent" >
<@datacolumn rowMerge=true key="category" label="${getHtmlText('ec.module.category')}" width="120" />
<@datacolumn key="code" label="${getHtmlText('ec.module.code')}"  width="120" />
<@datacolumn key="nameInternational" label="${getHtmlText('ec.module.name')}"  width="150" />
<@datacolumn key="isPublish" type="boolean" textalign="center" label="${getText('ec.module.yqd')}" width="60" />
<@datacolumn key="autoDeploy" type="checkbox" checkall="true" textalign="center" label="${getText('ec.module.fabu')}" width="80"/>
<@datacolumn key="checkboxFast" type="checkbox" checkall="true" textalign="center" label="${getText('ec.module.kuaisu')}" width="60"  />
<@datacolumn key="checkboxNormal" type="checkbox" checkall="true" textalign="center" label="${getText('ec.module.putong')}" width="60" />
</@datatable>

</div>

<div id="progressivebatchLog" style="display:none;" ></div>
</div>
<script>
var customThemeCode = '${isMsService}';
if(customThemeCode=='Mis'){
	$('[name="static"]').remove();
	$('#operateTool').hide();
	$('#operateAfter').after("<iframe style='margin-left:11px;width:748px;height:100%' name='mainIframe' src='/msService/ec/msModule/page?isMsService=${isMsService}' frameborder='0' scrolling='auto' ></iframe>");
}
var autoDeployMap = "";
function ec_modules_datatableRenderOverEvent() {
	//先把所有的模块发布的模式保存到全局变量autoDeployMap中
	setAutoDeployMap();
	$('#batchGenerateSelectOutDiv').find("td[key='checkboxFast'],td[key='checkboxNormal']").hide();
	scrollCheckedModuleIntoView('checkboxNormal');
	//在所有的自动发布的选项上绑定onclick事件
	setAutoDeployEvent();
}
function scrollCheckedModuleIntoView(checkedKey) {
    var checkedNode = ec_module_Tree.getSelectedNodes(),
    checkedCode;
    var checkedIsPublish;
	if (checkedNode.length) {
	    checkedCode = checkedNode[0].code;
	    if ((!checkedCode && !checkedNode[0].id) || checkedNode[0].id == -1) {
			handleCategorySubNodeCheck(checkedNode[0], checkedKey);
			return;
		}
	    if(autoDeployMap[checkedNode[0].code]==1){
			checkedKey = 'checkboxFast';
	    }
	    $(datatable_ec_modules_datatable.rowsJsonObj).each(function(index, rowObj) {
	        var rowCode = rowObj.code,
	            rowHtmlObj,
	            checkInput;
	        if (rowCode == checkedCode) {
	            rowHtmlObj = $(rowObj.rowHtmlObj);
	            checkInput = rowHtmlObj.find('td[key="' + checkedKey + '"] input')[0];
	            autoDeployInput = rowHtmlObj.find('td[key="autoDeploy"] input')[0];//在开始点开发布页面的时候勾上选中模块的自动发布勾
	            if (!checkInput.checked) {
	                checkInput.click();
	            }
	            if (!autoDeployInput.checked) {
	                autoDeployInput.click();
	            }
	            setTimeout(function(){
	                scrollCheckboxIntoViewIfNeed(checkInput);
	            }, 0)
	            return false;
	        }
	    })
	}
}


function handleCategorySubNodeCheck(checkedNode, checkedKey) {
	var checkedNodeCategory = checkedNode.category;
	//var autoDeployData = getAutoDeployData(checkedNode)
	$(datatable_ec_modules_datatable.rowsJsonObj).each(function(index, rowObj) {
        var rowCategory = rowObj.category,
            rowHtmlObj,
            checkInput;
        if ((rowCategory == checkedNodeCategory) || checkedNode.id == -1) {
        	//从autoDeployMap获取的数据中取得模块到底是快速发布还是普通发布
			if(autoDeployMap[rowObj.code]==1){
				checkedKey = 'checkboxFast';
			}else{
				checkedKey = 'checkboxNormal';
			}
            rowHtmlObj = $(rowObj.rowHtmlObj);
            checkInput = rowHtmlObj.find('td[key="' + checkedKey + '"] input')[0];
            autoDeployInput = rowHtmlObj.find('td[key="autoDeploy"] input')[0];
            if (!checkInput.checked) {
                checkInput.click();
            }
            if (!autoDeployInput.checked) {
	            autoDeployInput.click();
	        }
            setTimeout(function(){
                scrollCheckboxIntoViewIfNeed(checkInput);
            }, 0)
        }
    })
	var length = $('#ec_modules_datatable input[name="ec_modules_datatable_checkall_autoDeploy"]').length
	if(length>1){
		$('input[name="ec_modules_datatable_checkall_autoDeploy"]:eq(1)').trigger('click').trigger('click')
	}
}

function scrollCheckboxIntoViewIfNeed(checkInput){
	var $input = $(checkInput),
		$tr = $input.closest('tr'),
		$bd = $input.closest('.datagrid-bd'),
		$fd = $bd.siblings('.datagrid-fd'),
		trHeight = $tr.height(),
		trOffsetTop = $tr[0].offsetTop,
		bdHeight = $bd[0].clientHeight,
		diff = trOffsetTop + trHeight - bdHeight;
		
	if (diff > 0) {	
		$bd.scrollTop(diff);
		$fd.scrollTop(diff)
	}
}

function setAutoDeployEvent(){
	$(datatable_ec_modules_datatable.rowsJsonObj).each(function(index, rowObj) {
	    var rowHtmlObj = $(rowObj.rowHtmlObj);
	    var autoDeployInput = rowHtmlObj.find('td[key="autoDeploy"] input')[0];
	    if(autoDeployMap[rowObj.code]==1){
	        checkedKey = 'checkboxFast';
	    }else{
	        checkedKey = 'checkboxNormal';
	    }
	    var checkInput = rowHtmlObj.find('td[key="' + checkedKey + '"] input')[0];
	    var checkTd = rowHtmlObj.find('td[key="' + checkedKey + '"]');
	    $(autoDeployInput).unbind('change').change(function(){
	        if(!autoDeployInput.checked){
	            rowHtmlObj.find("td[key='checkboxFast']").attr("truevalue","false");
	            if(rowHtmlObj.find('td[key="checkboxFast"] input')[0].checked==true){
					rowHtmlObj.find('td[key="checkboxFast"] input').trigger('click')
	            }
	            rowHtmlObj.find("td[key='checkboxNormal']").attr("truevalue","false");
	            if(rowHtmlObj.find('td[key="checkboxNormal"] input')[0].checked==true){
					rowHtmlObj.find('td[key="checkboxNormal"] input').trigger('click')
	            }
	        }else{
	            checkTd.attr("truevalue","true");
	            if(checkInput.checked==false){
	            	$(checkInput).trigger('click')
	            }
	        }
	    })
	})
}
function setAutoDeployMap(){
	var moduleCodes='';
	var autoDeployList = '';
	$(datatable_ec_modules_datatable.rowsJsonObj).each(function(index, rowObj){
		moduleCodes+=','+rowObj.code;
	})
	moduleCodes=moduleCodes.substr(1);
	$.ajax({
		url : '/msService/ec/module/getAutoDelpoyState?moduleCodes='+moduleCodes,
		dataType: 'json',
    	type: 'POST',
    	async: false,
		success : function(data) {
			autoDeployList=data;
			autoDeployMap=data;
		}
	})
}
</script>