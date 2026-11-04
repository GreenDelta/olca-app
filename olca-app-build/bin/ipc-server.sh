#!/bin/bash
# This script will start a headless IPC server for a database in the openLCA
# workspace. The name of the database must be provided as the first argument to this
# script. You can, of course, also connect to other data folders and use another
# server setup by changing the start command below. See also the openLCA IPC
# documentation.

if [ -z "$1" ]; then
echo "Error: No database provided. Use this script as follows:"
echo "./ipc-server.sh {name of your database}"
exit 1
fi

# The location of this script
script_dir="$(dirname "$(realpath "$0")")"

# openLCA comes with an embedded JRE that we use
os="$(uname)"
if [[ "$os" == "Darwin" ]]; then
  java_exec="$script_dir/../jre/Contents/Home/bin/java"
else
  java_exec="$script_dir/../jre/bin/java"
fi

if [ ! -x "$java_exec" ]; then
  echo "Error: Java executable not found at $java_exec"
  exit 1
fi

# The library folder is our classpath
cp=$(realpath $script_dir/../plugins/olca-app*/libs)

# The JVM arguments; you can adjust the RAM allocation via the
# MaxRAMPercentage parameter etc. or set your own arguments via the JVM_ARGS
# environment variable before calling this script.
JVM_ARGS="${JVM_ARGS:--XX:+UseZGC -Xms256m -XX:SoftMaxHeapSize=4g -XX:MaxRAMPercentage=75 -XX:ZUncommitDelay=60 --enable-native-access=ALL-UNNAMED}"

# Set the Java classpath to the library folder and start the IPC server.
# It will use the default openLCA workspace in ~/openLCA-data-1.4 and connect
# to the database provided by the first parameter. You can change all of this;
# see the IPC documentation.
# shellcheck disable=SC2086 # JVM_ARGS must be split into separate arguments
"$java_exec" $JVM_ARGS -cp "$cp/*" org.openlca.ipc.Server -timeout 30 -db "$1"
