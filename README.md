olca-app
========
**Note that this is not a stable version yet.**

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
[Eclipse Standard](http://www.eclipse.org/downloads/), you may have to install
the Maven plugin for Eclipse).

Download the most recent version of the 
[openLCA RCP runtime](http://sourceforge.net/projects/openlca/files/openlca_framework/Platform/),
import it as project into the same workspace, open the `platform.target` file, and
run the command 'Set as target platform' from the menu.

After this, your development environment is ready. The project is build on
the Eclipse 3.x platform. For tutorials about Eclipse RCP development see for 
example http://www.vogella.com/eclipse.html. 

License
-------
Unless stated otherwise, all source code of the openLCA project is licensed under the 
[Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/). Please see the LICENSE.txt
file in the root directory of the source code.
