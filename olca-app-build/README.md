# openLCA build
This project contains the scripts for building the openLCA distribution packages
for Windows, Mac OS, and Linux. The build is based on Eclipse PDE and a set of
Ant scripts and property files which are located in the `olca-app-build`
directory. You can run the build by executing the `build.xml` script as
`Ant Build` directly in Eclipse. It is important to run the build within the
same JRE as the Eclipse platform because otherwise the PDE scripts cannot be
found (`Run AS -> Ant Build -> JRE -> Run in same JRE as the workspace`).
However, it seems that the PDE build stopped working in newer Eclipse versions.
The last version where it should work (and which we use to build the
distribution packages) is [Eclipse Luna](https://www.eclipse.org/luna/). With
the [Eclipse Luna package for RCP and RAP developers](https://www.eclipse.org/downloads/packages/release/luna/sr2) the build
should work.

The build of the distribution packages currently works on Windows only and
requires some additional tools and a quite complicated folder setup as
described in the following.


NSIS for Windows installers
---------------------------
To build the Windows installers, we use the [NSIS 2.46](http://nsis.sourceforge.net) framework. The packager expects that there is a nsis-2.46 folder with NSIS directly located in this build directory:

    olca-app-build
      - ...
      - nsis-2.46
        - Bin
        - ...
        - NSIS.exe
        - ...


7zip
----
We do a lot of repackaging during the build. The standard Ant zip task remove the file properties when (un)zipping a file under Windows. Fortunately, [7zip](http://www.7-zip.org/) preserve these file properties. The packager expects the 7zip executable directly in the `7zip` folder of this build directory.

    olca-app-build
      - ...
      - 7zip
        - ...
        - 7za.exe
        - ...


JRE
---
We distribute openLCA with a current version of the Java runtime environment 
(JRE) for Windows and Linux packages. To package the respective JREs create
the folder

    olca-app-build/runtime/jre
	
within this project directory. Then download the 32bit and 64bit versions
of the [JRE for Windows and Linux](http://www.oracle.com/technetwork/java/javase/downloads/index.html). For the respective platforms extract the JREs into the following folders (these folders are configured in the respective `build<platform>.properties` files):

* Windows 32bit: the JRE should be directly extracted into the folder
  `runtime/jre/win32`
* Windows 64bit: the JRE should be directly extracted into the folder
  `runtime/jre/win64`   
* Linux 32bit|64bit: download the respective JRE package, un-zip it (the 
  result is a tar), copy the tar into the folder `runtime/jre/linux32|64`,
  and adopt the path to this package in the `buildlinux32|64bit.properties`

The JRE folder should then look like this:

    olca-app-build
      - ...
      - runtime
        - jre
          - linux32
            - jre....tar    # the tar file
          - linux64
            - jre....tar    # the tar file
          - win32
            - bin           # the extracted zip
            - ...
          - win64
            - bin           # the extracted zip
            - ...    

For Mac OS X we currently cannot package a JRE. The user has to install the current JDK v8 to run the application.

In order to build openLCA with a newer JRE you may have to remove the entry

	Bundle-RequiredExecutionEnvironment: ...
	
from the bundle manifest.

