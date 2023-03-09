#!/bin/bash

# This script will start a headless gRPC server for a database in the openLCA
# workspace. The name of the database must be provided as the first argument to
# this script. You can of course also connect to other data folders and use
# another server setup by changing the start command below. See also the openLCA
# IPC documentation. 

if ["$1" = ""]
then
  echo "error: no database provided; use this script like this:"
  echo "./ipc-server.sh {name of your database}"
  exit 1
fi

# The location of this script
script_dir=$(dirname "$0")

# openLCA comes with an embedded JRE that we use
java="$script_dir/../jre/bin/java"

# The library folder is our classpath
cp=$(realpath $script_dir/../plugins/olca-app*/libs)

# Set the Java classpath to the library folder and start the gRPC server. You
# can give more RAM via the Xmx parameter etc. It will use the default openLCA
# workspace in ~/openLCA-data-1.4 and connect to the database provided by the
# first parameter. You can change all of this of course; see the IPC
# documentation.
java -Xmx3584M -cp "$cp/*" org.openlca.proto.io.server.Server -timeout 30 -db $1

