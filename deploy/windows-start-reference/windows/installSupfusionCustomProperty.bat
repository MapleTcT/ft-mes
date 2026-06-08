@echo off

set bap_home=%~dp0..\..\
set ServerName=supfusion-customProperty
set server_description=自定义字段管理服务
set sys_exist_flag=no
set script_path=%bap_home%\bap-server\base-Server\customProperty
set nssm_path=%~dp0\..\nssm
sc query %ServerName% state=all>nul && set sys_exist_flag=yes || set sys_exist_flag=no
echo [%date:~0,10% %time:~0,12%] %ServerName% has exist? %sys_exist_flag% >> %bap_home%\Commands\logs\service.log
if %sys_exist_flag%==no (
	echo [%date:~0,10% %time:~0,12%] start to install %ServerName% ... >> %bap_home%\Commands\logs\service.log
	%nssm_path%\nssm install %ServerName% %script_path%\startup.bat
	%nssm_path%\nssm set %ServerName% Description %server_description% 
	%nssm_path%\nssm set  %ServerName% Start SERVICE_DEMAND_START
	echo [%date:~0,10% %time:~0,12%] install %ServerName% success! >> %bap_home%\Commands\logs\service.log
)
