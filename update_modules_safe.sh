#!/bin/sh

# update the olca-modules in the lib-folder of the application

current_path=$(pwd)
app_path=$(pwd)/olca-app
modules_path=$(pwd)/../olca-modules
cs_api_path=$(pwd)/../olca-cs

echo "install olca-modules from $modules_path" 
cd $modules_path
mvn install

echo "install olca-cs from $cs_api_path"
cd $cs_api_path
mvn install

echo "update packages in $app_path/libs"
cd $app_path
mvn package

cd $current_path
echo "all done"
