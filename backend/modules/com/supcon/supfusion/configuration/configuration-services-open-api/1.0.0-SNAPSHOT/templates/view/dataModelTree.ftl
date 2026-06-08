<!-- 左侧 -->
<style type="text/css">	
		#advTree_select_elements{height:98%}
		#advTree_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#advTree_select_elements h2.current {cursor:default;}
		#advTree_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#advTree_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#advTree_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}		

</style>
<div id="advTree_select_elements">
	<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
	<div class="accordion_pane" style="display:block;overflow:auto;height:89%">
		<ul class="main_properties_container">
			<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
				<#assign properties = (subs.properties)>
				<#list properties as p>
					<#if p.type != "LONGTEXT" && p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT">
					<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT">
					<li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='listec.addAdvQueryField(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
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
					<li source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
						<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}","adv")'></img>
						${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
					</li>	
					</#if>
					<#assign i = i+1>
				</#list>
			</#if>
		</ul>
	</div>
	<h2>${getHtmlText('ec.view.one2manyattr')}</h2>
	<div class="accordion_pane" style="overflow:auto;height:87%"><ul class="onetomany_properties_container">
		<#if subs?? && (subs.oneToManyAssociatedInfos)?? && (subs.oneToManyAssociatedInfos)?size &gt; 0>
			<#assign oneToManyAssociatedInfos = (subs.oneToManyAssociatedInfos)>
			<#assign j = 1>
				<#list oneToManyAssociatedInfos as onetoManyAss>
					<#assign one2ManyOriginalTableName = (onetoManyAss.targetProperty.model.tableName)!>
					<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name)>
					<#if one2ManyOriginalTableName == 'WF_DEAL_INFO'>
						<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_DI')>
						<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Di')>
					<#elseif one2ManyOriginalTableName == 'WF_PAY_CLOSE_ATTENTION'>
						<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_PA')>
						<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Pa')>
					<#elseif one2ManyOriginalTableName == 'WF_SUPERVISION'>
						<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_SV')>
						<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Sv')>
					</#if>
					<li source='test' parentModelDataType='<#if (onetoManyAss.targetProperty.model.dataType)?? && (onetoManyAss.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='one2many' assTar='${onetoManyAss.targetProperty.code}' propertyCode="${onetoManyAss.targetProperty.code!}" assOrg="${onetoManyAss.originalProperty.code}" dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(one2ManyTargetPropertyName)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' relativeName='${(one2ManyOriginalTableName)!},${(onetoManyAss.targetProperty.columnName)!},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)}'>
						<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}","adv")'></img>
						${getHtmlText('${(onetoManyAss.targetProperty.displayName)!}')}[${getHtmlText('${(onetoManyAss.targetProperty.model.name)!}')}]
					</li>	
					<#assign j = j+1>
			</#list>
		 </#if>
	</ul></div>
</div>	
<script type="text/javascript">
		$(function(){
			$('#advTree_select_elements').tabs("#adv_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
		});
</script>	