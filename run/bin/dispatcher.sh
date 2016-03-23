#!/bin/bash
# -Dlogback.configurationFile=$LOG_CONFIG_FILE

CLASSES=../lib/dispatcher-0.0.1-SNAPSHOT.jar
CLASSES=$CLASSES:../cfg
CLASSES=$CLASSES:../lib/logback-classic-1.0.13.jar
CLASSES=$CLASSES:../lib/logback-core-1.0.13.jar
CLASSES=$CLASSES:../lib/slf4j-api-1.7.5.jar
CLASSES=$CLASSES:../lib/jcl-over-slf4j-1.7.5.jar
CLASSES=$CLASSES:../lib/commons-collections-3.2.1.jar
CLASSES=$CLASSES:../lib/commons-lang-2.6.jar
CLASSES=$CLASSES:../lib/commons-configuration-1.10.jar
CLASSES=$CLASSES:../lib/commons-dbutils-1.5.jar
CLASSES=$CLASSES:../lib/commons-pool-1.6.jar
CLASSES=$CLASSES:../lib/commons-dbcp-1.4.jar
CLASSES=$CLASSES:../lib/commons-codec-1.9.jar
CLASSES=$CLASSES:../lib/mysql-connector-java-5.1.28.jar
CLASSES=$CLASSES:../lib/activemq-broker-5.9.0.jar
CLASSES=$CLASSES:../lib/activemq-client-5.9.0.jar
CLASSES=$CLASSES:../lib/activeio-core-3.1.4.jar
CLASSES=$CLASSES:../lib/activemq-kahadb-store-5.9.0.jar
CLASSES=$CLASSES:../lib/hawtbuf-1.11.jar
CLASSES=$CLASSES:../lib/geronimo-jms-1.1.jar
CLASSES=$CLASSES:../lib/geronimo-j2ee-management-1.1.jar
CLASSES=$CLASSES:../lib/baseobjects-0.0.1-SNAPSHOT.jar
CLASSES=$CLASSES:../lib/baseobjects-0.0.1-MASTER-SNAPSHOT.jar

### libs for connection pooling

CLASSES=$CLASSES:../lib/activemq-jms-pool-5.10.2.jar
CLASSES=$CLASSES:../lib/activemq-pool-5.10.2.jar
CLASSES=$CLASSES:../lib/geronimo-jms_1.1_spec-1.1.1.jar
CLASSES=$CLASSES:../lib/geronimo-jta_1.0.1B_spec-1.0.1.jar

###

## Apache httpclient libs (plus commons-codec-1.6.jar - we are now using 1.9)
CLASSES=$CLASSES:../lib/httpcore-4.3.1.jar
CLASSES=$CLASSES:../lib/httpcore-nio-4.3.1.jar
CLASSES=$CLASSES:../lib/httpclient-4.3.2.jar
CLASSES=$CLASSES:../lib/httpasyncclient-4.0.jar
CLASSES=$CLASSES:../lib/fluent-hc-4.3.2.jar


#CLASSES=$CLASSES:../lib/apache-log4j/apache-log4j-extras-1.2.17.jar
#CLASSES=$CLASSES:../lib/commons-dbcp/commons-dbcp-1.2.2.jar
#CLASSES=$CLASSES:../lib/commons-dbutils/commons-dbutils-1.4.jar
#CLASSES=$CLASSES:../lib/commons-pool/commons-pool-1.6.jar
#CLASSES=$CLASSES:../lib/groovy/*
#CLASSES=$CLASSES:../lib/jcs/jcs-1.3.jar
#CLASSES=$CLASSES:../lib/oswego/oswego-util-concurrent.jar

JAVA=/opt/java/1.7.0_51/bin/java
JAVA_HOME=/opt/java/1.7.0_51/
AGENTPATH=/opt/profiler/lib/deployed/jdk16/linux-amd64

dispatcherId=`date +%Y-%m-%d_%H-%M-%S`_$$ 
LOOPCOUNTER=0


# create logs directory, if it does not exist.
if [ ! -e ../logs ]; then
  mkdir ../logs
  touch ../logs/gitkeep
fi




echo "Parameter:" $1
if [ "$1" = 'once' ]; then

  # run java only once
  $JAVA -XX:+UseParallelGC -XX:ParallelGCThreads=2 -Xms64m -Xmx1024m -XX:+PrintGCDetails  -XX:+PrintGCDateStamps  -Xloggc:../logs/dispatcher-$dispatcherId-gc.log -Dlogback.configurationFile=../cfg/dispatcher-logback.xml -DsuperrouterRun=true -verbose:gc  -XX:OnOutOfMemoryError="echo no memory;kill -9 %p" -classpath $CLASSES eu.smscarrier.superrouter.dispatcher.App ../cfg/dispatcher.xml $dispatcherId &> "../logs/stderr_stdout_$dispatcherId.txt" &

else

  # run java in endless loop
  while :
    do
	LOOPCOUNTER=$(( $LOOPCOUNTER + 1))
      echo "wait 5 seconds ... $dispatcherId $LOOPCOUNTER"
      sleep 5
      dispatcherId=`date +%Y-%m-%d_%H-%M-%S`_$$ 
      echo "starting dispatcher..."
  # $JAVA -Xms64m -Xmx256m -XX:+PrintGCDetails  -XX:+PrintGCDateStamps  -Xloggc:/var/log/services/Router/logs/master-router-$dispatcherId-gc.log -verbose:gc  -XX:OnOutOfMemoryError="echo no memory;kill -9 %p" -XX:+UseLinuxPosixThreadCPUClocks -agentpath:$AGENTPATH/libprofilerinterface.so=/opt/profiler/lib,5140 -classpath $CLASSES eu.smscarrier.smsc.Controller /var/SMSC/cfg/ThreadedMasterRouter.xml /var/SMSC/cfg/ThreadedMasterRouter-Log4j.xml $dispatcherId
  $JAVA -XX:+UseParallelGC -XX:ParallelGCThreads=2 -Xms64m -Xmx1024m -XX:+PrintGCDetails  -XX:+PrintGCDateStamps  -Xloggc:../logs/dispatcher-$dispatcherId-gc.log -Dlogback.configurationFile=../cfg/dispatcher-logback.xml -DsuperrouterRun=true -verbose:gc  -XX:OnOutOfMemoryError="echo no memory;kill -9 %p" -classpath $CLASSES eu.smscarrier.superrouter.dispatcher.App ../cfg/dispatcher.xml $dispatcherId &> "../logs/stderr_stdout_$dispatcherId-$LOOPCOUNTER.txt"
    done

fi
