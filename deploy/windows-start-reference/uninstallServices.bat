@echo off

set bap_home=%~dp0..\

call stopServices.bat

sc delete supfusion-notification-sms-jincang
sc delete supfusion-notification-app
sc delete supfusion-notification-mobile
sc delete supfusion-printer
sc delete supfusion-portal
sc delete supfusion-customProperty
sc delete supfusion-license
sc delete supfusion-task-scheduler-service
sc delete supfusion-counter
sc delete supfusion-fileServer
sc delete supfusion-minmo
sc delete supfusion-notification-admin
sc delete supfusion-notification-apiserver
sc delete supfusion-notification-engine
sc delete supfusion-websocket
sc delete supfusion-signature
sc delete supfusion-auth
sc delete supfusion-gateway
sc delete supfusion-iam
sc delete supfusion-i18n
sc delete supfusion-init
sc delete supfusion-module-registry
sc delete supfusion-organization
sc delete supfusion-rbac
sc delete supfusion-systemcode
sc delete supfusion-systemconfig
sc delete supfusion-theme
sc delete supfusion-flow
sc delete supfusion-operatetools
sc delete supfusion-baseService
sc delete supfusion-configuration
sc delete supfusion-sysmanagement
sc delete supfusion-basicmanagement
sc delete supfusion-orgmanagement

sc delete supfusion-websocket
sc delete supfusion-redis-middleware
sc delete supfusion-kafka-middleware
sc delete supfusion-keycloak
sc delete supfusion-nacos
sc delete supfusion-zookeeper-middleware
sc delete supfusion-nginx-middleware

rem pause



