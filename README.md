olca-app
========
This repository contains the source code of the [openLCA](http://openlca.org) 
application. openLCA is a Java application that runs on the Eclipse Rich Client
Platform ([Eclipse RCP](http://wiki.eclipse.org/index.php/Rich_Client_Platform)). This project depends on the [olca-modules](https://github.com/GreenDelta/olca-modules) project which is a plain [Maven](http://maven.apache.org/) project that contains the core functionalities (e.g. the model, database access, calculations, and data exchange). 

The repository has the following sub-projects:

* [olca-app](./olca-app): contains the source code of the openLCA RCP application.
* [olca-app-build](./olca-app-build): contains the build scripts for compiling
openLCA and creating the installers for Windows, Linux, and MacOS.
* [olca-app-html](./olca-app-html): contains the source code for the HTML views in openLCA (like the start page or the report views).
* [olca-app-runtime](./olca-app-runtime): contains the build scripts for creating the Eclipse RCP runtime for openLCA.

See also the README files that are contained in these sub-projects.

Building from source
--------------------
openLCA is an Eclipse RCP application with parts of the user interface written in HTML5 and JavaScript. To compile it from source you need to have the following tools installed:

* [Git](https://git-scm.com/) (optional)
* a [Java Development Kit 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven](http://maven.apache.org/)
* the [Eclipse package for RCP developers](https://www.eclipse.org/downloads/)
* [Node.js](https://nodejs.org/) and [Gulp](http://gulpjs.com/) (for building the HTML5 user interface components)

When you have these tools installed you can build the application from source via the following steps:

####Install the openLCA core modules
The core modules contain the application logic that is independent from the user interface and can be used in other applications. These modules are plain Maven projects and can be installed via `mvn install`. See the [olca-modules](https://github.com/GreenDelta/olca-modules) repository for a more information.

####Get the source code of the application
We recommend that to use Git to manage the source code but you can also download the source code as a [zip file](https://github.com/GreenDelta/olca-app/archive/master.zip). Create a development directory (the path should not contain whitespaces):

    mkdir olca
    cd olca

and get the source code:

    git clone https://github.com/GreenDelta/olca-app.git

Your development directory should now look like this:

    olca-app
      .git
      olca-app
      olca-app-build
      olca-app-html
      olca-app-runtime
      ...

####Building the HTML pages
To build the HTML pages of the user interface navigate to the `olca-app-html` folder:

    cd olca-app/olca-app-html

Then install the Node.js modules via [npm](https://www.npmjs.com/) (npm is a package manager that comes with your Node.js installation):

    npm install

This will create a folder `olca-app-html/node_modules` with the dependent modules. After this, you can create the html package via Gulp:

    gulp

This will build and package the HTML files to `olca-app/olca-app/html/base_html-.zip`.

####Prepare the Eclipse workspace
Download the current Eclipse package for RCP and RAP developers (to have everything together you can extract it into your development directory). Create a workspace directory in your development directory (e.g. under the eclipse folder to have a clean structure):

    eclipse
      ...
      workspace
    olca-app
      .git
      olca-app
      olca-app-build
      olca-app-html
      olca-app-runtime
      ...

After this, open Eclipse and select the created workspace directory. Import the projects into Eclipse via `Import/General/Existing Projects into Workspace`
(select the olca/olca-app directory). You should now see the olca-app, alca-app-build, and olca-app-runtime projects in your Eclipse workspace.

####Build the Eclipse runtime
Within Eclipse right-click on the `olca-app-runtime/build.xml` file and select `Run as.../Ant build`. This will download the RCP platform on which openLCA is built. When this script is finished (note that this could take a while) open the file `olca-app-runtime/platform.target` within Eclipse and click on 'Set as target platform' on the top right of the editor.

After this, go back to the command line and navigate to the `olca-app/olca-app` folder:

    cd olca-app/olca-app

and run 

    mvn package

This will copy the installed openLCA core modules and dependencies (see above) to the folder `olca-app/olca-app/libs`.

####Test the application
Refresh your Eclipse workspace (select all and press `F5`). Open the file [olca-app/openLCA.product](./olca-app/openLCA.product) within  Eclipse and click on the run icon. openLCA should now start.

If you want to build an installable product, see the description in the 
[olca-app-build](./olca-app-build) sub-project or simply use the Eclipse export
wizard (Export/Eclipse product).     


License
-------
Unless stated otherwise, all source code of the openLCA project is licensed under the [Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please see the LICENSE.txt file in the root directory of the source code.
