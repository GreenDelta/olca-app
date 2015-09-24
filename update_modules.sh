#!/bin/sh

# update the olca-modules in the lib-folder of the application

current_path=$(pwd)
app_path=$(pwd)/olca-app
modules_path=$(pwd)/../olca-modules
cloud_api_path=$(pwd)/../lca-cloud/lca-cloud-api

echo "install olca-modules from $modules_path" 
cd $modules_path
mvn install -DskipTests=true

echo "update packages in $app_path/libs"
cd $app_path
mvn package

cd $current_path
echo "all done"
