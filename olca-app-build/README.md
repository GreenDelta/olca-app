openLCA build
=============
This project contains the scripts for building the openLCA executables for 
Windows, Mac OS, and Linux platforms. The build is based on Eclipse PDE and 
requires a set of Ant scripts and property files which are located in this build 
directory. You can run the build using the `build.xml` script. It is important
to run the build within the same JRE as the Eclipse platform because otherwise
the PDE scripts cannot be found. Before you run the build you have to prepare the
build directory with the resources described below.

NSIS for Windows installers
---------------------------
To build the Windows installers, we use the [NSIS 2.46](http://nsis.sourceforge.net) 
framework. The packager expects that there is a nsis-2.46 folder with NSIS 
directly located in this build directory.

7zip
----
We do a lot of repackaging during the build. The standard Ant zip task remove
the file properties when (un)zipping a file under Windows. Fortunately, 
[7zip](http://www.7-zip.org/) preserve these file properties. The packager
expects the 7zip executable directly in the `7zip` folder of this build directory.

JRE
---------------
We distribute openLCA with a current version of the Java runtime environment 
(JRE) for Windows and Linux packages. To package the respective JREs create
the folder

	runtime/jre
	
within this project directory. Then download the 32bit and 64bit versions
of the JRE and Linux from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html). 
For the respective platforms extract the JREs into the following folders (these
folders are configured in the respective `build<platform>.properties` files):

* Windows 32bit: the JRE must be directly extracted into the folder
  `runtime/jre/win32`
* Windows 64bit: the JRE must be directly extracted into the folder
  `runtime/jre/win64`   
* Linux 32bit|64bit: download the respective JRE package, un-zip it (the 
  result is a tar), copy the tar into the folder `runtime/jre/linux32|64`,
  and adopt the path to this package in the `buildlinux32|64bit.properties`

For Mac OS X the user has to install a JRE (currently even a JDK 7).

Packaging a XulRunner
---------------------
For some platforms we can package a XulRunner runtime together with openLCA that
can be used as an embedded browser in openLCA. Currently, we use Eclipse SWT 3.8 
which supports bindings for 
[XulRunner 10.0](http://ftp.mozilla.org/pub/mozilla.org/xulrunner/releases/10.0/). 
For the different platforms extract the XulRunner into the following folders:

* Windows 32bit: the XulRunner must be directly extracted into the folder 
  `runtime/xulrunner/win32`
* Linux 32bit|64bit: download the respective XulRunner package, un-zip it
  (the result is a tar), copy the tar into the folder 
  `runtime/xulrunner/linux32|64`, and adopt the path to this package in the
  `buildlinux32|64bit.properties`
