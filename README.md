olca-app
========
This is the openLCA RCP application which is build on top of the 
[olca-modules](https://github.com/GreenDelta/olca-modules) project.

Building
--------
To build the openLCA application package, first, you need to install the 
[olca-modules](https://github.com/GreenDelta/olca-modules) into your local
Maven repository. Then, after you cloned this repository, navigate to the
folder olca-app in this repository and run the Maven build script via

    mvn package
	
This will copy the required dependencies into the `lib` folder of this project.
After this, import the olca-app project into the workspace of an Eclipse 
installation (we recommend to use the 
[Eclipse Standard package](http://www.eclipse.org/downloads/), you may have to install
the Maven plugin for Eclipse).

Download the most recent version of the 
[openLCA RCP runtime](http://sourceforge.net/projects/openlca/files/openlca_framework/Platform/),
import it as project into the same workspace, open the `platform.target` file, and
run the command 'Set as target platform' from the menu.

After this, your development environment is ready. The project is build on
the Eclipse 3.x platform. For tutorials about Eclipse RCP development see for 
example http://www.vogella.com/eclipse.html. 

Embedding a XulRunner
---------------------
To display HTML views it is possible to embed a 
[XulRunner](https://developer.mozilla.org/en/docs/XULRunner) in openLCA. When 
openLCA tries to open an HTML view the first time in a session, it checks if 
the feature-flag USE_MOZILLA_BROWSER is enabled and if a XULRunner directory 
is located in the installation folder. To 'install' a XulRunner just copy the
`xulrunner` folder into your target platform project (next to the folders
`plugins` and `features`). You can get the XulRunner runtime from 
[here](http://ftp.mozilla.org/pub/mozilla.org/xulrunner/releases/10.0/runtimes/).
Eclipse 3.8 which we currently use as runtime platform supports XulRunner 10.0.

License
-------
Unless stated otherwise, all source code of the openLCA project is licensed under the 
[Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please see the LICENSE.txt
file in the root directory of the source code.
