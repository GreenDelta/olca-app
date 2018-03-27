#!/bin/sh

# update the olca-modules in the lib-folder of the application

current_path=$(pwd)
app_path=$(pwd)/olca-app
modules_path=$(pwd)/../olca-modules
updates_path=$(pwd)/../olca-updates

echo "install olca-modules from $modules_path" 
cd $modules_path
mvn install

echo "install olca-updates from $updates_path"
cd $updates_path
mvn install

echo "update packages in $app_path/libs"
cd $app_path
mvn package

cd $current_path
echo "all done"
