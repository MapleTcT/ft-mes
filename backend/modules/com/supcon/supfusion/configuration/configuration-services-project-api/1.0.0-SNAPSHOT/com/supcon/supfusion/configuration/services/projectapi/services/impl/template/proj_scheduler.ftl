<?xml version="1.0" encoding="UTF-8"?>
<Scheduler>
	<#if schedulerJobList?? && schedulerJobList?has_content>
	<SchedulerJobs>
		<#list schedulerJobList as schedulerJob>
		<SchedulerJob>
			<moduleCode>${(schedulerJob.moduleCode)!}</moduleCode>
			<code>${(schedulerJob.code)!}</code>
			<shortCode>${(schedulerJob.shortCode)!}</shortCode>
			<name>${(schedulerJob.name)!}</name>
			<isScheduleImm>${((schedulerJob.isScheduleImm)!false)?string('true', 'false')}</isScheduleImm>
			<description><![CDATA[${(schedulerJob.description)!} ]]></description>
			<jobType>${(schedulerJob.jobType)!}</jobType>
			<jobContent>${(schedulerJob.jobContent)!}</jobContent>
			<startTime>${(schedulerJob.startTime)!}</startTime>
			<endTime>${(schedulerJob.endTime)!}</endTime>
			<triggerType>${(schedulerJob.triggerType?string)!}</triggerType>
			<repeatCount>${(schedulerJob.repeatCount)!}</repeatCount>
			<intervalNum>${(schedulerJob.intervalNum)!}</intervalNum>
			<intervalUnit>${(schedulerJob.intervalUnit)!}</intervalUnit>
			<cron><![CDATA[${(schedulerJob.cron)!}]]></cron>
			<datasouceCode>${(schedulerJob.datasouceCode)!}</datasouceCode>
			<status>${(schedulerJob.status)!}</status>
			<hasRunTimes>${(schedulerJob.hasRunTimes)!}</hasRunTimes>
			<nextRunTime>${(schedulerJob.nextRunTime)!}</nextRunTime>
			<errorMsg>${(schedulerJob.errorMsg)!}</errorMsg>
			<inheritFlag>${((schedulerJob.inheritFlag)!false)?string('true', 'false')}</inheritFlag>
		</SchedulerJob>
		</#list>
	</SchedulerJobs>
	</#if>
	<#if schedulerDatasourceList?? && schedulerDatasourceList?has_content>
	<SchedulerDatasources>
		<#list schedulerDatasourceList as schedulerDatasource>
		<SchedulerDatasource>
			<moduleCode>${(schedulerDatasource.moduleCode)!}</moduleCode>
			<code>${(schedulerDatasource.code)!}</code>
			<name>${(schedulerDatasource.name)!}</name>
			<datasourceAddress>${(schedulerDatasource.datasourceAddress)!}</datasourceAddress>
			<datasourceName>${(schedulerDatasource.datasourceName)!}</datasourceName>
			<datasourceType>${(schedulerDatasource.datasourceType?string)!}</datasourceType>
			<username>${(schedulerDatasource.name?string)!}</username>
			<password>${(schedulerDatasource.password)!}</password>
			<port>${(schedulerDatasource.port)!}</port>
		</SchedulerDatasource>
		</#list>
	</SchedulerDatasources>
	</#if>
</Scheduler>	