olca-app
========
This repository contains the source code of the [openLCA](http://openlca.org) 
application. openLCA is a Java application that runs on the Eclipse Rich Client
Platform ([Eclipse RCP](http://wiki.eclipse.org/index.php/Rich_Client_Platform)).
This project depends on the [olca-modules](https://github.com/GreenDelta/olca-modules) 
project which is a plain [Maven](http://maven.apache.org/) project that contains
the core functionalities (e.g. the model, database access, calculations, and data
exchange). 

The repository has the following sub-projects:

* [olca-app](./olca-app): contains the source code of the openLCA RCP application.
* [olca-app-build](./olca-app-build): contains the build scripts for compiling
  openLCA and creating the installers for Windows, Linux, and MacOS.
* [olca-app-html](./olca-app-html): contains the HTML and JavaScript source code	
  for the HTML views in openLCA (like the start page or the report views).
* [olca-app-runtime](./olca-app-runtime): contains the build scripts for creating
  the Eclipse RCP runtime for openLCA.

See also the README files that are contained in these sub-projects.


Building
--------
To compile openLCA you need to have the following tools installed:

* a [Java Development Kit 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven](http://maven.apache.org/)
* the current [Eclipse Standard package](https://www.eclipse.org/downloads/)

You can then build openLCA from source code with the following steps:

1. Compile and install the the openLCA modules into your local Maven repository
   as described in the [README](https://github.com/GreenDelta/olca-modules) file 
   of the olca-modules project.

2. Download this repository and import the projects of this repository into your
   Eclipse workspace (File/Import/Import existing projects).

3. Run the ANT script [olca-app-runtime/build.xml](./olca-app-runtime/build.xml) 
   that will download and prepare the Eclipse runtime for openLCA. Then open the 
   file [olca-app-runtime/platform.target](./olca-app-runtime/platform.target) 
   and click on 'Set as target platform'.

4. Run the script `update_modules.bat` or `update_modules.sh` (this will 
   copy/update the olca-modules in the olca-app folder) and refresh your 
   Eclipse workspace (via F5).

5. Finally, open the file [olca-app/openLCA.product](./olca-app/openLCA.product)
   and click on the run icon. openLCA should now start.

If you want to build an installable product, see the description in the 
[olca-app-build](./olca-app-build) sub-project or simply use the Eclipse export
wizard (Export/Eclipse product).     


License
-------
Unless stated otherwise, all source code of the openLCA project is licensed under the 
[Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please see the LICENSE.txt
file in the root directory of the source code.
