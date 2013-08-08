openLCA build
=============

JRE
---
The build packs a JRE into the installation package for the following
platforms:

* Windows 32bit: the JRE must be directly extracted into the folder
  `runtime/jre/win32` 
* Windows 64bit: the JRE must be directly extracted into the folder
  `runtime/jre/win64`   
* Linux 32bit|64bit: download the respective JRE package, un-zip it (the 
  result is a tar), copy the tar into the folder `runtime/jre/linux32|64`,
  and adopt the path to this package in the `buildlinux32|64bit.properties`

For Mac OS X the user has to install a JRE (currently even a JDK 7).

XulRunner
---------
The build packs a XulRunner 10.0 into the installation package for the 
following platforms:

* Windows 32bit: the XulRunner must be directly extracted into the folder 
  `runtime/xulrunner/win32`
* Linux 32bit|64bit: download the respective XulRunner package, un-zip it
  (the result is a tar), copy the tar into the folder 
  `runtime/xulrunner/linux32|64`, and adopt the path to this package in the
  `buildlinux32|64bit.properties`
* 