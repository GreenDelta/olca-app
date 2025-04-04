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
java_exec="$script_dir/../jre/bin/java"

# The library folder is our classpath
cp=$(realpath $script_dir/../plugins/olca-app*/libs)

# Set the Java classpath to the library folder and start the IPC server.
# You can adjust the RAM allocation via the Xmx parameter, etc.
# It will use the default openLCA workspace in ~/openLCA-data-1.4 and connect
# to the database provided by the first parameter. You can change all of this;
# see the IPC documentation.

"$java_exec" -Xmx3584M -cp "$cp/*" org.openlca.ipc.Server -timeout 30 -db "$1"
