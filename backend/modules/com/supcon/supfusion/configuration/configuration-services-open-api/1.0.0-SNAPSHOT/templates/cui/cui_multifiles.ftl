<#macro multifiles id,type,name="files",property_type="" linkId="-1",viewType="create",value="",isWrap=false,clickShowHeight=180,clickShowWidth=260,clickShowTitle="${getText('foundation.common.choiceFile')}",view=false,cssClass="multi-input",clickedClass="cui-search-click",propertyCode="",entityCode="">
	<div multifileType="multifiles" id="${id}MultiIDsContainerDiv" class="<#if viewType?? && viewType=='readonly'>multifile-container-readonly<#else>multifile-container</#if>"  style="height:auto!important;min-height:26px;">
	<span id="${id}MultiIDsContainer"><span style='width:auto;display:inline-block;'>&nbsp;</span>
		<#assign docList = getUploadedFileList(linkId?string, type, propertyCode) >
		<#if docList?? && docList?size gt 0>
		<#list docList as doc>
			<span style='width:auto;display:inline-block;padding-right:8px;' fullfilename='${doc.name}' docid='${doc.id}'><span title="${doc.name!} (${doc.sizeDis!}) <#if doc.createTime??>${doc.createTime?string('yyyy-MM-dd HH:mm')}</#if>" ondblclick="foundation.common.downloadSingle(${doc.id!}, '${entityCode!}');"><a href="#" style="text-decoration: none;" onclick="javascript:foundation.common.downloadSingle(${doc.id!}, '${entityCode!}');"><img src='${fileIcon(doc.name)}'>${doc.name}</a></span><#if viewType?? && viewType!="readonly"><img src='/bap/static/ec/delete.gif' style='cursor:pointer;vertical-align:middle;padding-bottom:3px;' onclick='deleteMulti${id}(this)' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' /></#if></span>
		</#list>
		</#if>
	</span>
	<#if !view>
	<#if viewType?? && viewType!="readonly">
	<span class="${clickedClass}"></span>
	<#--
	<input type="file" class="multi-file-input" name="${name}" size="35" onchange="addRowClone${id}(this)" />
	-->
	<input type="file" class="multi-file-input" name="files" property_type="propertyattachment" size="35" onchange="addRowClone${id}(this)" />
	</#if>
	<#--
	<input type="button" class="${clickedClass}" ></input>
	-->
	</div>
	<script type="text/javascript">
		(function(){
			if(YAHOO.env.ua.webkit){
				$('input:file:not([compType="image"])').attr('class', 'multi-file-input-chrome');
			}
			$('#${id}MultiIDsContainerDiv').data('origFileInput', $('#${id}MultiIDsContainerDiv input:file').eq(0).clone(true));
			if($('input[name="${name}.type"]',$('#${id}MultiIDsContainerDiv')).length == 0) {
				$('<input type="hidden" name="${name}.type" value="${type!}"/>').appendTo($('#${id}MultiIDsContainerDiv'));
			}
			if($('input[name="uploadName"]',$('#${id}MultiIDsContainerDiv')).length == 0) {
				$('<input type="hidden" name="uploadName" value="${name}"/>').appendTo($('#${id}MultiIDsContainerDiv'));
			}
		})();
		function addRowClone${id}(_selfObj) {
			var isLogin = true;
			$.ajax({url:"/foundation/workbench/timer",
				async: false,
				complete: function (xhr,status) {
					if (status == "error") {
						isLogin = false;
						showLoginDialog();
					}
				}
			});
			if (isLogin == false) {
				return false;
			} else {
				/*
				var isIE = /msie/i.test(navigator.userAgent) && !window.opera;         
				var fileSize = 0;          
				if (isIE && !_selfObj.files) {      
					var filePath = _selfObj.value;      
					var fileSystem = new ActiveXObject("Scripting.FileSystemObject");         
					var file = fileSystem.GetFile (filePath);      
					fileSize = file.Size;     
				} else {     
					fileSize = _selfObj.files[0].size;      
				}      
				if(fileSize>104857600){   
					$(_selfObj).remove();
					$('#${id}MultiIDsContainerDiv').append($('#${id}MultiIDsContainerDiv').data('origFileInput').clone(true));
					CUI.Dialog.alert("${getHtmlText('foundation.common.filesize', 100)}");   
					return;
				}   
				*/
				var frameId = "jUploadFrameuplaodFile";
				if($('#'+frameId)){
					$('#'+frameId).remove();
				}
				var tmpFileName = getFileName(_selfObj.value);
				if($('#${id}MultiIDsContainer span[fullfilename="' + tmpFileName + '"]').length > 0) {
					$(_selfObj).remove();
					$('#${id}MultiIDsContainerDiv').append($('#${id}MultiIDsContainerDiv').data('origFileInput').clone(true));
					CUI.Dialog.alert("${getHtmlText('foundation.common.existFileWarn')}");
				} else {
					var iframe = createUploadIframe("uplaodFile", false);
					var form = jQuery('<form action="/foundation/workbench/uploadFile" method="POST" enctype="multipart/form-data" target="' + iframe.id + '"></form>');
					var newSpan = $("<span style='width:auto;display:inline-block;padding-right:8px;margin-top:2px;'><img src='" + foundation.common.uploadFileType({fileName : tmpFileName}) + "'>"+tmpFileName+"<img src='/bap/static/ec/delete.gif' style='cursor:pointer;vertical-align:middle;padding-bottom:3px;' onclick='deleteMulti${id}(this)' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' /></span>");
					newSpan.attr('fullfilename', tmpFileName);
					newSpan.attr('isUploaded', 'false');
					$("#${id}MultiIDsContainer").append(newSpan);
					$('body').trigger('resize');
					$(_selfObj).hide();
					$('#${id}MultiIDsContainerDiv').append('<input type="hidden" name="${name}.propertyCode" value="${propertyCode}">');
					$('#${id}MultiIDsContainerDiv').append('<input type="hidden" name="${name}.fileType" value="attachment">');
					jQuery(form).appendTo('body');
					$(form).append('<input type="hidden" name="propertyCode" value="${propertyCode}">');
					$(form).append('<input type="hidden" name="fileType" value="attachment">');
					$(form).append(_selfObj);
					$('#${id}MultiIDsContainerDiv').append($('#${id}MultiIDsContainerDiv').data('origFileInput').clone(true));
					jQuery(form).submit();
					createLoadPanel(false, $(this).parents('.ewc-dialog-blove').attr('id'), {head:"${getHtmlText('foundation.fileupload.uploading')}", show: true, opacity:50, bgColor:"#666666"});
					var iframeCallBack = function(form){
						$(_selfObj).remove();
						var iframeId = "jUploadFrameuplaodFile";
						var io = document.getElementById(frameId);
						var xml = {};
						if (io.contentWindow) {
							xml.responseText = io.contentWindow.document.body ? io.contentWindow.document.body.innerHTML : null;
							xml.responseXML = io.contentWindow.document.XMLDocument ? io.contentWindow.document.XMLDocument : io.contentWindow.document;
			
						} else if (io.contentDocument) {
							xml.responseText = io.contentDocument.document.body ? io.contentDocument.document.body.innerHTML : null;
							xml.responseXML = io.contentDocument.document.XMLDocument ? io.contentDocument.document.XMLDocument : io.contentDocument.document;
						}
						$('#${id}MultiIDsContainerDiv').append('<input type="hidden" name="${name}.filePath" value="' + xml.responseText +'">');
						closeLoadPanel();
					}
					
					
					jQuery('#' + frameId).bind('load', function() {
						iframeCallBack();
						jQuery(form).remove();
					});
				}
			}
		}
		function getFileName(filePath) {
			var fileName = filePath.substring(filePath.lastIndexOf('\\')+1,filePath.length);
			if(fileName==filePath) {
				fileName = filePath.substring(filePath.lastIndexOf('/')+1,filePath.length);
			}
			return fileName;
		}
		function deleteMulti${id}(_selfObj) {
			var currSpan = $(_selfObj).parent('span:first');
			if(currSpan.attr('isUploaded') == 'false') {
				var indToRemove = $('#${id}MultiIDsContainer span[isUploaded="false"]').index(currSpan);
				var fileToRemove = $("#${id}MultiIDsContainerDiv input:file").eq(indToRemove);
				var propertyCodeToRemove = $("#${id}MultiIDsContainerDiv input[name='${name}.propertyCode']").eq(indToRemove);
				var fileTypeToRemove = $("#${id}MultiIDsContainerDiv input[name='${name}.fileType']").eq(indToRemove);
				var filePathToRemove = $("#${id}MultiIDsContainerDiv input[name='${name}.filePath']").eq(indToRemove);
				fileToRemove.remove();
				currSpan.remove();
				propertyCodeToRemove.remove();
				fileTypeToRemove.remove();
				filePathToRemove.remove();
			} else {
				var ids2del = $('input[name="ids2del"]');
				if(ids2del.length == 0) {
					$('<input name="ids2del" originalvalue="">').val(currSpan.attr('docid')).appendTo($('#${id}MultiIDsContainerDiv'));
					ids2del = $('input[name="ids2del"]');
					ids2del.hide();
				} else {
					ids2del.val(ids2del.val() + ',' + currSpan.attr('docid'));
				}
				currSpan.remove();
			}
			if($('#${id}MultiIDsContainerDiv').find('file').length == 0){ 
				$('#${id}MultiIDsContainerDiv').append($('#${id}MultiIDsContainerDiv').data('origFileInput').clone(true));
			}
		}
	</script>					
	</#if>
</#macro>