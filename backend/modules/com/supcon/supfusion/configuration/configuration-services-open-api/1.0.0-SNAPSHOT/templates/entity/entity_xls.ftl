<#assign result = entities.result />
<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet" xmlns:html="http://www.w3.org/TR/REC-html40">
	<DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
		<Created>1899-12-30T00:00:00</Created>
	</DocumentProperties>
	<ExcelWorkbook xmlns="urn:schemas-microsoft-com:office:excel">
		<WindowWidth>18800</WindowWidth>
 		<WindowHeight>10540</WindowHeight>
 		<ProtectStructure>False</ProtectStructure>
 		<ProtectWindows>False</ProtectWindows>
	</ExcelWorkbook>
	<Styles>
 		<Style ss:ID="Default" ss:Name="Normal">
  			<Alignment/>
  			<Borders/>
  			<Font ss:FontName="Arial" x:CharSet="134" ss:Size="12"/>
  			<Interior/>
 			<NumberFormat/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s4">
  			<NumberFormat ss:Format="_ * #,##0.00_ ;_ * -#,##0.00_ ;_ * &quot;-&quot;??_ ;_ @_ "/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s5">
  			<NumberFormat ss:Format="_ &quot;￥&quot;* #,##0.00_ ;_ &quot;￥&quot;* -#,##0.00_ ;_ &quot;￥&quot;* &quot;-&quot;??_ ;_ @_ "/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s2">
  			<NumberFormat ss:Format="0%"/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s1">
  			<NumberFormat ss:Format="_ * #,##0_ ;_ * -#,##0_ ;_ * &quot;-&quot;_ ;_ @_ "/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s3">
  			<NumberFormat ss:Format="_ &quot;￥&quot;* #,##0_ ;_ &quot;￥&quot;* -#,##0_ ;_ &quot;￥&quot;* &quot;-&quot;_ ;_ @_ "/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s6"/>
 		<Style ss:ID="s7">
  			<NumberFormat ss:Format="yyyy-m-d;-;-;@"/>
  			<Protection/>
  		</Style>
 		<Style ss:ID="s8">
  			<NumberFormat ss:Format="yyyy-m-d\ H:mm;-;-;@"/>
  			<Protection/>
  		</Style>
  		<Style ss:ID="s9">
  		<Font ss:FontName="宋体" x:CharSet="134" ss:Size="12" ss:Bold="1"/>
  		<Alignment  ss:Horizontal="Center"/>
  		</Style>
  		<Style ss:ID="s10">
  		<Font ss:FontName="Arial" x:CharSet="134" ss:Size="12" ss:Color="#0000FF" ss:Underline="Single"/>
  		<Alignment  ss:Horizontal="Center"/>
 		</Style>
 		<Style ss:ID="s11">
  		<Alignment  ss:Horizontal="Center"/>
 		</Style>
 	</Styles>
 	<Worksheet ss:Name="${getText('${module.name}')}${getText('ec.module.module')}">
		<Table  x:FullColumns="1" x:FullRows="1" ss:DefaultColumnWidth="100" ss:DefaultRowHeight="16.5">
 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('foundation.module.code')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('foundation.module.name')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.common.version')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.description')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('foundation.module.dependent')}</Data></Cell>
 			</Row>
 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s11">									
				 	<Data ss:Type="String">${module.code}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${getText('${module.name}')}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${module.projectVersion}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${module.description!}</Data>
				</Cell>
				<Cell ss:StyleID="s11">							
					<Data ss:Type="String">${names!}</Data>								
				</Cell>
 			</Row>
 			<Row ss:AutoFitHeight="0">
 			</Row>
 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.entityCode')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.entityName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.description')}</Data></Cell>
	 		</Row>
	 		<#list listentities as data>
	 		<Row ss:AutoFitHeight="0">
				<Cell ss:StyleID="s10" ss:HRef="#${data.name}!A1">
					<Data ss:Type="String"> ${data.entityName}</Data>
				</Cell>
				<Cell ss:StyleID="s10" ss:HRef="#${data.name}!A1">
					<Data ss:Type="String">${data.name}</Data>
				</Cell>
				<Cell ss:StyleID="s11" >
					<Data ss:Type="String">${data.description!}</Data>
				</Cell>
			</Row>
			</#list>
		</Table>
		<WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
  			<PageSetup>
				<Header x:Margin="0.511805555555556"/>
				<Footer x:Margin="0.511805555555556"/>
			</PageSetup>
			<Print>
				<HorizontalResolution>1200</HorizontalResolution>
				<VerticalResolution>1200</VerticalResolution>
			</Print>
				<Selected/>
				<PageBreakZoom>100</PageBreakZoom>
			<Panes>
				<Pane>
					<Number>3</Number>
					<ActiveRow>0</ActiveRow>
					<ActiveCol>2</ActiveCol>
					<RangeSelection>R1C3</RangeSelection>
				</Pane>
			</Panes>
			<ProtectObjects>False</ProtectObjects>
			<ProtectScenarios>False</ProtectScenarios>
		</WorksheetOptions>	
</Worksheet>
<#list listentities as data>
<Worksheet ss:Name="${data.name}">
		<Table  x:FullColumns="1" x:FullRows="1" ss:DefaultColumnWidth="150" ss:DefaultRowHeight="16.5">
 			<Row ss:AutoFitHeight="0">
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.entityCode')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.entityName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.prefix')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.workflowEnabled')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.groupEnabled')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.isBase')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.entity.isInherentedBase')}</Data></Cell>
 			</Row>
 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s11">									
				 	<Data ss:Type="String">${data.entityName}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${data.name}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${data.prefix!}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">
					<#if (data.workflowEnabled)??&&data.workflowEnabled>
						${getText('foundation.systemCode.true')}
					<#else>
						${getText('foundation.systemCode.false')}
					</#if>
					</Data>
				</Cell>
				<Cell ss:StyleID="s11">							
					<Data ss:Type="String">
					<#if (data.groupEnabled)??&&data.groupEnabled>
						${getText('foundation.systemCode.true')}
					<#else>
						${getText('foundation.systemCode.false')}
					</#if>
					</Data>								
				</Cell>
				
				<Cell ss:StyleID="s11">							
					<Data ss:Type="String">
					<#if (data.isBase)??&&data.isBase>
						${getText('foundation.systemCode.true')}
					<#else>
						${getText('foundation.systemCode.false')}
					</#if>
					</Data>								
				</Cell>
				
				<Cell ss:StyleID="s11">							
					<Data ss:Type="String">
					<#if (data.isInherentedBase)??&&data.isInherentedBase>
						${getText('foundation.systemCode.true')}
					<#else>
						${getText('foundation.systemCode.false')}
					</#if>
					</Data>								
				</Cell>
 			</Row>
	 		<#list data.models as modleinfo>
	 		<Row ss:AutoFitHeight="0">
 			</Row>
	 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.modelName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.tableName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.name')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.dataType')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.isMain')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.isExtraCol')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.isCache')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.model.description')}</Data></Cell>
	 		</Row>
		 		<Row ss:AutoFitHeight="0">
					<Cell ss:StyleID="s11">
						<Data ss:Type="String"> ${modleinfo.modelName}</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String">${modleinfo.tableName}</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String">${getText('${modleinfo.name}')}</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String">
						<#if (modleinfo.dataType)??>
							<#if modleinfo.dataType==1>
							${getText('ec.model.dataType.1')}
							<#elseif  modleinfo.dataType==2 >
							${getText('ec.model.dataType.2')}
							</#if>
						</#if>
						</Data>	
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String">
						<#if (modleinfo.isMain)??&&modleinfo.isMain>
							${getText('foundation.systemCode.true')}
						<#else>
							${getText('foundation.systemCode.false')}
						</#if>
						</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String"> 
						<#if (modleinfo.isCache)??&&modleinfo.isCache>
							${getText('foundation.systemCode.true')}
						<#else>
							${getText('foundation.systemCode.false')}
						</#if>
						</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String"> 
						<#if (modleinfo.isExtraCol)??&&modleinfo.isExtraCol	>
							${getText('foundation.systemCode.true')}
						<#else>
							${getText('foundation.systemCode.false')}
						</#if>
						</Data>
					</Cell>
					<Cell ss:StyleID="s11">
						<Data ss:Type="String"> ${modleinfo.description!}</Data>
					</Cell>
				</Row>
 			<Row ss:AutoFitHeight="0">
 				<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.property.name')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.property.columnName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.property.displayName')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.property.type')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.view.cell.showType')}</Data></Cell>
	 			<Cell ss:StyleID="s9"><Data ss:Type="String">${getText('ec.property.format')}</Data></Cell>
	 		</Row>
	 		<#list modleinfo.properties as property>
	 		<Row ss:AutoFitHeight="0">
				<Cell ss:StyleID="s11">
					<Data ss:Type="String"> ${property.name}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${property.columnName}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">${getText('${property.displayName}')}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String"> ${property.type!}</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String"> 
					<#if (property.fieldType)??>
						<#if property.fieldType=='TEXTFIELD'>
						普通文本
						<#elseif  property.fieldType=='TEXTAREA' >
						长文本
						<#elseif  property.fieldType=='RICHTEXT' >
						富文本编辑					
						<#elseif  property.fieldType=='DATE' >
						日期输入
						<#elseif  property.fieldType=='DATETIME' >
						日期时间
						<#elseif  property.fieldType=='SELECT' >
						选择下拉
						<#elseif  property.fieldType=='CHECKBOX' >
						多选框
						<#elseif  property.fieldType=='SELECTCOMP' >
						选择控件
						<#elseif  property.fieldType=='RADIO' >
						单选框
						<#elseif  property.fieldType=='PASSWORDFIELD' >
						密码框
						<#elseif  property.fieldType=='PICTURE' >
						图片
						<#elseif  property.fieldType=='PROPERTYATTACHMENT' >
						附件
						<#elseif  property.fieldType=='OFFICE' >
						Office文档控件
						<#else>
						${property.fieldType!}
						</#if>
					</#if>
					</Data>
				</Cell>
				<Cell ss:StyleID="s11">
					<Data ss:Type="String">
					<#if (property.format)??>
						<#if property.format=='TEXT'>
						文本
						<#elseif  property.format=='EMAIL' >
						电子邮件
						<#elseif  property.format=='URL' >
						网址
						<#elseif  property.format=='IP' >
						IP地址
						<#elseif  property.format=='PERCENT' >
						百分比
						<#elseif  property.format=='YMD' >
						2000-05-01
						<#elseif  property.format=='YM' >
						2000-05
						<#elseif  property.format=='Y' >
						2000
						<#elseif  property.format=='YMD_HMS' >
						2000-05-01 06:09:06
						<#elseif  property.format=='YMD_HM' >
						2000-05-01 06:09
						<#elseif  property.format=='YMD_H' >
						2000-05-01 06
						<#elseif  property.format=='SELECT' >
						下拉列表
						<#elseif  property.format=='CHECKBOX' >
						复选
						<#elseif  property.format=='SELECTCOMP' >
						选择控件
						<#elseif  property.format=='RADIO' >
						单选
						<#elseif  property.format=='THOUSAND' >
						千分位
						<#elseif  property.format=='TEN_THOUSAND' >
						万分位
						<#elseif  property.format=='PICTURE' >
						图片
						<#elseif  property.format=='OFFICE' >
						Office控件
						<#else>
						${property.format!}
						</#if>
					</#if>
					</Data>
				</Cell>
			</Row>
			</#list>
		</#list>	
		</Table>
		<WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
  			<PageSetup>
				<Header x:Margin="0.511805555555556"/>
				<Footer x:Margin="0.511805555555556"/>
			</PageSetup>
			<Print>
				<HorizontalResolution>1200</HorizontalResolution>
				<VerticalResolution>1200</VerticalResolution>
			</Print>
				<Selected/>
				<PageBreakZoom>100</PageBreakZoom>
			<Panes>
				<Pane>
					<Number>3</Number>
					<ActiveRow>0</ActiveRow>
					<ActiveCol>2</ActiveCol>
					<RangeSelection>R1C3</RangeSelection>
				</Pane>
			</Panes>
			<ProtectObjects>False</ProtectObjects>
			<ProtectScenarios>False</ProtectScenarios>
		</WorksheetOptions>	
</Worksheet>
</#list>
</Workbook>