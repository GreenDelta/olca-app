# olca-app
This repository contains the source code of the [openLCA](http://openlca.org) 
application. openLCA is a Java application that runs on the Eclipse Rich Client
Platform ([Eclipse RCP](http://wiki.eclipse.org/index.php/Rich_Client_Platform)).
This project depends on the [olca-modules](https://github.com/GreenDelta/olca-modules)
and [olca-update](https://github.com/GreenDelta/olca-updates) projects which are
plain [Maven](http://maven.apache.org/) projects that contain the core 
functionalities (e.g. the model, database access, calculations, and
data exchange) and update logic for databases. 

This repository has the following sub-projects:

* [olca-app](./olca-app): contains the source code of the openLCA RCP 
  application.
* [olca-app-build](./olca-app-build): contains the build scripts for compiling
  openLCA and creating the installers for Windows, Linux, and MacOS.
* [olca-app-html](./olca-app-html): contains the source code for the HTML views
  in openLCA (like the start page or the report views).
* [olca-app-runtime](./olca-app-runtime): contains the build scripts for
  creating the Eclipse RCP runtime for openLCA.

See also the README files that are contained in these sub-projects.

## Building from source
openLCA is an Eclipse RCP application with parts of the user interface written
in HTML5 and JavaScript. To compile it from source you need to have the
following tools installed:

* [Git](https://git-scm.com/) (optional)
* a [Java Development Kit 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven](http://maven.apache.org/)
* the [Eclipse package for RCP developers](https://www.eclipse.org/downloads/eclipse-packages/)
* [Node.js](https://nodejs.org/) and [Gulp](http://gulpjs.com/) (for building
  the HTML5 user interface components)

When you have these tools installed you can build the application from source
via the following steps:

#### Install the openLCA core and update modules
The core and update modules contain the application logic that is independent
from the user interface and can be also used in other applications. These
modules are plain Maven projects and can be installed via `mvn install`. See the
[olca-modules](https://github.com/GreenDelta/olca-modules) and 
[olca-updates](https://github.com/GreenDelta/olca-updates) repositories for more
information.

#### Get the source code of the application
We recommend that to use Git to manage the source code but you can also download
the source code as a [zip file](https://github.com/GreenDelta/olca-app/archive/master.zip).
Create a development directory (the path should not contain whitespaces):

```bash
mkdir olca
cd olca
```

and get the source code:

```bash
git clone https://github.com/GreenDelta/olca-app.git
```

Your development directory should now look like this:

    olca-app
      .git
      olca-app
      olca-app-build
      olca-app-html
      olca-app-runtime
      ...

#### Building the HTML pages
To build the HTML pages of the user interface navigate to the
[olca-app-html](./olca-app-html) folder:

```bash
cd olca-app/olca-app-html
```

Then install the Node.js modules via [npm](https://www.npmjs.com/) (npm is a
package manager that comes with your Node.js installation):

```
npm install
```

This will create a folder `olca-app-html/node_modules` with the dependent
modules. After this, you can create the html package via Gulp:

```
gulp
```

This will build and package the HTML files to `olca-app/olca-app/html/base_html-.zip`.

#### Prepare the Eclipse workspace
Download the current Eclipse package for RCP and RAP developers (to have
everything together you can extract it into your development directory). Create
a workspace directory in your development directory (e.g. under the eclipse
folder to have a clean structure):

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

After this, open Eclipse and select the created workspace directory. Import the
projects into Eclipse via `Import > General > Existing Projects into Workspace`
(select the `olca/olca-app` directory). You should now see the `olca-app`, 
`olca-app-build`, and `olca-app-runtime` projects in your Eclipse workspace.

#### Loading the target platform and 
The file `platform.target` in the `olca-app` project contains the definition of
the [target platform](https://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fconcepts%2Ftarget.htm)
of the openLCA RCP application. Just open the file with the `Target Editor`
and click on `Set as target platform` on the top right of the editor.

This will download the resources of the target platform into your local
workspace and, thus, may take a while. Unfortunately, setting up and
configuring Eclipse can be quite challenging. If you get errors like
`Unable locate installable unit in target definition`,
[this discussion](https://stackoverflow.com/questions/10547007/unable-locate-installable-unit-in-target-definition)
may help. 

### Copy the Maven modules
Go back to the command line and navigate to the 
`olca-app/olca-app` folder:

```bash
cd olca-app/olca-app
```

and run 

```bash
mvn package
```

This will copy the installed openLCA core modules and dependencies (see above)
to the folder `olca-app/olca-app/libs`.

#### Add the JavaFX-SWT bridge
Since version 1.6, openLCA uses JavaFX components (e.g. the JavaFX WebView or
charting components). The Java runtime contains a JavaFX-SWT bridge with which
it is possible to embed JavaFX components in SWT applications. This bridge is
contained in the library `jfxswt.jar` which you need to add to the Java
runtime in Eclipse.

To do this, open the preferences for the installed Java runtimes 
`Window > Preferences > Java > Installed JREs`, select the enabled JRE and
click on `Edit`. In the upcoming dialog, click on `Add External JARs` and select
the `jre/lib/jfxswt.jar` library from the respective Java installation.

#### Test the application
Refresh your Eclipse workspace (select all and press `F5`). Open the file
[olca-app/openLCA.product](./olca-app/openLCA.product) within  Eclipse and click
on the run icon. openLCA should now start.

If you want to build an installable product, see the description in the 
[olca-app-build](./olca-app-build) sub-project or simply use the Eclipse export
wizard (Export/Eclipse product).     

#### Build the database templates
The openLCA application contains database templates that are used when the user
creates a new database (empty, with units, or with all reference data). There
is a Maven project `olca-refdata` that creates these database templates and
copies them to the `olca-app/olca-app/db_templates` folder from which openLCA
loads these templates. To build the templates, navigate to the refdata project
and run the build:

```bash
cd olca-app/olca-refdata
mvn package
```

## Building with Eclipse Oxygen and Julia libraries
For Windows, it is now possible to build openLCA so that it runs on the
Eclipse Oxygen platform and uses some math libraries for high performance
computing from the [Julia](https://julialang.org/) project. This is currently
tested with [Eclipse Oxygen 3A](https://www.eclipse.org/downloads/packages/release/Oxygen/3A)
package for RCP/RAP development.

The initial steps for this build are the same as before:

* install/update the openLCA core and update modules
* build the HTML pages
* build the database templates

The current master branch of the `olca-app` project contains now a new target
platform definition `olca-app/olca-app/platform.target`. When you open this
file the first time with the Eclipse `Target Editor`, Eclipse will
download the resources of this target platform into your workspace. You may
get errors like `Unable locate installable unit in target definition` when
Eclipse fails to download some resources or the like (for possible fixes, see
[here](https://stackoverflow.com/questions/10547007/unable-locate-installable-unit-in-target-definition)).
But when it is downloaded it should work quite stable. Click on the
`Set as Target Platform` link in the upper right corner of the `Target Editor`
to use this as the target platform.

To build the Windows version, we currently use the standard PDE Export wizard.
Click on the `olca-app` project and then on `Export...` from the context menu.
Select `Plug-in Development > Eclipse Product` from the export wizard and
select the following options in the export dialog:

* Configuration: `/olca-app/openLCA.product` (should be the default)
* Root directory: `openLCA`
* Synchronize before exporting: yes [x]
* Destination directory: choose the `olca-app-build/build` folder of this project
* Generate p2 repository: no [ ] (would be just overhead)
* Export for multiple platforms: yes [x]
* (take the defaults for the others)

In the next page, select the platforms for which you want to build the product.
However, only the Windows `x86_64` version is currently fully supported. After
the export, you need to run the package script `pack10.py` to copy resources
like the Java runtime, Julia libraries, etc. to the application folder and to
create the installers. The layout of the build folder (with tools and runtime
libraries) is the same as for the old build (see the `olca-app-build` project)
but additionally contains a `julia` folder which contains the julia libraries.

The distribution packages for Linux and macOS are still build as before. There
is a `olca_platform` branch which contains the build configuration for the old
target platform. Switch to this branch and merge the current master into this
branch and the old build should work.

## License
Unless stated otherwise, all source code of the openLCA project is licensed
under the [Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please
see the LICENSE.txt file in the root directory of the source code.
