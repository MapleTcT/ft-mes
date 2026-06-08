<?xml version="1.0" encoding="UTF-8"?>
<module>
	<code>${(module.code)!}</code>
	<ecEnv>${(module.ecEnv)!}</ecEnv>
	<name>${(module.name)!}</name>
	<artifact>${(module.artifact)!}</artifact>
	<projectVersion>${(module.projectVersion)!}</projectVersion>
	<orgVersion>${(module.orgVersion)!}</orgVersion>
	<description>
		<![CDATA[${(module.description)!}]]>
	</description>
	<isInherentedBase>${(module.isInherentedBase?string)!}</isInherentedBase>
	
	<entities>
	<#if module.entities?has_content>
	<#list module.entities as entity>
		<entity>
			<code>${(entity.code)!}</code>
			<name>${(entity.name)!}</name>
			<entityName>${(entity.entityName)!}</entityName>
			<workflowEnabled>${(entity.workflowEnabled?string)!}</workflowEnabled>
			<prefix>${(entity.prefix)!}</prefix>
			<printTemplates>
			<#if entity.printTemplates?has_content>
			<#list entity.printTemplates as printTemplate>
				<printTemplate>
					<code>${(printTemplate.code)!}</code>
					<modelCode>${(printTemplate.modelCode)!}</modelCode>
					<templateName>${(printTemplate.templateName)!}</templateName>
					<templateRemark>${(printTemplate.templateRemark)!}</templateRemark>
					<template><![CDATA[${(printTemplate.template)!}]]></template>
					<templateScript>${(printTemplate.description)!}</templateScript>
					<description>${(printTemplate.description)!}</description>
					<viewCode>${(printTemplate.viewCode)!}</viewCode>
					<processVersion>${(printTemplate.processVersion)!}</processVersion>
					<processKey>${(printTemplate.processKey)!}</processKey>
					<templateCode>${(printTemplate.templateCode)!}</templateCode>
					<modelCode>${(printTemplate.modelCode)!}</modelCode>
					<isPublish>${(printTemplate.isPublish?string)!}</isPublish>
					<projFlag>${(printTemplate.description)!}</projFlag>
					<valid>${(printTemplate.valid?string)!}</valid>
					<version>${(printTemplate.version)!}</version>
					<templateEnabled>${(printTemplate.templateEnabled?string)!}</templateEnabled>
					<extraParam>${(printTemplate.extraParam?string)!}</extraParam>
					<extraParamCount>${(printTemplate.extraParamCount)!}</extraParamCount>
					<extraParamScript><![CDATA[${(printTemplate.extraParamScript)!}]]></extraParamScript>
					<templateBusinesscode>${(printTemplate.templateBusinesscode)!}</templateBusinesscode>
				</printTemplate>
			</#list>
			</#if>
			</printTemplates>
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
		</entity>
	</#list>
	</#if>
	</entities>
</module>