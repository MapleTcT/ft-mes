<#assign result = records.result />
<#assign exportFields = records.exportFields />
<#assign xmlState = xmlState>
<#assign exportFieldList = exportFields?split(',')>
<#if xmlState == 'START'>
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
 	</Styles>
 	<Worksheet ss:Name="${getText('ec.view.todo.worklist')}">
		<Table  x:FullColumns="1" x:FullRows="1" ss:DefaultColumnWidth="54" ss:DefaultRowHeight="16.5">
 			<Row ss:AutoFitHeight="0">
				<#list exportFieldList as item>
					<#if item?length gt 0>
				
						<#if item == 'TABLENO' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.tableNo')}</Data></Cell>
						</#if>
			 			<#if item == 'SUMMARY' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.property.summary')}</Data></Cell>
						</#if>
						<#if item == 'ACTIVETYPE' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.pendingType')}</Data></Cell>
						</#if>
						<#if item == 'FLOWNAME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.flowName')}</Data></Cell>
						</#if>
						<#if item == 'ACTIVENAME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.activeName')}</Data></Cell>
						</#if>
						<#if item == 'STAFFNAME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.owner')}</Data></Cell>
						</#if>
						<#if item == 'DEPTNAME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.ownerDept')}</Data></Cell>
						</#if>
						<#if item == 'CREATETIME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.createTime')}</Data></Cell>
						</#if>
						<#if item == 'CREATOR' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.inputor')}</Data></Cell>
						</#if>
						<#if item == 'MENUNAME' || item == '*'>
							<Cell><Data ss:Type="String">${getText('ec.pending.menuName')}</Data></Cell>
						</#if>
												
		 			</#if>
				</#list>
	 		</Row>
<#elseif xmlState == 'BODY'>	 		
	 		<#list result as data>
	 		<Row ss:AutoFitHeight="0">
				<#list exportFieldList as item>
					<#if item?length gt 0>
						<#if item == 'TABLENO' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.TABLENO)!}]]></Data></Cell>
						</#if>
			 			<#if item == 'SUMMARY' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.SUMMARY)!}]]></Data></Cell>
						</#if>
						<#if item == 'ACTIVETYPE' || item == '*'>
							<Cell><Data ss:Type="String"><#if (data.ACTIVETYPE)??><#if data.ACTIVETYPE ==0>${getText('ec.pending.normalPending')}<#elseif data.ACTIVETYPE ==1>${getText('ec.pending.normalPending')}<#else>${getText('ec.pending.proxyPending')}</#if></#if></Data></Cell>
						</#if>
						<#if item == 'FLOWNAME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.FLOWNAME)!}]]></Data></Cell>
						</#if>
						<#if item == 'ACTIVENAME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.ACTIVENAME)!}]]></Data></Cell>
						</#if>
						<#if item == 'STAFFNAME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.STAFFNAME)!}]]></Data></Cell>
						</#if>
						<#if item == 'DEPTNAME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.DEPTNAME)!}]]></Data></Cell>
						</#if>
						<#if item == 'CREATETIME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.CREATETIME)!}]]></Data></Cell>
						</#if>
						<#if item == 'CREATOR' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.CREATOR)!}]]></Data></Cell>
						</#if>
						<#if item == 'MENUNAME' || item == '*'>
							<Cell><Data ss:Type="String"><![CDATA[${(data.MENUNAME)!}]]></Data></Cell>
						</#if>
												
		 			</#if>
				</#list>
			</Row>
			</#list>
<#elseif xmlState == 'END'>			
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
</Workbook>
</#if>