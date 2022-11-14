#!/bin/sh
realtime_check=`ps -ef | grep -v "grep" | grep "vactAccount_Terminate" | wc -l`

if [ "$realtime_check" == "0"  ]; then
    cd /home/bkwinners/MARU_DAEMON/bin

    # SET LIBRARY
    for i in ../lib/*.jar; do
        CP=$CP:$i
    done
    CP=`echo $CP | cut -c2-`


    # JVM_ARGS for VM
    ##########################
    JVM_ARGS="-DvactAccount_Terminate -DCP_CONF=../conf -Dlogback.configurationFile=../conf/logback.xml -Dfile.encoding=UTF-8"
    JVM_ARGS="$JVM_ARGS -Xss512k -Xms64m -Xmx128m"
    JVM_ARGS="$JVM_ARGS -cp $CP:../classes"


    java $JVM_ARGS com.pgmate.dm.main.ChargeSettleRetryNoti
else
    echo "======== vactAccount_Terminate Process Alive =======" >> /var/lib/jenkins/workspace/MARU_DAEMON/logs/root.txt
fi
