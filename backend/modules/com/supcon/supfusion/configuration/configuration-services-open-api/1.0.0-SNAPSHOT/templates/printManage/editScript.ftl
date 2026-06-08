<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=8"/>
<style type="text/css">
.t{background:blue;cursor:pointer;}
.t tr{background:#FFF;}
.selected{background:#f2f200;}
.layoutselect{display:none;}
.cui-search-click {right:2px!important;top:3px;}
.baphelp-icon {
	vertical-align: text-bottom;	
}

.baphelp-info {
	
}

.baphelp-example {
	background-color: #f2f2f2;
	margin-left: -12px;
	margin-right: -12px;
	padding: 5px 10px;
	margin-top: 5px;
	margin-bottom: 5px;
}


.baphelp-hint {
	margin-top: 5px;
	padding: 6px 5px 0 0;
	border-top: 1px dashed #e4e4e4;
	color: #b10000;
}

.baphelp-code code{
	font-family: arial;
}
</style>
<@errorbar id="ec_print_template_formDialogErrorBar"></@errorbar>
<@s.form  id="ecPrintTemplateEditScriptForm" action="editScript" namespace="/msService/ec/printManage" validate="false" callback="ec.print.template.scriptCallBack">
	<@s.hidden name="printTemplate.isPublish" />
	<@s.hidden name="printTemplate.ecEnv" />
	<@s.hidden name="printTemplate.version" />
	<@s.hidden name="printTemplate.entity.code" />
	<@s.hidden name="printTemplate.code" />
	<@s.hidden name="entity.workflowEnabled" />
	<@s.hidden name="printTemplate.processKey" />
	<@s.hidden name="printTemplate.processVersion" />
	<@s.hidden name="printTemplate.templateEnabled" />
	<@s.hidden name="printTemplate.extraParamScript" />
	<table style="margin-top:8px" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la">${getText("ec.print.template.beforeScript")}
			<span id="beforeScriptHelpinfo" class="baphelp-icon"></span>
			<div id="beforeScriptHelpinforef" style="display:none">
				<p class="baphelp-info">提供groovy脚本接口，脚本在点击打印按钮之后，弹出选择打印模板框之前执行，可对打印模板集合进行过滤，脚本可获取当前视图关联的所有打印模板（templatesList），当前视图编码（viewCode），
				当前表单/基础数据对象（obj），脚本需输出的参数为：过滤后的打印模板集合</p>
				<p class="baphelp-example">范例</p>
				<div class="baphelp-code">
					<pre><code>def resultTemp=[]
for(int i=0;i&lt;templatesList.size();i++){
    if(templatesList.get(i).getTemplateBusinesscode().equals("123")){
          resultTemp&lt;&lt;templatesList.get(i);
     }
}
return resultTemp;</code></pre>
</div>
				<p class="baphelp-hint">注意：TemplateBusinesscode为业务特征码，在编辑模板时可配置</p>
				<script type="text/javascript">
							$('#beforeScriptHelpinfo').helptip({refElm: "#beforeScriptHelpinforef", html: true , isCustom :false, width: 550 , title :"说明"});
				</script>
			</div>
			</td>
			<td class="co" colspan="3">
					<@s.textarea  name="beforeScript"  cssClass="cui-edit-textarea"  cssStyle="margin-top:5px;width:550px;height:200px"/>
			</td>
		</tr>
		<tr>
			<td class="la">${getText("ec.print.template.afterScript")}
													<span id="afterScriptHelpinfo" class="baphelp-icon"></span>
			<div id="afterScriptHelpinforef" style="display:none">
				<p class="baphelp-info">提供groovy脚本接口，在选择打印模板后执行，脚本可获取当前打印使用的模板（printTemplate），当前视图编码（viewCode），
				当前表单/基础数据对象（obj），打印生成的pdf路径（pathTemp），打印完成接口不需要输出参数</p>
				<p class="baphelp-example">范例</p>
				<div class="baphelp-code">
					<pre><code>import java.nio.file.Files
import java.nio.file.Paths
File directory = new File("."); 
def printThreadInfo={
	Thread currentThread = Thread.currentThread()
	File source = new File(directory.getCanonicalPath()+"/"+pathTemp)//待拷贝文件路径
	File dest = new File("E://print//"+viewCode+obj.getTableInfoId()+System.currentTimeMillis()+".pdf")//拷贝后路径
	try{
		Files.copy(source.toPath(), dest.toPath());
	} catch (IOException e){
		e.printStackTrace();
	}
 }
 new Thread( {
 	new Object().sleep(5000)//由于是生成pdf是异步过程，需要等待pdf生成后再进行拷贝
 	printThreadInfo();
    }).start()</code></pre>
</div>
				<p class="baphelp-hint">注意：TemplateBusinesscode为业务特征码，在编辑模板时可配置，pathTemp路径为以“bap-workspace”开始的pdf文件路径</p>
				<script type="text/javascript">
							$('#afterScriptHelpinfo').helptip({refElm: "#afterScriptHelpinforef", html: true , isCustom :false, width: 720 , title :"说明"});
				</script>
			</td>
			<td class="co" colspan="3">
					<@s.textarea  name="afterScript"  cssClass="cui-edit-textarea"  cssStyle="margin-top:5px;width:550px;height:200px"/>
			</td>
		</tr>
	</table>
</@s.form>