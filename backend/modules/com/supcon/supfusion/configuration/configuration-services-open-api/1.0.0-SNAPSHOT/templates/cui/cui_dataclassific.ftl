<#macro dataclassific id,formId,dataTableId,viewCode="",dgList="",confirmClick="",cancelClick="",style="",isExtra=false,layoutName="" >
<#assign dcData = dgList>
<#if !(dcData?? && dcData?length gt 0)>
<#if !(layoutName?? && layoutName?length gt 0)>
<#assign dgDatas = getDataClassific(viewCode,layoutName,'')>
<#else>
<#assign dgDatas = getDataClassific(viewCode)>
</#if>
<#if dgDatas?? && dgDatas?length gt 0>
<#assign datas = "[">
<#list dgDatas as dg>
<#if dg_index != 0><#assign datas = datas + ","></#if>
<#assign datas = datas + "{'dgname':'" + getText((dg.displayName)!) + "','dgcode':'" + dg.code + "','dgtype':" + dg.isMultiple?string("true","false") + ",'dgvalue':[">
<#if dg.dataClassifics??>
<#list dg.dataClassifics as dc>
<#if dc_index != 0><#assign datas = datas + ","></#if>
<#assign datas = datas + "{'code':'" + dc.code + "','dcvalue':'" + getText((dc.displayName)!) + "'}" >
</#list>
</#if>
<#assign datas = datas + "]}">
</#list>
<#assign datas = datas + "]">
<#assign dcData = datas>
</#if>
</#if>
<div id="${formId}_data_classify" class="data-classify" style="_display:inline;${style}">
	<span id="${formId}_dcSpan" style="float:left;" bakSpanText="${getText("ec.view.dataclassific")}">${getText("ec.view.dataclassific")}</span>
	<span class="dc-arrow-down" style="float:right;margin-top:5px;">
	</span>
	<#if isExtra?? && isExtra>
	<#local layoutName = layoutName?replace("-","_")>
	<#assign dataTableIds = dataTableId?split(",") >
	<#if dataTableIds??>
	<#list dataTableIds as dataTableIdStr>
	<input type="hidden" id="${dataTableIdStr}_selectClassify">	
	</#list>
	<script>
		var dataTableIds${layoutName!}="";
	<#list dataTableIds as dataTableIdStr>
		dataTableIds${layoutName!} += "," + "${dataTableIdStr}";
	</#list>
		dataTableIds${layoutName!} = dataTableIds${layoutName!}.substr(1);
	</script>
	</#if>
	<#else>
	<input type="hidden" id="${dataTableId}_selectClassify">
	</#if>
</div>
<script type="text/javascript">
	$('#${formId}_data_classify').click(function(e){
		// 停止事件冒泡
		stopBubble(e);
		var menuBox = $('.dc-menu-box', '#${formId}_data_classify');
		if(menuBox.size() == 0) {
			${layoutName?replace('-', '_')!}createClassificDiv();
			menuBox = $('.dc-menu-box', '#${formId}_data_classify');
		}
		menuBox.unbind('click').bind('click',function(e){
	 		// 停止事件冒泡
			stopBubble(e);
		});
		if(menuBox.css("display") == "none"){
			if($(".select-iframe",'#${formId}').length > 0) {
			 	$(".select-iframe",'#${formId}').hide();
			}
			<#if isExtra?? && isExtra>
			for(var i=0;i<dataTableIds${layoutName!}.split(",");i++){
				var classifyCodes = $('#'+dataTableIds${layoutName!}[i]+'_selectClassify').val();
				var classifiArray = classifyCodes.split(",");
				$('input[type="checkbox"],input[type="radio"]',$('#${formId}_dcUl')).each(function(item){
					var flag = false;
					for(var i = 0; i < classifiArray.length; i++){
						if(classifiArray[i] == this.value) {
							this.checked = true;
							flag = true;
							break;
						}
					}
					if(!flag){
						this.checked = false;
					}
				});
			}		
			<#else>
			var classifyCodes = $('#${dataTableId}_selectClassify').val();
			var classifiArray = classifyCodes.split(",");
			$('input[type="checkbox"],input[type="radio"]',$('#${formId}_dcUl')).each(function(item){
				var flag = false;
				for(var i = 0; i < classifiArray.length; i++){
					if(classifiArray[i] == this.value) {
						this.checked = true;
						flag = true;
						break;
					}
				}
				if(!flag){
					this.checked = false;
				}
			});
			</#if>			
			if($(".lc.edit-select-box").length > 0){
				$(".lc.edit-select-box").hide();
				$(".shadow-mask").hide();
				$(".dropselectbox").removeClass("hover");
			}
			menuBox.show();
			$(".dc-menu-iframe",'#${formId}_data_classify').show();
			if($('#${formId}_data_classify').attr('widthInit') != 'yes') {
				if($('#${formId}_dcUl').height() > 260) {
					$('#${formId}_dcUl').css('height',260);
					$('#${formId}_dcUl').css('overflow','auto');
				}				
				$(".dc-menu-iframe",'#${formId}_data_classify').css( { 'width': $('#${formId}_btSpan').parent().outerWidth(), 'height': $('#${formId}_btSpan').parent().outerHeight() });
				$('#${formId}_data_classify').attr('widthInit','yes');
			}
		}else{
			menuBox.hide();
			$(".dc-menu-iframe").hide();
		}
		
		if(menuBox.parents('div[id="${formId}_allContainer"]').length == 0) {
			$(".dc-menu-box","#${formId}_data_classify").css("left","12px").css("top","42px").css("width", "170px");
			$(".dc-menu-iframe","#${formId}_data_classify").css("left","12px").css("top","42px");
		} else {
			$(".dc-menu-box","#${formId}_data_classify").css("left","-1px").css("top","26px").css("width", "100%");;
			$(".dc-menu-iframe","#${formId}_data_classify").css("left","-1px").css("top","26px");
		}
		
		if(window.navigator.userAgent.indexOf('MSIE') > -1){
			$('#${formId}_btSpan').css('width',parseInt($('#${formId}_dcUl').css("width"),10) + ( $.browser.msie7 ? -5 : -10 ) );
			$('.dc-split','#${formId}_dcUl').css('width',parseInt($('#${formId}_dcUl').css("width"),10));
		} else {
			$('#${formId}_btSpan').css('width',parseInt($('#${formId}_dcUl').css("width"),10)-7);
		}
		
	});
	
	function ${layoutName?replace('-', '_')!}createClassificDiv() {
		<#if layoutName?? && layoutName != ""><#else>
		var defaultValue = '';
		</#if>
		var ${formId}_dc = <#if dcData?? && dcData?length gt 0>${dcData}<#else>null</#if>;
		var ${formId}_adv = <#if viewCode?? && viewCode != "">${layoutName?replace('-', '_')!}getAdv("${viewCode}")<#else>null</#if>;
		var ${formId}_copyRadio = null;		
		$('#${formId}_data_classify').append('<div class="dc-menu-box"><ul id="${formId}_dcUl" style="border:none;position:relative;overflow-x:hidden;" class="white-corner checkbox-list"></ul><div class="dc-btn-split"><div class="dc-split-line" ></div></div><div id="${formId}_btSpan" class="clearfix dc-btn-span"><div style="float:right;"><a class="cui-btn-blue cui-btn-blue-primary" style="margin-right:10px;margin-top:0;" id="${formId}_confirm"><span class="btn_r">${getText("ec.common.confirm")}</span></a><a class="cui-btn-blue" style="margin-right:10px;margin-top:0;" id="${formId}_cancel"><span class="btn_r">${getText("ec.common.cancel")}</span></a></div></div></div><iframe class="dc-menu-iframe" frameborder="0" style="display:none;background-color:#000;"></iframe>');
		
		if(${formId}_dc != null && ${formId}_dc.length > 0) {
			for(var j = 0; j < ${formId}_dc.length; j++) {
		 		var dataclassify = ${formId}_dc[j];
		 		var multipleFlag = dataclassify.dgtype;
		 		var dgspan = $('<span class="list-cal">' + dataclassify.dgname + '：</span>');
		 		$('.white-corner', '#${formId}_data_classify').append(dgspan);
		 		var dgValues = dataclassify.dgvalue;
		 		for(var d = 0; d < dgValues.length; d++) {
		 			var menuLi;
		 			if(multipleFlag) {
		 				menuLi = $('<li title="${getText('cui.dataClassific.liTitle')}"><nobr><input type="checkbox" typeUse="dataClassific" reset=false dcname="'+ dgValues[d].dcvalue +'" value="' + dgValues[d].code + '"/><label>' + dgValues[d].dcvalue + '</label></nobr></li>');
		 			} else {
		 				menuLi = $('<li title="${getText('cui.dataClassific.liTitle')}"><nobr><input type="radio" typeUse="dataClassific" reset=false name="' + dataclassify.dgcode + '" dcname="'+ dgValues[d].dcvalue +'" value="' + dgValues[d].code + '"/><label>' + dgValues[d].dcvalue + '</label></nobr></li>');
		 			}
		 			if(dgValues[d]!= null && dgValues[d].isDefault==true){
		 				menuLi.find('input').attr('checked','checked');
						<#if layoutName?? && layoutName != ""><#else>
						defaultValue = defaultValue + ","+dgValues[d].code;
						</#if>
		 			}
		 			$('input[type="checkbox"],input[type="radio"]',menuLi).unbind('click').bind('click',function(e){
						// 停止事件冒泡
						stopBubble(e);
						var obj = $(this);
						if(obj.attr('type') == 'radio') {
							if(null != ${formId}_copyRadio && (obj.prop('checked') == ${formId}_copyRadio.prop('checked'))){
								obj.prop('checked',false);
								${formId}_copyRadio = null;
							}else{
								${formId}_copyRadio = obj;
							}
						}
					});
		 			menuLi.unbind('click').bind('click',function(){
		 				if($('input[type="checkbox"],input[type="radio"]',this).prop('checked') == false) {
		 					$('input[type="checkbox"],input[type="radio"]',this).prop('checked',true);
		 				} else {
		 					$('input[type="checkbox"],input[type="radio"]',this).prop('checked',false);
		 				}
		 			});
		 			$('.white-corner', '#${formId}_data_classify').append(menuLi);
		 		}
		 		if(j < ${formId}_dc.length - 1) {
		 			$('.white-corner', '#${formId}_data_classify').append('<div class="dc-split"><div class="dc-split-line" ></div></div>');
		 		}
		 	}
			<#if layoutName?? && layoutName != ""><#else>
			$('#${dataTableId}_selectClassify').val(defaultValue.substring(1));
			</#if>
	 	}
	 	
	 	if(${formId}_adv != null) {
	 		if(${formId}_dc != null && ${formId}_dc.length > 0) {
				$('.white-corner', '#${formId}_data_classify').append('<div class="dc-split"><div class="dc-split-line" ></div></div>');
			}
	 		var multipleFlag = ${formId}_adv.advtype;
	 		var dgspan = $('<span class="list-cal">' + ${formId}_adv.advname + '：</span>');
	 		$('.white-corner', '#${formId}_data_classify').append(dgspan);
	 		var advValues = ${formId}_adv.advvalue;
	 		for(var d = 0; d < advValues.length; d++) {
	 			var menuLi;
	 			if(multipleFlag) {
	 				menuLi = $('<li title="${getText('cui.dataClassific.liTitle')}"><nobr><input type="checkbox" typeUse="dataClassific" reset=false dcname="'+ advValues[d].condname +'" value="' + advValues[d].code + '"/><label>' + advValues[d].condname + '</label></nobr></li>');
	 			} else {
	 				menuLi = $('<li title="${getText('cui.dataClassific.liTitle')}"><nobr><input type="radio" typeUse="dataClassific" reset=false name="' + advValues.advcode + '" dcname="'+ advValues[d].condname +'" value="' + advValues[d].code + '"/><label>' + advValues[d].condname + '</label></nobr></li>');
	 			}
	 			$('input[type="checkbox"],input[type="radio"]',menuLi).unbind('click').bind('click',function(e){
					// 停止事件冒泡
					stopBubble(e);
					var obj = $(this);
					if(obj.attr('type') == 'radio') {
						if(null != ${formId}_copyRadio && (obj.prop('checked') == ${formId}_copyRadio.prop('checked'))){
							obj.prop('checked',false);
							${formId}_copyRadio = null;
						}else{
							${formId}_copyRadio = obj;
						}
					}
				});
	 			menuLi.unbind('click').bind('click',function(){
	 				if($('input[type="checkbox"],input[type="radio"]',this).prop('checked') == false) {
	 					$('input[type="checkbox"],input[type="radio"]',this).prop('checked',true);
	 				} else {
	 					$('input[type="checkbox"],input[type="radio"]',this).prop('checked',false);
	 				}
	 			});
	 			$('.white-corner', '#${formId}_data_classify').append(menuLi);
	 		}
	 		if(j < ${formId}_adv.length - 1) {
	 			$('.white-corner', '#${formId}_data_classify').append('<div class="dc-split"><div class="dc-split-line" ></div></div>');
	 		}
		 }	
	 	
		$('li', '#${formId}_data_classify').hover(function(){
			$(this).addClass("hover");
		},function(){
			$(this).removeClass("hover");
		});
		
		$('#${formId}_cancel', '#${formId}_data_classify').unbind('click').bind('click',function(){
			var cancelClick = "${cancelClick!}";
			$('#${formId}_data_classify').removeClass('data-classify-hover');
			$('.dc-menu-box', '#${formId}_data_classify').hide();
			$(".dc-menu-iframe",'#${formId}_data_classify').hide();
			if(cancelClick && cancelClick != ""){
				eval(cancelClick);
				return;
			}
		});
		
		$('#${formId}_confirm', '#${formId}_data_classify').unbind('click').bind('click',function(){
			var dataClassifyCodes = '';
			var dataClassifyTitle = '';
			var confirmClick = "${confirmClick!}";
			<#if isExtra?? && isExtra>
			<#list dataTableId?split(",") as dgId>
			if (typeof ${dgId}_selectClassify_before === 'function'){
				 ${dgId}_selectClassify_before();
			}
			</#list>
			<#else>
			if (typeof ${dataTableId}_selectClassify_before === 'function'){
				 ${dataTableId}_selectClassify_before();
			}
			</#if>
			$('input[type="checkbox"]:checked,input[type="radio"]:checked',$('#${formId}_dcUl')).each(function(item){
				dataClassifyCodes += "," + $(this).val();
				dataClassifyTitle += "," + $(this).attr('dcname') + "";
			});
			<#if isExtra?? && isExtra>
			for(var i=0;i<dataTableIds${layoutName!}.split(",");i++){
				$('#'+dataTableIds${layoutName!}[i]+'_selectClassify').val(dataClassifyCodes.substr(1));
			}
			<#else>
			$('#${dataTableId}_selectClassify').val(dataClassifyCodes.substr(1));
			</#if>
			if(dataClassifyCodes != '') {
				$('#${formId}_dcSpan','#${formId}_data_classify').attr('title',dataClassifyTitle.substr(1));
				var dataClassifyNameShows = dataClassifyTitle.length > 8 ? dataClassifyTitle.substr(1,7)+"..." : dataClassifyTitle.substr(1);
				$('#${formId}_dcSpan','#${formId}_data_classify').addClass('dc-text');
				$('#${formId}_dcSpan','#${formId}_data_classify').text(dataClassifyNameShows);
			} else {
				$('#${formId}_dcSpan','#${formId}_data_classify').attr('title','');
				$('#${formId}_dcSpan','#${formId}_data_classify').removeClass('dc-text');
				$('#${formId}_dcSpan','#${formId}_data_classify').text( $('#${formId}_dcSpan','#${formId}_data_classify').attr('bakSpanText') );
			}
			$('#${formId}_data_classify').removeClass('data-classify-hover');
			$('.dc-menu-box', '#${formId}_data_classify').hide();
			$(".dc-menu-iframe",'#${formId}_data_classify').hide();
			if(confirmClick && confirmClick != ""){
				eval(confirmClick);
				return;
			}
			<#if isExtra?? && isExtra>
			var dataTableIds${layoutName!}s = dataTableIds${layoutName!}.split(",");
			if(dataClassifyCodes != '') {
				for(var i=0;i<dataTableIds${layoutName!}s.length;i++){
					$('#'+dataTableIds${layoutName!}s[i]+'_selectClassify').val(dataClassifyCodes.substr(1));
				}	        	
	        } else {
	        	for(var i=0;i<dataTableIds${layoutName!}s.length;i++){
	        		$('#'+dataTableIds${layoutName!}s[i]+'_selectClassify').val('');
	        	}	        	
	        }
	        $('#classifyCodes','#${formId}').val(dataClassifyCodes.substr(1));
	        <#else>
	        if(dataClassifyCodes != '') {
	        	$('#${dataTableId}_selectClassify').val(dataClassifyCodes.substr(1));
	        	$('#classifyCodes','#${formId}').val(dataClassifyCodes.substr(1));
	        } else {
	        	$('#${dataTableId}_selectClassify').val('');
	        	$('#classifyCodes','#${formId}').val('');
	        }
	        </#if>
	        <#if isExtra?? && isExtra>
	        var datatableIds  = dataTableIds${layoutName!};
	        if(null != datatableIds && datatableIds.length > 0){	        
	        	var datatableId = datatableIds.split(",");
		        for(var i=0;i<datatableId.length;i++){
		        	var datatableObj = eval(datatableId[i]+"Widget");
		       		if(datatableObj!=null){
						var queryFunc = datatableObj.get('queryFunc');
						if(null != queryFunc && "undefined" != queryFunc){
							var pageNo = CUI('.PageLink-PageSelect','#${dataTableId}').val();
							eval(queryFunc.substr(0,queryFunc.lastIndexOf(')')) + "," + pageNo + ")");
						} else {
							var postData = "";
							postData = datatableObj._getFormData(false);
							if(postData && postData.indexOf("classifyCodes=") > -1) {
								var postDataArrs = postData.split("classifyCodes=");
								if(postDataArrs[0] && postDataArrs[0].length > 0) {
									postDataArrs[0] = postDataArrs[0].substr(0, postDataArrs[0].length - 1);
								}
								if(postDataArrs[1] && postDataArrs[1].length > 0 && postDataArrs[1].indexOf("&") > -1) {
									postDataArrs[1] = postDataArrs[1].substr(postDataArrs[1].indexOf("&"));
								}
								postData = postDataArrs[0] + postDataArrs[1];
							}
							if(dataClassifyCodes != '') {
								postData += "&classifyCodes=" + dataClassifyCodes.substr(1);
							}
							postData = postData.substr(1);
							var pageSize = CUI('input[name="'+datatableId+'_PageLink_PageCount"]').val();
							datatableObj.setRequestData('page.pageSize='+pageSize, postData, datatableObj.requestUrl);
						}
					}
		    	}
	    	}
	        <#else>
			var datatableObj = eval("${dataTableId}Widget");
			var queryFunc = datatableObj.get('queryFunc');
			if(null != queryFunc && "undefined" != queryFunc){
				var pageNo = CUI('.PageLink-PageSelect','#${dataTableId}').val();
	    		eval(queryFunc.substr(0,queryFunc.lastIndexOf(')')) + "," + pageNo + ")");
	    	} else {
	    		var postData = "";
	    		postData = datatableObj._getFormData(false);
		        if(postData && postData.indexOf("classifyCodes=") > -1) {
		        	var postDataArrs = postData.split("classifyCodes=");
		        	if(postDataArrs[0] && postDataArrs[0].length > 0) {
		        		postDataArrs[0] = postDataArrs[0].substr(0, postDataArrs[0].length - 1);
		        	}
		        	if(postDataArrs[1] && postDataArrs[1].length > 0 && postDataArrs[1].indexOf("&") > -1) {
		        		postDataArrs[1] = postDataArrs[1].substr(postDataArrs[1].indexOf("&"));
		        	}
		        	postData = postDataArrs[0] + postDataArrs[1];
		        }
		        if(dataClassifyCodes != '') {
					postData += "&classifyCodes=" + dataClassifyCodes.substr(1);
		       	}
		        postData = postData.substr(1);
				var pageSize = CUI('input[name="${dataTableId}_PageLink_PageCount"]').val();
				datatableObj.setRequestData('page.pageSize='+pageSize, postData, datatableObj.requestUrl);
	    	}
	        </#if>
	        <#if isExtra?? && isExtra>
			<#list dataTableId?split(",") as dgId>
			if (typeof ${dgId}_selectClassify_after === 'function'){
				${dgId}_selectClassify_after();
			}
			</#list>
			<#else>
	        if (typeof ${dataTableId}_selectClassify_after === 'function'){
				 ${dataTableId}_selectClassify_after();
			}		
			</#if>	
		});
	}
	
	$('#${formId}_data_classify').hover(function(){
		$('#${formId}_data_classify').addClass('data-classify-hover');
	},function(){
		if($('.dc-menu-box', '#${formId}_data_classify').length == 0) {
			$('#${formId}_data_classify').removeClass('data-classify-hover');
		} else {
			if($('.dc-menu-box', '#${formId}_data_classify').css("display") == "none") {
				$('#${formId}_data_classify').removeClass('data-classify-hover');
			}
		}
	});
	
	 function ${layoutName?replace('-', '_')!}getAdv(viewCode) {
	 	var advFuncStr = null;
	 	var datas = new Object();
	 	datas.viewCode = viewCode;
	 	<#if layoutName?? && layoutName?length gt 0>
	 	datas.layoutName = '${layoutName?replace('-', '_')!}';	
	 	</#if>
    	$.ajax({
			url: '/msService/ec/advQuery/getAdv',
			type: 'post',
			async: false,
			data: datas,
			success: function(msg) {
				if(msg != null && msg.length > 0) {
					advFuncStr = "{'advname':'${getText('common.button.advancedquery')}','advcode':'bap:adv:group','advtype':false,'advvalue':[";
					$.each(msg,function(index,item){
						if(index != 0) {
							advFuncStr += ",";
						}
						advFuncStr += "{'code':'bap:adv:classific:" + item.id + "','condname':'" + item.condName + "'}";
					});
					advFuncStr += "]}";
				}
			}
		});
		return advFuncStr == null ? null : eval("(" + advFuncStr + ")");
    }
	
	$("body").bind("click.select", function(e){
        $(".dc-menu-box").hide();
        $(".dc-menu-iframe").hide();
        if($("#${formId}_data_classify").hasClass("data-classify-hover")){
        	$("#${formId}_data_classify").removeClass("data-classify-hover");
        }
    });
</script>	
</#macro>