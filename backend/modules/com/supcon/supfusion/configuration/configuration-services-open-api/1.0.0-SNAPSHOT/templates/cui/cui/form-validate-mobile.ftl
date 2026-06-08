<script type="text/javascript">
$(function(){
	function ajaxFileUploadCommon(s) {
		// TODO introduce global settings, allowing the client to modify
		// them for all requests, not only timeout
		s = $.extend({}, $.ajaxSettings, s);
		var id = new Date().getTime();
		var form = createUploadForm(id, s.formId);
		var io = createUploadIframe(id, s.secureuri);
		var frameId = 'jUploadFrame' + id;
		var formId = 'jUploadForm' + id;
		// Watch for a new set of requests
		if (s.global && !$.active++) {
			$.event.trigger("ajaxStart");
		}
		var requestDone = false;
		// Create the request object
		var xml = {}
		if (s.global)
			$.event.trigger("ajaxSend", [ xml, s ]);
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
				if(foundation.common._errorCode) {
					foundation.common.showErrorMsg({"_errorCode" : foundation.common._errorCode});
				}
			} catch (e) {
				 workbenchErrorBarWidget.showMessage(e,"f",null,"${getText('ec.common.error.submit')}");
				//$.handleError(s, xml, null, e);
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
							$.event.trigger("ajaxSuccess", [ xml, s ]);
					} else
						 workbenchErrorBarWidget.showMessage(status,"f");
						//$.handleError(s, xml, status);
				} catch (e) {
					if(e.name == 'SyntaxError') {
					 	if(xml && xml.responseText) {
						 	var arr1 = xml.responseText.match('<span[\\s\\S^/]*>.+</span>');
						 	var msg = arr1[0].replace(/<span[^/>]*>/,'').replace('</span>','');
						 	workbenchErrorBarWidget.showMessage(msg, 'f');
					 	}
						//$.handleError(s, xml, status, e);
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
					$.event.trigger("ajaxComplete", [ xml, s ]);

				// Handle the global AJAX counter
				if (s.global && !--$.active)
					$.event.trigger("ajaxStop");

				// Process result
				if (s.complete)
					s.complete(xml, status);

				$(io).unbind();

				setTimeout(function() {
					try {
						$(io).remove();
						$(form).remove();
	
					} catch (e) {
						 workbenchErrorBarWidget.showMessage(e,"f",null,"${getText('ec.common.error.submit')}");
					}
				}, 100);
				xml = null;
				CUI.hideLoadPanel();
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

			var form = $('#' + formId);
			$(form).attr('action', s.url);
			$(form).attr('method', 'POST');
			$(form).attr('target', frameId);
			if (form.encoding) {
				$(form).attr('encoding', 'multipart/form-data');
			} else {
				$(form).attr('enctype', 'multipart/form-data');
			}

			$(form).submit();
			setTimeout(function(){
				var allfiles = $('#' + s.formId + ' input[type="file"]');
				$('input[type="file"]', form).each(function(ind){
					$(allfiles[ind]).before($(this));
					$(allfiles[ind]).remove();
				});
			}, 2000);
		} catch (e) {
			 workbenchErrorBarWidget.showMessage(e,"f");
			//$.handleError(s, xml, null, e);
		}
		$('#' + frameId).bind('load', function() {
			uploadCallback();
		});
		return {
			abort : function() {
			}
		};

	}
	
	function uploadHttpData(r, type) {
		var data = !type;
		data = type == "xml" || data ? r.responseXML : r.responseText;
		if (type == "script")
			$.globalEval(data);
		if (type == "json")
			eval("data = " + data);
		if (type == "html")
			$("<div>").html(data).evalScripts();
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
						var datagrids = $('body').data(pageId + '__mobile___datagrids');
					}
					if(msg && msg.success == false){
	        			var errorMsgs = "";
						$.each(msg.items,function(index,item){
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
								var field = $("#${parameters.id} *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
			                		showErrorField(field.next());
			                	} else {
			                		showErrorField(field);
			                	}
							}
							$("#${parameters.id} *[name='"+index+"']").first().focus();
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
						$.each(msgErrors,function(index,item){
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
						CUI.hideLoadPanel();
					}else{
						window.onbeforeunload = null;
						CUI.hideLoadPanel();
						<#if parameters.dynamicAttributes["callback"]??>${parameters.dynamicAttributes["callback"]}(msg,$('#'+formId).serialize());</#if>
						// 提示app刷新待办
						try{
							if ( CUI.isAndroid ){
								window.mobilejs.pendingRefresh()
							} else if ( CUI.isIos ) {
							
							}
						}catch(e){}
					}
				}
			}
		);
	}
	$('#${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}').bind('submit',function(){
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
		if(ecFormFlag && !validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}())	{
			return false;
		}
		<#if parameters.dynamicAttributes["onsubmitMethod"]??>
			var successFlag = ${parameters.dynamicAttributes["onsubmitMethod"]};
			if(successFlag!=null && !successFlag) {/*if(CUI.Dialog) CUI.Dialog.toggleAllButton('${parameters.id}', true);*/return false;}
		</#if>
		CUI.showLoadPanel();
		var formId = '${parameters.id}';
	    $('input[type="text"]','#'+formId).each(function(){
	        var v=$.trim($(this).val());
	        $(this).val(v);
	    });

		ajaxFileUpload($('#'+formId).attr('action'), formId);
		return false;
	});
});
</script>
