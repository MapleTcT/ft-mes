<@errorbar id="proxyPendIngDialogErrorBar" offsetY=50 />
<form errorBarId="proxyPendIngDialogErrorBar" id="SubmitForm" onsubmit="return false;" style="padding-top:15px;">
	<@s.hidden name="pendingId" id="pendingId" />
	<@s.hidden name="pendingIds" id="pendingIds" />
	<@s.hidden name="proxyUsers" id="proxyUsersInput" />
	<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">
		<div class="row">
			<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
				<label class="label-wd">
					${getText('ec.proxyPending.proxyType')}
				</label>
			</div>
	
			<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
				<#if !(isCountersign?? && isCountersign)>
					<input type="radio" id="proxyType" class="cui-radio" name="proxyType" value="2" checked="true"></input>${getHtmlText('ec.proxyPending.copyType')}&nbsp;&nbsp;&nbsp;&nbsp;
				</#if>
					<input type="radio" name="proxyType" id="proxyType1" class="cui-radio" onkeydown="if(event.keyCode==13) return false" value="3" />${getHtmlText('ec.proxyPending.allType')}
			</div>
		</div>
		<div class="row">
			<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
				<label class="label-wd">
					${getText('ec.proxyPending.proxySources')}
				</label>
			</div>
	
			<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
				<@mneclient reftitle="${getText('ec.edit.refStaff')}" mneTip="${getText('ec.expectedConsign.consinger')}" isCrossCompany=true isWrap=true multiDivStyle="height:20px;overflow:hidden;" conditionfunc="proxyUsers_querycustomFunc()"  name="proxyUsers_" id="proxyUsers_" type="User" url="/msService/ec/foundation/user/common/userListFrameset.action" displayFieldName="staffname"  ids="" names=""  onkeyupfuncname="getproxyUsers_MultiInfo()" funcparam="crossCompanyFlag=true&multiSelect=true" clicked=true multiple=true mnewidth=260 formId="SubmitForm" isEdit=true classStyle="form-control" />
			</div>
		</div>
		<div class="row">
			<div class="col-xs-12 col-sm-3 col-md-4  col-lg-4">
				<label class="label-wd">
					${getText('ec.proxyPending.description')}
				</label>
			</div>
	
			<div class="col-xs-12 col-sm-9 col-md-8 col-lg-8 margin-bottom-5">
				<@ec_textarea name="proxDesc" cssClass="form-control" />
			</div>
		</div>
	</div>
</form>

