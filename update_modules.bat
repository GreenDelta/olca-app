@echo off

set current_path=%cd%
set app_path=%current_path%\olca-app
set modules_path=%cd%\..\olca-modules
set cs_api_path=%current_path%\..\olca-cs

echo "install olca-modules from %modules_path%" 
cd %modules_path%
call mvn install -DskipTests=true

echo "install olca-cs from %cs_api_path%"
cd %cs_api_path%
call mvn install -DskipTests=true

echo "update packages in %app_path%/libs"
cd %app_path%
call mvn package

cd %current_path%
echo "all done"
