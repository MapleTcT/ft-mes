@echo off

set bap_home=%~dp0..\..\
set ServerName=supfusion-keycloak
set server_description=»ù´¡×é¼þkeycloak
set sys_exist_flag=no
set script_path=%bap_home%\bap-server\assembly\keycloak-10.0.1\bin
set nssm_path=%~dp0\..\nssm
sc query %ServerName% state=all>nul && set sys_exist_flag=yes || set sys_exist_flag=no
echo [%date:~0,10% %time:~0,12%] %ServerName% has exist? %sys_exist_flag% >> %bap_home%\Commands\logs\service.log
if %sys_exist_flag%==no (

	echo [%date:~0,10% %time:~0,12%] start to install %ServerName% ... >> %bap_home%\Commands\logs\service.log
	%nssm_path%\nssm install %ServerName% %script_path%\startKeyCloak.bat
	%nssm_path%\nssm set %ServerName% Description %server_description% 
	%nssm_path%\nssm set  %ServerName% Start SERVICE_DEMAND_START
	echo [%date:~0,10% %time:~0,12%] install %ServerName% success! >> %bap_home%\Commands\logs\service.log
)
