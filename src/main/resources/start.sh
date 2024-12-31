#!/usr/bin/env bash


function parse_yaml {
   local prefix=$2
   local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
   sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p"  $1 |
   awk -F$fs '{
      indent = length($1)/2;
      vname[indent] = $2;
      for (i in vname) {if (i > indent) {delete vname[i]}}
      if (length($3) > 0) {
         vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
         printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
      }
   }'
}

eval $(parse_yaml "/etc/momo/env.yaml" "conf_")
###解析yaml获取环境变量
env=null
if [ "$conf_global_corp" == overseas ]; then
  env="prod"
elif [ "$conf_global_corp" == aws-us-east-1 ]; then
    env="ue"
elif [ "$conf_global_corp" == alpha ]; then
  env="dev"
else
  env=""
fi

if [ "$env" == "" ]; then
  echo "null env found!";
  exit
fi


port=5223

logPath=/home/logs/imc-modified
mkdir -p $logPath

echo "start connector, port:${port} , jmxPort:${jmxPort} , logPath : ${logPath}, env : ${env}"

JAVA_OPT="${JAVA_OPT} -server -Xmx8g -Xms8g"
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled"
JAVA_OPT="${JAVA_OPT} -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=25"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${logPath}/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -Dcom.immomo.im.connector.tcpPort=${port} -Dcom.immomo.connector.logpath=${logPath}"

JAVA_OPT="${JAVA_OPT} -Dlog4j.configuration=log4j.properties -Dlog4j.configurationFile=log4j2.xml"
###根据yaml配置的环境，传入环境变量
JAVA_OPT="${JAVA_OPT} -Dboot.env=${env}"

echo "java opts:"
echo "${JAVA_OPT}"

if [ "$env" == dev ]; then
  exec java -cp .:conf/*:lib/*  ${JAVA_OPT} com.immomo.connector.LiveImBootstrap  >> ${logPath}/stdout.sh.log 2>&1
  exit
fi

curl --connect-timeout 3 --max-time 3 -s http://123.com/resource/skywalking > skywalking-agent.sh

#aws-us-east-1-prod::LEAP_im-connector-ue
service_name="momo-prod::LEAP_im-connector"
if [ $env == ue ];then
  service_name="aws-us-east-1-prod::LEAP_im-connector-ue"
fi

. ./skywalking-agent.sh
exec java -javaagent:/home/deploy/skywalking/agent/skywalking-agent.jar -Dskywalking.agent.service_name=${service_name} -cp .:conf/*:lib/*  ${JAVA_OPT} com.immomo.connector.LiveImBootstrap  >> ${logPath}/stdout.sh.log 2>&1
#exec java -cp .:conf/*:lib/*  ${JAVA_OPT} com.immomo.connector.LiveImBootstrap  >> ${logPath}/stdout.sh.log 2>&1