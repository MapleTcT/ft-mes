<style type="text/css">
  .ec-config-page .entity-set-table td{
	padding: 4px 14px 4px 0;
  }
  .entity-set-table td.lab{
    white-space:nowrap;
  }
  #MneDiv{
    margin-left:-2px;
  }
  .dg-hd-td-label input[type='checkbox'],.dg-bd-tr-td-div .dt_checkbox{
    position: absolute!important;
    top: 0!important;
    left: 0;
    bottom: 0;
    right: 0;
    margin: auto!important;
  }
</style>
<@frameset id="ec_wf_manage">
	<@frame id="ec_wf_main" region="center" offsetH=4>
	<input type="hidden" id="wfEntityCode" name="wfEntityCode" value="${entityCode!}" />
	<input type="hidden" id="moduleCode" name="moduleCode" value="${moduleCode!}"/>
	<input type="hidden" name="flowId" id="flowId"/>
	<input type="hidden" name="wfDeploymentId" id="wfDeploymentId"/>
	<input type="hidden" name="wfFlowVersion" id="wfFlowVersion"  <#if wfFlowVersion??>value="${wfFlowVersion}" <#else>value="0"</#if>/>
	<input type="hidden" name="wfVersion" id="wfVersion" <#if wfVersion??>value="${wfVersion}" <#else>value="0"</#if> />
	<input type="hidden" name="flowEditFlag" id="flowEditFlag"/>
	<input type="hidden" name="flowEditFlag" id="flowEditFlag"   value="${flowEditFlag!}"/>
	<input type="hidden" name="entityMobile" id="entityMobile" value="${entityMobile!}"/>
	<input type="hidden" name="showHistoryVersion" id="showHistoryVersion" value="false" />
		<@errorbar id="ec_wf_tableErrorBar" />
		<@datatable dtPage="deployments" hidekey="['id','menuInfoId','deploymentId','version','flowEditFlag','name','requiredTime','mobileinitiate','mobileapprove','allowInvalid','graduallyReject','signatureEnable','recallAble','recallRemainTime','mainViewViewCode','crossCompanyFlag']"  id="ec_wf_table" dataUrl="/msService/ec/entity/wf-list?entity.code=${entityCode}&showHistoryVersion=false" dblclick="ec.wf.dbclickFlowFunction">
			<@operatebar operates="code:ec_wf_btn_add||name:${getHtmlText('ec.entity.wf.new')}||iconcls:add||onclick:ec.wf.manager('add');
			code:ec_wf_btn_edit||name:${getHtmlText('ec.entity.wf.edit')}||iconcls:edit||onclick:ec.wf.manager('modify');
			code:ec_wf_btn_config||name:${getHtmlText('ec.view.config')}||iconcls:config||onclick:ec.wf.manager('config');
			code:ec_wf_btn_del||name:${getHtmlText('ec.entity.wf.del')}||iconcls:del||onclick:ec.wf.manager('del');
			code:ec_wf_btn_setCurrentVersion||name:${getHtmlText('ec.entity.wf.setCurrentVersion')}||iconcls:eighteen-dt-set-current-version||onclick:ec.wf.setToCurrentVersion();
			code:ec_wf_btn_showHistoryVersion||name:${getHtmlText('ec.entity.wf.showHistoryVersion')}||iconcls:eighteen-dt-display-histor-version||onclick:ec.wf.showHistoryVersion()" operateType="noPower" resultType="json">
			</@operatebar>
			<@datacolumn textalign="center" label="" checkall="true"  key="checkbox" type="checkbox" width=60  />
			<@datacolumn key="processKey" label="Key" width=100/>
			<@datacolumn key="processVersion" textalign="right" label="${getHtmlText('ec.entity.wf.version')}" width=50/>
			<@datacolumn key="nameInternational"  label="${getHtmlText('ec.entity.wf.name')}" width=100/>
			<@datacolumn textalign="center" key="publishFlag" type="boolean" label="${getHtmlText('ec.entity.wf.published')}"  width=80 />
			<@datacolumn textalign="center" key="isCurrentVersion" type="boolean" label="${getHtmlText('ec.entity.wf.isCurrentVersion')}"  width=80 />
			<@datacolumn key="createStaff.name" label="${getHtmlText('ec.entity.wf.creator')}"  width=100/>
			<@datacolumn key="createTime" textalign="center" label="${getHtmlText('ec.entity.wf.createTime')}" type="datetime" width=150/>
			<@datacolumn key="modifyTime" textalign="center" label="${getHtmlText('ec.entity.wf.modifyTime')}"  type="datetime"  width=150/>
			<@datacolumn key="publishTime" textalign="center" label="${getHtmlText('ec.entity.wf.publishTime')}"  type="datetime"  width=150/>
			<@datacolumn key="description" label="${getHtmlText('ec.entity.wf.desc')}" width=200/>
		</@datatable>
		<div id="wf_deployer_form_new_form" style="display:none;">
		<div id="formDialogErrorBar"></div>
		<form>
			<table class="cui-fd-infotable entity-set-table" cellpadding="0" cellspacing="0" width="95%">
				<tr>
					<td width="10%" class="lab" style="color:#B30303">${getHtmlText('ec.entity.wf.code')}</td>
					<td width="30%">
					<div class="fix-input"><input type="text" id="flowCode" name="flowCode" class="cui-noborder-input"/></div>
					</td>
					<td width="20%" class="lab" style="color:#B30303">
					${getHtmlText('ec.entity.wf.name')}
					</td>
					<td width="30%">
					
					<@international name="workflow.name" isNew=true isOldEdit=true moduleCode="${artifact!}" key="" cssClass="cui-edit-field"></@international>
					<!--
					<input type="text" id="flowName" name="flowName" class="cui-edit-field" style="width:98%" />
					-->
					</td>
					
				</tr>
				<tr>
					<td class="lab" style="color:#B30303">${getHtmlText('ec.entity.wf.menu')}</td>
					<td>
					<select id="flowMenuId" name="flowMenuId" class="cui-edit-field">
					<option value=""></option>
					<#if menuList?? && menuList?size gt 0>
					<#list menuList as ml>
					<option value="${(ml.id)!}">${(ml.name)!}</option>
					</#list>
					</#if>
					</select>
					<td class="lab">${getHtmlText('ec.entity.wf.requiredTime')}</td>
					<td>
					<input id="requiredTime" name="requiredTime" type="text" maxlength="10" style="width:75%" class="cui-edit-field"/>${getHtmlText('ec.entity.wf.hour')}
					</td>
				</tr>
				<tr>
					<td class="lab" >${getHtmlText('ec.view.wf.supervision')}</td>
					<td colspan="3">	
					<input type="hidden" name="superviseIds" <#if superviseIds?? >value="${superviseIds}"</#if> id="superviseIds">
					<@mneclient json=true display_key="'id','code','name','deptname'"  name="superviseNames" url="other" isCrossCompany=true onkeyupfuncname="getsuperviseNamesMultiInfo()" id="superviseNames"  displayFieldName="name"  funcparam="unassignUserSupport=true&systemAdminFlag=true&multiSelect=true" classStyle="cui-noborder-input" type="Staff" cssStyle="width:97%;float:left;"  multiple=true clicked=true isEdit=true/>
					</td>
				</tr>
				<tr>
					<!--
					<td class="lab"><input type="checkbox" id="mobilequery" name="mobilequery" <#if entityMobile?? && entityMobile=='false'>disabled="true"</#if> /></td><td>${getHtmlText('ec.entity.wf.mobilequery')}</td>
					-->
					<td class="lab"><input type="checkbox" id="mobileinitiate" name="mobileinitiate" <#if entityMobile?? && entityMobile=='false'>disabled="true"</#if>/></td><td>${getHtmlText('ec.entity.wf.mobileinitiate')}</td>
					<td class="lab"><input type="checkbox" id="mobileapprove" name="mobileapprove" /></td><td>${getHtmlText('ec.entity.wf.mobileapprove')}</td>
				</tr>
				<tr>
					<td class="lab"><input type="checkbox" id="allowInvalid" name="allowInvalid" /></td><td>${getHtmlText('foundation.ec.wf.allowInvalid')}</td>
					<td class="lab"><input type="checkbox" id="graduallyReject" name="graduallyReject" /></td><td>${getHtmlText('foundation.ec.wf.graduallyReject')}</td>
				</tr>
				<tr>
					<td class="lab"><input type="checkbox" id="recallAble" name="recallAble" onclick="ec.wf.recallCheck()"/></td><td>${getHtmlText('ec.entity.wf.recallAble')}</td>
					<td class="lab" name="recallRemainTimeTd">${getHtmlText('ec.entity.wf.recallRemainTime')}</td>
					<td name="recallRemainTimeTd"><input id="recallRemainTime" name="recallRemainTime" type="text" maxlength="10" style="width:75%" class="cui-edit-field"/>${getHtmlText('ec.entity.wf.second')}</td>
				</tr>
				<tr>
                    <td class="lab"><input type="checkbox" id="crossCompanyFlag" name="crossCompanyFlag" /></td><td>${getHtmlText('ec.entity.crossCompanyFlag')}</td>
                </tr>
				<tr>
					<td class="lab">${getHtmlText('ec.view.property.view')}</td>
					<td>
					<select id="mainViewViewCode" name="mainViewViewCode" class="cui-edit-field">
					<option value=""></option>
					<#if views?? && views?size gt 0>
					<#list views as v>
					<option value="${(v.code)!}">${getText(v.displayName)!}</option>
					</#list>
					</#if>
					</select>
					<#-- 添加电子签名选择框 -->
					<td class="lab"><input type="checkbox" id="signature" name="signature"/></td><td>${getHtmlText('ec.entity.wf.signatureEnabled')}</td>
				</tr>
				<tr>
					<td class="lab" style="vertical-align:top;">${getHtmlText('ec.entity.wf.descript')}</td>
					<td colspan="3"><textarea style="width:97%;height:50px;" class="cui-edit-textarea" id="des" name="des"></textarea></td>
				</tr>
			</table>
		</form>	
		</div>
	</@frame>
</@frameset>
<script type="text/javascript">
	var ec_entity_edit_formDialogErrorBarWidget = null;
	YAHOO.util.Event.onDOMReady(function() {
		ec_entity_edit_formDialogErrorBarWidget = new CUI.ErrorBar('formDialogErrorBar','38');
	});
</script>
<script type="text/javascript">
	(function($){
		CUI.ns("ec.wf");
		ec.wf.dbclickFlowFunction=function(){
			//双击进入流程组态页面
			ec.wf.manager('config');
		}
		ec.wf.manager=function(mode){
			$("#superviseNamesMultiIDs").val('');
			$("#superviseNamesDeleteIds").val('');
			$("#superviseNamesAddIds").val('');
			$("#superviseNamesMultiIDsContainer").html("");
			var titleName="${getText('ec.flowActive.flowInfo')}";
			if(mode=='add'){
				$("#flowId").val("");
				$("#flowName").val("");
				$("#wfFlowVersion").val("");
				$("#flowCode").val("");
				$("#des").val("");
				$("#wfDeploymentId").val("");
				$("#wfVersion").val("");
				$("#flowCode").removeAttr("readOnly");
				$("#flowEditFlag").val("");
				$("#requiredTime").val("");
				//$("#mobilequery").val("");
				$("#mobileinitiate").val("");
				$("#mobileapprove").prop('checked',true);
				$("#flowMenuId").val("");
				<#if recallAble ?? >
					$("#recallAble").prop('checked',true);
					$("#recallRemainTime").val(${recallRemainTime!});
					$('[name="recallRemainTimeTd"]').show();
				<#else>
					$("#recallAble").prop('checked',false);
					$("#recallRemainTime").val(${recallRemainTime!});
					$('[name="recallRemainTimeTd"]').hide();
				</#if>
				$("#mainViewViewCode").val("");
				$('#allowInvalid').prop('checked',true);
				$('#graduallyReject').removeAttr("checked");
				$('#crossCompanyFlag').removeAttr("checked");
				$("#international_workflowname").val("");
				$("#international_workflowname_showName").val("");
				$("#signature").prop('checked',false);
			}else if(mode=='modify'){
			    //未选中行或选中了不止一条时显示提示信息
			    if(ec_wf_tableWidget==null||ec_wf_tableWidget.selectedRows==null||ec_wf_tableWidget.selectedRows.length!=1){
					CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choice')}");return;
				}
			    bindCurrentTrInfo()//绑定当前操作行
			}else if(mode=='config'){
			    //点击配置按钮
			    //未选中行或选中了不止一条时显示提示信息
			    if(ec_wf_tableWidget==null||ec_wf_tableWidget.selectedRows==null||ec_wf_tableWidget.selectedRows.length!=1){
					CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choice')}");return;
				}
				//未配置菜单直接打开工作流编辑页面
				var menuId=ec_wf_tableWidget.selectedRows[0].menuInfoId;
				if(menuId==null||menuId==""){
					ec.wf.manager('modify');//打开修改表单
                    //  CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choiceMenu')}",function(){
                    //     ec.wf.manager('modify');//打开修改表单
                    //  });
				     return ;
				}
				bindCurrentTrInfo();//绑定当前操作行
			    ec.wf.openFlowConfig();//打开流程配置頁面
			    return;
			}else{
				if(ec_wf_tableWidget==null||ec_wf_tableWidget.selectedRows==null||ec_wf_tableWidget.selectedRows.length==0){
					CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choice')}");return;
				}
				
				//if(!confirm("${getText('ec.entity.checkDeleteFlow')}")){
					//return false;
				//}
				CUI.Dialog.confirm("${getText('ec.entity.checkDeleteFlow')}", function(){
				   createLoadPanel(false, null,{head:"${getText('ec.common.dealing')}", show: true, opacity:(50), bgColor:("#666666")});
					var ids="";
					var rows=ec_wf_tableWidget.selectedRows;
					for(var i=0;i<rows.length;i++){
						ids+=","+rows[i].id;
					}
					if(ids!=""){
						ids=ids.substr(1);
					}
					CUI.ajax({
						type : 'post',
						url : '/msService/ec/workflow/delete',
						data : {deploymentIds : ids},
						success : function(msg){
							if(msg && msg.dealSuccessFlag == true){
							    closeLoadPanel();//关闭加载中
								CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
								ec.wf.refleshDataTable();
							}else {
								if(msg && msg.exceptionMsg){CUI.Dialog.alert(msg.exceptionMsg);}
								else{CUI.Dialog.alert("${getHtmlText('common.delete.failure')}");}
							}
							
						}
					});
				});
			    return ;
			}
			ec.wf.editFlowDialog=new CUI.Dialog({
				title : titleName,
				width : 650,
				height : 360,
				modal:true,
				close:true,
				elementId : "wf_deployer_form_new_form",
				buttons:[
				    {	name:"${getText('ec.flow.saveAndConfig')}",
						handler:function(){ec.wf.saveAndConfig();}
					},
					{	name:"${getText('ec.flow.save')}",
						handler:function(){ec.wf.submit();}
					},
					{	name:"${getText('common.button.cancel')}",
						handler:function(){this.close();}
					}]
			})
			ec.wf.editFlowDialog.show();
		}
		//保存并配置流程
		ec.wf.saveAndConfig=function(){
		   ec.wf.submit(function(param){
		       ec.wf.openFlowConfig(param);//打开流程配置頁面
		   });
		}
		//保存、修改流程属性配置信息
		ec.wf.submit=function(callback){
			if($("#flowName").val()==""){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.workflowName')}");
				return ;
			}
			if($("#international_workflowname_showName").val()==""){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.workflowName')}");
				return ;
			}
			if($("#flowCode").val()==""){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.workflowCode')}");
				return ;
			}
			
			if($("#flowMenuId").val()==""){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choiceMenu')}");
				return ;
			}
			var re= /^\d+$/;
			var re2=/^\d+(\.\d+)?$/;
			var re3=/^\+?[1-9][0-9]*$/;
			var retime=$("#requiredTime").val();
			if((!re.test(retime)&&!re2.test(retime)&&retime!="")||retime=="0"){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.requiredTimeMustNumber')}");
				return false;
			}
			var recallRemainTime = $('#recallRemainTime').val()
			if((!re.test(recallRemainTime)&&!re2.test(recallRemainTime)&&recallRemainTime!="")||recallRemainTime=="0" || !re3.test(recallRemainTime)){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.recallRemainTimeMustNumber')}");
				return false;
			}
			//验证改processkey是否在其他实体中存在
			var processKey = $.trim($("#flowCode").val());
			var entityCode = $("#wfEntityCode").val();
			var params = "processKey="+processKey+"&entityCode="+entityCode;
			if($('#wfDeploymentId').val() != ""){
				params += "&deploymentId=" + $('#wfDeploymentId').val();
			}
			CUI.ajax({
				type : 'post',
				url : '/msService/ec/workflow/repeat',
				data : params,
				success : function(msg){
					if(msg && msg.dealSuccessFlag == false){
						CUI.Dialog.alert(msg.errorMsg);
						return false;
					}else{
						ec.wf.saveFlow(callback);//保存流程设置
					}
				}
			});
			
		}
	
		ec.wf.setnow=function(){
			if(ec_wf_tableWidget.selectedRows.length == 0){
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choice')}");return;
			}
			var key = ec_wf_tableWidget.selectedRows[0].key;
			var version = ec_wf_tableWidget.selectedRows[0].processVersion;
			CUI.ajax({
				type : 'GET',
				url : '/msService/ec/workflow/process-set-to-be-current',
				data : {key : key,version:version},
				success : function(msg){
					if(msg && msg.success == true){
						ec_wf_tableWidget.setRequestDataUrl('/msService/ec/workflow/process-list?entityCode=${entityCode!}');
					}else if(msg && msg.success == false){CUI.Dialog.alert(msg.exceptionMsg);}
					else{CUI.Dialog.alert("${getHtmlText('ec.entity.workflow.failure')}");}
				}
			});
		}
		ec.wf.refleshDataTable=function(){
			var url='/msService/ec/entity/wf-list?entity.code=${entityCode}&showHistoryVersion=' + $("#showHistoryVersion").val();
			datatable_ec_wf_table.setRequestDataUrl(url);
		}
		ec.wf.recallCheck=function(){
			if($('#recallAble').prop('checked')==true){
				$('[name="recallRemainTimeTd"]').show();
			}else{
				$('[name="recallRemainTimeTd"]').hide();
			}
		}
		
		ec.wf.setToCurrentVersion = function(){
			if (ec_wf_tableWidget.getSelectedRow().length < 1) {
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.choice')}");
				return;
			} else if (ec_wf_tableWidget.getSelectedRow().length > 1) {
				CUI.Dialog.alert("${getHtmlText('ec.entity.wf.checkSetCurrentVersion')}");
				return;
			} else {
				if (ec_wf_tableWidget.getSelectedRow()[0].isCurrentVersion == true) {
					CUI.Dialog.alert("${getHtmlText('ec.entity.wf.alreadyCurrentVersion')}");
					return;
				}
				if(ec_wf_tableWidget.getSelectedRow()[0].processVersion == 0){
					CUI.Dialog.alert("${getHtmlText('ec.entity.wf.checkIsPublish')}");
					return;
				}
				CUI.Dialog.confirm("${getHtmlText('ec.entity.wf.setCurrentVersionHint')}", function(){
					$.post("/msService/ec/workflow/setToCurrentVersion",
						{ deploymentId : ec_wf_tableWidget.getSelectedRow()[0].id }, function(res){
						if (res.dealSuccessFlag == true) {
							ec_wf_tableErrorBarWidget.showMessage("${getHtmlText('ec.entity.wf.setSuccess')}" , "s");
							ec.wf.refleshDataTable();
						} else {
							ec_wf_tableErrorBarWidget.showMessage("${getHtmlText('ec.entity.workflow.failure')}" , "f");
						}
					});
				});
			}
		}
		
		ec.wf.showHistoryVersion = function(){
			if (String($("#showHistoryVersion").val()) == 'true') {
				$("#showHistoryVersion").val(false);
				$("#ec_wf_btn_showHistoryVersion").text("${getText('ec.entity.wf.showHistoryVersion')}");
			} else {
				$("#showHistoryVersion").val(true);
				$("#ec_wf_btn_showHistoryVersion").text("${getText('ec.entity.wf.hideHistoryVersion')}");
			}
			ec_wf_tableWidget.setRequestDataUrl("/msService/ec/entity/wf-list?entity.code=${entityCode}&showHistoryVersion=" + $("#showHistoryVersion").val());
		}
		//绑定当前操作行信息
		function bindCurrentTrInfo(){
		    $('#wf_deployer_form_new_form input:checkbox').prop('checked', false);
			$('#wf_deployer_form_new_form input:text').val('');
			var id=ec_wf_tableWidget.selectedRows[0].id;
			var flowName=ec_wf_tableWidget.selectedRows[0].name;
			var nameInternational=ec_wf_tableWidget.selectedRows[0].nameInternational;
			var key=ec_wf_tableWidget.selectedRows[0].nameInternational;
			var flowCode=ec_wf_tableWidget.selectedRows[0].processKey;
			var des=ec_wf_tableWidget.selectedRows[0].description;
			var flowVersion=ec_wf_tableWidget.selectedRows[0].processVersion;
			var menuId=ec_wf_tableWidget.selectedRows[0].menuInfoId;
			var version =ec_wf_tableWidget.selectedRows[0].version;
			var flowEditFlag=ec_wf_tableWidget.selectedRows[0].flowEditFlag;//是否可以修改发布的标志
			//特别注意这里的deploymentId=jbpm引擎中真正的deploymentId，不是ec下的deploymentId
			var flowId=ec_wf_tableWidget.selectedRows[0].deploymentId;
			var requiredTime=ec_wf_tableWidget.selectedRows[0].requiredTime;
			//var mobilequery=ec_wf_tableWidget.selectedRows[0].mobilequery;
			var mobileinitiate=ec_wf_tableWidget.selectedRows[0].mobileinitiate;
			var mobileapprove=ec_wf_tableWidget.selectedRows[0].mobileapprove;
			var allowInvalid=ec_wf_tableWidget.selectedRows[0].allowInvalid;
			var graduallyReject=ec_wf_tableWidget.selectedRows[0].graduallyReject;
			var signatureEnable=ec_wf_tableWidget.selectedRows[0].signatureEnable;
			var recallAble=ec_wf_tableWidget.selectedRows[0] && ec_wf_tableWidget.selectedRows[0].recallAble;
			var crossCompanyFlag = ec_wf_tableWidget.selectedRows[0].crossCompanyFlag;
			console.log(ec_wf_tableWidget +"-------"+ recallAble)
			var recallRemainTime =ec_wf_tableWidget.selectedRows[0] &&  ec_wf_tableWidget.selectedRows[0].recallRemainTime; 
			if (recallRemainTime == null || recallRemainTime == '' ) {
				<#if recallRemainTime ?? >
					recallRemainTime = ${recallRemainTime};
				</#if>
				
			}
			var mainViewViewCode = ec_wf_tableWidget.selectedRows[0] && ec_wf_tableWidget.selectedRows[0].mainViewViewCode;
			if(flowId!=null){
				$("#flowId").val(flowId);
			}
			if(id!=null){
				$("#wfDeploymentId").val(id);
			}
			if(version!=null){
				$("#wfVersion").val(version);
			}
			if(flowName!=null){
				$("#flowName").val(flowName);
			}
			if(flowVersion!=null){
				$("#wfFlowVersion").val(flowVersion);
			}
			if(flowCode!=null){
				$("#flowCode").val(flowCode);
				$("#flowCode").prop("readOnly","true");
			}
			if(des!=null){
				$("#des").val(des);
			}else{
			    $("#des").val("");
			}
			if(menuId!=null){
				$("#flowMenuId").val(menuId);
			}
			$("#mainViewViewCode").val(mainViewViewCode);
			$("#requiredTime").val(requiredTime);
			if(flowEditFlag!=null){
				if(flowEditFlag==true||flowEditFlag=='true'){
					$("#flowEditFlag").val(1);
				}else{
					$("#flowEditFlag").val(0);
				}
				
			}
			var entityMobile = $('#entityMobile').val();
			<#--
			if(mobilequery==true && entityMobile=='true'){
				$('#mobilequery').prop('checked',true);
			}
			-->
			if(mobileinitiate==true && entityMobile=='true'){
				$('#mobileinitiate').prop('checked',true);
			}
			if(mobileapprove==true){
				$('#mobileapprove').prop('checked',true);
			}
			if(recallAble==true){
				$("#recallAble").prop('checked',true);
				$("#recallRemainTime").val(recallRemainTime);
				$('[name="recallRemainTimeTd"]').show();
			}else{
				$('[name="recallRemainTimeTd"]').hide();
			}
			if(signatureEnable==true){
				$("#signature").prop('checked',true);
			} else {
				$("#signature").prop('checked',false);
			}
			if(allowInvalid==true){
				$('#allowInvalid').prop('checked',true);
			}else{
				$("#allowInvalid").removeAttr("checked");
			}
			if(graduallyReject==true){
				$('#graduallyReject').prop('checked',true);
			}else{
				$("#graduallyReject").removeAttr("checked");
			}
			if(crossCompanyFlag==true){
                $("#crossCompanyFlag").prop('checked',true);
            } else {
                $("#crossCompanyFlag").prop('checked',false);
            }

			$("#international_workflowname").val(flowName);
			$("#international_workflowname_showName").val(nameInternational);
			var staffIds="";
			var staffHtml='';
			CUI.ajax({
				type : 'post',
				url : '/msService/ec/workflow/getSupervises',
				data : {deploymentId : id},
				async:false,//同步顺序执行
				success : function(res){
					if(res!=null){
						for(var i=0;i<res.length;i++){
							var staffObj=res[i];
							staffIds+=staffObj.id+",";
							staffHtml+='<span class="mne-select-span">'+staffObj.name+'<img src="/bap/static/ec/delete.gif" class="multi-mne-img" onmouseover="deleteBtnChange(this)" onmouseout="deleteBtnChange(this)" mneobjid="'+staffObj.id+'" onclick=" deletesuperviseNamesMulti(this)"></span>';
						}
						
						$("#superviseNamesMultiIDs").val(staffIds);
						$("#superviseNamesMultiIDsContainer").html(staffHtml);
					}
				}
			});
		}
		//打开流程图h5配置页面
		ec.wf.openFlowConfig=function(param){
		    if(!param) var param=getParameter();//获取参数信息
		    var url="/msService/ec/workflow/flowEditH5";//新h5页面
		    url+="?flowName="+param.flowName;
            url+="&namekey="+param.namekey;
			url+="&flowKey="+param.flowKey;
			url+="&flowVersion="+$("#wfFlowVersion").val();
			url+="&des="+param.des;
			url+="&entityCode="+param.entityCode;
			url+="&moduleCode="+param.moduleCode;
			url+="&flowId="+$("#flowId").val();
			url+="&deploymentId="+param.deploymentId;
			url+="&menuId="+param.menuId;
			url+="&version="+param.version;
			url+="&flowEditFlag="+param.flowEditFlag;
			url+="&superviseNamesMultiIDs="+param.superviseNamesMultiIDs;
			url+="&requiredTime="+param.requiredTime;
			url+="&mobileinitiate="+param.mobileinitiate;
			url+="&mobileapprove="+param.mobileapprove;
			url+="&allowInvalid="+param.allowInvalid;
			url+="&graduallyReject="+param.graduallyReject;
			url+="&recallAble="+param.recallAble;
			url+="&recallRemainTime="+param.recallRemainTime;
			url+="&signature="+param.signature;
			url+="&mainViewViewCode="+param.mainViewViewCode;
			url +="&env=ec";
			window.open(url);			
		}
		//保存流程配置信息
		ec.wf.saveFlow=function(callback){
		   var param=getParameter();//获取参数信息
		   CUI.ajax({
				type : 'post',
				url : '/msService/ec/workflow/flowSave',
				data : param,
				success : function(msg){
				    var xmlChild=msg.childNodes;
			        if(xmlChild[0]&&xmlChild[0].nodeName=="flow"){
			           ec_entity_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.dataclassific.submitsuccessful')}","s");
			           setTimeout(function(){
				           ec.wf.editFlowDialog.close();//关闭浮层描述
				           ec_wf_tableWidget.setRequestDataUrl("/msService/ec/entity/wf-list?entity.code=${entityCode}&showHistoryVersion=" + $("#showHistoryVersion").val());//重新请求数据
				           if(typeof callback == "function"){
				                if(!param.deploymentId||param.deploymentId==""){
					              param.deploymentId=xmlChild[0].getAttribute("deploymentId");//新建的工作流需回填deploymentId
					              param.version=xmlChild[0].getAttribute("version");//新建的工作流需回填version
					              param.flowVersion=xmlChild[0].getAttribute("processVersion");//新建的工作流需回填processVersion
					            } 
					           callback(param);
					       }  
				        },2000);
			           
			        }else{
			           ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitfailure')}","f");
			        }  
				},
				error:function(e){
				  console.log("保存失败，/ec/workflow/flowSave接口报错");
				}
			});
		}
		//获取参数信息
		function getParameter(){
		   var param={};
		   var requiredTime=$("#requiredTime").val();
		   var staffIds=$("#superviseNamesMultiIDs").val();
		   param["deploymentId"]=$("#wfDeploymentId").val();//流程id
		   param["version"]=$("#wfVersion").val();//流程版本
		   param["flowKey"]=$.trim($("#flowCode").val());//流程编码
		   param["flowName"]=encodeURIComponent($.trim($("#international_workflowname_showName").val()));//流程名称
		   param["namekey"]=$("#international_workflowname").val();//流程名称国际化编码
		   param["menuId"]=$("#flowMenuId").val();//菜单id
		   param["requiredTime"]=requiredTime==""?0:requiredTime;//规定完成时间
		   param["superviseNamesMultiIDs"]=staffIds.substr(0,staffIds.length-1);//督办人
		   param["mobileinitiate"]=$('#mobileinitiate')[0].checked;;//移动客户端发起流程
		   param["mobileapprove"]=$('#mobileapprove')[0].checked;;//启用客户端审批
		   param["allowInvalid"]=$('#allowInvalid')[0].checked;//允许管理员作废
		   param["graduallyReject"]=$('#graduallyReject')[0].checked;//逐级驳回
		   param["recallAble"]=$('#recallAble')[0].checked;//可撤回
		   param["crossCompanyFlag"]=$('#crossCompanyFlag')[0].checked;//跨公司
		   param["recallRemainTime"]=$('#recallRemainTime').val();//撤回时效
		   param["mainViewViewCode"]=$('#mainViewViewCode').val();//主查看视图
		   param["signature"]=$('#signature')[0].checked;;//支持电子签名
		   param["des"]=$("#des").val();//描述
		   param["entityCode"]=$("#wfEntityCode").val();//实体编码
		   param["moduleCode"]=$("#moduleCode").val();//模块编码
		   param["flowEditFlag"]=$("#flowEditFlag").val();//是否可以修改发布的标志
		   return param;
		}
	})(jQuery);
	//刷新列表
	function reloadTableData(){
	   ec_wf_tableWidget.setRequestDataUrl("/msService/ec/entity/wf-list?entity.code=${entityCode}&showHistoryVersion=" + $("#showHistoryVersion").val());//重新请求数据
	}
</script>