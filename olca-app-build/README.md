## Building the distribution packages
To build the distribution packages, we currently use the standard PDE Export
wizard. Click on the `olca-app` project and then on `Export...` from the context
menu. Select `Plug-in Development > Eclipse Product` from the export wizard and
select the following options in the export dialog:

* Configuration: `/olca-app/openLCA.product` (should be the default)
* Root directory: `openLCA`
* Synchronize before exporting: yes [x]
* Destination directory: choose the `olca-app-build/build` folder of this project
* Generate p2 repository: no [ ] (would be just overhead)
* Export for multiple platforms: yes [x]
* (take the defaults for the others)

In the next page, select the platforms for which you want to build the product.
After the export, you need to run the package script `make.py` to copy
resources like the Java runtime, the native math libraries, etc. to the
application folder and to create the installers.

The packager script can build distribution packages for the following platforms
(but you do not need to build them all, if a platform product is missing it is
simply ignored in the package script):

* Linux gtk x86_64
* macOS cocoa x86_64
* Windows win32 x86_64

The build of the distribution packages currently works on Windows only and
requires some additional tools and Java Runtime Environments in a specific
folder structure as described in the following.

## 7zip
The packager expects the [7zip](http://www.7-zip.org/) executable in the
`tools/7zip` folder of this build directory:

```
olca-app-build
  - tools
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
there is a `tools/nsis-2.46` folder with NSIS located in the build directory:

```
olca-app-build
  - tools
    - nsis-2.46
      - Bin
      - ...
      - NSIS.exe
      - ...
```

## JRE
We distribute openLCA with a version of a Java runtime environment (JRE) from
the [OpenJDK](https://adoptopenjdk.net/). The JREs for the specific platforms
need to be located in the `jre` folder of the build directory:

```
olca-app-build
  - ...
  - runtime
    - jre
      - win64     # the extracted JRE for windows 64 bit
      - *linux*.tar
      - *mac*.tar
```

## Math libraries
Additionally, we package some high performance math libraries with openLCA. The
current library packages are available from the
[olca-rust](https://github.com/msrocka/olca-rust/releases) and go into the
following folder structure for the build:

```
olca-app-build
  - ...
  - runtime
    - julia
      - linux
        - olcar.so
        - ...
      - macos
        - olcar.dylib
        - ...
      - win64
        - olcar.dll
        - ...
```

## Steps when building a release package

1. Check that the `olca-app` and `olca-modules` repositories are on the master
   branch and are in sync with our Github repository
2. Update the `olca-modules` libraries in the `olca-app` project, e.g. by
   running the `update_modules` script:

```bash
cd olca-app
./update_modules.sh
```

3. Build the reference databases (also to make sure that they have the current
   schema):

```bash
cd olca-refdata
mvn package
cd ..
```

4. Build the html-package and make sure that it contains the current background
   image:

```bash
cd olca-app-html
npm run build
cd ..
```

5. Run the PDE export as described above
6. Run the packaging script

```bash
cd olca-app-build
python make.py
```

7. Do a smoke test ...
