<div id="export-config-buttons">


<div class="paginatorbar-operatebar">
    <a id="export-action-add" onclick="ec.model.exportObj.save()">
        <span id="ec_view_btn_add" processkey="" class="buttonbar-button cui-btn-save">保存</span>
    </a>
    <#if isProj?string('true', 'false')=="true">
    <a id="export-action-add" onclick="ec.model.exportObj.restore()">
        <span id="ec_view_btn_copy" processkey="" class="buttonbar-button cui-btn-restore">还原</span>
    </a>
    </#if>
    <#if isProj?string('true', 'false')=="false">
    <a id="export-action-submit" onclick="ec.model.exportObj.publish()">
        <span id="ec_view_btn_mod" processkey="" class="buttonbar-button cui-btn-publish">发布</span>
    </a>
    </#if>

</div>

</div>
<div id="export-config-field-list">
	<!-- 左侧 -->
	<div id="fast_select_elements">
		<h2 class="current">
			<span i18n="ec.view.entityattribute">所有字段</span>
		</h2>
		<div class="accordion_pane">
			<ul class="main_properties_container" id="export-config-field-list-container">
                <#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
                    <#assign properties = (subs.properties)>
                    <#list properties as p>
                        <#if p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT">
                        <#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec'>
                        <li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='listec.addExportField(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}'    columnName='${(p.columnName)!}'    propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'   seniorSystemCode='${(p.seniorSystemCode!false)?string('true','false')}'   showWidth='${p.showWidth!}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'  isCustom='${(p.isCustom!false)?string('true','false')}'>
                            ${getHtmlText('${(p.displayName)!}')}
                        </li>
                        </#if>	
                        </#if>
                    </#list>
                </#if>
                <#if subs?? && (subs.associatedInfos)??>
                    <#assign associatedInfos = (subs.associatedInfos)>
                    <#assign i = 1>
                    <#list associatedInfos as ass>
                        <#if ass.originalProperty?? && ass.originalProperty.name != 'status'>
                        <li source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}'  multable='${(ass.targetProperty.multable!false)?string('true','false')}'   seniorSystemCode='${(ass.targetProperty.seniorSystemCode!false)?string('true','false')}'  entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}' nullable='${(ass.originalProperty.nullable!false)?string('true','false')}' isCustom='${(ass.originalProperty.isCustom!false)?string('true','false')}'>
                            <img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showExportAssPropertyNew(this,"${(ass.targetProperty.model.code)!}")'></img>
                            ${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
                        </li>	
                        </#if>
                        <#assign i = i+1>
                    </#list>
                </#if>
            </ul>
		</div>
	</div>
</div>
<!-- end left region -->
<div id="export-config-field-enabled-wrap">
	<div id="export-config-field-enabled">
		<h2 class="current">
			<span i18n="ec.view.entityattribute">已启用字段</span>
		</h2>
		<#if isSysbase?string('true', 'false')=="false" >
        <div id="checkAll">
            <input type="checkbox" name="isCheckAll" id="isCheckAll" onclick="ec.model.exportObj.checkAllItemOfEntity('${(model.code)!''}');">
            <span i18n="export.page.exportAllProps">导入模型所有属性(包括关联模型主键)</span>
        </div>
        </#if>
		<div id="export-config-field-enabled-content">
			<div id="content-fluid-table">
                <table cellpadding="0" id="fastColTable" class="infoTable">
                    <tbody id="fastColOrder">
                        <#if mapDatas?? >
                            <#list mapDatas as element>
                                <#assign cellCode = element.cellCode!ecCodeInit()>
                                <#if element??>
                                    <#if (element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
                                    
                                    <#else>
                                    <tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" partDepend="${(element.partDepend)!'common'}" selfType="${(element.selfType)!(element.columnType)!''}"  columnName="${(element.name)!}" showWidth="${(element.showWidth)!}" key="${(element.name)!}" ondblclick="listec.selectRow('fast',this);listec.setFieldDisplayName(this);" onmousedown="listec.selectRow('fast',this);" defaultValue="${(element.defaultValue!)?string}" propDefaultValue="${(element.defaultValue!)?string}" defaultValueHasChanged="${(element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(element.partDepend)!'common'}"     propshowformat="${(element.propshowformat)!''}"    columntype="${(element.columntype)!''}" name="${(element.name)!}" namekey="${(element.namekey)!}" key="${(element.key)!}" mnecode="${(element.mnecode!false)?string('true','false')}"   multable="${(element.multable!'')}"    propPrecision="${(element.propPrecision!'')}"  seniorsystemcode="${(element.seniorsystemcode!'')}"     iscrosscompany='${(element.iscrosscompany!false)?string('true','false')}'   isCustom="${(element.isCustom)!}"   isrefselect='${(element.isrefselect!false)?string('true','false')}' propertyCode="${(element.propertyCode)!}" layRec="${(element.layRec)!}" nullable="${(element.nullable)!}" isreadonly="${(element.readonly)!}" exp="${(element.exp)!}" entityCode="${(element.entityCode)!}" layRec="${(element.layRec)!}" assTar="${(element.assTar)!}" assOrg="${(element.assOrg)!}"  columnLong="${(element.columnLong)!}" readonly="${(element.readonly)!}" checkname="${ckname!}" showType="${(element.showType)!}"   sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${((element.callbackbody)!)?html}" callbackname="${((element.callbackname)!)?html}" funcname="${((element.funcname)!)?html}" funcbody="${((element.funcbody)!)?html}" selectCompType="${(element.selectCompType)!}" cssstyle="${((element.cssstyle)!"")?html}" referenceview="${(element.referenceview)!}" modelcode="${(element.modelcode)!}" columntype="${(element.columntype)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
                                        showFormat="${(element.showFormat)!}" assPropertyName="${element.assPropertyName!}" <#if (element.refCondition)?has_content>refCondition="${(element.refCondition)?html}"</#if> modelDataType="simple" containLower="false"  fill='{<#if (element.fill)?has_content><#list (element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.fill.fillType)?has_content && (element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.fill.fillOrder?has_content><#list element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.fill.fillContent)[ne])?html}"</#list><#else><#list (element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.fill.fillContent)[ne])?html}"</#list></#if>}<#else>"${((element.fill)[fe])?html}"</#if></#list></#if>}'>
                                    </#if>
                                    <#if element.namekey??&&element.namekey!=""&&element.namekey?index_of(",")!=-1 >
                                    <#assign  finalString="" />
                                    <#list element.namekey?split(",") as item >
                                            <#assign  finalString=finalString+getHtmlText(item!'')+"."  />
                                        </#list>
                                        <td class="field-name<#if (element.nullable!false)?string=='false'> field-nullable</#if>">${finalString?substring(0,finalString?length-1)!''}</td>
                                    <#else>
                                        <#-- <td align="center" style="width:90%;">${getHtmlText('${(element.namekey)!element.name}')}</td> -->
                                        <td class="field-name <#if (element.nullable!false)?string=='false'> field-nullable</#if>">${getHtmlText('${(element.namekey)!""}')}</td>
                                    </#if>
                                        <td class="field-action" onclick="listec.deleteFastQueryField(this)"  del_td="true"><img title="点击删除该字段" style="cursor:pointer;" src="/bap/static/ec/images/importTemplate/icon_delete.png" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)'></img></td>
                                    </tr>
                                </#if>
                            </#list>
                        </#if>
                    </tbody>
                </table>
			</div>
			<div id="content-fixed-sort">
                <div id="fastContent">
                    <div class="ec-list-dis-topbtn" onclick="listec.firstRow('fast')" id="firstMove"></div>
                    <div class="ec-list-dis-prevbtn" onclick="listec.upRow('fast')" id="upMove"></div>
                    <div class="ec-list-dis-nextbtn" onclick="listec.downRow('fast')" id="downMove"></div>
                    <div class="ec-list-dis-lastbtn" onclick="listec.lastRow('fast')" id="lastMove"></div>
                </div>
            </div>
		</div>
	</div>
</div>
<!-- end center region -->