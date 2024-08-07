#!/bin/sh

# update the olca-modules in the lib-folder of the application

current_path=$(pwd)
app_path=$(pwd)/olca-app
modules_path=$(pwd)/../olca-modules
cs_client_path=$(pwd)/../cs-client

echo "install olca-modules from $modules_path" 
cd $modules_path
mvn install -DskipTests=true

echo "install cs-client from $cs_client_path"
cd $cs_client_path
mvn install -DskipTests=true

echo "update packages in $app_path/libs"
cd $app_path
mvn package

cd $current_path
echo "all done"
