@echo off

rem This script will start a headless gRPC server for a database in the openLCA
rem workspace. The name of the database must be provided as first argument to
rem this script. You can of course also connect to other data folders and use
rem another server setup by changing the start command below. See also the
rem openLCA IPC documentation.

if "%1" == "" (
    echo error: no database provided; use this script like this:
    echo   .\grpc-server.bat {name of your database}
    goto :END
)

rem The location of this script
set script_dir=%~dp0

rem openLCA comes with an embedded JRE that we use
set java=%script_dir%..\jre\bin\java.exe

rem Go into the library folder
cd %script_dir%..\plugins\olca-app*\libs

rem The JVM arguments; you can adjust the RAM allocation via the
rem MaxRAMPercentage parameter etc. or set your own arguments via the JVM_ARGS
rem environment variable before calling this script.
if not defined JVM_ARGS set JVM_ARGS=-XX:+UseZGC -Xms256m -XX:SoftMaxHeapSize=4g -XX:MaxRAMPercentage=75 -XX:ZUncommitDelay=60 --enable-native-access=ALL-UNNAMED

rem Set the Java classpath to the library folder and start the gRPC server. It
rem will use the default openLCA workspace in ~/openLCA-data-1.4 and connect to
rem the database provided by the first parameter. You can change all of this of
rem course; see the IPC documentation.
"%java%" %JVM_ARGS% -cp * org.openlca.proto.io.server.Server -timeout 30 -db %1

:END
