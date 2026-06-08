<?xml version="1.0" encoding="UTF-8"?>
<module>
	<entities>
		<#if entityList?has_content>
		<#list entityList as entity>
		<entity>
			<code>${(entity.code)!}</code>
			<ecEnv>${(entity.ecEnv)!}</ecEnv>
			<name>${(entity.name)!}</name>
			<entityName>${(entity.entityName)!}</entityName>
			<models>
			<#if entity.models?has_content>
			<#list entity.models as model>
				<model>
					<code>${(model.code)!}</code>
					<ecEnv>${(model.ecEnv)!}</ecEnv>
					<name>${(model.name)!}</name>
					<entityClass>${(model.entityClass)!}</entityClass>
					<modelName>${(model.modelName)!}</modelName>
					<jpaName>${(model.jpaName)!}</jpaName>
					<enableOperationAudit>${(model.enableOperationAudit?string)!}</enableOperationAudit>
					<enableDataAudit>${(model.enableDataAudit?string)!}</enableDataAudit>
				</model>
			</#list>
			</#if>
			</models>
		</entity>
		</#list>
		</#if>
	</entities>
</module>