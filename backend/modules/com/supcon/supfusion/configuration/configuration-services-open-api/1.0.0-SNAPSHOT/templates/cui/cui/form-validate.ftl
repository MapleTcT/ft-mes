<script type="text/javascript">
String.prototype.trim=function(){
	return this.replace(/^\s*/,'').replace(/\s*$/,'');
}
CUI(function(){
	function ajaxFileUploadCommon(s) {
		// TODO introduce global settings, allowing the client to modify
		// them for all requests, not only timeout
		s = jQuery.extend({}, jQuery.ajaxSettings, s);
		var id = new Date().getTime();
		var form = createUploadForm(id, s.formId);
		var io = createUploadIframe(id, s.secureuri);
		var frameId = 'jUploadFrame' + id;
		var formId = 'jUploadForm' + id;
		// Watch for a new set of requests
		if (s.global && !jQuery.active++) {
			jQuery.event.trigger("ajaxStart");
		}
		var requestDone = false;
		// Create the request object
		var xml = {}
		if (s.global)
			jQuery.event.trigger("ajaxSend", [ xml, s ]);
		// Wait for a response to come back
		var uploadCallback = function(isTimeout) {
			var io = document.getElementById(frameId);
			try {
				if (io.contentWindow) {
					xml.responseText = io.contentWindow.document.body ? io.contentWindow.document.body.innerHTML : null;
					xml.responseXML = io.contentWindow.document.XMLDocument ? io.contentWindow.document.XMLDocument : io.contentWindow.document;
	
				} else if (io.contentDocument) {
					xml.responseText = io.contentDocument.document.body ? io.contentDocument.document.body.innerHTML : null;
					xml.responseXML = io.contentDocument.document.XMLDocument ? io.contentDocument.document.XMLDocument : io.contentDocument.document;
				}
				if(foundation.common._errorCode && foundation.common._errorCode != 404) {
					foundation.common._errorCode = null;
					foundation.common.showErrorMsg({"_errorCode" : foundation.common._errorCode});
				}
			} catch (e) {
				 workbenchErrorBarWidget.showMessage(e,"f",null,"${getText('ec.common.error.submit')}");
				//jQuery.handleError(s, xml, null, e);
			}
			if (xml || isTimeout == "timeout") {
				requestDone = true;
				var status;
				try {
					status = isTimeout != "timeout" ? "success" : "error";
					// Make sure that the request was successful or
					// notmodified
					if (status != "error") {
						// process the data (runs the xml through
						// httpData regardless of callback)
						var data = uploadHttpData(xml, s.dataType);
						// If a local callback was specified, fire it
						// and pass it the data
						if (s.success)
							s.success(data, status);
	
						// Fire the global callback
						if (s.global)
							jQuery.event.trigger("ajaxSuccess", [ xml, s ]);
					} else
						 workbenchErrorBarWidget.showMessage(status,"f",null,"${getText('ec.common.error.submit')}");
						//jQuery.handleError(s, xml, status);
				} catch (e) {
					//add by yubo20180119
					if(e.name == 'SyntaxError') {
					 	if(xml && xml.responseText) {
					 		$("#load_mask").remove();
							$("#load_iframe_ie").remove();
							$("#loading_wrap").remove();
					 		var res = xml.responseText.toLowerCase();
						 	var arr1 = res.match('<span[\\s\\S^/]*>.+</span>');
						 	var msg = arr1[0].replace(/<span[^/>]*>/,'').replace('</span>','');
						 	//workbenchErrorBarWidget.showMessage(msg, 'f',null,"${getText('ec.common.error.submit')}");
						 	//showLoginDialog();						 	
						 	
					 	}
						//jQuery.handleError(s, xml, status, e);
					} else if(e.name == 'TypeError' && e.message == "Cannot read property 'dealSuccessFlag' of undefined") {
						status = "error";
					 	workbenchErrorBarWidget.showMessage("${getText('ec.common.error.network')}","f",null,"${getText('ec.common.error.submit')}");
					} else {
						status = "error";
					 	workbenchErrorBarWidget.showMessage(e,"f",null,"${getText('ec.common.error.submit')}");
					}
				}

				// The request was completed
				if (s.global)
					jQuery.event.trigger("ajaxComplete", [ xml, s ]);

				// Handle the global AJAX counter
				if (s.global && !--jQuery.active)
					jQuery.event.trigger("ajaxStop");

				// Process result
				if (s.complete)
					s.complete(xml, status);

				jQuery(io).unbind();

				setTimeout(function() {
					try {
						jQuery(io).remove();
						jQuery(form).remove();
	
					} catch (e) {
						 workbenchErrorBarWidget.showMessage(e,"f",null,"${getText('ec.common.error.submit')}");
					}
				}, 100);
				xml = null;
				//closeLoadPanel();
			}
		}
		// Timeout checker
		if (s.timeout > 0) {
			setTimeout(function() {
				// Check to see if the request is still happening
				if (!requestDone)
					uploadCallback("timeout");
			}, s.timeout);
		}
		try {

			var form = jQuery('#' + formId);
			jQuery(form).attr('action', s.url);
			jQuery(form).attr('method', 'POST');
			jQuery(form).attr('target', frameId);
			if (form.encoding) {
				jQuery(form).attr('encoding', 'multipart/form-data');
			} else {
				jQuery(form).attr('enctype', 'multipart/form-data');
			}

			//ceshi by yubo20180119
			//window.onbeforeunload = null;
			
			
			$.ajax({
	   		 //data:{username:username,password:password},// 参数
	   		 dataType:"json", //一般就给json， 以json格式传递参数，包括返回数据
	   		 type:"post", //post， get，一般就用post好了
	   		 url:'/sso-auth2',// 调用的url
	   		 async:false, //是否异步调用，如果true，则表示是异步调用，如果是false，则表示是同步调用。
	   		 success:function(re){
	   		 		jQuery(form).submit();
	   		 		setTimeout(function(){
						var allfiles = jQuery('#' + s.formId + ' input[type="file"]');
						jQuery('input[type="file"]', form).each(function(ind){
							jQuery(allfiles[ind]).before(jQuery(this));
							jQuery(allfiles[ind]).remove();
						});
					}, 2000);
	   			 	//checkFlag=re;
	   		 }, //正确返回时，调用的函数
	   		 error : function(XMLHttpRequest,
						textStatus, errorThrown) {
						if(XMLHttpRequest.status == 401){
							//alert(2);
						}
						//alert(3);
						
				}
			});
			
		} catch (e) {
			 workbenchErrorBarWidget.showMessage(e,"f");
			//jQuery.handleError(s, xml, null, e);
		}
		jQuery('#' + frameId).bind('load', function() {
			uploadCallback();
		});
		return {
			abort : function() {
			}
		};

	}
	
	//add by yubo20180119
	function showLoginDialog(fun) {
	
		if( $('#dialog_login').length > 0 ){
			return;
		}
		new CUI.Dialog({
			elementId:"login",
			title : '${getText("foundation.user.login.first")}',
			modal : true,
			height : 150,
			width : 320,
			url : CUI_CONFIG_URL.loginDialog + '?lang=${lang!"zh_CN"}',
			dragable : true,
			buttons : [
					{
						name : "${getText("foundation.login.loginto")}",
						handler : function() {
							checkLogin(this);
						}
					}, {
						name : "${getText("foundation.common.cancel")}",
						handler : function() {
							this.close()
						}
					} ]
		}).show();
	};
		
	function uploadHttpData(r, type) {
		var data = !type;
		data = type == "xml" || data ? r.responseXML : r.responseText;
		// If the type is "script", eval it in global context
		if (type == "script")
			jQuery.globalEval(data);
		// Get the JavaScript object, if JSON is used.
		if (type == "json")
			eval("data = " + data);
		// evaluate scripts within html
		if (type == "html")
			jQuery("<div>").html(data).evalScripts();
		return data;
	}
	function ajaxFileUpload(sUrl,formId) {
		ajaxFileUploadCommon({
				url:sUrl,
				formId:formId,
				secureuri:false,
				dataType: 'json',
				beforeSend:function() {
					$("#loading").show();
				},
				complete:function() {
					$("#loading").hide();
				},
				success : function(msg) {
					var formId = '${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}';
					if(formId && formId.length > 5){
						var pageId = formId.substring(0, formId.length - 5);
						var datagrids = $('body').data(pageId + '_datagrids');
					}
					if(msg && msg.success == false){
	        			var errorMsgs = "";
						CUI.each(msg.items,function(index,item){
							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
								$("#${parameters.id} *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else if($("#${parameters.id} input[name='"+index+"AddIds'][type='hidden']").length > 0) {
								// 多选控件验证
								showErrorField($("#${parameters.id} *[name='"+index+"AddIds']").parent('div'));
							} else {
								var field = CUI("#${parameters.id} *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
			                		showErrorField(field.next());
			                	} else {
			                		showErrorField(field);
			                	}
							}
							CUI("#${parameters.id} *[name='"+index+"']").first().focus();
							for(var i = 0 ; i < item.length ; i++){
								if(index.indexOf('[') != -1 && index.indexOf('List[') != -1){
									var tmpIndex = index.substring(0, index.indexOf('List['));
									var dgwidget = null;
									if(datagrids && tmpIndex) {
										for(var firstInd = 0; firstInd < datagrids.length ; firstInd++) {
											if(datagrids[firstInd]) {
												for(var secondInd = 0; secondInd < datagrids.length ; secondInd++) {
													if(datagrids[firstInd][secondInd] && datagrids[firstInd][secondInd].endsWith(tmpIndex)) {
														var dgwidget = eval(datagrids[firstInd][secondInd]+'Widget');
														break;
													}
												}
												if(dgwidget) {
													break;
												}
											}
										}
									}
									if(dgwidget) {
										var json = eval(dgwidget.parseEditedData());
										var num = parseInt(index.substring(index.indexOf('[')+1, index.indexOf(']')));
										if(json[num] && json[num].rowIndex) {
											num = parseInt(json[num].rowIndex) + 1;
										}
										dgwidget.addErrorStyle(num - 1, index.substring(index.indexOf('].')+2));
										errorMsgs += item[i].split('${getText("ec.common.validate.colon")}')[0] + '${getText("ec.common.validate.colon")}${getText("ec.common.validate.sequence")}<b>' + num + '</b>${getText("ec.common.validate.line")}' + item[i].split('${getText("ec.common.validate.colon")}')[1] + '<br/>';
									} else {
										var num = parseInt(index.substring(index.indexOf('[')+1, index.indexOf(']'))) + 1;
										errorMsgs += item[i].split('${getText("ec.common.validate.colon")}')[0] + '${getText("ec.common.validate.colon")}${getText("ec.common.validate.sequence")}<b>' + num + '</b>${getText("ec.common.validate.line")}' + item[i].split('${getText("ec.common.validate.colon")}')[1] + '<br/>';
									}
								}else{
									errorMsgs += item[i] + '<br/>';
								}
							}
						});
						CUI.each(msgErrors,function(index,item){
							errorMsgs += item + '<br/>';
						});
						if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
							errorMsgs += msg.exceptionMsg + '<br/>';
						}
						var oErrorWidget = null;
					<#if parameters.dynamicAttributes["errorBarId"]??>
						oErrorWidget = ${parameters.dynamicAttributes["errorBarId"]}Widget;
					<#else>
						oErrorWidget = ${parameters.id}DialogErrorBarWidget;
					</#if>
						oErrorWidget.showMessage(errorMsgs);
						// oErrorWidget.show(errorMsgs);
						if(CUI.Dialog){
					    	 CUI.Dialog.toggleAllButton('${parameters.id}', true);
					     }
					}else{
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							//setTimeout(function(){closeLoadPanel();}, 2800);
						}
						<#if (Parameters.openType)?? && (Parameters.openType) == 'frame'>
						CUI.Dialog.toggleAllButton('${parameters.id}', true);
						<#elseif parameters.dynamicAttributes["toggle_all_button"]?? && parameters.dynamicAttributes["toggle_all_button"]=='true'>
						CUI.Dialog.toggleAllButton('${parameters.id}', true);	
						</#if>
						<#if parameters.dynamicAttributes["callback"]??>${parameters.dynamicAttributes["callback"]}(msg,CUI('#'+formId).serialize());</#if>
					}
				}
			}
		);
	}
	
	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
		var ecFormFlag = <#if parameters.dynamicAttributes["ecform"]?has_content && parameters.dynamicAttributes["ecform"]=='true'>true<#else>false</#if>;
		<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
		ecFormFlag = true;
		</#if>
		var retrialFormFlag = <#if parameters.dynamicAttributes["retrialform"]?has_content && parameters.dynamicAttributes["retrialform"]=='true'>true<#else>false</#if>;
		if(ecFormFlag && ( $('#${parameters.id}').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
			<#else>
			ecFormFlag = false;
			</#if>
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		<#if parameters.dynamicAttributes["foundationform"]?has_content && parameters.dynamicAttributes["foundationform"]=='true'>
			ecFormFlag = false;
		</#if>
		//前台验证通过之后出进度条
		<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
		createLoadPanel(false, $('.ewc-dialog-blove').attr('id') ,{head:'${getText("ec.common.saving")}', show: true, opacity:50, bgColor:"#666666"});
		<#else>
		CUI.Dialog.toggleAllButton('${parameters.id}',true,ecFormFlag, true);
		</#if>
	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
	setTimeout(function(){
		
			// 延迟保存数据, 解决onchange事件无法触发问题
			<#if parameters.dynamicAttributes["editPageNs"]??>
			${parameters.dynamicAttributes["editPageNs"]}.beforeSaveProcess();
			</#if>
			var formId = '${parameters.id}';
			var ecformflag = false;
			<#if parameters.dynamicAttributes["ecform"]?has_content && parameters.dynamicAttributes["ecform"]=='true'>
			ecformflag = true;
			</#if>
			
			<#if parameters.dynamicAttributes["foundationform"]?has_content && parameters.dynamicAttributes["foundationform"]=='true'>
			  ecformflag = false;
			</#if>
			
		    $('input[type="text"]','#'+formId).each(function(){
		        var v=$.trim($(this).val());
		        $(this).val(v);
		    });
			var files = $('input[type="file"]', '#' + formId);
			if(ecformflag || (files!=null&&files.length>0)) {
				ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
			} else {
			<#if parameters.dynamicAttributes["dataprocess"]??>postData = ${parameters.dynamicAttributes["dataprocess"]}(postData);</#if>
			var postData = CUI('#'+formId).serialize();
			CUI.ajax({
				url : CUI('#'+formId).attr('action'),
				type : 'POST',
				dataType : 'json',
				data : postData,
				error : function(XMLHttpRequest, textStatus, errorThrown){
					//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
					if (XMLHttpRequest.status==401) {
						//showLoginDialog();
						return ;
					}
					var msg = CUI.parseJSON(XMLHttpRequest.responseText);
					var errorMsgs = "";
					CUI.each(msg.items,function(index,item){
						if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
							$("#${parameters.id} *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
								if($(this).parents('td[nullable=false]').length > 0) {
									showErrorField($(this));
								}
							});
						} else {
							var field = CUI("#${parameters.id} *[name='"+index+"']");
							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		showErrorField(field.next());
		                	} else {
		                		showErrorField(field);
		                	}
						}
						CUI("#${parameters.id} *[name='"+index+"']").first().focus();
						for(var i = 0 ; i < item.length ; i++){
							errorMsgs += item[i] + '<br/>';
						}
					});
					CUI.each(msgErrors,function(index,item){
						errorMsgs += item + '<br/>';
					});
					if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
						errorMsgs += msg.exceptionMsg + '<br/>';
					}
					var oErrorWidget = null;
				<#if parameters.dynamicAttributes["errorBarId"]??>
					oErrorWidget = ${parameters.dynamicAttributes["errorBarId"]}Widget;
				<#else>
					oErrorWidget = ${parameters.id}DialogErrorBarWidget;
				</#if>
					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
					}	else {
						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
					}					
					if(CUI.Dialog){
				    	<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
						closeLoadPanel();
						<#else>
						CUI.Dialog.toggleAllButton('${parameters.id}', true);
						</#if>
				    }
				},
				success : function(msg){
					window.onbeforeunload = null;
					if(window.containerLoadPanelWidget) {
						setTimeout(function(){closeLoadPanel();}, 500);
					}
					<#if (Parameters.openType)?? && (Parameters.openType) == 'frame'>
					CUI.Dialog.toggleAllButton('${parameters.id}', true);
					<#elseif parameters.dynamicAttributes["toggle_all_button"]?? && parameters.dynamicAttributes["toggle_all_button"]=='true'>
					CUI.Dialog.toggleAllButton('${parameters.id}', true);	
					</#if>
					<#if parameters.dynamicAttributes["callback"]??>${parameters.dynamicAttributes["callback"]}(msg,postData);</#if>
				}
			});
		}
	}, 600);
		return false;
	}
	function singleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
		if(res.isSuccess) {
			var form=CUI('#'+formId);
			if($('#signature_username1').length == 0) {
	    		form.append("<input type='hidden' id='signature_username1' name='signatureLog.firstUserName' value=''>");
	   	 	}
	   	 	if($('#signature_reason1').length == 0) {
	    		form.append("<input type='hidden' id='signature_reason1' name='signatureLog.firstReason' value=''>");
	   	 	}
			if($('#signature_signatureType').length == 0) {
				form.append("<input type='hidden' id='signature_signatureType' name='signatureLog.signatureType' value='singleSign'>");
	    	}
	    	if($('#signature_buttonCode').length == 0) {
	    		form.append("<input type='hidden' id='signature_buttonCode' name='signatureLog.buttonCode' value='"+buttonCode+"'>");
	    	}
	    	if($('#signature_password1').length == 0) {
	    		form.append("<input type='hidden' id='signature_password1' name='signature.password1' value=''>");
	    	}
	    	if($('#signature_remark1').length == 0) {
	    		form.append("<input type='hidden' id='signature_remark1' name='signatureLog.firstRemark' value=''>");
	    	}
	    	if($('#signature_signTime1').length == 0) {
	    		form.append("<input type='hidden' id='signature_signTime1' name='signatureLog.firstSignTime' value=''>");
	    	}
	    	if($('#signatureEnabled').length == 0) {
	    		form.append("<input type='hidden' id='signatureEnabled' name='signatureEnabled' value='true'>");
	    	}
	    	if(namespace != undefined && namespace != "") {
				eval("$('#signature_reason1').val(parent.$('#foundation_signature_reason1').val())");
				eval("$('#signature_username1').val(parent.$('#foundation_signature_username1').val())");
				eval("$('#signature_password1').val(parent.$('#foundation_signature_password1').val())");
				eval("$('#signature_remark1').val(parent.$('#foundation_signature_remark1').val())");
				eval("$('#signature_signTime1').val(new Date().getTime())");
			}
			else {
				$('#signature_reason1').val($('#foundation_signature_reason1').val());
				$('#signature_username1').val($('#foundation_signature_username1').val());
				$('#signature_password1').val($('#foundation_signature_password1').val());
				$('#signature_remark1').val($('#foundation_signature_remark1').val());
				$('#signature_signTime1').val(new Date().getTime());
			}
			if(namespace != undefined && namespace != "") {
				parent.signatureDialog.close();
			}
			else {
				signatureDialog.close();
			}
			if(formId == "ImportForm") {
				foundation.importExcel.submitForm(isReplace,isIgnore);
			}
			else {
				if($('#workflow_comments').length > 0) {
					 var comments=$('#workflow_comments').val("${getText('foundation.signature.workflow.comment')}"+$('#signature_remark1').val());
				}
	 			submitBapForm();
	 		}
		}
		else {
			if(namespace != undefined && namespace != "") {
				parent.signature_singleSign_formDialogErrorBarWidget.showMessage("${getText('foundation.signature.authenticationFail')}");
			}
			else {
				signature_singleSign_formDialogErrorBarWidget.showMessage("${getText('foundation.signature.authenticationFail')}");
			}
		}
	}
	function doubleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore) {
	var isSuccess=res.isSuccess;
	if(isSuccess) {
		if(!res.checkPowerSuccess) {
			if(namespace != '') {
				parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.noPowertoSign')}');
			}
			else {
				signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.noPowertoSign')}');
			}
			return;
		}
		if(namespace != '') {
			parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.signSuccess')}','s');
		}
		else {
			signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.signSuccess')}','s');
		}
		try {
			if(namespace != '') {
				namespace=eval(namespace);
			}
			else {
				namespace = null;
			}
		}
		catch(e){}
		if(namespace != null ) {
			var username1=parent.$('#foundation_signature_username1').val();
			var reason1=parent.$('#foundation_signature_reason1').val();
			var remark1=parent.$('#foundation_signature_remark1').val();
			var password1=parent.$('#foundation_signature_password1').val();
			var username2=parent.$('#foundation_signature_username2').val();
			var reason2=parent.$('#foundation_signature_reason2').val();
			var remark2=parent.$('#foundation_signature_remark2').val();
			var password2=parent.$('#foundation_signature_password2').val();
			var signTime1=parent.$('#signature_signTime1').val();
		}
		else {
			var username1=$('#foundation_signature_username1').val();
			var reason1=$('#foundation_signature_reason1').val();
			var remark1=$('#foundation_signature_remark1').val();
			var password1=$('#foundation_signature_password1').val();
			var username2=$('#foundation_signature_username2').val();
			var reason2=$('#foundation_signature_reason2').val();
			var remark2=$('#foundation_signature_remark2').val();
			var password2=$('#foundation_signature_password2').val();
			var signTime1=$('#signature_signTime1').val();
		}
		var form=$("#"+formId);
		if($('#signature_username1').length == 0) {
    		form.append("<input type='hidden' id='signature_username1' name='signatureLog.firstUserName' value=''>");
    	}
		if($('#signature_reason1').length == 0) {
    		form.append("<input type='hidden' id='signature_reason1' name='signatureLog.firstReason' value=''>");
   	 	}
   	 	if($('#signature_remark1').length == 0) {
    		form.append("<input type='hidden' id='signature_remark1' name='signatureLog.firstRemark' value=''>");
   	 	}
		if($('#signature_password1').length == 0) {
    		form.append("<input type='hidden' id='signature_password1' name='signature.password1' value=''>");
   	 	}
		if($('#signature_signatureType').length == 0) {
    		form.append("<input type='hidden' id='signature_signatureType' name='signatureLog.signatureType' value='doubleSign'>");
    	}
    	if($('#signature_buttonCode').length == 0) {
    		form.append("<input type='hidden' id='signature_buttonCode' name='signatureLog.buttonCode' value='"+buttonCode+"'>");
    	}
		if($('#signature_username2').length == 0) {
    		form.append("<input type='hidden' id='signature_username2' name='signatureLog.secondUserName' value=''>");
    	}
		if($('#signature_reason2').length == 0) {
    		form.append("<input type='hidden' id='signature_reason2' name='signatureLog.secondReason' value=''>");
   	 	}
   	 	if($('#signature_remark2').length == 0) {
    		form.append("<input type='hidden' id='signature_remark2' name='signatureLog.secondRemark' value=''>");
   	 	}
		if($('#signature_password2').length == 0) {
    		form.append("<input type='hidden' id='signature_password2' name='signature.password2' value=''>");
   	 	}
   	 	if($('#signatureEnabled').length == 0) {
   	 		form.append("<input type='hidden' id='signatureEnabled' name='signatureEnabled' value='true'>");
   	 	}
   	 	if(namespace != null ){
   	 		if(parent.$('#signature_signTime1').length > 0) {
	    		form.append(parent.$('#signature_signTime1'));
	    	}
   	 	}
   	 	else {
   	 		if($('#signature_signTime1').length > 0) {
	    		form.append($('#signature_signTime1'));
	    	}
   	 	}
   	 	
    	if($('#signature_signTime2').length == 0) {
    		form.append("<input type='hidden' id='signature_signTime2' name='signatureLog.secondSignTime' value=''>");
    	}
		$('#signature_reason1').val(reason1);
		$('#signature_remark1').val(remark1);
		$('#signature_username1').val(username1);
		$('#signature_password1').val(password1)
    	$('#signature_username2').val(username2);
    	$('#signature_reason2').val(reason2);
    	$('#signature_remark2').val(remark2);
    	$('#signature_password2').val(password2);
    	$('#signature_signTime2').val(new Date().getTime());
		setTimeout(function(){
			if(namespace != null ) {
				parent.signatureDialog.close();
			}
			else {
				signatureDialog.close();
			}
			if(custom_callback != "undefined" && custom_callback != '') {
				var signatureInfo="signatureLog.firstUserName="+encodeURIComponent(username1)+"&signatureLog.firstReason="+encodeURIComponent(reason1)+"&signatureLog.firstSignTime="+signTime1+"&signatureLog.firstRemark="+encodeURIComponent(remark1)+"&signatureLog.buttonCode="+encodeURIComponent(buttonCode)+"&signatureLog.signatureType=doubleSign";
				signatureInfo+="&signatureLog.secondUserName="+encodeURIComponent(username2)+"&signatureLog.secondReason="+encodeURIComponent(reason2)+"&signatureLog.secondSignTime="+new Date().getTime()+"&signatureLog.secondRemark="+encodeURIComponent(remark2)+"&signature.password1="+password1+"&signature.password2="+password2+"&signatureEnabled=true";
				eval(custom_callback+"('"+signatureInfo+"')");
			}
			if(formId == "ImportForm") {
				//var isReplace="${request.getParameter('isReplace')!''}";
				//var isIgnore="${request.getParameter('isIgnore')!''}";
				//foundation.importExcel.submitForm(isReplace,isIgnore);
			}
			else {
				submitBapForm();
			}
			},500);
	}
	else {
		if(namespace != undefined && namespace != "") {
			parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.authenticationFail')}');
		}
		else {
			signature_doubleSign_formDialogErrorBarWidget.showMessage('${getText('foundation.signature.authenticationFail')}');
		}
	}
}
	function singleSignOk (namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
		if(namespace != '') {
			var username=parent.$('#foundation_signature_username1').val();
			var password=parent.$('#foundation_signature_password1').val();
			var reason=parent.$('#foundation_signature_reason1').val();
			var remark=parent.$('#foundation_signature_remark1').val();
		}
		else {
			var username=$('#foundation_signature_username1').val();
			var password=$('#foundation_signature_password1').val();
			var reason=$('#foundation_signature_reason1').val();
			var remark=$('#foundation_signature_remark1').val();
		}
		var errorMsg='';
		if(!signatureUtil.pwdAllowEmpty() && signatureUtil.isEmpty(password.trim())){
			errorMsg+="${getText('foundation.signature.passwordNotNull')}<br/>";
		}
		
		if(signatureUtil.isEmpty(reason.trim())) {
			errorMsg+="${getText('foundation.signature.reasonNotNull')}";
		}
		if(!signatureUtil.isEmpty(reason.trim()) && reason.length > 500) {
			errorMsg+="${getText('foundation.signature.resonNotBeyond')}";
		}
		if(!signatureUtil.isEmpty(remark.trim()) && remark.length > 300) {
			errorMsg+="${getText('foundation.signature.remarkNotBeyond')}";
		}
		if(errorMsg != '') {
			if(namespace != '') {
				parent.signature_singleSign_formDialogErrorBarWidget.showMessage(errorMsg);
			}
			else {
				signature_singleSign_formDialogErrorBarWidget.showMessage(errorMsg);
			}
			return ;
		}
		CUI.post("/signature/checkUserPassword/check?isFirstSigner=true&username="+encodeURIComponent(username)+"&password="+password,function(res){singleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore)},"json");
	}
	function doubleSignOK(obj,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
	var canclick=$(obj).attr('canclick');
	if(canclick == 'false') {
		return;
	} else {
		if(namespace != '') {
			var username=parent.$('#foundation_signature_username2').val();
			if(!parent.isIE() || parent.isNotIE8()) {
				parent.$('#foundation_signature_password2').val(parent.$('#foundation_signature_password2').next().val());
			}
			var password=parent.$('#foundation_signature_password2').val();
			var reason=parent.$('#foundation_signature_reason2').val();
			var remark=parent.$('#foundation_signature_remark2').val();
		} else {
			var username=$('#foundation_signature_username2').val();
			if(!isIE() || isNotIE8()) {
				$('#foundation_signature_password2').val($('#foundation_signature_password2').next().val());
			}
			var password=$('#foundation_signature_password2').val();
			var reason=$('#foundation_signature_reason2').val();
			var remark=$('#foundation_signature_remark2').val();
		}
		var errorMsg='';
		if(signatureUtil.isEmpty(username.trim())) {
			errorMsg+='${getText('foundation.signature.usernameNotNull')}<br/>';
		}
		if(namespace != '' ) {
			if(username == parent.$('#foundation_signature_username1').val()) {
				errorMsg+='${getText('foundation.signature.userNotRepeat')}<br/>';
			}
		}
		else {
			if(username == $('#foundation_signature_username1').val()) {
				errorMsg+='${getText('foundation.signature.userNotRepeat')}<br/>';
			}
		}
		if(!signatureUtil.pwdAllowEmpty() && signatureUtil.isEmpty(password.trim())){
			errorMsg+='${getText('foundation.signature.passwordNotNull')}<br/>';
		}
		if(signatureUtil.isEmpty(reason.trim())) {
			errorMsg+='${getText('foundation.signature.reasonNotNull')}';
		}
		if(!signatureUtil.isEmpty(reason.trim()) && reason.length > 500) {
			errorMsg+='${getText('foundation.signature.resonNotBeyond')}';
		}
		if(!signatureUtil.isEmpty(remark.trim()) && remark.length > 300) {
			errorMsg+='${getText('foundation.signature.remarkNotBeyond')}';
		}
		if(errorMsg != '') {
			if(namespace != '' ) {
				parent.signature_doubleSign_formDialogErrorBarWidget.showMessage(errorMsg);
			}
			else {
				signature_doubleSign_formDialogErrorBarWidget.showMessage(errorMsg);
			}
			return ;
		}
		var buttonCode='${request.getParameter("buttonCode")!""}';
		CUI.post("/signature/checkUserPassword/check?isFirstSigner=false&buttonCode="+encodeURIComponent(buttonCode)+"&username="+encodeURIComponent(username)+"&password="+password,function(res){doubleSign_callback(res,namespace,formId,buttonCode,false,'')},"json");
	}
}
	CUI('#${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}').unbind('submit.bapForm').bind('submit.bapForm',function(){
		//每次提交时先隐藏报错信息
		try{
			<#if parameters.dynamicAttributes["errorBarId"]??>
			${parameters.dynamicAttributes["errorBarId"]}Widget.close();
			</#if>
		}catch(e){
		
		}
		// 清除错误标红
		try{clearErrorLabels();}catch(e){}
		var ecFormFlag = <#if parameters.dynamicAttributes["ecform"]?has_content && parameters.dynamicAttributes["ecform"]=='true'>true<#else>false</#if>;
		<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
		ecFormFlag = true;
		</#if>
		var retrialFormFlag = <#if parameters.dynamicAttributes["retrialform"]?has_content && parameters.dynamicAttributes["retrialform"]=='true'>true<#else>false</#if>;
		if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			<#if parameters.dynamicAttributes["showLoading"]?has_content && parameters.dynamicAttributes["showLoading"]== 'true'>
			<#else>
			ecFormFlag = false;
			</#if>
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		<#if parameters.dynamicAttributes["foundationform"]?has_content && parameters.dynamicAttributes["foundationform"]=='true'>
			ecFormFlag = false;
		</#if>
		
		//禁用所有按钮
		//CUI("body").one("click", function(event){
		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('${parameters.id}',true,ecFormFlag);
		//});
		<#if parameters.validate?default(false) == true && validators??>
			try {
				if ( validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')} && !validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}() ) {
					return false;
				}
			}catch(e){}
		<#elseif parameters.validate?default(false) == true>
			//if(!validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}())	return false;
		<#elseif parameters.dynamicAttributes["ecform"]?has_content && parameters.dynamicAttributes["ecform"]=='true'>
			try {
				if ( validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')} && !validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}() ) {
					return false;
				}
			}catch(e){}
		</#if>
		$('#${parameters.id}').trigger('beforeSubmit');
		<#if parameters.dynamicAttributes["onsubmitMethod"]??>
			var successFlag = ${parameters.dynamicAttributes["onsubmitMethod"]};
			if(successFlag!=null && !successFlag) {/*if(CUI.Dialog) CUI.Dialog.toggleAllButton('${parameters.id}', true);*/return false;}
		</#if>
		if($('#${parameters.id} input[name="operateType"]').val() == "submit"){
			var deploymentId=$('#${parameters.id} input[name="deploymentId"]');
			var buttonCode=$('#${parameters.id} input[name="buttonCode"]');
			var namespace=$('#${parameters.id} input[name="namespace"]');
			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
				if(signatureInfo[0] != '') {
					var cancelItem = $('input[name="workFlowVarStatus"]');
					if(cancelItem.val() != "cancel") {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','${parameters.id}');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','${parameters.id}','ec.flow.submit',false)});
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','${parameters.id}');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','${parameters.id}','ec.edit.remove',false)});
					}
					
				}
				else {
					submitBapForm();
				}
			}
			else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
				if(signatureInfo[0] != '') {
					if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"${parameters.id}",false,'');
						if(signatureInfo[0] == 'singleSign') {
							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'${parameters.id}',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'${parameters.id}',buttonCode.val(),false)});},2000);
						}	
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"${parameters.id}",false,'');
						if(signatureInfo[0] == 'singleSign') {
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','${parameters.id}',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','${parameters.id}',buttonCode.val(),false)});},2000);
						}
					}
				}
				else {
					submitBapForm();	
				}
			}
			else {
				submitBapForm();
			}
		}
		else {
			submitBapForm();
		}
		return false;
	});
});

</script>
