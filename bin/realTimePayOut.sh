#!/bin/sh
realtime_check=`ps -ef | grep -v "grep" | grep "MARU_REALTIME_PAYOUT" | wc -l`

if [ "$realtime_check" == "0"  ]; then
    cd /home/bkwinners/MARU/MARU_DAEMON/bin

    # SET LIBRARY
    for i in ../lib/*.jar; do
        CP=$CP:$i
    done
    CP=`echo $CP | cut -c2-`


    # JVM_ARGS for VM
    ##########################
    JVM_ARGS="-DMARU_REALTIME_PAYOUT -DCP_CONF=../conf -Dlogback.configurationFile=../conf/logback.xml -Dfile.encoding=UTF-8"
    JVM_ARGS="$JVM_ARGS -Xss512k -Xms64m -Xmx128m"
    JVM_ARGS="$JVM_ARGS -cp $CP:../classes"


    java $JVM_ARGS com.pgmate.dm.main.RealTimePayOut
else
    echo "======== MARU_REALTIME_PAYOUT Process Alive =======" >> /home/bkwinners/MARU/MARU_DAEMON/logs/root.txt
fi
