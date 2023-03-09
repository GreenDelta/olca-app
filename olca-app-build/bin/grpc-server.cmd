@echo off

rem This script will start a headless gRPC server for a database in the openLCA
rem workspace. The name of the database must be provided as first argument to
rem this script. You can of course also connect to other data folders and use
rem another server setup by changing the start command below. See also the
rem openLCA IPC documentation. 

if "%1" == "" (
    echo error: no database provided; use this script like this:
    echo   .\ipc-server.bat {name of your database}
    goto :END
)

rem The location of this script
set script_dir=%~dp0

rem openLCA comes with an embedded JRE that we use
set java=%script_dir%..\jre\bin\java.exe

rem Go into the library folder
cd %script_dir%..\plugins\olca-app*\libs

rem Set the Java classpath to the library folder and start the gRPC server. You
rem can give more RAM via the Xmx parameter etc. It will use the default openLCA
rem workspace in ~/openLCA-data-1.4 and connect to the database provided by the
rem first parameter. You can change all of this of course; see the IPC
rem documentation.
%java% -Xmx3584M -cp * org.openlca.proto.io.server.Server -timeout 30 -db %1

:END