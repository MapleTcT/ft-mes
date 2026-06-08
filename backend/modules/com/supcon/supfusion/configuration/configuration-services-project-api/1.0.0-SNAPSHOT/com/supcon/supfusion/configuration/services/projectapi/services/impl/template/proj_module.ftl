<?xml version="1.0" encoding="UTF-8"?>
<module>
	<code>${(module.code)!}</code>
	<ecEnv>${(module.ecEnv)!}</ecEnv>
	<name>${(module.name)!}</name>
	<artifact>${(module.artifact)!}</artifact>
	<projectVersion>${(module.projectVersion)!}</projectVersion>
	<orgVersion>${(module.orgVersion)!}</orgVersion>
	<description><![CDATA[${(module.description)!}]]></description>
	<isInherentedBase>${(module.isInherentedBase?string)!}</isInherentedBase>
	<entities>
		<#if module.entities?has_content>
		<#list module.entities as entity>
		<#if (entity.projFlag!false)>
		<entity>
			<code>${(entity.code)!}</code>
			<name>${(entity.name)!}</name>
			<entityName>${(entity.entityName)!}</entityName>
			<workflowEnabled>${(entity.workflowEnabled?string)!}</workflowEnabled>
			<prefix>${(entity.prefix)!}</prefix>
			<crossCompanyFlag>${(entity.crossCompanyFlag?string)!}</crossCompanyFlag>
		</entity>
		</#if>
		</#list>
		</#if>
	</entities>
</module>