# openLCA
This repository contains the source code of [openLCA](http://openlca.org).
openLCA is a Java application that runs on the Eclipse Rich Client Platform
([Eclipse RCP](http://wiki.eclipse.org/index.php/Rich_Client_Platform)). This
project depends on the [olca-modules](https://github.com/GreenDelta/olca-modules)
project which is a plain [Maven](http://maven.apache.org/) project that contains
the core functionalities of openLCA (e.g. the model, database access,
calculations, data exchange, and database updates). 

This repository has the following sub-projects:

* [olca-app](./olca-app): contains the source code of the openLCA RCP 
  application.
* [olca-app-build](./olca-app-build): contains the build scripts for compiling
  openLCA and creating the installers for Windows, Linux, and macOS.
* [olca-app-html](./olca-app-html): contains the source code for the HTML views
  in openLCA (like the start page or the report views).
* [olca-refdata](./olca-refdata): contains the current reference data (units,
  quantities, and flows) that are packaged with openLCA.

See also the README files that are contained in these sub-projects.

## Building from source
openLCA is an Eclipse RCP application with parts of the user interface written
in HTML5 and JavaScript. To compile it from source you need to have the
following tools installed:

* [Git](https://git-scm.com/) (optional)
* a [Java Development Kit >= v13](https://adoptopenjdk.net/)
* [Maven](http://maven.apache.org/)
* the [Eclipse package for RCP developers](https://www.eclipse.org/downloads/packages/)
* [Node.js](https://nodejs.org/) 

When you have these tools installed you can build the application from source
via the following steps:

#### Install the openLCA core modules
The core modules contain the application logic that is independent from the user
interface and can be also used in other applications. These modules are plain
Maven projects and can be installed via `mvn install`. See the
[olca-modules](https://github.com/GreenDelta/olca-modules) repository for more
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

```
olca-app
  .git
  olca-app
  olca-app-build
  olca-app-html
  olca-refdata
  ...
```

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

This also installs a local version of `webpack` which is used to create the
distribution package. The build of this package can be invoked via:

```bash
npm run build
```

The output is generated in the `dist` folder of this directory and packaged
into a zip file that is copied to the `../olca-app/html` folder.

#### Prepare the Eclipse workspace
Download the current Eclipse package for RCP and RAP developers (to have
everything together you can extract it into your development directory). Create
a workspace directory in your development directory (e.g. under the eclipse
folder to have a clean structure):

```
eclipse
  ...
  workspace
olca-app
  .git
  olca-app
  olca-app-build
  olca-app-html
  olca-app-refdata
  ...
```

After this, open Eclipse and select the created workspace directory. Import the
projects into Eclipse via `Import > General > Existing Projects into Workspace`
(select the `olca/olca-app` directory). You should now see the `olca-app`, 
`olca-app-build`, projects in your Eclipse workspace.

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
