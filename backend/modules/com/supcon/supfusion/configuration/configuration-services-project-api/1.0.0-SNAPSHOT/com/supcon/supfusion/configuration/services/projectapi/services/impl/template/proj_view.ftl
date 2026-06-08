<?xml version="1.0" encoding="UTF-8"?>
<entity>
	<projEvents>
		<#if eventList?? && eventList?size gt 0>
		<#list eventList as event>
		<event>
			<name><![CDATA[${event.name!}]]></name>
			<code><![CDATA[${(event.code)!}]]></code>
			<ecEnv><![CDATA[${(event.ecEnv)!}]]></ecEnv>
			<layoutCode><![CDATA[${(event.layoutCode)!}]]></layoutCode>
			<function><![CDATA[${(event.function)!}]]></function>
			<moduleCode>${(event.moduleCode)!}</moduleCode>
			<entityCode>${(event.entityCode)!}</entityCode>
			<projFlag>${(event.projFlag?string)!}</projFlag>
		</event>
		</#list>
		</#if>
	</projEvents>
	<customerConditions>
		<#if customerConditions?? && customerConditions?size gt 0>
		<#list customerConditions as customerCondition>
		<customerCondition>
			<code><![CDATA[${customerCondition.code!}]]></code>
			<ecEnv><![CDATA[${(customerCondition.ecEnv)!}]]></ecEnv>
			<sql><![CDATA[${customerCondition.sql!}]]></sql>
			<jsonCondition><![CDATA[${customerCondition.jsonCondition!}]]></jsonCondition>
			<#if customerCondition.view??>
			<view>
				<code><![CDATA[${customerCondition.view.code!}]]></code>
			</view>
			</#if>
			<#if customerCondition.dataGrid??>
			<dataGrid>
				<code><![CDATA[${customerCondition.dataGrid.code!}]]></code>
			</dataGrid>
			</#if>
			<#if customerCondition.dataClassific??>
			<dataClassific>
				<code><![CDATA[${customerCondition.dataClassific.code!}]]></code>
			</dataClassific>
			</#if>
			<moduleCode>${(customerCondition.moduleCode)!}</moduleCode>
			<entityCode>${(customerCondition.entityCode)!}</entityCode>
			<projFlag>${(customerCondition.projFlag?string)!}</projFlag>
		</customerCondition>
		</#list>
		</#if>
	</customerConditions>
	<views>
		<#if viewList?has_content>
		<#list viewList as view>
		<view>
			<code>${(view.code)!}</code>
			<ecEnv>${(view.ecEnv)!}</ecEnv>
			<name>${(view.name)!}</name>
			<title>${(view.title)!}</title>
			<displayName>${(view.displayName)!}</displayName>
			<type>${(view.type)!}</type>
			<openType>${(view.openType)!}</openType>
			<showType>${(view.showType)!}</showType>
			<isControl>${(view.isControl?string)!}</isControl>
			<isAudit>${(view.isAudit?string)!}</isAudit>
			<hasAttachment>${(view.hasAttachment?string)!}</hasAttachment>
			<dealInfoShow>${(view.dealInfoShow?string)!}</dealInfoShow>
			<enableSimpleDealInfo>${(view.enableSimpleDealInfo!false)?string}</enableSimpleDealInfo>
			<dealInfoGroup>${(view.dealInfoGroup)!}</dealInfoGroup>
			<usedForWorkFlow>${(view.usedForWorkFlow?string)!}</usedForWorkFlow>
			<onlyForQuery>${(view.onlyForQuery?string)!}</onlyForQuery>
			<type>${(view.type)!}</type>
			<mainView>${(view.mainView?string)!}</mainView>
			<description><![CDATA[${(view.description)!}]]></description>
			<url>${(view.url)!}</url>
			<customFlag>${(view.customFlag?string)!}</customFlag>
			<usedForTree>${(view.usedForTree?string)!}</usedForTree>
			<includeChildren>${(view.includeChildren?string)!}</includeChildren>
			<assTreeModelCode>${(view.assTreeModelCode)!}</assTreeModelCode>
			<assTreeLayRec>${(view.assTreeLayRec)!}</assTreeLayRec>
			<assTreePath>${(view.assTreePath)!}</assTreePath>
			<dialogType>${(view.dialogType)!}</dialogType> 
			<height>${(view.height)!}</height>
			<width>${(view.width)!}</width>
			<dataGridType>${(view.dataGridType)!}</dataGridType> 
			<layoutCode>${(view.layoutCode)!}</layoutCode>
			<isShadow>${(view.isShadow?string)!}</isShadow>
			<isSign>${(view.isSign?string)!}</isSign>
			<mobile>${(view.mobile?string)!}</mobile>
			<mobileEnableFlag>${(view.mobileEnableFlag?string)!}</mobileEnableFlag>
			<isHandSign>${(view.isHandSign?string)!}</isHandSign>
			<isReference>${(view.isReference?string)!}</isReference>
			<closePageAfterSave>${(view.closePageAfterSave?string)!}</closePageAfterSave>
			<isPrint>${(view.isPrint?string)!}</isPrint>
			<controlPrint>${(view.controlPrint?string)!}</controlPrint>
			<controlName>${(view.controlName?string)!}</controlName>
			<controlCode>${(view.controlCode?string)!}</controlCode>
			<controlSetingName>${(view.controlSetingName?string)!}</controlSetingName>
			<isBatchControlPrint>${(view.isBatchControlPrint!false)?string}</isBatchControlPrint>
			<batchControlPrintSelectView>
				<code>${(view.batchControlPrintSelectView.code)!}</code>
			</batchControlPrintSelectView>
			<isPermission>${(view.isPermission!false)?string}</isPermission>
			<permissionCode>${view.permissionCode!}</permissionCode>
			<moduleCode>${(view.moduleCode)!}</moduleCode>
			<retrialFlag>${(view.retrialFlag?string)!}</retrialFlag>
			<importFlag>${(view.importFlag?string)!}</importFlag>
			<editViewType>${(view.editViewType?string)!}</editViewType>
			<scriptCode>${(view.scriptCode)!}</scriptCode>
			<hasCustomSection>${((view.hasCustomSection)!false)?string}</hasCustomSection>
			<menuName>${(view.menuName?string)!}</menuName>
			<parentMenuCode>${(view.parentMenuCode?string)!}</parentMenuCode>
			<projFlag>${(view.projFlag?string)!}</projFlag>
			<inheritType>${(view.inheritType)!}</inheritType>
			<projEnabled>${(view.projEnabled?string)!}</projEnabled>
			<shadowView>
				<code>${(view.shadowView.code)!}</code>
			</shadowView>
			<reference>
				<code>${(view.reference.code)!}</code>
			</reference>
			<assModel>
				<code>${(view.assModel.code)!}</code>
			</assModel>
			<extraView>
				<#if !view.isShadow?? || !view.isShadow>
				<config>${((view.extraView.config)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</config>
				<viewJson>${((view.extraView.viewJson)!)}</viewJson>
				</#if>
			</extraView>
			<#if view.type=="EXTRA" || ((view.type=="EDIT" || view.type=="VIEW") && view.editViewType==1)>
			<fastQueryJsons>
				<#list view.fastQueryJson as fastQueryJson>
					<fastQueryJson>
						<code>${(fastQueryJson.code)!}</code>
						<queryConfig>${((fastQueryJson.queryConfig)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</queryConfig>
						<layoutName>${(fastQueryJson.layoutName)!}</layoutName>
						<targetModel>
							<code>${(fastQueryJson.targetModel.code)!}</code>
						</targetModel>
					</fastQueryJson>
				</#list>
			</fastQueryJsons>
			<#else>
			<fastQueryJson>
				<queryConfig><#if view.fastQueryJson?has_content><#list view.fastQueryJson as fastQueryJson>${(fastQueryJson!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</#list></#if></queryConfig>
			</fastQueryJson>
			</#if>
			<#if view.type=="EXTRA" || ((view.type=="EDIT" || view.type=="VIEW") && view.editViewType==1)>
			<advQueryJsons>
				<#list view.advQueryJson as advQueryJson>
					<advQueryJson>
						<code>${(advQueryJson.code)!}</code>
						<queryConfig>${((advQueryJson.queryConfig)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</queryConfig>
						<name>${(advQueryJson.name)!}</name>
						<layoutName>${(advQueryJson.layoutName)!}</layoutName>
						<targetModel>
							<code>${(advQueryJson.targetModel.code)!}</code>
						</targetModel>
					</advQueryJson>
				</#list>
			</advQueryJsons>
			<#else>
			<advQueryJson>
				<queryConfig><#if view.advQueryJson?has_content><#list view.advQueryJson as advQueryJson>${(advQueryJson!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</#list></#if></queryConfig>
			</advQueryJson>
			</#if>
			<#if (view.defaultAdvCond.content)??>
			<defaultAdvCond>
				<code><![CDATA[${(view.defaultAdvCond.code)!}]]></code>
				<ecEnv>${(view.defaultAdvCond.ecEnv)!}</ecEnv>
				<viewCode><![CDATA[${(view.defaultAdvCond.viewCode)!}]]></viewCode>
				<content><![CDATA[${(view.defaultAdvCond.content)!}]]></content>
			</defaultAdvCond>
			</#if>
			<#if dataGroupMap?? && dataGroupMap?size gt 0 && dataGroupMap[view.code]?? && dataGroupMap[view.code]?size gt 0>
			<dataGroups>
				<#list dataGroupMap[view.code] as dataGroup>
				<dataGroup>
					<code><![CDATA[${dataGroup.code!}]]></code>
					<ecEnv><![CDATA[${(dataGroup.ecEnv)!}]]></ecEnv>
					<name><![CDATA[${dataGroup.name!}]]></name>
					<displayName><![CDATA[${dataGroup.displayName!}]]></displayName>
					<isMultiple><![CDATA[${(dataGroup.isMultiple!false)?string}]]></isMultiple>
					<sort>${(dataGroup.sort)!}</sort>
					<layoutName>${(dataGroup.layoutName)!}</layoutName>
					<moduleCode>${(dataGroup.moduleCode)!}</moduleCode>
					<entityCode>${(dataGroup.entityCode)!}</entityCode>
					<targetModel>
						<code>${(dataGroup.targetModel.code)!}</code>
					</targetModel>
					<#if dataGroup.dataClassifics?? && dataGroup.dataClassifics?size gt 0>
					<dataClassifics>
						<#list dataGroup.dataClassifics as dataClassific>
						<dataClassific>
							<code><![CDATA[${dataClassific.code!}]]></code>
							<ecEnv><![CDATA[${(dataClassific.ecEnv)!}]]></ecEnv>
							<name><![CDATA[${dataClassific.name!}]]></name>
							<displayName><![CDATA[${dataClassific.displayName!}]]></displayName>
							<condition><![CDATA[${dataClassific.condition!}]]></condition>
							<sort>${(dataClassific.sort)!}</sort>
							<moduleCode>${(dataGroup.moduleCode)!}</moduleCode>
							<entityCode>${(dataGroup.entityCode)!}</entityCode>
							<isDefault><![CDATA[${(dataClassific.isDefault!false)?string}]]></isDefault>
						</dataClassific>
						</#list>
					</dataClassifics>
					</#if>
				</dataGroup>
				</#list>
			</dataGroups>
			</#if>
			<dataGrids>
				<#if view.dataGrids?has_content>
				<#list view.dataGrids as dataGrid>
				<dataGrid>
					<code>${(dataGrid.code)!}</code>
					<ecEnv>${(dataGrid.ecEnv)!}</ecEnv>
					<name>${(dataGrid.name)!}</name>
					<dataGridName>${(dataGrid.dataGridName)!}</dataGridName>
					<config>${((dataGrid.config)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}</config>
					<targetModel>
						<code>${(dataGrid.targetModel.code)!}</code>
					</targetModel>
					<orgProperty>
						<code>${(dataGrid.orgProperty.code)!}</code>
					</orgProperty>
					<ex>${(dataGrid.ex?string)!}</ex>
					<dataGridType>${(dataGrid.dataGridType)!}</dataGridType>
					<isPermission>${(dataGrid.isPermission!false)?string}</isPermission>
					<permissionCode>${(dataGrid.permissionCode)!}</permissionCode>
					<operateName>${(dataGrid.permissionCode)!}</operateName>
					<moduleCode>${(dataGrid.moduleCode)!}</moduleCode>
					<projFlag>${(dataGrid.projFlag?string)!}</projFlag>
					<entityCode>${(dataGrid.entityCode)!}</entityCode>
				</dataGrid>
				</#list>
				</#if>
			</dataGrids>
			<sqls>
				<#if view.sqls?has_content>
					<#list view.sqls as sql>
					<sql>
						<sql><![CDATA[${(sql.sql)!}]]></sql>
						<type>${(sql.type)!}</type>
						<viewCode>${(sql.viewCode)!}</viewCode>
						<code>${(sql.code)!}</code>
						<ecEnv>${(sql.ecEnv)!}</ecEnv>
					</sql>
					</#list>
				</#if>
			</sqls>
			<extraQueryJson>
				<#if view.extraQueryJson?? && view.extraQueryJson?has_content>
					<queryConfig><![CDATA[${((view.extraQueryJson.queryConfig)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}]]></queryConfig>
				</#if>
			</extraQueryJson>
		</view>
		</#list>
		</#if>
	</views>
</entity>