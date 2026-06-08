@echo off
title stopServices

set root_home=%~dp0..\

call stopAllAPP.bat

net stop supfusion-notification-app
net stop supfusion-notification-mobile
net stop supfusion-portal
net stop supfusion-customProperty
net stop supfusion-license
net stop supfusion-task-scheduler-service
net stop supfusion-counter
net stop supfusion-fileServer
net stop supfusion-minmo
net stop supfusion-notification-admin
net stop supfusion-notification-apiserver
net stop supfusion-notification-engine
net stop supfusion-websocket
net stop supfusion-signature
net stop supfusion-auth
net stop supfusion-gateway
net stop supfusion-iam
net stop supfusion-i18n
net stop supfusion-module-registry
net stop supfusion-organization
net stop supfusion-rbac
net stop supfusion-systemcode
net stop supfusion-systemconfig
net stop supfusion-theme
net stop supfusion-flow
net stop supfusion-operatetools
net stop supfusion-baseService
net stop supfusion-printer
net stop supfusion-configuration
net stop supfusion-sysmanagement
net stop supfusion-basicmanagement
net stop supfusion-orgmanagement

net stop supfusion-websocket
net stop supfusion-redis-middleware
net stop supfusion-kafka-middleware
net stop supfusion-keycloak
net stop supfusion-nacos
net stop supfusion-zookeeper-middleware
net stop supfusion-nginx-middleware

rem pause