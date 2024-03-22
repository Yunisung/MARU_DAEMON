#!/bin/sh

cd /var/lib/jenkins/workspace/MARU_DAEMON/bin

# SET LIBRARY
for i in ../lib/*.jar; do
    CP=$CP:$i
done
CP=`echo $CP | cut -c2-`


# JVM_ARGS for VM
##########################
JVM_ARGS="-DMARU_WELCOME_DIFF_TRX_DOWNLOAD -DCP_CONF=../conf -Dlogback.configurationFile=../conf/logback.xml -Dfile.encoding=UTF-8"
JVM_ARGS="$JVM_ARGS -Xss512k -Xms64m -Xmx128m"
JVM_ARGS="$JVM_ARGS -cp $CP:../classes"

TARGET_DATE="$(date +%Y%m%d)"

java $JVM_ARGS com.pgmate.dm.main.WelcomeDiffTrxDownLoad $TARGET_DATE