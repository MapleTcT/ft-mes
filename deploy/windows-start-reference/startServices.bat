@echo off

set bap_home=%~dp0..\
set current_path=%~dp0
set nginx_path=%~dp0..\nginx
rem %nginx_path%\nginx.exe -s  quit


net start supfusion-nginx-middleware
net start supfusion-zookeeper-middleware
net start supfusion-nacos
net start supfusion-keycloak
net start supfusion-kafka-middleware
net start supfusion-redis-middleware
net start supfusion-websocket

choice /t 30 /d y /n >nul

net start supfusion-i18n
choice /t 30 /d y /n >nul

net start supfusion-sysmanagement
choice /t 10 /d y /n >nul
net start supfusion-basicmanagement
choice /t 10 /d y /n >nul
net start supfusion-orgmanagement
choice /t 10 /d y /n >nul
net start supfusion-iam
choice /t 10 /d y /n >nul
net start supfusion-gateway
choice /t 10 /d y /n >nul
net start supfusion-license
choice /t 30 /d y /n >nul
net start supfusion-configuration
choice /t 20 /d y /n >nul

net start supfusion-operatetools
choice /t 20 /d y /n >nul
net start supfusion-baseService
choice /t 20 /d y /n >nul
net start supfusion-flow
choice /t 10 /d y /n >nul
net start supfusion-notification-admin
choice /t 10 /d y /n >nul
net start supfusion-notification-apiserver
choice /t 10 /d y /n >nul
net start supfusion-notification-engine
choice /t 30 /d y /n >nul
net start supfusion-minmo
choice /t 10 /d y /n >nul
net start supfusion-fileServer
choice /t 10 /d y /n >nul
net start supfusion-task-scheduler-service
choice /t 10 /d y /n >nul
net start supfusion-customProperty
choice /t 10 /d y /n >nul
net start supfusion-notification-app
choice /t 10 /d y /n >nul
net start supfusion-notification-mobile


rem pause








