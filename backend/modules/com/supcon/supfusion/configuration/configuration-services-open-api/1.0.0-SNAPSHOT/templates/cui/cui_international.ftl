<#-- 
isNew true 表示使用新样式 false 表示使用旧样式,maxLength=长度限制
showKey 是否显示key值
userInTree true/false表示禁用/启用树根节点随机生成国际化key
isOldEdit 老版放大镜修改样式
-->
<#macro international name,key="",readonly=false,view=false ,moduleCode="foundation",modelName="",cssClass="",cssStyle="",style="",isNew=false,maxLength=200,showKey=false,usedInTree=false,isOldEdit=false>
	<#if isNew>
		<#if isOldEdit>
			<#assign random="International"+datetime("YYYYMMDDHHmmss")/>
			<@internationalNewOldEdit usedInTree=usedInTree name=name key=key readonly=readonly view=view moduleCode=moduleCode modelName=modelName cssClass=cssClass cssStyle=cssStyle style=style formId=random maxLength=maxLength showKey=showKey isOldEdit=isOldEdit/>
		<#else>
			<#assign random="International"+datetime("YYYYMMDDHHmmss")/>
			<@internationalNew usedInTree=usedInTree name=name key=key readonly=readonly view=view moduleCode=moduleCode modelName=modelName cssClass=cssClass cssStyle=cssStyle style=style formId=random maxLength=maxLength showKey=showKey isOldEdit=isOldEdit/>
		</#if>
	<#else>
		<@internationalOld name=name key=key view=view moduleCode=moduleCode modelName=modelName cssClass=cssClass cssStyle=cssStyle style=style usedInTree=usedInTree isOldEdit=isOldEdit/>
	</#if>
</#macro>
<#macro internationalNewOldEdit name,usedInTree=false,key="",readonly=readonly,view=false ,moduleCode="foundation",modelName="",cssClass="",cssStyle="",style="",formId="",maxLength=-1,showKey=true,isOldEdit=isOldEdit>
	<#assign showValue="${getText(key)}">
	<#assign languages = languageMethod()>
	<#if view>
	<div class="fix-input-readonly"  <#if cssStyle??>style="${cssStyle}"</#if>>
		<div class="fix-search-click">
		<input type="hidden" name="${name!}" id="international_${name?replace(".", "")}" value="${key!?html}"/>
		<input type="text" onchange="fnInternational_isOldEdit_${name?replace(".", "")}OnChange(this)" <#if readonly??&&readonly> readonly </#if> name="international_${name?replace(".", "")}_showName" languageName="${getCurrent('language')}" id="international_${name?replace(".", "")}_showName"  class="cui-noborder-input" <#if style??>style="${style}"</#if> value="${showValue}"/>
		<input type="button" id="international_${name?replace(".", "")}_loading" class="cui-international-save-loading"/>
		<input type="button" onclick="${formId}_InternationalDialog_${name?replace(".", "")}(this,event);" title="${getText('foundation.theme.selectLanguage')}" value="" class="cui-international-click"/>
		</div>
	</div>
	<#else>
	<div class="fix-input">
		<div class="fix-search-click">
		<input type="hidden" name="${name!}" id="international_${name?replace(".", "")}" value="${key!?html}"/>
		<input type="text" onchange="fnInternational_isOldEdit_${name?replace(".", "")}OnChange(this)" name="international_${name?replace(".", "")}_showName" languageName="${getCurrent('language')}" id="international_${name?replace(".", "")}_showName"  class="cui-noborder-input" <#if style??>style="${style}"</#if> value="${showValue}"/>
		<input type="button" id="international_${name?replace(".", "")}_loading" class="cui-international-save-loading"/>
		<input type="button" onclick="${formId}_InternationalDialog_${name?replace(".", "")}(this,event);" title="${getText('foundation.theme.selectLanguage')}" value="" class="cui-international-click"/>
		</div>
	</div>
	</#if>
	
	<style type="text/css">
	 .InterDivContainer_class{position:absolute;border:1px solid #C0D8F0;z-index:20001;text-align:left;white-space:nowrap;text-overflow:clip;background:#fff;overflow:hidden;background:#fff;padding:0;padding-bottom:10px;}
	.cui-international-click {
		background: url("/bap/static/images/language/${getCurrent('language')}.png") no-repeat scroll 0 0 transparent;
		border: medium none;
		cursor: pointer;
		display: block;
		width: 16px;
		height: 16px;
		position: absolute;
		right: 5px;
		top: 4px !important;
	}
	.cui-international-save-loading {
		background: none;
		border: medium none;
		cursor: auto;
		display: none;
		width: 16px;
		height: 16px;
		position: absolute;
		right: 25px;
		top: 1px !important;
		background-size: 16px 16px !important;
	}
	.ec-config-page .cui-international-click {
		top: 1px;
	}
	.cui-international-span-font {
		position:absolute;
		right:12px;
		top:5px;
		text-decoration:underline;
		color:blue;
		cursor:pointer;
	}
	.InterDivContainer_class .dropdown {
		width: 120px;
		height: 28px;
		line-height: 25px;
		display: inline-block;
		float: none;
		background-color: #fff;
	}
	.select-border-style {
		width:120px;
		position: absolute;
		height: 28px;
		opacity: 0;
		font: 14px/20px "Microsoft YaHei";
	}
	.language-flag {
		padding-bottom: 10px;
	}
	input.language-value {
		float: right;
	}
	ul.edit-select-box {
		z-index: 20003;
	}
	</style>
	
	<script type="text/javascript">
		
		CUI.ns('foundation.international');
		
		// 选择按钮回调	
		foundation.international.sendBackInternational${name?replace(".", "")}Select=function(){
			foundation.international.select${name?replace(".", "")}International();
		}	
		
		var lanList = [];
		<#list languages as l>
			lanList.push('${l.key}');
		</#list>;
		
		foundation.international.saveInternationalValue_${name?replace(".", "")} = function (key, obj){
			var currentLanguage = '${getCurrent('language')}';
			// 更改key
			$('#' + foundation.international.${name?replace(".", "")}inputkeyid).val(key);			
			
			// 循环去值
			for (var i=0; i<lanList.length; i++){
				if (typeof obj[lanList[i]] === 'undefined'){
					obj[lanList[i]] = '';
				}
			}
			
			// 循环设值
			for (var k in obj){
				if (k != currentLanguage){// 改变其他语言值
					// 更改多语言key
					// 更改多语言显示值
					$('[languagename="'+ k +'"]', '#${formId}_content_${name?replace(".", "")}').val(obj[k]).attr('key', key);
				} else {// 优先改变当前显示值
					$('#' + foundation.international.${name?replace(".", "")}inputshownameid).val( obj[currentLanguage] );
				}
			}			
		}
		
		//提取国际化值
		foundation.international.getInternationalValue_${name?replace(".", "")} = function (){
			// 旧版直接保持从key中查询结果，不覆盖
			foundation.international.currentEditValObj = false;
		};
	
		//旧版
		//国际化key隐藏域id
		foundation.international.${name?replace(".", "")}inputkeyid = 'international_${name?replace(".", "")}';	
		//国际化实际显示值
		foundation.international.${name?replace(".", "")}inputshownameid = 'international_${name?replace(".", "")}_showName';
		
		// 国际化对话框中选择某项回调(双击)
		foundation.international.get${name?replace(".", "")}International=function(arr){
			
			try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}
			var obj = arr[0]
			var key=arr[0].key;
			delete obj.id;
			delete obj.key;
			foundation.international.saveInternationalValue_${name?replace(".", "")}(key, obj);
		}
		
		// 修改国际化对话框保存回调
		foundation.international.addCallback${name?replace(".", "")}=function(res){
			if(res.dealSuccessFlag == true){							
				
				var obj = foundation.international.getInternationalValueInput();
				// 先取值再关闭				
				try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}				
				var key=res.key;			
				foundation.international.saveInternationalValue_${name?replace(".", "")}(key, obj);
			}else{
				international_edit_formDialogErrorBarWidget.showMessage("提交失败","f");
			}
		}
		
		// 弹出修改国际化对话框	
		foundation.international.${name?replace(".", "")}InternationalManage=function(el){
			$(el).closest('.InterDivContainer_class').hide();
			
			// 提取当前页面国际化值
			foundation.international.getInternationalValue_${name?replace(".", "")}
			var selectFlag="";
			var idStr='#international_${name?replace(".", "")}';
			var key='';
			key=key || CUI("#" + foundation.international.${name?replace(".", "")}inputkeyid).val();
			
			if(key==""){
				var d=new Date();
				var radion=d.getTime();
				
				<#if modelName?? && modelName?length gt 0>
				key='${moduleCode}.'+'${modelName?replace(".", "")}'+'.radion'+radion;
				<#else>
				key='${moduleCode}.'+'${name?replace(".", "")}'+'.radion'+radion;
				</#if>
				<#if usedInTree>
					<#if modelName?? && modelName?length gt 0>
						key='${modelName!''}.' +'${name?replace(".", "")}';
					</#if>
				</#if>
			}
			
			var url="/msService/ec/foundation/international/editListFrame?moduleCode=${moduleCode}&name=${name?replace(".", "")}&callBackFuncName=foundation.international.get${name?replace(".", "")}International"
			url+="&key="+encodeURIComponent(key);
			var height=550;
			var width=850;
			var buttons=[
						
						{	name:"${getHtmlText('common.button.choose')}",
							handler:function(){foundation.international.sendBackInternational${name?replace(".", "")}Select();}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}];
			foundation.international.${name?replace(".", "")}dialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.basic.internationalizationManagement')}",
				url: url,
				modal:true,
				height:height,
				width: width,
				buttons:buttons
			});
			foundation.international.${name?replace(".", "")}dialog.show();			
		}	
	
		var ${formId}_id_${name?replace(".", "")} = '';
		
		<#-- 国际化默认修改不替换key，除非修改，且只第一次修改替换key -->
		function fnInternational_isOldEdit_${name?replace(".", "")}OnChange(obj) {
			var _nLimit = ${maxLength}; // 限制长度
			var _sValue=$.trim(CUI(obj).val());
			CUI(obj).val(_sValue);//替换去除空格后的值，和实际保存值保持统一
			var _languageName = CUI(obj).attr("languagename");
			var inputObj = CUI(obj).prev('input'); // 当前国际化key
			var bakKey = inputObj.attr('bak-key'); // 国际化key备份
			var _sKey = inputObj.val();
			// 如果国际化为空，则不存储
			if (!_sValue) {				
				// 判断是否是已有国际化，如果是，做好备份，然后清除该国际化值
				if (!bakKey && _sKey) {
					inputObj.attr('bak-key', _sKey);
				}
				// 值为空时，取消绑定当前国际化
				inputObj.val('');
				return;
			}
			
			// 如果有值，先判断bak是否有值，以及当前是否有国际化，如果没有恢复为bak
			if (!_sKey && bakKey){
				_sKey = bakKey;
				inputObj.val(bakKey);
			}
			if(_nLimit>0 && obj.value.length > _nLimit){
				_sValue = _sValue.substring(0, _nLimit);
			}
			var _sKey=CUI(obj).prev('input').val();
			
			// 如果数据不以.flag结尾则重新生成key且key添加.flag
			if (!_sKey.endsWith('.flag')) {
				var _sOldKey = _sKey;
				// 生成key
				var randon="randon"+new Date().getTime();
				<#if modelName?? && modelName?length gt 0>
					_sKey='${moduleCode}.${modelName?replace(".", "")}.'+randon;
				<#else>
					_sKey='${moduleCode}.${name?replace(".", "")}.'+randon;
				</#if>
				_sKey += '.flag';
				
				// 重新保存非主语言
				copyLocalMessage (_sKey, _sOldKey);
			}
			
			// 赋回原来的KEY
			CUI(obj).prev('input').val(_sKey);
			CUI(obj).val(_sValue);
			// loading样式切换
			// 兼容IE8 判断是否为IE8
			var browser = navigator.appName;
			var b_version = navigator.appVersion; 
			var version = b_version.split(";"); 
			var trim_Version =version[1]&&version[1].replace(/[ ]/g,""); 
			<#--做一个保存的假象-->
			if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
				CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/bap/static/images/wait.gif', sizingMethod='scale')", "display":"block"});
			} else {
				CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"url('/bap/static/images/wait.gif') no-repeat scroll 0 0 transparent", "display":"block"});
			}
			// 国际化存储
			saveLocalMessage(_sKey, _sValue, _languageName); // 保存国际化
			
			// 给所有地方附上国际化的key
			var _nLanguageLength = $("input[name='languageValue']").size();
			for (var n=0; n<_nLanguageLength; n++) {
				$("input[name='languageValue']").eq(n).attr("key", _sKey); // 给所有地方增加key这个字段，并附上国际化的key
			}
			
			<#--做一个保存的假象 做一些延迟渲染-->
			setTimeout(function () {
				// 判断是否为IE8
				if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/bap/static/images/success.gif', sizingMethod='scale')"});
				} else {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"url('/bap/static/images/success.gif') no-repeat scroll 0 0 transparent"});
				}
			},700);
			setTimeout(function () {
				// 判断是否为IE8
				if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"", "display": "none"});
				} else {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "display": "none"});
				}
			},1500);
		}
		
		<#-- 国际化默认修改不替换key，除非修改，且只第一次修改替换key -->
		function fnInternational_isOldEdit_${name?replace(".", "")}OnChangeNotDefaultLanguage(obj) {
			var _nLimit = ${maxLength}; // 限制长度
			var _sValue=CUI(obj).val();
			var _languageName = CUI(obj).attr("languagename");
			if(_nLimit>0 && obj.value.length > _nLimit){
				_sValue = _sValue.substring(0, _nLimit);
			}
			var _sKey=CUI(obj).attr("key");
			
			CUI(obj).val(_sValue);
			// loading样式切换
			// 兼容IE8 判断是否为IE8
			var browser = navigator.appName;
			var b_version = navigator.appVersion; 
			var version = b_version.split(";"); 
			var trim_Version = version[1].replace(/[ ]/g,""); 
			<#--做一个保存的假象-->
			if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
				CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/bap/static/images/wait.gif', sizingMethod='scale')", "display":"block"});
			} else {
				CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"url('/bap/static/images/wait.gif') no-repeat scroll 0 0 transparent", "display":"block"});
			}
			// 国际化存储
			// 如果国际化为空，则删除当前国际化
			if (!_sValue) {
				delSingleLanguage (_languageName, _sKey);
			} else {
				saveLocalMessage(_sKey, _sValue, _languageName); // 保存国际化
			}
			
			<#--做一个保存的假象 做一些延迟渲染-->
			setTimeout(function () {
				// 判断是否为IE8
				if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"progid:DXImageTransform.Microsoft.AlphaImageLoader(src='/bap/static/images/success.gif', sizingMethod='scale')"});
				} else {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"url('/bap/static/images/success.gif') no-repeat scroll 0 0 transparent"});
				}
			},700);
			setTimeout(function () {
				// 判断是否为IE8
				if ( browser == "Microsoft Internet Explorer" && trim_Version == "MSIE8.0" ) {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "filter":"", "display": "none"});
				} else {
					CUI(obj).parents(".fix-search-click").find(".cui-international-save-loading").css({"background":"none", "display": "none"});
				}
			},1500);
		}
		
		<#--国际化切换语言部分代码-->
		// 保存外部传进来的国际化的key
		var singleInternationalKey = "";
		function ${formId}_InternationalDialog_${name?replace(".", "")}(valueobj,e) {
			if (typeof YAHOO != "undefined" ) {
				var YUD = YAHOO.util.Dom;
			}
			// 保存外部传进来的国际化的key
			singleInternationalKey = CUI(valueobj).parent().children("input:eq(0)").val();
			${formId}_id_${name?replace(".", "")}='InterDivContainer_'+$(valueobj).parents('.ewc-dialog-blove:eq(0)').attr('id')+'_${name?replace(".", "")}';
			var xlength,ylength;
			ev = e || window.event;
			if (window.event) { // 停止事件冒泡
			 window.event.cancelBubble = true;
	        } else {
	         ev.stopPropagation();
	        }
			var divcontainer;
			//document.getElementById("${formId}_InterDivContainer_${name?replace(".", "")}")
			if(document.getElementById(${formId}_id_${name?replace(".", "")})){
				divcontainer = $("#"+${formId}_id_${name?replace(".", "")});
				if(divcontainer.is(":hidden")){
					$(".InterDivContainer_class").hide();
					xlength = YUD.getX(valueobj)+$(valueobj).width()-10;
					ylength =YUD.getY(valueobj)+$(valueobj).height();
					var widthOffset=$(document.body).width()-(xlength+350);
					if(widthOffset<0){
						xlength=xlength+widthOffset;
					}
					divcontainer.css({"left":xlength+"px","top":ylength+"px"});
					divcontainer.show(170);
				}else{
					divcontainer.hide(170);
					$(".InterDivContainer_class").hide(170);
				}
				return ;
			}else{
				divcontainer = document.createElement("div");
				divcontainer.id = ${formId}_id_${name?replace(".", "")};//"${formId}_InterDivContainer_${name?replace(".", "")}"; 
				divcontainer.style.width = 350 + 'px';
				$(divcontainer).addClass('InterDivContainer_class');
				document.body.appendChild(divcontainer);
				$("#"+${formId}_id_${name?replace(".", "")}).click(
					function(e){
						ev = e || window.event;
						if (window.event) { // 停止事件冒泡
							window.event.cancelBubble = true;
		        		} else {
		        			ev.stopPropagation();
		        		}
					}
			   )	
			}
			var s = "<div style='overflow: hidden;margin-bottom: 5px;'>"
					+"<h3 style='font-size:1.2em;font-weight:bold;padding:3px 0px 0px 10px;'><span>${getHtmlText('foundation.theme.otherLanguage')}</span></h3>"
					+'<span onclick="foundation.international.${name?replace(".", "")}InternationalManage(this)" class="cui-international-span-font">${getText('foundation.international.customSettings')}<span>'//运行打开修改对话框
					+"</div>";
			var options='<option value=""> </option>'
						<#list languages as l>
						+'<option value="${l.key}" internationalValue="${getText(l.internationalKey)?html}">${getText(l.internationalKey)?html}</option>'
						</#list>;
			var d = '<ul id="${formId}_content_${name?replace(".", "")}" class="international" style="padding-bottom: 20px;">'
					 <#if showKey>
					 +'<div style="margin-left: 4px;">${getHtmlText('foundation.international.key')}: <input onchange="${formId}_keyChange_${name?replace(".", "")}(this);" type="text" id="showKey" name="key" value="" style="border: 1px solid #C0D8F0;height: 18px;line-height: 18px; padding-left: 2px; padding-right: 1px;width: 260px;" /></div>'
					 </#if>
					<#assign n = 0 />
					<#list languages as l>
					  <#if l.key!=getCurrent('language')>
						<#if n<10>
						<#assign n = n+1 />
						+'<li>'
						+'<div style="float:left;*margin-top:0px;margin-left:4px;">'
						+'<img class="language-flag" src="/bap/static/images/language/spacer.png">'
						+'<select class="select-border-style" onchange="${formId}_selectChange_${name?replace(".", "")}(this);" lastselected="${l.key}" >'
						+'<option value=""></option>'
						<#list languages as la>
						+'<option value="${la.key}" internationalValue="${InternationalResource(key,la.key)?replace("'", "")?html}" <#if la==l>selected="selected"</#if> >${getText(la.internationalKey)}</option>'
						</#list>
						+'</select>'
						+'<input type="text" key="" value="${InternationalResource(key,l.key)?html}" name="languageValue" languagename="${l.key}" class="language-value" onchange="fnInternational_isOldEdit_${name?replace(".", "")}OnChangeNotDefaultLanguage(this)">'
						+'</div>'
						+'<div style="float: left;margin-left: 15px;display: none;">'
						+'<button type="button" id="add" class="aui-icon aui-toolbar-first aui-buttonitem-icon-only aui-state-default" onclick="${formId}_createDiv_${name?replace(".", "")}(this);"><span class="aui-icon aui-icon-plus"></span></button>'
						+'<button type="button" id="delete" class="aui-icon aui-toolbar-last aui-buttonitem-icon-only aui-state-default" onclick="${formId}_deleteDiv_${name?replace(".", "")}(this);"><span class="aui-icon aui-icon-minus"></span></button>'
						+'</div></li>'
						</#if>
					  </#if>
					</#list>
					+'</ul>';
			//$('#${formId}_InterDivContainer_${name?replace(".", "")}').append(s);
			//$('#${formId}_InterDivContainer_${name?replace(".", "")}').append(d);
			$('#'+${formId}_id_${name?replace(".", "")}).append(s);
			$('#'+${formId}_id_${name?replace(".", "")}).append(d);
			var _nLanguageLength = $("input[name='languageValue']").size();
			// 重新设置国际化的值
			// 给所有地方增加key这个字段，并附上国际化的key
			for (var n=0; n<_nLanguageLength; n++) {
				$("input[name='languageValue']").eq(n).attr("key", singleInternationalKey); // 给所有地方增加key这个字段，并附上国际化的key
				var languageValue=getSingleMessage(singleInternationalKey, $("input[name='languageValue']").eq(n).attr("languagename"));
				if(languageValue!=singleInternationalKey){
				    $("input[name='languageValue']").eq(n).val(languageValue); // 给所有地方增加key这个字段，并附上国际化的key
				}else{
				    $("input[name='languageValue']").eq(n).val("");
				}
			}
			xlength = YUD.getX(valueobj)+$(valueobj).width()-10;
			ylength =YUD.getY(valueobj)+$(valueobj).height();
			var widthOffset=$(document.body).width()-(xlength+350);
			if(widthOffset<0){
				xlength=xlength+widthOffset;
			}
			divcontainer.style.left = xlength + "px";
			divcontainer.style.top = ylength + "px";
			$("#${formId}_content_${name?replace(".", "")} li").hover(function(){
				$(this).addClass('liHover');
			}, function(){
				$(this).removeClass('liHover');
			});
			$("#${formId}_content_${name?replace(".", "")} li select").each(function(){
					var img=$(this).siblings('.language-flag')[0];
					var url='/bap/static/images/language/'+$(this).val()+'.png';
					$(img).attr("src",url);
			});
			if($("#${formId}_content_${name?replace(".", "")} #showKey")){
				$("#${formId}_content_${name?replace(".", "")} #showKey").val(CUI('#international_${name?replace(".", "")}').val());
			}
			$(".select-border-style").mSelect().each(function(){
			    $(this).disabledSelect()
			});
		}
		function ${formId}_createDiv_${name?replace(".", "")}(obj){
			var d = '<li>'
					+'<div style="float:left;margin-top: 1.5px;*margin-top:0px;margin-left:4px;">'
					+'<img class="language-flag" src="/bap/static/images/language/spacer.png">'
					+'<select class="select-border-style" onchange="${formId}_selectChange_${name?replace(".", "")}(this);" lastselected="">'
					+'<option value=""></option>'
					<#list languages as l>
					+'<option value="${l.key}" internationalValue="${getText(l.internationalKey)?html}">${getText(l.internationalKey)?html}</option>'
					</#list>
					+'</select>'
					+'<input type="text" key="" value="" name="languageValue" languagename="" onchange="fnInternational_isOldEdit_${name?replace(".", "")}OnChangeNotDefaultLanguage(this)" class="language-value" id="">'
					+'</div>'
					+'<div style="float: left;margin-left: 15px;display: none;">'
					+'<button type="button" id="add" class="aui-icon aui-toolbar-first aui-buttonitem-icon-only aui-state-default" onclick="${formId}_createDiv_${name?replace(".", "")}(this);"><span class="aui-buttonitem-icon aui-icon aui-icon-plus"></span></button>'
					+'<button type="button" id="delete" class="aui-icon aui-toolbar-last aui-buttonitem-icon-only aui-state-default" onclick="${formId}_deleteDiv_${name?replace(".", "")}(this);"><span class="aui-buttonitem-icon aui-icon aui-icon-minus"></span></button>'
					+'</div></li>';
				$(d).insertAfter($(obj).parent().parent());
				$("#${formId}_content_${name?replace(".", "")} li").hover(function(){
					$(this).addClass('liHover');
				}, function(){
					$(this).removeClass('liHover');
				});
		}
		function errorClick(obj){
			$(obj).parent().parent().remove();
			$("ul.edit-select-box").hide();
		}
		function ${formId}_selectChange_${name?replace(".", "")}(obj){
			$("#InternationalError").remove();
			var error='<div id="InternationalError" style="margin-bottom: 8px;display: block; width: 348px;top: 4px;  position: relative;box-shadow:1px 1px 1px #999999;" class="error-info-container ui-draggable dialog-modul">'
					  +'<div class="error-info-wrap  show-error" style="padding: 2px 10px;"><div class="error-info-content" style="height: auto;">'
					  +'<p>${getText("foundation.international.language")} '+$(obj).find("option:selected").text()+'${getText("foundation.international.selectOtherLanguage")}</p></div>'
				      +'<span style="display: block;" id="closeErrorInfo" class="error-info-close" onclick="errorClick(this)" title="${getText('foundation.international.TurnOffErrorMessages')}"></span></div></div>'
			if(obj.value=='${getCurrent('language')}'){
				obj.value=$(obj).attr("lastselected");
				var insertId="#"+${formId}_id_${name?replace(".", "")}+" ul";
				$(error).insertBefore($(insertId));
				//$(error).insertBefore($("#${formId}_InterDivContainer_${name?replace(".", "")} ul"));
				return ;
			 };
			$("#${formId}_content_${name?replace(".", "")} select").each(function(){
				var insertId="#"+${formId}_id_${name?replace(".", "")}+" ul";
				if(this!=obj){
					if(this.value!=""){	
						if(this.value==obj.value){
							obj.value=$(obj).attr("lastselected");
							//$("#InternationalError").remove();
							$(error).insertBefore($(insertId));
							return ;
						}
					}
				}
			})
			var url='/bap/static/images/language/'+obj.value+'.png';
			if(obj.value==""){
				url='/bap/static/images/language/spacer.png';
			}
			var img=$(obj).siblings('.language-flag')[0];
			$(img).attr("src",url);
			$(obj).siblings('.language-value').val('');
			$(obj).siblings('.language-value').attr("languagename",obj.value);
			$(obj).attr("lastselected",obj.value);
			//把默认值给value
			var intervalue=$(obj).find("option:selected").attr("internationalValue");
			if(intervalue!=''){
				$(obj).siblings('.language-value').val(intervalue);
				$(obj).siblings('.language-value').trigger("change");
			}
			
			// 重新设置国际化的值
			var _nLanguageLength = $("input[name='languageValue']").size();
			// 给所有地方增加key这个字段，并附上国际化的key
			for (var n=0; n<_nLanguageLength; n++) {
				$("input[name='languageValue']").eq(n).attr("key", singleInternationalKey); // 给所有地方增加key这个字段，并附上国际化的key
				$("input[name='languageValue']").eq(n).val(getSingleMessage(singleInternationalKey, $("input[name='languageValue']").eq(n).attr("languagename"))); // 给所有地方增加key这个字段，并附上国际化的key
			}
		}
		function ${formId}_deleteDiv_${name?replace(".", "")}(obj){
			var liId="#"+${formId}_id_${name?replace(".", "")}+" li";
			var count=$(liId).length;
			var _sLanguage = $(obj).parents("li").children('div').children("input").attr('languagename');
			var _sKey = $(obj).parents("li").children('div').children("input").attr("key");
			// 删除特定key、language国际化资源文件
			delSingleLanguage (_sLanguage, _sKey);
			if(count==1){
				var value=CUI('#international_${name?replace(".", "")}').val();
				$($("#${formId}_content_${name?replace(".", "")} select")[0]).attr("lastselected","");
				var inputs="#"+${formId}_id_${name?replace(".", "")}+" :input[type!=button]";
			    $(inputs).each(function(){
				   $(this).val("");
				   $(this).attr("languagename","");
				});
				var url='/bap/static/images/language/spacer.png';
				var imgflag="#"+${formId}_id_${name?replace(".", "")}+" .language-flag";
				var img=$(imgflag)[0];
				$(img).attr("src",url);
				return ;
			}
			$(obj).parent().parent().remove();
		}
		YUE.on(document.body,'click',function(event){
			if(document.getElementById(${formId}_id_${name?replace(".", "")})) {
				if ($(document.activeElement).parents().is(".InterDivContainer_class")) {
					$('#'+${formId}_id_${name?replace(".", "")}).show();
				} else {
					$('#'+${formId}_id_${name?replace(".", "")}).hide();
				}
				/*$("[id*='_InterDivContainer_']").each(function(){
					$(this).hide();
				});
				*/
				/*
				var divs = $("#${formId}_InterDivContainer_${name?replace(".", "")}");
				if(divs.is(":visible")){
					divs.hide();
				}
				*/
			}
		});
		function ${formId}_keyChange_${name?replace(".", "")}(obj){
			var value=CUI('#international_${name?replace(".", "")}').val();
			CUI('#international_${name?replace(".", "")}').val(value);
		}
		$(window).resize(function() {
			if(document.getElementById(${formId}_id_${name?replace(".", "")})) {
				$('#'+${formId}_id_${name?replace(".", "")}).hide();
				/*$("[id*='_InterDivContainer_']").each(function(){
					$(this).hide();
				});*/
				/*
				var divs = $("#${formId}_InterDivContainer_${name?replace(".", "")}");
				if(divs.is(":visible")){
					divs.hide();
				}
				*/
			}
		});
	</script>
</#macro>

<#macro internationalNew name,usedInTree,key="",readonly=readonly,view=false ,moduleCode="foundation",modelName="",cssClass="",cssStyle="",style="",formId="",maxLength=-1,showKey=true,isOldEdit=isOldEdit>
	<#assign showValue="${getText(key)}">
	<#assign languages = languageMethod()>
	<#if view>
	<div class="fix-input-readonly"  <#if cssStyle??>style="${cssStyle}"</#if>>
		<div class="fix-search-click">
		<input type="hidden" name="${name!?html}" id="${formId}_international_${name?replace(".", "")}" key="" value=""/>
		<input type="text"  <#if readonly??&&readonly> readonly  </#if> name="international_${name?replace(".", "")}_showName" language="${getCurrent('language')}" id="international_${name?replace(".", "")}_showName"  class="cui-noborder-input" <#if style??>style="${style} <#if !view&&showValue==""> ;width:80%;</#if>"</#if> value="${showValue?html}" />
		<input type="button" value="" class="cui-international-click"/>
		</div>
	</div>
	<#else>
	<div class="fix-input">
		<div class="fix-search-click">
		<input type="hidden" name="${name!}" id="${formId}_international_${name?replace(".", "")}" key="" value=""/>
		<input type="text" name="international_${name?replace(".", "")}_showName" language="${getCurrent('language')}" id="international_${name?replace(".", "")}_showName"  class="cui-noborder-input" <#if style??>style="${style} <#if !view&&showValue==""> ;width:80%;</#if>"</#if> value="${showValue?html}" />
		<input type="button" onclick="${formId}_InternationalDialog_${name?replace(".", "")}(this,event);" title="${getText('foundation.theme.selectLanguage')}" value="" class="cui-international-click"/>
		</div>
	</div>
	</#if>
	<style type="text/css">
	 .InterDivContainer_class{position:absolute;border:1px solid #C0D8F0;z-index:20001;text-align:left;white-space:nowrap;text-overflow:clip;background:#fff;overflow:hidden;background:#fff;padding:0;padding-bottom:10px;}
	.cui-international-click {
		background: url("/bap/static/images/language/${getCurrent('language')}.png") no-repeat scroll 0 0 transparent;
		border: medium none;
		cursor: pointer;
		display: block;
		width: 16px;
		height: 16px;
		position: absolute;
		right: 5px;
		top: 10px !important;
	}
	.ec-config-page .cui-international-click {
		top: 1px;
	}
	.cui-international-span-font {
		position:absolute;
		right:12px;
		top:5px;
		text-decoration:underline;
		color:blue;
		cursor:pointer;
	}
	.InterDivContainer_class .dropdown {
		width: 120px;
		height: 28px;
		line-height: 25px;
		display: inline-block;
		float: none;
		background-color: #fff;
	}
	.select-border-style {
		width:120px;
		position: absolute;
		height: 28px;
		opacity: 0;
		font: 14px/20px "Microsoft YaHei";
	}
	.language-flag {
		padding-bottom: 10px;
	}
	input.language-value {
		float: right;
	}
	ul.edit-select-box {
		z-index: 20003;
	}
	</style>
	<script type="text/javascript">
	//新版
	CUI.ns('foundation.international');
	
	var lanList = [];
	<#list languages as l>
		lanList.push('${l.key}');
	</#list>;
	
	//回调刷新当前页面国际化值
	foundation.international.saveInternationalValue_${name?replace(".", "")} = function (key, obj){			
		var val = 'key=' + key;
		var currentLanguage = '${getCurrent('language')}';
		// 更改key
		$('#' + foundation.international.${name?replace(".", "")}inputkeyid).attr('key', val);		
		// 循环去值
		for (var i=0; i<lanList.length; i++){
			if (typeof obj[lanList[i]] === 'undefined'){
				obj[lanList[i]] = '';
			}
		}
		
		// 循环设值
		for (var key in obj){
			if (key != currentLanguage){// 改变其他语言值
				$('[language="'+ key +'"]', '#${formId}_content_${name?replace(".", "")}').val(obj[key]);
			} else {// 优先改变当前显示值
				$('#' + foundation.international.${name?replace(".", "")}inputshownameid).val( obj[currentLanguage] );
			}
			val += '$&#' + key + '=' + obj[key];
		}
		// 更改值
		$('#' + foundation.international.${name?replace(".", "")}inputkeyid).val(val);
	}
	
	//提取国际化值
	foundation.international.getInternationalValue_${name?replace(".", "")} = function (){		
		var obj = {};
		var val = $('#' + foundation.international.${name?replace(".", "")}inputkeyid).val();
		var arr = val.split('$&#');
		if (arr.length>1){
			arr = arr.slice(1);
			for (var i=0; i<arr.length; i++){
				var arr2 = arr[i].split('=');
				obj[arr2[0]] = arr2[1];
			}
		}
		foundation.international.currentEditValObj = obj;
	};
	
	//国际化key隐藏域id
	foundation.international.${name?replace(".", "")}inputkeyid = '${formId}_international_${name?replace(".", "")}';	
	//国际化实际显示值
	foundation.international.${name?replace(".", "")}inputshownameid = 'international_${name?replace(".", "")}_showName';
	
	// 选择按钮回调	
	foundation.international.sendBackInternational${name?replace(".", "")}Select=function(){
		foundation.international.select${name?replace(".", "")}International();
	}
	
	// 国际化对话框中选择某项回调(双击)
	foundation.international.get${name?replace(".", "")}International=function(arr){		
		try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}
		var obj = arr[0]
		var key=arr[0].key;
		delete obj.id;
		delete obj.key;
		foundation.international.saveInternationalValue_${name?replace(".", "")}(key, obj);
	}
	
	// 修改国际化对话框保存回调
	foundation.international.addCallback${name?replace(".", "")}=function(res){
		if(res.dealSuccessFlag == true){
			// 事先存储当前国际化值
			var obj = foundation.international.getInternationalValueInput();			
			// 更改key
			var key=res.key;								
			
			foundation.international.saveInternationalValue_${name?replace(".", "")}(key, obj);
			try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}
			
		}else{
			international_edit_formDialogErrorBarWidget.showMessage("${getJsText('ec.model.submitfailurer')}","f");
		}
	}
	
	// 弹出修改国际化对话框	
	foundation.international.${name?replace(".", "")}InternationalManage=function(el){
		//优先保存当前语言列表的显示值
		foundation.international.getInternationalValue_${name?replace(".", "")}();
		
		$(el).closest('.InterDivContainer_class').hide();
		var selectFlag="";
		var idStr='#international_${name?replace(".", "")}';
		var key='';
		key = $('#' + foundation.international.${name?replace(".", "")}inputkeyid).attr('key').slice(4);//新版国际化key需去除开头key=字符
				
		var moduleCode = $("[name='menuInfo.moduleCode']").val()
		if(moduleCode != undefined && moduleCode != ""){
			var d=new Date();
			var radion = d.getTime();
			<#if modelName?? && modelName?length gt 0>
			key = moduleCode + '.${modelName?replace(".", "")}.radion'+radion;
			<#else>
			key = moduleCode + '.${name?replace(".", "")}.radion'+radion;
			</#if>
		}else if(key==""){
			var d=new Date();
			var radion = d.getTime();
			moduleCode = '${moduleCode}';
			<#if modelName?? && modelName?length gt 0>
			key='${moduleCode}.${modelName?replace(".", "")}.radion'+radion;
			<#else>
			key='${moduleCode}.${name?replace(".", "")}.radion'+radion;
			</#if>
			<#if usedInTree>
				<#if modelName?? && modelName?length gt 0>
					key='${modelName!''}.' +'${name?replace(".", "")}';
				</#if>
			</#if>
		}else{
			moduleCode = '${moduleCode}';
		}
		
		var url="/msService/ec/foundation/international/editListFrame?moduleCode=" + moduleCode + "&name=${name?replace(".", "")}&callBackFuncName=foundation.international.get${name?replace(".", "")}International"
		url+="&key="+encodeURIComponent(key);
		var height=550;
		var width=850;
		var buttons=[
					
					{	name:"${getHtmlText('common.button.choose')}",
						handler:function(){foundation.international.sendBackInternational${name?replace(".", "")}Select();}
					},
					{	name:"${getHtmlText('common.button.cancel')}",
						handler:function(){this.close()}
					}];
		foundation.international.${name?replace(".", "")}dialog = new CUI.Dialog({
			title: "${getHtmlText('foundation.basic.internationalizationManagement')}",
			url: url,
			modal:true,
			height:height,
			width: width,
			buttons:buttons
		});
		foundation.international.${name?replace(".", "")}dialog.show();
		
	}
	
	var ${formId}_id_${name?replace(".", "")} = '';
	$("#international_${name?replace(".", "")}_showName").change(function(e){
		${formId}_international_${name?replace(".", "")}OnChange(this);
	})
	$("#international_${name?replace(".", "")}_showName").keyup(function(e){
		${formId}_checkLimit_${name?replace(".", "")}(this);
	})
	
	function ${formId}_international_${name?replace(".", "")}initKey(){
			var key;
			<#if key=="" || !key?contains('.')>
			var d=new Date();
			var randon="randon"+d.getTime();
			<#if modelName?? && modelName?length gt 0>
			key='key=${moduleCode}.${modelName?replace(".", "")}.'+randon;
			<#else>
			key='key=${moduleCode}.${name?replace(".", "")}.'+randon;
			</#if>
			<#else>
			key="key=${key?html}";
			</#if>
			<#if showValue!="">
				var value=key+'$&#${getCurrent('language')}=${showValue?html}';
				CUI('#${formId}_international_${name?replace(".", "")}').val(value);
			</#if>
			CUI('#${formId}_international_${name?replace(".", "")}').attr("key",key);
			CUI('#${formId}_InterDivContainer_${name?replace(".", "")}').remove();
	}
	${formId}_international_${name?replace(".", "")}initKey();
	function ${formId}_checkLimit_${name?replace(".", "")}(obj){
		var limit=${maxLength};
		if(limit>0&&obj.value.length>limit){
			obj.value=obj.value.substring(0,limit);
			${formId}_international_${name?replace(".", "")}OnChange(obj);
		}
	}
	//监听input change
	function ${formId}_international_${name?replace(".", "")}OnChange(obj){
        var objValue = $.trim(obj.value);
		var value=CUI('#${formId}_international_${name?replace(".", "")}').val();
		var key=CUI('#${formId}_international_${name?replace(".", "")}').attr("key");
		if(value==""){
			value=key;
		}
		var language=obj.getAttribute("language") || obj.getAttribute("languageName");
		if(language==""){
			return ;
		}
		var index = value.indexOf(language+"=");
		if(index>0){
			var end=value.indexOf("$&#",index);
			if(end>0){
				if(objValue==""){
					value=value.substring(0,value.indexOf("=",index)+1)+value.substring(end);
				}else{
					value=value.substring(0,index)+language+"="+objValue+value.substring(end);
				}
			}else{
				if(objValue==""){
					value=value.substring(0,index-3);
				}else{
					value=value.substring(0,index)+language+"="+objValue;
				}
			}
		}else{
			if(objValue!=""){
				value=value+"$&#"+language+"="+objValue;
			}
		}
		if(value==key){
			value="";
		}
		CUI('#${formId}_international_${name?replace(".", "")}').val(value);
	}
	function ${formId}_InternationalDialog_${name?replace(".", "")}(valueobj,e) {
		if (typeof YAHOO != "undefined" ) {
			var YUD = YAHOO.util.Dom;
		}
		${formId}_id_${name?replace(".", "")}='InterDivContainer_'+$(valueobj).parents('.ewc-dialog-blove:eq(0)').attr('id')+'_${name?replace(".", "")}';
		var xlength,ylength;
		ev = e || window.event;
		if (window.event) { // 停止事件冒泡
		 window.event.cancelBubble = true;
        } else {
         ev.stopPropagation();
        }
		var divcontainer;
		//document.getElementById("${formId}_InterDivContainer_${name?replace(".", "")}")
		if(document.getElementById(${formId}_id_${name?replace(".", "")})){
			divcontainer = $("#"+${formId}_id_${name?replace(".", "")});
			if(divcontainer.is(":hidden")){
				$(".InterDivContainer_class").hide();
				xlength = YUD.getX(valueobj)+$(valueobj).width()-10;
				ylength =YUD.getY(valueobj)+$(valueobj).height();
				var widthOffset=$(document.body).width()-(xlength+350);
				if(widthOffset<0){
					xlength=xlength+widthOffset;
				}
				divcontainer.css({"left":xlength+"px","top":ylength+"px"});
				divcontainer.show(170);
			}else{
				divcontainer.hide(170);
				$(".InterDivContainer_class").hide(170);
			}
			return ;
		}else{
			divcontainer = document.createElement("div");
			divcontainer.id = ${formId}_id_${name?replace(".", "")};//"${formId}_InterDivContainer_${name?replace(".", "")}"; 
			divcontainer.style.width = 350 + 'px';
			$(divcontainer).addClass('InterDivContainer_class');
			document.body.appendChild(divcontainer);
			$("#"+${formId}_id_${name?replace(".", "")}).click(
				function(e){
					ev = e || window.event;
					if (window.event) { // 停止事件冒泡
						window.event.cancelBubble = true;
	        		} else {
	        			ev.stopPropagation();
	        		}
				}
		   )	
		}
		var s = "<div style='overflow: hidden;margin-bottom: 5px;'>"
				+"<h3 style='font-size:1.2em;font-weight:bold;padding:3px 0px 0px 10px;'><span>${getHtmlText('foundation.theme.otherLanguage')}</span></h3>"
				+'<span onclick="foundation.international.${name?replace(".", "")}InternationalManage(this)" class="cui-international-span-font">${getText('foundation.international.customSettings')}<span>'//运行打开修改对话框
				+"</div>";
		var options='<option value=""> </option>'
					<#list languages as l>
					+'<option value="${l.key}" >${getText(l.internationalKey)?html}</option>'
					</#list>;
		var d = '<ul id="${formId}_content_${name?replace(".", "")}" class="international" style="padding-bottom: 20px;">'
				 <#if showKey>
				 +'<div style="margin-left: 4px;">${getHtmlText('foundation.international.key')}: <input onchange="${formId}_keyChange_${name?replace(".", "")}(this);" type="text" id="showKey" name="key" value="" style="border: 1px solid #C0D8F0;height: 18px;line-height: 18px; padding-left: 2px; padding-right: 1px;width: 260px;" /></div>'
				 </#if>
				<#assign n = 0 />
				<#list languages as l>
				  <#if l.key!=getCurrent('language')>
					<#if n<10>
					<#assign n = n+1 />
					+'<li>'
					+'<div style="float:left;*margin-top:0px;margin-left:4px;">'
					+'<img class="language-flag" src="/bap/static/images/language/spacer.png">'
					+'<select class="select-border-style" onchange="${formId}_selectChange_${name?replace(".", "")}(this);" lastselected="${l.key}" >'
					+'<option value=""></option>'
					<#list languages as la>
					+'<option value="${la.key}" internationalValue="${InternationalResource(key,la.key)?replace("'", "")?html}" <#if la==l>selected="selected"</#if> >${getText(la.internationalKey)}</option>'
					</#list>
					+'</select>'
					<#assign InternationalVal=InternationalResource(key,l.key) />
					+'<input type="text" <#if InternationalVal?? &&InternationalVal != key> value="${InternationalVal?html}"</#if> name="languageValue" language="${l.key}" class="language-value" onchange="${formId}_international_${name?replace(".", "")}OnChange(this)"  onkeyup="${formId}_checkLimit_${name?replace(".", "")}(this)">'
					+'</div>'
					+'<div style="float: left;margin-left: 15px;display: none;">'
					+'<button type="button" id="add" class="aui-icon aui-toolbar-first aui-buttonitem-icon-only aui-state-default" onclick="${formId}_createDiv_${name?replace(".", "")}(this);"><span class="aui-icon aui-icon-plus"></span></button>'
					+'<button type="button" id="delete" class="aui-icon aui-toolbar-last aui-buttonitem-icon-only aui-state-default" onclick="${formId}_deleteDiv_${name?replace(".", "")}(this);"><span class="aui-icon aui-icon-minus"></span></button>'
					+'</div></li>'
					</#if>
				  </#if>
				</#list>
				+'</ul>';
		//$('#${formId}_InterDivContainer_${name?replace(".", "")}').append(s);
		//$('#${formId}_InterDivContainer_${name?replace(".", "")}').append(d);
		$('#'+${formId}_id_${name?replace(".", "")}).append(s);
		$('#'+${formId}_id_${name?replace(".", "")}).append(d);
		xlength = YUD.getX(valueobj)+$(valueobj).width()-10;
		ylength =YUD.getY(valueobj)+$(valueobj).height();
		var widthOffset=$(document.body).width()-(xlength+350);
		if(widthOffset<0){
			xlength=xlength+widthOffset;
		}
		divcontainer.style.left = xlength + "px";
		divcontainer.style.top = ylength + "px";
		$("#${formId}_content_${name?replace(".", "")} li").hover(function(){
			$(this).addClass('liHover');
		}, function(){
			$(this).removeClass('liHover');
		});
		$("#${formId}_content_${name?replace(".", "")} li input[type='text']").each(function(){
				${formId}_international_${name?replace(".", "")}OnChange(this);
				$(this).keyup(function(){
					${formId}_checkLimit_${name?replace(".", "")}(this);
				});
				$(this).change(function(){
					${formId}_international_${name?replace(".", "")}OnChange(this);
				});
			});
		$("#${formId}_content_${name?replace(".", "")} li select").each(function(){
				var img=$(this).siblings('.language-flag')[0];
				var url='/bap/static/images/language/'+$(this).val()+'.png';
				$(img).attr("src",url);
		});
		if($("#${formId}_content_${name?replace(".", "")} #showKey")){
			$("#${formId}_content_${name?replace(".", "")} #showKey").val(CUI('#${formId}_international_${name?replace(".", "")}').attr("key").substring(4));
		}
		$(".select-border-style").mSelect().each(function(){
		    $(this).disabledSelect()
		});
	}
	function ${formId}_createDiv_${name?replace(".", "")}(obj){
		var d = '<li>'
				+'<div style="float:left;margin-top: 1.5px;*margin-top:0px;margin-left:4px;">'
				+'<img class="language-flag" src="/bap/static/images/language/spacer.png">'
				+'<select class="select-border-style" onchange="${formId}_selectChange_${name?replace(".", "")}(this);" lastselected="">'
				+'<option value=""></option>'
				<#list languages as l>
				+'<option value="${l.key}" >${getText(l.internationalKey)?html}</option>'
				</#list>
				+'</select>'
				+'<input type="text" value="" name="languageValue" language="" onchange="${formId}_international_${name?replace(".", "")}OnChange(this)" class="language-value" id="" onkeyup="${formId}_checkLimit_${name?replace(".", "")}(this)">'
				+'</div>'
				+'<div style="float: left;margin-left: 15px;display: none;">'
				+'<button type="button" id="add" class="aui-icon aui-toolbar-first aui-buttonitem-icon-only aui-state-default" onclick="${formId}_createDiv_${name?replace(".", "")}(this);"><span class="aui-buttonitem-icon aui-icon aui-icon-plus"></span></button>'
				+'<button type="button" id="delete" class="aui-icon aui-toolbar-last aui-buttonitem-icon-only aui-state-default" onclick="${formId}_deleteDiv_${name?replace(".", "")}(this);"><span class="aui-buttonitem-icon aui-icon aui-icon-minus"></span></button>'
				+'</div></li>';
			$(d).insertAfter($(obj).parent().parent());
			$("#${formId}_content_${name?replace(".", "")} li").hover(function(){
				$(this).addClass('liHover');
			}, function(){
				$(this).removeClass('liHover');
			});
	}
	function errorClick(obj){
		$(obj).parent().parent().remove();
		$("ul.edit-select-box").hide();
	}
	function ${formId}_selectChange_${name?replace(".", "")}(obj){
		$("#InternationalError").remove();
		var error='<div id="InternationalError" style="margin-bottom: 8px;display: block; width: 348px;top: 4px;  position: relative;box-shadow:1px 1px 1px #999999;" class="error-info-container ui-draggable dialog-modul">'
				  +'<div class="error-info-wrap  show-error" style="padding: 2px 10px;"><div class="error-info-content" style="height: auto;">'
				  +'<p>${getText("foundation.international.language")} '+$(obj).find("option:selected").text()+'${getText("foundation.international.selectOtherLanguage")}</p></div>'
			      +'<span style="display: block;" id="closeErrorInfo" class="error-info-close" onclick="errorClick(this)" title="${getText('foundation.international.TurnOffErrorMessages')}"></span></div></div>'
		if(obj.value=='${getCurrent('language')}'){
			obj.value=$(obj).attr("lastselected");
			var insertId="#"+${formId}_id_${name?replace(".", "")}+" ul";
			$(error).insertBefore($(insertId));
			//$(error).insertBefore($("#${formId}_InterDivContainer_${name?replace(".", "")} ul"));
			return ;
		 };
		$("#${formId}_content_${name?replace(".", "")} select").each(function(){
			var insertId="#"+${formId}_id_${name?replace(".", "")}+" ul";
			if(this!=obj){
				if(this.value!=""){	
					if(this.value==obj.value){
						obj.value=$(obj).attr("lastselected");
						//$("#InternationalError").remove();
						$(error).insertBefore($(insertId));
						return ;
					}
				}
			}
		})
		var url='/bap/static/images/language/'+obj.value+'.png';
		if(obj.value==""){
			url='/bap/static/images/language/spacer.png';
		}
		var img=$(obj).siblings('.language-flag')[0];
		$(img).attr("src",url);
		$(obj).siblings('.language-value').val('');
		$(obj).siblings('.language-value').attr("language",obj.value);
		$(obj).attr("lastselected",obj.value);
		//把默认值给value
		var intervalue=$(obj).find("option:selected").attr("internationalValue");
		if(intervalue!=''){
			$(obj).siblings('.language-value').val(intervalue);
			$(obj).siblings('.language-value').trigger("change");
		}
	}
	function ${formId}_deleteDiv_${name?replace(".", "")}(obj){
		var liId="#"+${formId}_id_${name?replace(".", "")}+" li";
		var count=$(liId).length;
		${formId}_clearDataAfterDelete_${name?replace(".", "")}($($(obj).parent().parent()).find(".language-value")[0]);
		if(count==1){
			var value=CUI('#${formId}_international_${name?replace(".", "")}').val();
			var key=CUI('#${formId}_international_${name?replace(".", "")}').attr("key");
			if(value==key){
				CUI('#${formId}_international_${name?replace(".", "")}').val("");
			}
			$($("#${formId}_content_${name?replace(".", "")} select")[0]).attr("lastselected","");
			var inputs="#"+${formId}_id_${name?replace(".", "")}+" :input[type!=button]";
		    $(inputs).each(function(){
			   $(this).val("");
			   $(this).attr("language","");
			});
			var url='/bap/static/images/language/spacer.png';
			var imgflag="#"+${formId}_id_${name?replace(".", "")}+" .language-flag";
			var img=$(imgflag)[0];
			$(img).attr("src",url);
			return ;
		}
		$(obj).parent().parent().remove();
	}
	YUE.on(document.body,'click',function(event){
		if(document.getElementById(${formId}_id_${name?replace(".", "")})) {
			if ($(document.activeElement).parents().is(".InterDivContainer_class")) {
				$('#'+${formId}_id_${name?replace(".", "")}).show();
			} else {
				$('#'+${formId}_id_${name?replace(".", "")}).hide();
			}
			/*$("[id*='_InterDivContainer_']").each(function(){
				$(this).hide();
			});
			*/
			/*
			var divs = $("#${formId}_InterDivContainer_${name?replace(".", "")}");
			if(divs.is(":visible")){
				divs.hide();
			}
			*/
		}
	});
	function ${formId}_clearDataAfterDelete_${name?replace(".", "")}(obj){
		var key=CUI('#${formId}_international_${name?replace(".", "")}').val();
		var language=obj.getAttribute("language");
		var index = key.indexOf("$&#"+language+"=");
		if(index>0){
			var end=key.indexOf("$&#",index+3);
			if(end>0){
				key=key.substring(0,index-1)+key.substring(end);
			}else{
				key=key.substring(0,index) +"$&#"+language+"=";
			}
		}
		CUI('#${formId}_international_${name?replace(".", "")}').val(key);
	}
	function ${formId}_keyChange_${name?replace(".", "")}(obj){
		var value=CUI('#${formId}_international_${name?replace(".", "")}').val();
		var first=4;
		var end=value.indexOf("$&#",first);
		if(end>0){
				value=value.substring(0,first)+obj.value+value.substring(end);
		}else{
				value=value.substring(0,first)+obj.value;
		}
		CUI('#${formId}_international_${name?replace(".", "")}').val(value);
		CUI('#${formId}_international_${name?replace(".", "")}').attr("key","key="+obj.value);
	}
	$(window).resize(function() {
		if(document.getElementById(${formId}_id_${name?replace(".", "")})) {
			$('#'+${formId}_id_${name?replace(".", "")}).hide();
			/*$("[id*='_InterDivContainer_']").each(function(){
				$(this).hide();
			});*/
			/*
			var divs = $("#${formId}_InterDivContainer_${name?replace(".", "")}");
			if(divs.is(":visible")){
				divs.hide();
			}
			*/
		}
	});
	</script>
	
</#macro>

<#macro internationalOld name,key="",view=false ,moduleCode="foundation",modelName="",cssClass="",cssStyle="",style="",usedInTree=false,isOldEdit=isOldEdit>
	<#assign showValue="${getText(key)}">
	<#if view>
	<div class="fix-input-readonly">
	<#else>
	<div class="fix-input">
	</#if>
	<div class="fix-search-click">
	<input type="hidden" name="${name!}" id="international_${name?replace(".", "")}" value="${key!?html}"/>
	<input type="text" <#if !view&&showValue==""> onclick="foundation.international.${name?replace(".", "")}InternationalManage()" </#if>name="international_${name?replace(".", "")}_showName" id="international_${name?replace(".", "")}_showName"  class="cui-noborder-input" <#if style??>style="${style} <#if !view&&showValue==""> ;cursor: pointer;</#if>"</#if> readOnly="true"   value="${showValue}"/>
	<#if !view>
		<input type="button"  onclick="foundation.international.${name?replace(".", "")}InternationalManage()" title="${getText('foundation.menuInfo.international.title')}" value="" class="cui-search-click"/>
	</#if>
	</div>
	</div>
	<script type="text/javascript">
			
			CUI.ns("foundation.international");
			foundation.international.${name?replace(".", "")}showName="international_${name?replace(".", "")}_showName";
			foundation.international.${name?replace(".", "")}key="${name!}";
			foundation.international.${name?replace(".", "")}InternationalManage=function(){
				var selectFlag="";
				var idStr='#international_${name?replace(".", "")}';
				var key='';
				if(foundation.common.ecInternal) {
					key=CUI("#international_${name?replace(".", "")}", foundation.common.ecInternal).val();
				}
				key=key || CUI("#international_${name?replace(".", "")}").val();
				
				if(key==""){
					var d=new Date();
					var radion=d.getTime();
					
					<#if modelName?? && modelName?length gt 0>
					key='${moduleCode}.'+'${modelName?replace(".", "")}'+'.radion'+radion;
					<#else>
					key='${moduleCode}.'+'${name?replace(".", "")}'+'.radion'+radion;
					</#if>
					<#if usedInTree>
						<#if modelName?? && modelName?length gt 0>
							key='${modelName!''}.' +'${name?replace(".", "")}';
						</#if>
					</#if>
				}
				
				var url="/msService/ec/foundation/international/editListFrame?moduleCode=${moduleCode}&name=${name?replace(".", "")}&callBackFuncName=foundation.international.get${name?replace(".", "")}International"
				url+="&key="+encodeURIComponent(key);
				var height=550;
				var width=850;
				var buttons=[
							
							{	name:"${getHtmlText('common.button.choose')}",
								handler:function(){foundation.international.sendBackInternational${name?replace(".", "")}Select();}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}];
				foundation.international.${name?replace(".", "")}dialog = new CUI.Dialog({
					title: "${getHtmlText('foundation.basic.internationalizationManagement')}",
					url: url,
					modal:true,
					height:height,
					width: width,
					buttons:buttons
				});
				foundation.international.${name?replace(".", "")}dialog.show();
				
			}
			foundation.international.sendBackInternational${name?replace(".", "")}Select=function(){
				foundation.international.select${name?replace(".", "")}International();
			}
			foundation.international.sendBackInternationalSave=function(){
				
				CUI("#international_edit_form").submit();
			}
			foundation.international.addCallback${name?replace(".", "")}=function(res){
				if(res.dealSuccessFlag == true){
					
					try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}
					//international_edit_formDialogErrorBarWidget.showMessage("提交成功","s");
					var key=res.key;
					var value=res.value;
					var keyIds= null;
					var showIds= null;
					if(foundation.common.ecInternal) {
						keyIds = CUI("input[name='"+foundation.international.${name?replace(".", "")}key+"']", foundation.common.ecInternal);
						showIds = CUI("input[name='"+foundation.international.${name?replace(".", "")}showName+"']", foundation.common.ecInternal);
					}
					keyIds = keyIds || CUI("input[name='"+foundation.international.${name?replace(".", "")}key+"']");
					showIds = showIds || CUI("input[name='"+foundation.international.${name?replace(".", "")}showName+"']");

					if(keyIds.length>1){
						for(var i=0;i<keyIds.length;i++){
							CUI(keyIds.get(i)).val(key);
							CUI(showIds.get(i)).val(value);
						}
					}else{
						keyIds.val(key);
						showIds.val(value);
					}
					
				}else{
					international_edit_formDialogErrorBarWidget.showMessage("提交失败","f");
				}
			}
			foundation.international.get${name?replace(".", "")}International=function(arr){
				var key=arr[0].key;
				var value="";
				CUI.post("/msService/ec/foundation/international/getLanguageText?key=" + key, function(res){
					if(res.value!=null){
						value=res.value;
						try{foundation.international.${name?replace(".", "")}dialog.close();}catch(e){}
						var keyIds=CUI("input[name='"+foundation.international.${name?replace(".", "")}key+"']");
						var showIds=CUI("input[name='"+foundation.international.${name?replace(".", "")}showName+"']");
						if(keyIds.length>1){
							for(var i=0;i<keyIds.length;i++){
								CUI(keyIds.get(i)).val(key);
								CUI(showIds.get(i)).val(value);
							}
						}else{
							keyIds.val(key);
							showIds.val(value);
						}
					
					}else{
						international_edit_formDialogErrorBarWidget.showMessage("提交失败","f");
					}
					
				}, "json");
			}
	</script>
</#macro>