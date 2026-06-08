<#macro uploadfile type,callback,namespace="upload.defalut",integrateFlag=false,linkId="",multi=true,onClickBefore="",acceptType="all",maxSize=10485760,look="button",width=650,height=500,text="">
<#assign ns=namespace> 
<#if integrateFlag>
	<script type="text/javascript" language="javascript" charset="utf-8">
	CUI.ns("${ns}");
	${ns}.fileIsexists = false;
	${ns}.beforeSubmitUploadForm = function(){
		var filesWrapDiv = CUI('*[id="${ns}.filesWrap"]');
		${ns}.fileIsexists = false;
		var file = '';
		CUI(filesWrapDiv).find('input').each(function(i){
			if('true'==CUI(this).attr('removed')||true==CUI(this).attr('removed')) {
				void(0);
			} else {
				file = CUI(this).val();
				if(file != null && file != undefined && file != '') {
					${ns}.fileIsexists = true;
				}
			}
		});
		// 获取需要删除的id
		${ns}.getids2Delete();
		return true;
	}
	${ns}.downloadFiles = function(){
		${ns}.downloadMulti();
	}
	${ns}.getFileName = function(filePath) {
		var fileName = filePath.substring(filePath.lastIndexOf('\\')+1,filePath.length);
		if(fileName==filePath) {
			fileName = filePath.substring(filePath.lastIndexOf('/')+1,filePath.length);
		}
		return fileName;
	}

	${ns}.resumeLabel = "${getText("foundation.staff.infoset.restore")}";
	${ns}.deleteLabel = "<img src="/bap/static/foundation/images/icon-del.gif">";
	${ns}.trNodeDis = null;
	
	CUI(document).ready(function() {
		${ns}.trNodeDis = CUI('*[id="${ns}.selectedFiles"]').find('tr').first().clone(true);
	});
	
	${ns}.addRowClone = function(_selfObj) {
		var filesWrapDiv = CUI('*[id="${ns}.filesWrap"]');
		var tblBodyDis = CUI('*[id="${ns}.selectedFiles"]').find('tbody').first();
		CUI('*[id="${ns}.selectedTable"]').height(CUI('*[id="${ns}.selectedTable"]').height()+26);
		var exists = false;
		var lastInd = filesWrapDiv.find('input').size()-1;
		filesWrapDiv.find('input').each(function(i){
			if(lastInd!=i && filesWrapDiv.find('input').eq(lastInd).val()==CUI(this).val()) {
				exists = true;
				return false;
			}
		});
		if(exists) {
			filesWrapDiv.find('input').eq(lastInd).remove();
		}
		filesWrapDiv.append('<input type="file" name="files" size="35" onchange="javascript:${ns}.addRowClone(this);" accept="audio/basic" />');
		// 清空显示内容
		tblBodyDis.empty();
		for(var i=0;i<CUI('input', filesWrapDiv).length;i++) {
			if(i<CUI('input', filesWrapDiv).length-1) {
				filesWrapDiv.find('input').eq(i).hide();
			}
		}
		CUI('input', filesWrapDiv).each(function(i){
			var name = CUI(this).val();
			var tr;
			if(name!=null&&name!=undefined&&name!='') {
				tblBodyDis.append(${ns}.trNodeDis.clone(true));
				tr = tblBodyDis.find('tr').eq(i);
				tr.show();
				tr.find('td[fieldType="no"]').html((i+1)+'.');
				var tmpFileName = ${ns}.getFileName(name);
				if(tmpFileName.length>15) {
					tr.find('td[fieldType="fileName"]').html(tmpFileName.substring(0,15)+'...');
					tr.find('td[fieldType="fileName"]').attr('title', tmpFileName);
				} else {
					tr.find('td[fieldType="fileName"]').html(tmpFileName.substring(0,15));
				}
			} else {
				return false;
			}
			var removeFlag = CUI(this).attr('removed');
			if(removeFlag) {
				removeFlag = removeFlag;
			} else {
				removeFlag = 'false';
			}
			if(removeFlag=='false') {
				tr.find('input').attr('removed',false);
				tr = tblBodyDis.find('tr').eq(i);
				tr.find('td[fieldType="no"]').removeClass('remove').addClass('regular');
				tr.find('td[fieldType="fileName"]').removeClass('remove').addClass('regular');
				tr.find('a[fieldType="operate"]').html(${ns}.deleteLabel);
				tr.find('input').prop('disabled',false);
			} else {
				tr.find('input').attr('removed',true);
				tr = tblBodyDis.find('tr').eq(i);
				tr.find('td[fieldType="no"]').removeClass('regular').addClass('remove');
				tr.find('td[fieldType="fileName"]').removeClass('regular').addClass('remove');
				tr.find('a[fieldType="operate"]').html(${ns}.resumeLabel);
				tr.find('input').prop('disabled',true);
			}
		});
	};

	${ns}.toggerFile = function(obj) {
		var filesWrapDiv = CUI('*[id="${ns}.filesWrap"]');
		var tblBodyDis = CUI('*[id="${ns}.selectedFiles"]').find('tbody').first();
		var indToRemove = CUI('tr', tblBodyDis).index(CUI(obj).parent().parent());
		var currInput=CUI('input', filesWrapDiv).eq(indToRemove);
		var removeFlag = currInput.attr('removed');
		if(removeFlag) {
			removeFlag = removeFlag;
		} else {
			removeFlag = 'false';
		}
		var tr = null;
		if(removeFlag=='true'||removeFlag==true) {
			currInput.attr('removed',false);
			currInput.prop('disabled',false);
			tr = CUI('tr',tblBodyDis).eq(indToRemove);
			CUI('td[fieldType="no"]', tr).removeClass('remove').addClass('regular');
			CUI('td[fieldType="fileName"]', tr).removeClass('remove').addClass('regular');
			CUI('a[fieldType="operate"]', tr).html(${ns}.deleteLabel);
			CUI('input', tr).prop('disabled',false);
		} else {
			currInput.attr('removed',true);
			currInput.prop('disabled',true);
			tr = CUI('tr',tblBodyDis).eq(indToRemove);
			CUI('td[fieldType="no"]', tr).removeClass('regular').addClass('remove');
			CUI('td[fieldType="fileName"]', tr).removeClass('regular').addClass('remove');
			CUI('a[fieldType="operate"]', tr).html(${ns}.resumeLabel);
			CUI('input', tr).prop('disabled',true);
		}
	};
	${ns}.ids2del = {};
	// 导出
	${ns}.downloadSingle = function(evt,obj){
		var url = "/foundation/workbench/download?id="+obj.id;
		${ns}.openExportFrame(url);
	};
	${ns}.deleteSingle = function(evt,obj){
		var tmp = ${ns}.ids2del[obj.id];
		if(tmp!=null&&tmp!=undefined&&tmp!='') {
			delete ${ns}.ids2del[obj.id];
		} else {
			${ns}.ids2del[obj.id] = obj.id;
		}

		var ids = '';
		CUI.each(${ns}.ids2del,function(i,v){
			if(i!=null&&i!='') {
				ids +=','+i;
			}
		});
		if(ids!='') {
			ids = ids.substring(1);
		}
		var formObj = CUI('input[name="linkId"]').parents("form");
		if(CUI('input[name="ids2del"]', formObj).length>0) {
			CUI('input[name="ids2del"]', formObj).val(ids);
		} else {
			CUI('<input type="hidden" name="ids2del" />').val(ids).appendTo(formObj);
		}
	};
	${ns}.downloadMulti = function(){
		var url = "/foundation/workbench/downloads?linkId="+CUI('input[name="linkId"]').val()+"&type="+CUI('input[name="type"]').val();
		${ns}.openExportFrame(url);
	}
	${ns}.openExportFrame = function(url) {
		var handle = null;
		var open_url= url;
		var window_height = window.screen.availHeight-63;
		var window_width  = window.screen.availWidth-20;
		ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,top=0,left=0,toolbar=no,menubar=no,location=no,status=no";
		handle = window.open(open_url,"",ShowStyle);
		handle = null;
	};
	${ns}.getids2Delete = function(){
		var ids = '';
		CUI.each(${ns}.ids2del,function(i,v){
			if(i!=null&&i!='') {
				ids +=','+i;
			}
		});
		if(ids!='') {
			ids = ids.substring(1);
		}
		CUI('<input type="hidden" name="ids2del" value="' + ids + '"/>').appendTo(CUI('#ec_main_form'));
	};
	</script>
	<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function() {
		var noNestColumnDefs = new Array();
		var testnode = new Object();
		testnode.key = 'name';
		testnode.label = "${getText("foundation.staff.infoset.fileName")}";
		testnode.width = '150';
		testnode.edit = false;
		testnode.textalign = 'left';
		testnode.colclick=function(obj){${ns}.downloadSingle(null,obj);};
		noNestColumnDefs[noNestColumnDefs.length] = testnode;

		testnode = new Object();
		testnode.key = 'sizeDis';
		testnode.label = "${getText('foundation.staff.infoset.fileSize')}";
		testnode.width = '50';
		testnode.edit = false;
		testnode.textalign = 'right';
		noNestColumnDefs[noNestColumnDefs.length] = testnode;

		testnode = new Object();
		testnode.key = 'createTimeDis';
		testnode.label = "${getText("foundation.staff.infoset.uploadTime")}";
		testnode.width = '80';
		testnode.edit = false;
		noNestColumnDefs[noNestColumnDefs.length] = testnode;

		testnode = new Object();
		testnode.key = 'createStaffName';
		testnode.label = "${getText("foundation.staff.infoset.uploader")}";
		testnode.width = '60';
		testnode.edit = false;
		noNestColumnDefs[noNestColumnDefs.length] = testnode;

		testnode = new Object();
		testnode.key = 'memo';
		testnode.title = 'memo';
		testnode.label = "${getText("foundation.staff.infoset.meno")}";
		testnode.width = '120';
		testnode.edit = false;
		noNestColumnDefs[noNestColumnDefs.length] = testnode;

		testnode = new Object();
		testnode.key = 'memo';
		testnode.type = 'checkbox';
		testnode.label = "${getText("foundation.staff.infoset.delete")}";
		testnode.colclick=function(obj){${ns}.deleteSingle(null,obj);};
		testnode.width = '25';
		testnode.edit = true;
		noNestColumnDefs[noNestColumnDefs.length] = testnode;
		try{
		   var url = encodeURI('/foundation/workbench/upload-list?linkId=${linkId}&type=${type}');
		   if(YAHOO.env.ua.ie){
		        ${ns}.DT_ExistsFiles = new CUI.DataTable("${ns}.DT_ExistsFiles",noNestColumnDefs,null,{
		          	  ShowSetPath : '/foundation/workbench/upload-list?linkId=${linkId}&type=${type}',
		              paginator : false,
		              dblclick:"${ns}.downloadSingle",
		              method : "POST",
		              editable:true,
		              hideKey : ['id','path']
		        });
		   }else{
			    ${ns}.DT_ExistsFiles = new CUI.DataTable("${ns}.DT_ExistsFiles",noNestColumnDefs,null,{
			          ShowSetPath : '/foundation/workbench/upload-list?linkId=${linkId}&type=${type}',
				      paginator : false,
				      dblclick: "${ns}.downloadSingle",
				      method : "POST",
				      editable:true,
				      hideKey : ['id','path']
			     });
		   }

   	   	   ${ns}.DT_ExistsFiles.setRequestDataUrl(url);
		}catch(e){}
	});
	</script>

	<input type="hidden" name="linkId" value="${linkId}"/>
	<#if staff?exists>
		<input type="hidden" name="files.staffId" value="${staff.id?c}" />
	</#if>
	<input type="hidden" name="files.type" value="${type}"/>

	<div id="${ns}.filesWrap" class="fileWrap" style="width:60%;height:20px;padding:15px 0 10px 15px;overflow:hidden;">
		<span>${getHtmlText('foundation.staff.infoset.attachment')}：</span>
		<input type="file" name="files" size="35" onchange="javascript:${ns}.addRowClone(this);" accept="audio/basic" />
	</div>
		<div id="${ns}.selectedTable" style="width:60%;height:1px;overflow:hidden;">
			<table id="${ns}.selectedFiles" cellpadding="0" cellspacing="0" border="0" height="100%" style="margin-left: 12px;">
				<tr style="display: none;" height="26px">
					<td fieldType="no"  width="5"></td>
					<td fieldType="fileName" width="30%"></td>
					<td width="5%"><a fieldType="operate" href="#" onclick="javascript:${ns}.toggerFile(this);"><img src="/bap/static/foundation/images/icon-del.gif"></a></td>
					<td width="64%" nowrap>${getHtmlText('foundation.staff.infoset.meno')}<input size="30" maxlength="300" type="text" name="files.memos" style="width:93%;" class="cui-edit-field"></input></td>
				</tr>
			</table>
		</div>
	<div id="${ns}.DT_ExistsFiles" style="margin:10px 8px;"></div>

<#else>

	<script type="text/javascript" charset="utf-8" language="javascript">
	CUI.ns("${ns}");
	var uploadDialog = null;
	${ns}._showUploadDialog = function(){
		try{
			try{
				var _callbefore_ = "${onClickBefore}";
				if(_callbefore_!=null&&_callbefore_!='') {
					var retValue = eval("${onClickBefore}()");
					if(retValue==false) {
						return;
					}
				}
			}catch(e){}
			var params = {};
			params.acceptType = "${acceptType?lower_case}";
			if(params.acceptType=='all') {
				params.acceptType = '';
			}
			var url = "/foundation/workbench/upload-init?linkId=${linkId}&type=${type}&callback=${callback}&multi=${multi?string('true','false')}&maxSize=${maxSize}";
			uploadDialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.staff.infoset.title')}",
				url:url,
				argument:params,
				<#if acceptType!='all'>
				description:"${getText('foundation.staff.infoset.allowUpload')}"+":"+ params.acceptType,
				</#if>
				modal:true,
				height:'${height}',
				width: '${width}',
				dragable:true,
				buttons:[
					<#if multi>
						{name:"${getHtmlText('upload.download')}",handler:function(){${ns}.downloadFiles();}},{name:"${getHtmlText('common.button.save')}",handler:function(){submitUploadForm();}},{name:"${getHtmlText('foundation.common.closed')}",id:"close",handler:function(){this.close();}}
					<#else>
						{name:"${getHtmlText('common.button.upload')}",handler:function(){submitUploadForm();}},{name:"${getHtmlText('common.button.cancel')}",id:"close",handler:function(){this.close();}}
					</#if>
					]
			});
			uploadDialog.show();
		}catch(e){}
	}
	${ns}.downloadFiles = function(){
		${ns}.downloadMulti();
	}
	${ns}.downloadMulti = function(){
		var url = "/foundation/workbench/downloads?linkId="+CUI('input[name="linkId"]').val()+"&type="+CUI('input[name="type"]').val();
		${ns}.openExportFrame(url);
	}
	${ns}.openExportFrame = function(url) {
		var handle = null;
		var open_url= url;
		var window_height = window.screen.availHeight-63;
		var window_width  = window.screen.availWidth-20;
		ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,top=0,left=0,toolbar=no,menubar=no,location=no,status=no";
		handle = window.open(open_url,"",ShowStyle);
		handle = null;
	};
	</script>
	<#if look == 'button'>
		<input name="_uploadButton" id="_uploadButton" type="button" value="${text}" onclick="javascript:${ns}._showUploadDialog();" />
	<#else>
		<span name="_uploadLabel" id="_uploadLabel" style="cursor:pointer;border:none;" onclick="javascript:${ns}._showUploadDialog();">${text}</span>
	</#if>
</#if>
</#macro>