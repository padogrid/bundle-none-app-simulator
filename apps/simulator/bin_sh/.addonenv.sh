#!/usr/bin/env bash

SCRIPT_DIR="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"
APP_DIR="$(dirname "$SCRIPT_DIR")"
APPS_DIR="$(dirname "$APP_DIR")"
BASE_DIR=$PADOGRID_HOME/$PRODUCT
pushd  $BASE_DIR/bin_sh > /dev/null 2>&1
. $BASE_DIR/bin_sh/.addonenv.sh
popd > /dev/null 2>&1

APP_ETC_DIR=$APP_DIR/etc
APP_LOG_DIR=$APP_DIR/log
if [ ! -d "$APP_LOG_DIR" ]; then
   mkdir -p "$APP_LOG_DIR"
fi

HAZELCAST_CLIENT_CONFIG_FILE=$APP_ETC_DIR/hazelcast-client.xml
HAZELCAST_CLIENT_FAILOVER_CONFIG_FILE=$APP_ETC_DIR/hazelcast-client-failover.xml
LOG_CONFIG_FILE=$APP_ETC_DIR/log4j2.properties
export LOG_DIR=$APP_DIR/log

if [[ ${OS_NAME} == CYGWIN* ]]; then
   HAZELCAST_CLIENT_CONFIG_FILE="$(cygpath -wp "$HAZELCAST_CLIENT_CONFIG_FILE")"
   HAZELCAST_CLIENT_FAILOVER_CONFIG_FILE="$(cygpath -wp "$HAZELCAST_CLIENT_FAILOVER_CONFIG_FILE")"
   LOG_CONFIG_FILE="$(cygpath -wp "$LOG_CONFIG_FILE")"
   export LOG_DIR="$(cygpath -wp "$LOG_DIR")"
fi

# Source in app specifics
. $APP_DIR/bin_sh/setenv.sh

# Log properties for log4j2. The log file name is set in executable scripts.
JAVA_OPTS="$JAVA_OPTS -Dhazelcast.logging.type=log4j2 \
   -Dlog4j.configurationFile=file:$LOG_CONFIG_FILE"
JAVA_OPTS="$JAVA_OPTS -Dhazelcast.client.config=$APP_ETC_DIR/hazelcast-client.xml"

CLASSPATH="$APP_DIR/lib/*:$CLASSPATH"

# Installs the specified PadoGrid artifacts to the local Maven repo. The following is a list
# of available artifacts as of writing. For a complete list, see $PADOGRID_HOME.
#
#   - geode-addon-core
#   - hazelcast-addon-common
#   - hazelcast-addon-core 3
#   - hazelcast-addon-core 4
#   - hazelcast-addon-core 5
#   - hazelcast-addon-jet-demo 3
#   - hazelcast-addon-jet-demo 4
#   - hazelcast-addon-jet-core 4
#   - kafka-addon-core
#   - padogrid-common
#   - padogrid-mqtt
#   - padogrid-tools
#   - redisson-addon-core
#   - snappydata-addon-core
#
# @param artifactId PadoGrid artifact ID
# @param productMajorVersionNumber  Optional product major version. For example, Hazelcast has
#                                   three (3) major version, 3, 4, and 5. If unspecified, then
#                                   Hazelcast defaults to 5.
function installMavenPadogridJar
{
   local artifactId=$1
   local productMajorVersionNumber=$2
   local product=${artifactId%%-*}
   local jarPath

   case "$product" in
   padogrid)
      case "$artifactId" in
         padogrid-mqtt)
            jarPath="$PADOGRID_HOME/mosquitto/lib/$artifactId-$PADOGRID_VERSION.jar" ;;
         *)
            jarPath="$PADOGRID_HOME/lib/$artifactId-$PADOGRID_VERSION.jar" ;;
         esac
         ;;
   hazelcast)
      case "$artifactId" in
      hazelcast-addon-common)
         jarPath="$PADOGRID_HOME/hazelcast/lib/$artifactId-$PADOGRID_VERSION.jar" ;;
      *)         
         if [ "$productMajorVersionNumber" == "" ]; then
            productMajorVersionNumber="5"
         fi
         jarPath="$PADOGRID_HOME/hazelcast/lib/v$productMajorVersionNumber/$artifactId-$productMajorVersionNumber-$PADOGRID_VERSION.jar" ;;
      esac
      ;;
   redisson)
      jarPath="$PADOGRID_HOME/redis/lib/$artifactId-$PADOGRID_VERSION.jar"
      ;;
   *)
      jarPath="$PADOGRID_HOME/$product/lib/$artifactId-$PADOGRID_VERSION.jar"
      ;;
   esac

   local jarFileName=$(basename $jarPath)
   local groupId="padogrid.addon"
   
   if [ ! -d tmp/padogrid/jars ]; then
      mkdir -p /tmp/padogrid/jars
   else
      rm -r /tmp/padogrid/jars/*
   fi
   pushd /tmp/padogrid/jars > /dev/null
   jar -xf $jarPath
   rm -r META-INF/maven
   jar -cf /tmp/padogrid/$jarFileName .
   mvn install:install-file -Dfile=/tmp/padogrid/$jarFileName -DgroupId=$groupId \
       -DartifactId=$artifactId -Dversion=$PADOGRID_VERSION -Dpackaging=jar
   rm -r /tmp/padogrid 
   popd > /dev/null
}
