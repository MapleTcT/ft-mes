#!/bin/sh
set -e
# 判断nacos是否正常
nc -w1 -z ${NACOS_HOST:-127.0.0.1} ${NACOS_PORT:-8848} || (echo -e "\033[31m nacos service not served\033[0m" && exit 3)

# 工作目录
home=$(cd $(dirname $0)&&cd ..&&pwd)
# 配置文件
springConfigLocation=$home/conf/application.yml
logbackConfigFile=$home/conf/logback-spring.xml

# 判断是否程序已经运行
pid=$(ps -ef | grep 'java' | grep $home | awk '{print $2}')
if [ -z "$pid" ]; then
    echo "the is not running, go to start it"
    # 执行
    java -server ${MEM_OPTS} ${JAVA_OPTS} -Dintegration.supos.enabled=true -Dwork.dir=$home -Dlogback.configurationFile=$logbackConfigFile -Dspring.config.location=$springConfigLocation -jar bootstrap.jar
else
    echo "the server is running, pid=$pid"
fi