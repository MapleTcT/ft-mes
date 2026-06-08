<#macro texteditor name,formId,width,value,cssStyle,isReadOnly=false>
<#assign textareaid=name?replace('.','')>
<textarea id="${name?replace('.','')}" name="${name}" style="width:${width};${(cssStyle)!}" originalValue="${value?html}">${value}</textarea>
<script type="text/javascript">
var editor_${textareaid};
$(function(){
    editor_${textareaid} = KindEditor.create(
    	'#${textareaid}',
    	{
    		syncType:"",
    		<#if isReadOnly>readonlyMode:true,</#if>
    		afterChange:function(){this.sync();},
    		uploadJson:"/foundation/workbench/upload-text-editor",
    		/*allowFlashUpload:false,allowImageUpload:false,allowMediaUpload:false,*/
    		afterUpload:function(url){
    			var arrs = url.split('?id=');
    			var uploadDocIds = $('input[name="uploadDocIds"]');
    			if(uploadDocIds && uploadDocIds.length > 0) {
    				uploadDocIds.val(uploadDocIds.val() + ',' + arrs[1]);
    			} else {
    				$('<input type="hidden" name="uploadDocIds">').val(arrs[1]).appendTo('#${formId}');
    			}
    		}
    	});
    $("#${formId}").bind("beforeSubmit",function(){editor_${textareaid}.sync();});
});
</script>
</#macro>