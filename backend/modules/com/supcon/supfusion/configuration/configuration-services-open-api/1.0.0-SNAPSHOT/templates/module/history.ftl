
<div id="ec_module_history_edit_main_div" class="edit-content-dialog" style="overflow: hidden; height:40px ">
    <ul class="edit-tabs" style="width:796px">
		<li id ='upload'>${getText('ec.module.uploadInfo')}</li>
	</ul>
</div>
<div style="display: block;">
	<form id="uploadBatch_queryForm" name="uploadBatch_queryForm" onsubmit="return false;">
		<@quickquery formId="uploadBatch_queryForm"  fieldcodes="base_staff_name:${getText('ec.module.uploadstaff')}||upload_modelName:ec.module.modulename" expandType="all" isExpandAll=true unique="LAST_QUERY_audits_operationAudit_frame">

				<@queryfield formId="uploadBatch_queryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="uploadBatchQuery()"></@queryfield>
				
				<@queryfield formId="uploadBatch_queryForm" code="upload_modelName" key="module.name"  isCustomize=true showFormat="TEXT" style="" >
				<input type="text"  multiable="false" class="cui-edit-field" name="foundation.module.name" property_type="" isprecise="true" formid="uploadBatch_queryForm" tabindex="" id="module_name" value="" originalvalue="" style="" title=""  exp=""  iscrosscompany="false" refviewcode="" currentviewcode="" autocomplete="off">
				</@queryfield>
				
				<div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;float:right">
					<@querybutton formId="uploadBatch_queryForm" type="search" onclick="uploadBatchQuery()" />									
		 			<@querybutton formId="uploadBatch_queryForm" type="clear"  />
		 		</div>
				
		</@quickquery>
	</form>
	<@datatable moreButtonResizeFlag=false firstLoad=true id="ec_module_uploadBatch_datatable" dataUrl="/msService/ec/module/uploadInfoBatch/list-query" queryFunc="uploadBatchQuery('query')" renderOverEvent="uploadBatch_renderOverEvent" pageInitMethod="uploadBatch_PageInitMethod" hidekey="['id']">
		<@datacolumn key="uploadStaff.name" label="${getHtmlText('ec.module.uploadstaff')}" width=70  sortable=false />
		<@datacolumn key="uploadState" label="${getHtmlText('ec.module.uploadState')}" width=70 sortable=false />
		<#assign uploadDate_displayDefaultType  = ''>
		<@datacolumn columnName="UPLOAD_DATE" showFormat="YMD_HMS" defaultDisplay="${uploadDate_displayDefaultType!}"  key="uploadDate"   label="${getText('ec.module.uploadDate')}" textalign="center"  width=150   type="datetime" sortable=false />
		<@datacolumn key="describe" label="${getHtmlText('ec.module.describe')}" width=200 sortable=false />
		<@datacolumn key="totalTime" label="${getHtmlText('ec.module.uploadTime')}" width=80 sortable=false />
		<@datacolumn key="uploada" label="${getHtmlText('ec.module.uploada')}" textalign="center" width=70 sortable=false />
	</@datatable>
</div>
<div style="display: none;">
	<form id="deploy_queryForm" name="deploy_queryForm" onsubmit="return false;">
		<@quickquery formId="deploy_queryForm"  fieldcodes="base_staff_name:${getText('ec.module.deployuser')}||upload_modelName:${getText('ec.module.publishTask')}" expandType="all" isExpandAll=true unique="LAST_QUERY_audits_operationAudit_frame" >
				

				<@queryfield formId="deploy_queryForm" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="deployQuery()"></@queryfield>
				
				<@queryfield formId="deploy_queryForm" code="upload_modelName" key="module.name"  isCustomize=true showFormat="TEXT" style="" >
				<input type="text"  multiable="false" class="cui-edit-field" name="foundation.module.name" property_type="" isprecise="true" formid="deploy_queryForm" tabindex="" id="module_name" value="" originalvalue="" style="" title=""  exp=""  iscrosscompany="false" refviewcode="" currentviewcode="" autocomplete="off">
				</@queryfield>

				<div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;float:right">
					<@querybutton formId="deploy_queryForm" type="search" onclick="deployQuery()" />									
		 			<@querybutton formId="deploy_queryForm" type="clear"  />
		 		</div>
				
		</@quickquery>
	</form>
</div>
<script type="text/javascript">
	$(function(){
		$('.edit-tabs').css('background','#ffffff');
		$('#upload').bind('click',function(){
	            $(this).addClass('current');
	            $(this).siblings().removeClass('current');
	            $('#ec_module_uploadBatch_datatable').parent().css('display','block');
	            $('#ec_module_deploy_datatable').parent().css('display','none');
				$(window).trigger('resize');
		       })
		$('#deploy').bind('click',function(){
	            $(this).addClass('current');
	            $(this).siblings().removeClass('current');
	            $('#ec_module_uploadBatch_datatable').parent().css('display','none');
	            $('#ec_module_deploy_datatable').parent().css('display','block');
		       })
		//ie情况下调整宽度
		if($.browser.msie8){
			$('#ec_module_history_edit_main_div').find('ul').css('width','800px');
		}
		//$('#ec_module_history_edit_main_div #upload').trigger('click');
		//$('#ec_module_history_edit_main_div #upload').trigger('click');
		//绑定查询回车键函数
		$("#deploy_queryForm input[type='text'][mneType!='mnemonic']").unbind("keydown").bind("keydown", function(evt) {
            if (evt.keyCode == 13) {
                eval("deployQuery('query')");
            }
        });
        $("#uploadBatch_queryForm input[type='text'][mneType!='mnemonic']").unbind("keydown").bind("keydown", function(evt) {
            if (evt.keyCode == 13) {
                eval("uploadBatchQuery('query')");
            }
        });
	});

	/**
	 * 发布记录查询
	 * @method deployQuery
	 */
	function deployQuery(type,pageNo,sortFlag){
		var url = "/msService/ec/module/deployInfoBatch/list-query?a=1";
		var dataPost="";
		var staffId = CUI('#deploy_queryForm input[name="staff.id"]').val();
		var staffName = $('#deploy_queryForm').find('input[name="staff.name"]').val();
		var	moduleName = CUI('#deploy_queryForm #module_name').val();
		//var time_start = CUI('#createDate_start').val();
		//var time_end = CUI('#createDate_end').val();
		var pageSize=CUI('input[name="ec_module_deploy_datatable_PageLink_PageCount"]').val();
		if(null != pageSize && undefined != pageSize && "" != pageSize) {
							dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
		}
		if(null != pageNo && undefined != pageNo && "" != pageNo) {
							dataPost += "&page.pageNo="+encodeURIComponent(pageNo);	
		}
		if(null != staffId && undefined != staffId && "" != staffId) {
			dataPost += "&deployInfoBatch.deployStaff.id=" + staffId;
		}else if(null != staffName && undefined != staffName && "" != staffName){
			dataPost += "&deployInfoBatch.deployStaff.name=" + staffName;
		}
		if(null != moduleName && undefined != moduleName && "" != moduleName) {
			dataPost += "&deployInfoBatch.moduleName=" + moduleName;
		}
		/*暂时先不用这个条件
		if(time_start != "" && time_end != ""){
			if(time_end < time_start){
				workbenchErrorBarWidget.showMessage(getHtmlText('foundation.audit.timecheck'),'f')
				return false;
			}
		}
		if(time_start != ""){
			dataPost += "&cTime_start=" + time_start;
		}
		if(time_end != ""){
			dataPost += "&cTime_end=" + time_end;
		}
		*/
		ec_module_deploy_datatableWidget.setRequestDataUrl(url,dataPost);
	};	
	
	
	function deploy_renderOverEvent(){
		$('#ec_module_deploy_datatable #ec_module_deploy_datatable_tbody tr').each(function(index,obj){
		    var target = $(obj); 
		    var id = target.find('input[name="id"]').val();
		    var url = "/msService/ec/module/deployInfDetial?id="+id;
		    //var href = '<a href='+url+'>查看详细</a>'
		    var href = '<a href="#" onclick="openDeployDetial('+id+');">${getText("foundation.synchronize.viewLog")}</a>';
		    target.find('td[key="deploya"] div').html(href);
		})
		$.each($('#ec_module_deploy_datatable_tbody tr'),function(index,data){
		    var state = $(data).find("td[key='deployState']").text();
		    if(null != state && undefined != state && "" != state && '失败'==state){
		        $(data).find("td[key=deployState]").css('color','#ff0000');
		    }
		    }
		)
	}
	var openDeployDetial = function(id){
	    var url = "/msService/ec/module/deployInfDetial?deployBatchId="+id;
	    new CUI.Dialog({
	        title : "${getHtmlText('ec.module.publishDetails')}",
	        width : 800,
	        height : 500,
	        modal : false,
	        iframe : 'uploadBatchDetial',
	        url : url,
	        buttons:[
	            {	name:"${getHtmlText('common.button.close')}",
	                handler:function(){this.close();}
	            }]
	    }).show();
	}
	function deploy_PageInitMethod(){
		$('.configtable').hide();
	}
	/**
	 * 上载批量记录查询
	 * @method uploadQuery
	 */
	function uploadBatchQuery(type,pageNo,sortFlag){
		var url = "/msService/ec/module/uploadInfoBatch/list-query?a=1";
		var dataPost="";
		var staffId = CUI('#uploadBatch_queryForm input[name="staff.id"]').val();
		var staffName = $('#uploadBatch_queryForm').find('input[name="staff.name"]').val();
		var	moduleName = CUI('#uploadBatch_queryForm #module_name').val();
		var pageSize=CUI('input[name="ec_module_uploadBatch_datatable_PageLink_PageCount"]').val();
		if(null != pageSize && undefined != pageSize && "" != pageSize) {
			dataPost += "&page.pageSize="+encodeURIComponent(pageSize);
		}
		if(null != pageNo && undefined != pageNo && "" != pageNo) {
			dataPost += "&page.pageNo="+encodeURIComponent(pageNo);	
		}
		if(null != staffId && undefined != staffId && "" != staffId) {
			dataPost += "&uploadInfoBatch.uploadStaff.id=" + staffId;
		}else if(null != staffName && undefined != staffName && "" != staffName){
			dataPost += "&uploadInfoBatch.uploadStaff.name=" + staffName;
		}
		if(null != moduleName && undefined != moduleName && "" != moduleName) {
			dataPost += "&uploadInfoBatch.moduleName=" + moduleName;
		}
		ec_module_uploadBatch_datatableWidget.setRequestDataUrl(url,dataPost);
	};
	/**
	 * 上载批量记录页面渲染
	 * @method uploadQuery
	 */
	function uploadBatch_renderOverEvent(){
		$('#ec_module_uploadBatch_datatable #ec_module_uploadBatch_datatable_tbody tr').each(function(index,obj){
		    var target = $(obj); 
		    var id = target.find('input[name="id"]').val();
		    var url = "/msService/ec/module/uploadInfDetial?id="+id;
		    //var href = '<a href='+url+'>查看详细</a>'
		    var href = '<a href="#" onclick="openUploadDetial('+id+');">${getText("foundation.synchronize.viewLog")}</a>';
		    target.find('td[key="uploada"] div').html(href);
		})
		$.each($('#ec_module_uploadBatch_datatable_tbody tr'),function(index,data){
		    var state = $(data).find("td[key=uploadState]").text();
		    if(null != state && undefined != state && "" != state && '失败'==state){
		        $(data).find("td[key=uploadState]").css('color','#ff0000');
		    }
		    }
		)
	}
	var openUploadDetial = function(id){
	    var url = "/msService/ec/module/uploadInfDetial?uploadBatchId="+id;
	    new CUI.Dialog({
	        title : "${getHtmlText('ec.module.uploada')}",
	        width : 800,
	        height : 500,
	        modal : false,
	        iframe : 'uploadBatchDetial',
	        url : url,
	        buttons:[
	            {	name:"${getHtmlText('common.button.close')}",
	                handler:function(){this.close();}
	            }]
	    }).show();
	}
	function uploadBatch_PageInitMethod(){
		$('#ec_module_history_edit_main_div #deploy').trigger('click');
	}
</script>