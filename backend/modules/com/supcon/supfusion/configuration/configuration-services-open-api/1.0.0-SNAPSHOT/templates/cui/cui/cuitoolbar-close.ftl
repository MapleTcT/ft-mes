<ul id="toolbar">			
 <li id="toolbar_new"><a href="###">新增</a></li>
 <li id="tollbar_delete"><a href="###">删除</a></li>
 <li id="toolbar_copy"><a href="###">复制</a></li>
 <li><a href="###">|</a></li>
<#list parameters.operates as operate>
 <#if operate.type?exists && operate.type == 'separator'>
 <li><a href="###">|</a></li><#rt/>
 <#elseif !operate.type?exists || (operate.type?exists && operate.type != 'override')>
 <li <#rt/>
 <#if operate.id?exists>id="${operate.id}"</#if><#rt/>
 <#if operate.cssClass?exists>class="${operate.cssClass}"</#if><#rt/>
 <#if operate.cssStyle?exists>style="${operate.cssStyle}"</#if><#rt/>
 <#if operate.title?exists>title="${operate.title}"</#if><#rt/>
 <#if operate.onclick?exists>onclick="${operate.onclick}"</#if><#rt/>
 <#if operate?exists>action="${operate}"</#if><#rt/>
 <#if operate.target?exists>target="${operate.target}"</#if><#rt/>
 ><a href="###">${operate.label}</a></li>
 <#elseif operate.type?exists && operate.type == 'override'>
 <script language="javascript">
 <#if operate.forId?exists>
    if(document.getElementById("${operate.forId}") != undefined){
    <#if operate.label?exists && operate.label != "">
     document.getElementById("${operate.forId}").children[0].innerText = "${operate.label}";
    </#if>
    <#if operate.cssClass?exists && operate.cssClass != "">
     document.getElementById("${operate.forId}").className = "${operate.cssClass}";
    </#if>
    <#if operate.cssStyle?exists && operate.cssStyle != "">
     document.getElementById("${operate.forId}").style = "${operate.cssStyle}";
    </#if>
    <#if operate.title?exists && operate.title != "">
     document.getElementById("${operate.forId}").title = "${operate.title}";
    </#if>
    <#if operate.target?exists && operate.target != "">
     document.getElementById("${operate.forId}").target = "${operate.target}";
    </#if>
    <#if operate?exists && operate != "">
     document.getElementById("${operate.forId}") = "${operate}";
    </#if>
    <#if operate.onclick?exists && operate.onclick != "">
     document.getElementById("${operate.forId}").onclick = "";
     document.getElementById("${operate.forId}").attachEvent('onclick',function(){${operate.onclick}});
    </#if>
    }
 </#if>
 </script>
 </#if>
 </#list>
 <li><a href="###">|</a></li>
 <li id="toolbar_export"><a href="###">导出</a></li>					    
 <li id="toolbar_print"><a href="###">打印</a></li>
 <li><a href="###">|</a></li>
 <li id="toolbar_addfavorite"><a href="###">添加到收藏夹</a></li>
</ul>
<script language="javascript">
//定义action事件，如果存在onclick，则action失效
var boolbarElements = document.getElementById("toolbar").getElementsByTagName("LI");
for(var i=0; i<boolbarElements.length; i++){
	if(boolbarElements[i] == undefined) continue;
	if(boolbarElements[i].onclick != undefined && boolbarElements[i].onclick != "") continue;
	boolbarElements[i].attachEvent("onclick",function(){
		var handle = null;
		var open_url= event.srcElement;
		var window_height = window.screen.availHeight-63;
		var window_width  = window.screen.availWidth-20;
		ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,top=0,left=0,toolbar=no,menubar=no,location=no,status=yes";
		handle = window.open(open_url,"",ShowStyle);
		
		handle = null;
	});
}
</script>
</div>