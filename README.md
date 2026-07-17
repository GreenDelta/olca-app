# openLCA

This repository contains the source code of [openLCA](http://openlca.org).
openLCA is a Java application that runs on the Eclipse Rich Client Platform
(Eclipse RCP). The source code is structured in the following sub-projects:

* [olca-app](./olca-app): contains the source code of the openLCA RCP
  application.
* [olca-app-build](./olca-app-build): contains the build scripts creating the
  distribution packages for Windows, Linux, and macOS.
* [olca-app-html](./olca-app-html): contains the source code for the HTML views
  in openLCA (like the start page or the report views).
* [olca-refdata](./olca-refdata): contains the build scripts fro the reference
  databases (with units, flows, locatios, etc.) that are packaged with openLCA.

See also the README files of these sub-projects for more information.


## Running from source

In order to compile and run openLCA from source, you need to have the following
tools installed:

* a [Java Development Kit >= v25](https://adoptium.net)
* [Maven](http://maven.apache.org/)
* the [Eclipse package for RCP
  developers](https://www.eclipse.org/downloads/packages/)
* [Node.js](https://nodejs.org/)
* (Python and [uv](https://github.com/astral-sh/uv) in case you want to build
  the distribution packages )

When you have these tools installed, you can build and run the application with
the following steps:

#### Install the openLCA core modules

The core modules contain the application logic that is independent from the user
interface and can be also used in other applications. These modules are plain
Maven projects and can be installed via `mvn install`. See the
[olca-modules](https://github.com/GreenDelta/olca-modules) repository for more
information. Note that we also distribute the latest stable version of the
modules on the Maven Central repository. However, if you want to test the
current openLCA development branch, you probably need to install them first.
There is also a script for installing them when you put them next to the
`olca-app` folder:

```bash
./update_modules.sh
# or on Windows
./update_modules.bat
```

#### Building the HTML pages

To build the HTML pages of the user interface, navigate to the
[olca-app-html](./olca-app-html) and run the following commands:

```bash
cd olca-app/olca-app-html
# install the dependencies
npm install
# build and package the HTML pages for the app
npm run build
```

#### Prepare the Eclipse workspace

Download the current Eclipse package for RCP and RAP developers, start it and
create a workspace directory. Import the projects into Eclipse via `Import >
General > Existing Projects into Workspace` (select the `olca-app` directory).
You should now see the `olca-app`, `olca-app-build`, projects in your Eclipse
workspace.

#### Loading the target platform

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

#### Copy the Maven modules
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

#### Test the application
Refresh your Eclipse workspace (select all and press `F5`). Open the file
[olca-app/openLCA.product](./olca-app/openLCA.product) within  Eclipse and click
on the run icon inside the `openLCA.product` tab. openLCA should now start.

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

## License
Unless stated otherwise, all source code of the openLCA project is licensed
under the [Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please
see the LICENSE.txt file in the root directory of the source code.
