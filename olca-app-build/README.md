# openLCA build
This project contains the scripts for building the openLCA distribution packages
for Windows, Mac OS, and Linux. It is based on the Eclipse product export and
and a Python script `packager.py` that creates the distribution packages and
installers. Thus, you need to have Python 3 installed in order to run the build.

## Export the products
The first thing you need to do, is to export the Eclipse RCP products. Right click
on the `olca-app` project and select `Export > Eclipse product`. In the
export wizard select the folder `olca-app-build/build` as export folder as
this is the folder where the `packager.py` script expects to find the product
builds. The export should be done with the following settings:

* Configuration: `/olca-app/openLCA.product`
* Root directory: `openLCA`
* Destination directory: `../olca-app-build/build`
* Uncheck: `Generate p2 repository`
* Check: `Export for multiple platforms`

The packager script can build distribution packages for the following platforms
(but you do not need to build them all, if a platform product is missing it is
simply ignored in the package):

* Linux gtk x86_64
* macOS cocoa x86_64
* Windows win32 x86_64

## Runtime packaging
...


TODO: old content starts here ...

You can run the build by executing the `build.xml`
script as `Ant Build` directly in Eclipse (this will also call the Python
packager). It is important to run the build within the same JRE as the Eclipse
platform because otherwise the PDE scripts cannot be found (
`Run AS -> Ant Build -> JRE -> Run in same JRE as the workspace`).

However, it seems that the PDE build stopped working in newer Eclipse versions.
The last version where it should work (and which we use to build the
distribution packages) is [Eclipse Luna](https://www.eclipse.org/luna/). With
the [Eclipse Luna package for RCP and RAP developers](https://www.eclipse.org/downloads/packages/release/luna/sr2)
the build should work.

The build of the distribution packages currently works on Windows only and
requires some additional tools and Java Runtime Environments in a specific
folder structure as described in the following.

## 7zip
The packager expects the [7zip](http://www.7-zip.org/) executable directly in
the `7zip` folder of this build directory:

```
olca-app-build
  - 7zip
    - ...
    - 7za.exe
     - ...
```

`7zip` is fast and keeps the file attributes so that the executables work on
the specific target platform.


## NSIS for Windows installers
To build the Windows installers, we use the
[NSIS 2.46](http://nsis.sourceforge.net) framework. The packager expects that
there is a `nsis-2.46` folder with NSIS located in the build directory:

```
olca-app-build
  - ...
  - nsis-2.46
    - Bin
    - ...
    - NSIS.exe
    - ...
```

## JRE
We distribute openLCA with a version of the Java runtime environment (JRE). The
Java runtimes for the specific platforms need to be located in the `jre` folder
of the build directory:

```
olca-app-build
  - ...
  - jre
    - win32     # the extracted JRE for windows 32 bit
    - win64     # the extracted JRE for windows 64 bit
    - jre-<version>-linux-x64.tar
    - jre-<version>-maxosx-x64.tar
```

Note that openLCA currently only correctly works with a JRE 8u101.
